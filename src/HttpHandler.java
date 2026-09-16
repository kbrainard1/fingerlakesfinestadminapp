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
}
