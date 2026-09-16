import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.restfb.json.Json;
import com.restfb.json.JsonObject;

public class GithubConnector {
    
    private static final String GITHUB_REPO = "kbrainard1/fingerlakesfinest";
    private static final String LOCAL_CHECKOUT_DIR = "repo";
    private static final String EDIT_BRANCH = "staging";
    private static final String TOKEN_KEY = "GH_TOKEN";
    private static final String KEY_EXPIRY = "GH_TOKEN_EXPIRATION";
    private static final String CLIENT_ID = "Iv23liOUVV6L6OCzqPwb"; // not secret
    
    private static GHRepository repo;
    private static String token;
    
    // Use the API until/unless everything is cached locally
    private static AtomicBoolean localCheckoutComplete = new AtomicBoolean(false);
    
    public static interface RedirectDisplay {
        public void displayActionNeeded(String url, String userCode);
    }
    
    public static void login(RedirectDisplay display) {
        CreateListingFrontend.threadPool.submit(() -> initialClone());
        String refreshToken = AuthHandler.AUTH_HANDLER.getToken(TOKEN_KEY, "");
        AtomicBoolean needToRefresh = new AtomicBoolean(false);
        if (refreshToken.isBlank()) {
            authorizeUser(display);
        } else {
            long expiry = Long.parseLong(AuthHandler.AUTH_HANDLER.getToken(KEY_EXPIRY, "0"));
            if (expiry < System.currentTimeMillis()) { // roughly every 6 months
                authorizeUser(display);
            } else {
                needToRefresh.set(true);
            }
        }
        
        // Common case, do in background
        CreateListingFrontend.threadPool.submit(() -> {
            try {
                if (needToRefresh.get()) {
                    refreshAccessToken(); 
                }
                GitHub github = new GitHubBuilder().withOAuthToken(token).build();
                repo = github.getRepository(GITHUB_REPO);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        
    }
    
    private static GHRepository getRepo() {        
        // hacky, but avoids locks for the 99% use case
        while (repo == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return repo;
    }
    
    private static interface GitRunner {
        void run(Git git) throws GitAPIException;
    }
    
    private static void runWithLocalGit(GitRunner runner) {
        getRepo(); // in case someone does something that needs credentials, make sure they're g2g
        initialClone();
        try (Git git = new Git(new FileRepositoryBuilder()
                    .setGitDir(new File("repo/.git"))
                    .build())) {
                runner.run(git);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void refreshAccessToken() {
        
        // start the process
        HttpRequest refreshRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json") 
                .POST(HttpRequest.BodyPublishers.ofString("client_id=" + CLIENT_ID
                        + "&grant_type=refresh_token"
                        + "&refresh_token=" + AuthHandler.AUTH_HANDLER.getToken(TOKEN_KEY, "")))
                .build();

        try {
            HttpResponse<String> response = HttpHandler.sendHttp(refreshRequest);

            JsonObject refreshJson = Json.parse(response.body()).asObject();
            token = refreshJson.getString("access_token", "");
            AuthHandler.AUTH_HANDLER.putToken(TOKEN_KEY, refreshJson.getString("refresh_token", ""));
            AuthHandler.AUTH_HANDLER.putToken(KEY_EXPIRY,  "" + (System.currentTimeMillis() + 1000 * refreshJson.getLong("refresh_token_expires_in", 0)));

            CreateListingFrontend.threadPool.submit(() -> {
                // typically 8 hours
                try {
                    Thread.sleep(1000 * refreshJson.getLong("expires_in", 0));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                refreshAccessToken();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void authorizeUser(RedirectDisplay display) {
        
        // start the process
        HttpRequest initialRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/device/code"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json") 
                .POST(HttpRequest.BodyPublishers.ofString("client_id=" + CLIENT_ID))
                .build();

        try {
            HttpResponse<String> response = HttpHandler.sendHttp(initialRequest);
            if (response.statusCode() != 200) {
                throw new RuntimeException("Issue connecting to github: " + response.statusCode());
            }
            JsonObject initialResponse = Json.parse(response.body()).asObject();
           
            display.displayActionNeeded(initialResponse.getString("verification_url", "https://github.com/login/device"), initialResponse.getString("user_code",  ""));
            String deviceCode = initialResponse.getString("device_code", "");
            
            // don't time out too soon - wait expiration + 2* polling
            long timeout = System.currentTimeMillis() + 1000 * initialResponse.getLong("expires_in", 15) + 2 * 1000 * initialResponse.getLong("interval", 5);

            boolean authorized = false;
            while (!authorized) {
                if (System.currentTimeMillis() > timeout) {
                    throw new RuntimeException("Authorization attempt timed out");
                }
                Thread.sleep(1000 * initialResponse.getLong("interval", 5));
                HttpRequest pollingRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://github.com/login/oauth/access_token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json") 
                        .POST(HttpRequest.BodyPublishers.ofString("client_id=" + CLIENT_ID + 
                                "&device_code=" + deviceCode +
                                "&grant_type=urn:ietf:params:oauth:grant-type:device_code" +
                                "&repository=kbrainard1/fingerlakesfinest"))
                        .build();
                HttpResponse<String> pollingResponse = HttpHandler.sendHttp(pollingRequest);
                JsonObject pollingResponseJson = Json.parse(pollingResponse.body()).asObject();
                if (pollingResponse.statusCode() == 200 && pollingResponseJson.getString("error", "").isBlank()) {
                    AuthHandler.AUTH_HANDLER.putToken(TOKEN_KEY, pollingResponseJson.getString("refresh_token", ""));
                    AuthHandler.AUTH_HANDLER.putToken(KEY_EXPIRY,  "" + (System.currentTimeMillis() + 1000 * pollingResponseJson.getLong("refresh_token_expires_in", 0)));
                    token = pollingResponseJson.getString("access_token", "");
                    CreateListingFrontend.threadPool.submit(() -> {
                        // typically 8 hours
                        try {
                            Thread.sleep(1000 * pollingResponseJson.getLong("expires_in", 0));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        refreshAccessToken();
                    });
                    authorized = true;
                }
            }
                
               

        } catch (Exception e) {
            throw new RuntimeException(e);
        }    
    }

    
    /** VERY SLOW if you actually need to clone (expect 1-2 minutes) */
    public static synchronized void initialClone() {
        if (!new File(LOCAL_CHECKOUT_DIR + "/.git").exists()) {
            new File(LOCAL_CHECKOUT_DIR).mkdir();
            try {
                Git.cloneRepository()
                .setURI("https://www.github.com/" + GITHUB_REPO)
                .setDirectory(new File(LOCAL_CHECKOUT_DIR))
                .call();
               
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try (Git git = new Git(new FileRepositoryBuilder()
                .setGitDir(new File(LOCAL_CHECKOUT_DIR + "/.git"))
                .build())) {
            // Seriously, git?
            if (!git.getRepository().getBranch().equals(EDIT_BRANCH)) {
                try {
                    git.checkout().setName(EDIT_BRANCH);
                } catch (Exception e) {
                    git.checkout().setName(EDIT_BRANCH).setUpstreamMode(SetupUpstreamMode.TRACK).setCreateBranch(true).call();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        localCheckoutComplete.set(true);
    }
    
    public static GHContent getRetriably(String file) throws IOException {
        for (int i = 0; i < 3; i++) {
            try {
                return getRepo().getFileContent(file, EDIT_BRANCH);
            } catch (IOException e) {
               // ignore and retry
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }
        return getRepo().getFileContent(file, EDIT_BRANCH);
    }
    
    public static List<String> readFile(GHContent fileContent) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader buff = new BufferedReader(new InputStreamReader(fileContent.read()))) {
            String line;
            while ((line = buff.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
     }

    public static void commitChange(String repoFile, String locationOfContents) throws IOException {
        GHContent content = getRetriably(repoFile);
        getRepo().createContent()
        .content(Files.readAllBytes(Path.of(locationOfContents)))
        .message("Content created")
        .branch(EDIT_BRANCH)
        .sha(content.getSha())
        .path(repoFile)
        .commit();
    }
    
    public static boolean commitNew(String repoFile, String locationOfContents) throws IOException {
        try {
            getRepo().createContent()
            .content(Files.readAllBytes(Path.of(locationOfContents)))
            .message("Content created")
            .branch(EDIT_BRANCH)
            .path(repoFile)
            .commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void cloneStaging() {
        // public repo, no auth
        runWithLocalGit(git -> {
            git.fetch().call();
            git.pull().call();
        });
    }

    public static void mergeStaging() {
        try {
            GHRepository repo = getRepo();
            repo.getBranch("main").merge(repo.getBranch(EDIT_BRANCH), "deploy site");
            repo.getBranch(EDIT_BRANCH).merge(repo.getBranch("main"), "avoid conflicts");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> getImageDirectory(String directory) throws IOException {
        if (localCheckoutComplete.get()) {
            cloneStaging(); // pulls locally
            return Arrays.asList(new File(LOCAL_CHECKOUT_DIR + "/" + directory).listFiles());
        }
        for (int i = 0; i < 3; i++) {
            try {
                return cacheLocally(directory, getRepo().getDirectoryContent(directory, EDIT_BRANCH));
            } catch (IOException e) {
               // ignore and retry
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }
        return cacheLocally(directory, getRepo().getDirectoryContent(directory, EDIT_BRANCH));
    }

    private static List<File> cacheLocally(String dirName, List<GHContent> contents) throws IOException {
        new File(dirName).mkdirs();
        List<File> results = new ArrayList<>();
        for (GHContent content : contents) {
            Files.copy(content.read(), Path.of(dirName + "/" + content.getName()), StandardCopyOption.REPLACE_EXISTING);
            results.add(new File(dirName, content.getName()));
        }
        return results;
    }

    public static String getString(GHContent file) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader buff = new BufferedReader(new InputStreamReader(file.read()))) {
            String line;
            while ((line = buff.readLine()) != null) {
                result.append(line);
                result.append(System.lineSeparator());
            }
        }
        return result.toString();
    }

}
