import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class SimpleServer {
    
    private static String authCode = null;
    
    public static String getAuthCode() {
        return authCode;
    }

    public static void start() throws IOException {
        int port = 8080;  
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);  

        String staticDir = "repo";  
        server.createContext("/", new StaticFileHandler(staticDir));  
        
        // Facebook auth
        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code = null;

            if (query != null && query.contains("code=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "code".equals(pair[0])) {
                        code = pair[1];
                        break;
                    }
                }
            }

            // Display a quick confirmation page in the user's browser
            if (code != null) {
                authCode = code;
                String responseText = "<html><body><h2 style=\"text-align:center; color:rgb(0,128,0); margin-top:50px;\">Authentication complete! You may close this tab.</h2></body></html>";
                exchange.sendResponseHeaders(200, responseText.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseText.getBytes());
                }
            } else {
                // No code found in redirect URL
                String responseText = "<html><body><h2>Authentication Failed, please try again</h2></body></html>";
                exchange.sendResponseHeaders(200, responseText.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseText.getBytes());
                }
            }
        });

        server.start();  
    }
}
