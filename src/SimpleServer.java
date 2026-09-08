import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class SimpleServer {

    public static void start() throws IOException {
        int port = 8080;  
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);  

        // Configure static file handler (path to your static directory)  
        // For Maven/Gradle: Use "src/main/resources/static"  
        // For plain Java: Use the absolute path (e.g., "/path/to/project/static")  
        String staticDir = "staging";  
        server.createContext("/", new StaticFileHandler(staticDir));  

        server.start();  
    }
}
