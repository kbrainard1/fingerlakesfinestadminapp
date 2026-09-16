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

public class FbConnector {

    private static final String PAGE_ID = "1351602908027780"; // Dev page, need to find id for FLF
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v19.0";
    private static final String USER_ACCESS_TOKEN = "";
    
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = HttpHandler.sendHttp(request);
        JsonObject jsonResponse = Json.parse(response.body()).asObject();

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
        String pageAccessToken = getPageAccessToken(USER_ACCESS_TOKEN, PAGE_ID);
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
        String pageAccessToken = getPageAccessToken(USER_ACCESS_TOKEN, PAGE_ID);
        String url = GRAPH_API_BASE + "/" + PAGE_ID + "/feed?fields=id,message&limit=30&access_token=" + encode(pageAccessToken);
        horseName = horseName.toLowerCase();
        
        // only look through the previous 30 posts - can add paging if needed 
        // (see commented out code), but this should go back almost a year at current post rates
        //while (!url.isBlank()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpHandler.sendHttp(request);

            if (response.statusCode() != 200) {
                throw new RuntimeException("Unexpected error finding post " + response.statusCode());
            }

            JsonObject jsonResponse = Json.parse(response.body()).asObject();
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
        String pageAccessToken = getPageAccessToken(USER_ACCESS_TOKEN, PAGE_ID);
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
}