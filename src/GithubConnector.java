import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.restfb.json.Json;
import com.restfb.json.JsonObject;

public class GithubConnector {
    
    public static enum CommitType {
        ADD_HORSE("Add horse"),
        EDIT_HORSE("Edit horse"),
        MARK_PLACED("Mark horse as placed");
        
        private final String message;

        private CommitType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
    
    private static final String GITHUB_REPO = "kbrainard1/fingerlakesfinest";
    private static final String LOCAL_CHECKOUT_DIR = "repo";
    private static final String EDIT_BRANCH = "test";
    private static final String MAIN_BRANCH = "testmain";
    private static final String TOKEN_KEY = "GH_TOKEN";
    private static final String KEY_EXPIRY = "GH_TOKEN_EXPIRATION";
    private static final String CLIENT_ID = "Iv23liOUVV6L6OCzqPwb"; // not secret
    
    private static GHRepository repo;
    private static String gitUsername;
    private static Git git;
    private static String token;
    private static Future<?> currentPush;
    
    private static AtomicBoolean localCheckoutComplete = new AtomicBoolean(false);
    private static AtomicBoolean initialUpdateComplete = new AtomicBoolean(false);
    
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
                gitUsername = github.getMyself().getLogin();
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
        if (!localCheckoutComplete.get()) {
            initialClone();
        }
        try {
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
  
    public static synchronized void initialClone() {
        if (localCheckoutComplete.get()) {
            return;
        }
        File localRepoFile = new File(LOCAL_CHECKOUT_DIR + "/.git");
        File localRepoSuccess = new File(LOCAL_CHECKOUT_DIR + "/.success");
        if (localRepoFile.exists() && !localRepoSuccess.exists()) {
            deleteRecursive(localRepoFile); // incomplete checkout, start fresh
        }
        
        if (!localRepoFile.exists()) {
            new File(LOCAL_CHECKOUT_DIR).mkdir();
            try {
                Git.cloneRepository()
                .setURI("https://www.github.com/" + GITHUB_REPO)
                .setDirectory(new File(LOCAL_CHECKOUT_DIR))
                .call();
                Files.writeString(localRepoSuccess.toPath(), "Success", StandardOpenOption.CREATE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            git = new Git(new FileRepositoryBuilder()
                    .setGitDir(localRepoFile)
                    .build());
            if (!git.getRepository().getBranch().equals(EDIT_BRANCH)) {
                try {
                    git.checkout().setName(EDIT_BRANCH).call();
                } catch (Exception e) {
                    git.checkout().setName(EDIT_BRANCH).setUpstreamMode(SetupUpstreamMode.TRACK).setCreateBranch(true).call();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        CreateListingFrontend.threadPool.submit(() -> {
            while (true) {
                updateFromRemote();
                initialUpdateComplete.set(true);
                Thread.sleep(60 * 1000);
            }
        });
        localCheckoutComplete.set(true);
    }
    
    // for obvious reasons, this should be treated as a best-effort indicator
    // Currently it's used for deciding whether or not to put up a UI spinner while the initial clone
    // finishes
    public static boolean isLocalCheckoutDone() {
        return localCheckoutComplete.get();
    }
    
    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                deleteRecursive(child);
            }
        }
        f.delete();
    }

    // Caution! All edits to the file should go through editFile, otherwise they
    // won't get staged
    public static File getFile(String file) throws IOException {
        if (!initialUpdateComplete.get()) {
            updateFromRemote();
            initialUpdateComplete.set(true);
        }
        return new File(LOCAL_CHECKOUT_DIR + "/" + file);
    }
    
    public static Document getHtmlFile(String file) throws IOException {
        return Jsoup.parse(Files.readString(getFile(file).toPath()));
    }
    
    public static void editFile(String repoFile, String localFile) throws IOException {
        if (!initialUpdateComplete.get()) {
            updateFromRemote();
            initialUpdateComplete.set(true);
        }
        com.google.common.io.Files.copy(new File(localFile), getFile(repoFile));
        try {
            synchronized (git) {
                git.add().addFilepattern(repoFile).call();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
  
    public static void commitAndPush(CommitType type) {
        runWithLocalGit(git -> {
            // git add --all is for some reason insanely slow, 
            // so rely on edit to have staged everything
            git.commit().setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", token))
            .setMessage(type.getMessage())
            .setCommitter(gitUsername, "dontneedthis@nope.com")
            .call();
            
            synchronized (git) {
                doPushUnsafe(git);
            }
        });
    }

    private static void doPushUnsafe(Git git) {
        if (currentPush != null) {
            try {
                currentPush.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        currentPush = CreateListingFrontend.threadPool.submit(() -> git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", token)).call());
    }

    public static void updateFromRemote() {
        // No auth, and called during initial setup, so don't use
        // the runner framework
        try {
            git.fetch().call();
            git.pull().call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public static void mergeStaging() {
        try {
            synchronized (git) {
                if (currentPush != null) {
                    try {
                        currentPush.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    currentPush = null;
                }
                GHRepository repo = getRepo();
                repo.getBranch(MAIN_BRANCH).merge(repo.getBranch(EDIT_BRANCH), "deploy site");
                repo.getBranch(EDIT_BRANCH).merge(repo.getBranch(MAIN_BRANCH), "avoid conflicts");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> getImageDirectory(String directory) throws IOException {
        if (!initialUpdateComplete.get()) {
            updateFromRemote();
            initialUpdateComplete.set(true);
        }
        return Arrays.asList(new File(LOCAL_CHECKOUT_DIR + "/" + directory).listFiles());
    }

    public static void waitForPush() {
        if (currentPush != null) {
            try {
                currentPush.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentPush = null;
        }
    }
    
    public static class CommitTypeFilter extends RevFilter {
        
        private CommitType type;
        
        public CommitTypeFilter(CommitType type) {
            this.type = type;
        }

        @Override
        public boolean include(RevWalk walker, RevCommit commit) {
            String message = commit.getFullMessage().toLowerCase();
            return message.contains(type.getMessage().toLowerCase()) &&
                    !message.contains("revert");
        }
        
        @Override
        public RevFilter clone() {
           return new CommitTypeFilter(type);
        }
    }

    public static void revertLast(GithubConnector.CommitType type) {
        synchronized (git) {
            git.revert();
            try {
               for (RevCommit toRevert : git.log().all().setMaxCount(10).setRevFilter(new CommitTypeFilter(type)).call()) {
                   git.revert().include(toRevert).call();
                   break; // revert at most 1
               }
               doPushUnsafe(git);
            } catch (IOException | GitAPIException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
