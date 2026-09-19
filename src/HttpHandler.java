import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpHandler {
    private static HttpClient sharedClient = HttpClient.newHttpClient();
    
    public static HttpResponse<String> sendHttp(HttpRequest request) {
        try {
            return sharedClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
    }
    
    public static String curl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = sendHttp(request);
            if (response.statusCode() != 200) {
                throw new RuntimeException("Http error: " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
    }
}
