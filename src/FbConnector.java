import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.restfb.json.Json;
import com.restfb.json.JsonArray;
import com.restfb.json.JsonObject;
import com.restfb.json.JsonValue;

// Open question: should this migrate to FacebookClient?
// At the moment, seems unnecessary, and the main thing it does is stick the access token in a header
// (see DefaultWebRequestor.initHeaderAccessToken). Lord knows I don't want to interact with those APIs
// any more than I have to, so feature expansion in this area feels unlikely
public class FbConnector {

    private static final String PAGE_ID = "1351602908027780"; // Dev page, need to find id for FLF
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v19.0";
    private static final String TOKEN_KEY = "FB_TOKEN";
    private static final String APP_SECRET_KEY = "FB_APP_SECRET";
    private static final String APP_ID = "1850725296366206"; // not secret
    private static final String APP_SECRET = AuthHandler.AUTH_HANDLER.getToken(APP_SECRET_KEY, "");
    private static String token = "";
    
  //  for testing
    public static void main(String[] args) {
        try {
            List<String> images = Arrays.asList("horsePages/ringoftherise_files/h1.jpg", "horsePages/ringoftherise_files/h2.jpg",
                    "horsePages/ringoftherise_files/h3.jpg");
            createPagePost("Hello world 2 photos", images);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String  getPageAccessToken(String userAccessToken, String pageId) throws IOException, InterruptedException {
        String url = GRAPH_API_BASE + "/me/accounts?fields=id,name,access_token&access_token=" + encode(userAccessToken);

        // Fetches page access tokens for all pages for postfiltering
        JsonObject jsonResponse = Json.parse(HttpHandler.curl(url)).asObject();

        if (jsonResponse.contains("error")) {
            throw new RuntimeException("Error fetching pages: " + jsonResponse.get("error").asObject().getString("message", ""));
        }

        List<JsonValue> pages = jsonResponse.get("data").asArray().values();
        for (int i = 0; i < pages.size(); i++) {
            JsonObject page = pages.get(i).asObject();
            if (page.getString("id", "").equals(PAGE_ID)) {
                return page.getString("access_token", "");
            }
        }
        throw new RuntimeException("You don't have access to that page"); 
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static void createPagePost(String message, List<String> imagePaths)
            throws IOException, InterruptedException {
        String pageAccessToken = getPageAccessToken(token, PAGE_ID);
        List<String> photoIds = new ArrayList<>();

        // Upload each image as an unpublished media object
        for (String imagePath : imagePaths) {
            String photoId = uploadUnpublishedPhoto(imagePath, pageAccessToken);
            photoIds.add(photoId);
        }

        // Create the feed post linking all photo IDs via `attached_media`
        publishPostWithAttachedMedia(message, photoIds, pageAccessToken);
    }

    private static String uploadUnpublishedPhoto(String imagePath, String pageAccessToken)
            throws IOException, InterruptedException {

        String url = GRAPH_API_BASE + "/" + PAGE_ID + "/photos";
        
        // generate a unique token for the boundary string
        String boundary = "----JavaHttpClientBoundary" + System.currentTimeMillis();

        Path path = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        String mimeType = Files.probeContentType(path);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }

        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
        PrintWriter bodyWriter = new PrintWriter(bodyStream);

        // Mark photo as unpublished
        addFormField(bodyWriter, boundary, "published", "false");
        
        // Mark as temporary to cleanly expire unused photos
        addFormField(bodyWriter, boundary, "temporary", "true");

        // Add Page Access Token
        addFormField(bodyWriter, boundary, "access_token", pageAccessToken);

        // Add Image Data
        bodyWriter.println(boundary);
        bodyWriter.println("Content-Disposition: form-data; name=\"source\"; filename=\"" + imagePath +"\"");
        bodyWriter.println("Content-Type: " + mimeType + "\r\n");
        bodyWriter.flush(); // flush so we can write bytes
        bodyStream.write(imageBytes);
        bodyWriter.println();

        // Close Boundary
        bodyWriter.println(boundary);
        bodyWriter.flush();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2)) // remove the leading -- for the header boundary def
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyStream.toByteArray()))
                .build();

        HttpResponse<String> response = HttpHandler.sendHttp(request);
        JsonObject jsonResponse = Json.parse(response.body()).asObject();

        if (jsonResponse.contains("error")) {
            throw new RuntimeException("Failed to upload photo: " + jsonResponse.get("error").asObject().getString("message", ""));
        }
        return jsonResponse.getString("id", "");
    }

    private static void publishPostWithAttachedMedia( String message, List<String> photoIds, String pageAccessToken)
            throws IOException, InterruptedException {

        String url = GRAPH_API_BASE + "/" + PAGE_ID + "/feed";

        // Build the attached_media JSON payload: [{"media_fbid":"ID1"}, {"media_fbid":"ID2"}]
        JsonArray attachedMedia = new JsonArray();
        for (String photoId : photoIds) {
            JsonObject item = new JsonObject();
            item.add("media_fbid", photoId);
            attachedMedia.add(item);
        }

        // Form parameters
        String formBody = String.format("message=%s&attached_media=%s&access_token=%s",
                encode(message),
                encode(attachedMedia.toString()),
                encode(pageAccessToken));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = HttpHandler.sendHttp(request);
        JsonObject jsonResponse = Json.parse(response.body()).asObject();

        if (jsonResponse.contains("error")) {
            throw new RuntimeException("Failed to publish post: " + jsonResponse.get("error").asObject().getString("message", ""));
        }
    }

    private static void addFormField(PrintWriter stream, String boundary, String name, String value) throws IOException {
        stream.println(boundary);
        stream.println("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
        stream.println(value);
    }
    
    public static record FbPost(String id, String contents) {}

    public static FbPost findPost(String horseName) throws IOException, InterruptedException {
        String pageAccessToken = getPageAccessToken(token, PAGE_ID);
        String url = GRAPH_API_BASE + "/" + PAGE_ID + "/feed?fields=id,message&limit=30&access_token=" + encode(pageAccessToken);
        horseName = horseName.toLowerCase();
        
        // only look through the previous 30 posts - can add paging if needed 
        // (see commented out code), but this should go back almost a year at current post rates
        //while (!url.isBlank()) {

            JsonObject jsonResponse = Json.parse(HttpHandler.curl(url)).asObject();
            JsonArray data = jsonResponse.get("data").asArray();

            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JsonObject post = data.get(i).asObject();
                    String message = post.getString("message", "");

                    if (message.toLowerCase().startsWith(horseName)) {
                        return new FbPost(post.getString("id", ""), message);
                    }
                }
            }

//            // The next link returned by Facebook will automatically retain the limit parameter
//            JsonObject paging = jsonResponse.get("paging").asObject();
//            url = paging != null ? paging.getString("next", "") : "";
//        }

        return null;
    }

    public static void updatePostText(String text, String postId) throws IOException, InterruptedException {
        String pageAccessToken = getPageAccessToken(token, PAGE_ID);
        String formBody = "message=" + encode(text) + "&access_token=" + encode(pageAccessToken);
        String url = GRAPH_API_BASE + "/" + postId;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = HttpHandler.sendHttp(request);
        JsonObject jsonResponse = Json.parse(response.body()).asObject();

        if (jsonResponse.contains("error")) {
            throw new RuntimeException("Failed to update post: " + jsonResponse.get("error").asObject().getString("message", ""));
        }
        
    }

    // TODO: figure out refresh/expiration flow
    public static void doLogin() throws Exception {
        token = AuthHandler.AUTH_HANDLER.getToken(TOKEN_KEY, "");
        if (token.isBlank()) {
            String redirectUri = "http://localhost:8080/callback";
            String loginUrl = "https://www.facebook.com/v19.0/dialog/oauth" +
                    "?client_id=" + APP_ID +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode("pages_show_list,pages_read_engagement,pages_manage_posts", StandardCharsets.UTF_8) +
                    "&response_type=code";
            Desktop.getDesktop().browse(new URI(loginUrl));

            // wait for the code
            while (SimpleServer.getAuthCode() == null) {
                Thread.sleep(300);
            }
            String authCode = SimpleServer.getAuthCode();

            // swap the code for a short-lived token
            String shortLivedToken = getShortLivedToken(authCode);

            // swap the short-lived token for a long-lived token (~2 months)
            String longLivedToken = getLongLivedToken(shortLivedToken);
            AuthHandler.AUTH_HANDLER.putToken(TOKEN_KEY, longLivedToken);
            token = longLivedToken;
        }
    }
    
    public static String getShortLivedToken(String code) throws IOException, InterruptedException {
        String url = "https://graph.facebook.com/v19.0/oauth/access_token" +
                "?client_id=" + APP_ID +
                "&redirect_uri=" + URLEncoder.encode("http://localhost:8080/callback", StandardCharsets.UTF_8) +
                "&client_secret=" + APP_SECRET +
                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

        JsonObject jsonResponse = Json.parse(HttpHandler.curl(url)).asObject();

        String shortTermToken = jsonResponse.getString("access_token", "");
        if (shortTermToken.isBlank()) {
            throw new RuntimeException("Failed to obtain short-lived token: " + jsonResponse);
        }
        return shortTermToken;
    }

    public static String getLongLivedToken(String shortLivedToken) throws IOException, InterruptedException {
        String url = "https://graph.facebook.com/v19.0/oauth/access_token" +
                "?grant_type=fb_exchange_token" +
                "&client_id=" + APP_ID +
                "&client_secret=" + APP_SECRET +
                "&fb_exchange_token=" + URLEncoder.encode(shortLivedToken, StandardCharsets.UTF_8);

        JsonObject jsonResponse = Json.parse(HttpHandler.curl(url)).asObject();

        String longLivedToken = jsonResponse.getString("access_token", "");
        if (longLivedToken.isBlank()) {
            throw new RuntimeException("Failed to obtain long-lived token: " + jsonResponse);
        }
        return longLivedToken;
    }
}