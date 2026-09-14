import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.restfb.json.Json;
import com.restfb.json.JsonObject;

public class GithubConnector {
    
    private static final String GITHUB_REPO = "kbrainard1/fingerlakesfinest";
    private static final String EDIT_BRANCH = "staging";
    private static final String TOKEN_KEY = "GH_TOKEN";
    private static final String KEY_EXPIRY = "GH_TOKEN_EXPIRATION";
    private static final String CLIENT_ID = "Iv23liOUVV6L6OCzqPwb"; // not secret
    
    private static GHRepository repo;
    private static String token;
    
    public static interface RedirectDisplay {
        public void displayActionNeeded(String url, String userCode);
    }
    
    public static void login(RedirectDisplay display) {
        String refreshToken = AuthHandler.AUTH_HANDLER.getToken(TOKEN_KEY, "");
        if (refreshToken.isBlank()) {
            authorizeUser(display);
        } else {
            long expiry = Long.parseLong(AuthHandler.AUTH_HANDLER.getToken(KEY_EXPIRY, "0"));
            if (expiry < System.currentTimeMillis()) { // roughly every 6 months
                authorizeUser(display);
            } else {
                refreshAccessToken(display);
            }
        }
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(token).build();
            repo = github.getRepository(GITHUB_REPO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void refreshAccessToken(GithubConnector.RedirectDisplay display) {
//        token = AuthHandler.AUTH_HANDLER.getToken("GH_PAT_TOKEN", "");
//        if (true) {
//            return;
//        }
        HttpClient client = HttpClient.newHttpClient();
        
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
            HttpResponse<String> response = client.send(refreshRequest, HttpResponse.BodyHandlers.ofString());

            JsonObject refreshJson = Json.parse(response.body()).asObject();
            token = refreshJson.getString("access_token", "");
            if (token.isBlank()) {
                authorizeUser(display);
            }
            AuthHandler.AUTH_HANDLER.putToken(TOKEN_KEY, refreshJson.getString("refresh_token", ""));
            AuthHandler.AUTH_HANDLER.putToken(KEY_EXPIRY,  "" + (System.currentTimeMillis() + 1000 * refreshJson.getLong("refresh_token_expires_in", 0)));

            CreateListingFrontend.threadPool.submit(() -> {
                // typically 8 hours
                try {
                    Thread.sleep(1000 * refreshJson.getLong("expires_in", 0));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                refreshAccessToken((url, code) -> {});
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void authorizeUser(RedirectDisplay display) {
//        token = AuthHandler.AUTH_HANDLER.getToken("GH_PAT_TOKEN", "");
//        if (true) {
//            return;
//        }
        HttpClient client = HttpClient.newHttpClient();
        
        // start the process
        HttpRequest initialRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/device/code"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json") 
                .POST(HttpRequest.BodyPublishers.ofString("client_id=" + CLIENT_ID))
                .build();

        try {
            HttpResponse<String> response = client.send(initialRequest, HttpResponse.BodyHandlers.ofString());
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
                HttpResponse<String> pollingResponse = client.send(pollingRequest, HttpResponse.BodyHandlers.ofString());
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
                        refreshAccessToken((url, code) -> {});
                    });
                    authorized = true;
                }
            }
                
               

        } catch (Exception e) {
            throw new RuntimeException(e);
        }    
    }
    
    public static GHContent getRetriably(String file) throws IOException {
        for (int i = 0; i < 3; i++) {
            try {
                return repo.getFileContent(file, EDIT_BRANCH);
            } catch (IOException e) {
               // ignore and retry
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }
        return repo.getFileContent(file, EDIT_BRANCH);
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
        repo.createContent()
        .content(Files.readAllBytes(Path.of(locationOfContents)))
        .message("Content created")
        .branch(EDIT_BRANCH)
        .sha(content.getSha())
        .path(repoFile)
        .commit();
    }
    
    public static boolean commitNew(String repoFile, String locationOfContents) throws IOException {
        try {
            repo.createContent()
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
        try {
            repo.readZip(is -> {
                Files.copy(is, Path.of("staging.zip"), StandardCopyOption.REPLACE_EXISTING);
                return null;
            }, EDIT_BRANCH);


            if (new File("staging").exists()) {
                deleteDirectory(new File("staging"));
            }

           
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream("staging.zip"))) {
                // outer folder is the repo name, skip that
                zis.getNextEntry();
                ZipEntry entry;
                byte[] buffer = new byte[1024];
                while ((entry = zis.getNextEntry()) != null) {
                    File newFile = new File("staging" + File.separator + removeFirstFolder(entry.getName()));
                    if (entry.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        new File(newFile.getParent()).mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int length;
                            while ((length = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String removeFirstFolder(String name) {
       return name.substring(name.indexOf("/") + 1);
    }

    private static void deleteDirectory(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }

    public static void mergeStaging() {
        try {
            repo.getBranch("main").merge(repo.getBranch(EDIT_BRANCH), "deploy site");
            repo.getBranch(EDIT_BRANCH).merge(repo.getBranch("main"), "avoid conflicts");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> getImageDirectory(String directory) throws IOException {
        for (int i = 0; i < 3; i++) {
            try {
                return cacheLocally(directory, repo.getDirectoryContent(directory, EDIT_BRANCH));
            } catch (IOException e) {
               // ignore and retry
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }
        return cacheLocally(directory, repo.getDirectoryContent(directory, EDIT_BRANCH));
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
