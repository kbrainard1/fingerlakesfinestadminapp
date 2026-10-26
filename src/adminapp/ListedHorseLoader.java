package adminapp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ListedHorseLoader {

    public static record ListedHorse(String page, String title, String thumbnail, boolean active,
            String snippet) {}
    
    public static List<ListedHorse> loadHorses() {
        try {
            List<ListedHorse> result = new ArrayList<>();

            
            Document available = GithubConnector.getHtmlFile("available.html");
            loadHorses(result, available, true);
            Document inactive = GithubConnector.getHtmlFile("inactive.html");
            loadHorses(result, inactive, false);
            return result;
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }

    private static void loadHorses(List<ListedHorse> result, Document available, boolean active) {
        for (Element availHorse : available.getElementsByClass("available_snippet_div")) {
            Element link = availHorse.getElementsByTag("a").get(0);
            String title = link.ownText();
            String page = link.attr("href");
            String thumb = availHorse.getElementsByClass("snippet_thumb").get(0).attr("src");
            result.add(new ListedHorse(page, title, thumb, active, availHorse.getElementsByTag("p").get(0).ownText()));
        }
    }
}
