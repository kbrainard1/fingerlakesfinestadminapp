import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AvailableHorsesLoader {

    public static record AvailableHorse(String page, String title, String thumbnail) {}
    
    public static List<AvailableHorse> loadHorses() {
        try {
           List<AvailableHorse> result = new ArrayList<>();
           
            List<String> lines = Files.readAllLines( GithubConnector.getFile("available.html").toPath());
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("available_title")) {
                    String listing = lines.get(i);
                    while (!lines.get(i).contains("<p>")) {
                        i++;
                        listing += lines.get(i);
                    }
                    int startPage = listing.indexOf("href") + 6;
                    String page = listing.substring(startPage, listing.indexOf("\"", startPage));
                    String title = listing.substring(listing.indexOf(">", startPage) + 1, listing.indexOf("<", startPage));
                    int startThumb = listing.indexOf("src") + 5;
                    String thumb = listing.substring(startThumb, listing.indexOf("\"", startThumb));
                    result.add(new AvailableHorse(page, title, thumb));
                }
            }
            return result;
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }
}
