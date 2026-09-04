import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MarkPlaced {

    private static interface DocumentUpdater {
        public void update(Document page);
    }

    private static void updatePage(String fileName, DocumentUpdater update) throws IOException {
        Document page = Jsoup.parse(Files.readString(Path.of(fileName)));
        update.update(page);
        page.outputSettings(page.outputSettings().prettyPrint(false));
        try (BufferedWriter out = new BufferedWriter(new FileWriter("temp.html"))) {
            out.write(page.outerHtml());
            out.newLine();
        }
        Files.copy(Path.of("temp.html"), Path.of(fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void markPlaced(String pathToCheckout, String hrefForHorsePage, String optionalDetails) throws IOException {
        // remove from available.html
        StringBuilder snippetToMove = new StringBuilder();
        updatePage(pathToCheckout + "/available.html", page -> {
            Elements snippets = page.select(".available_snippet_div");
            for (Element snippet : snippets) {
                String horsePage = snippet.select(".available_title").getFirst().attr("href");
                if (horsePage.equalsIgnoreCase(hrefForHorsePage)) {
                    snippetToMove.append(snippet.outerHtml());
                    snippet.remove();
                    break;
                }
            }
        });

        // remove from both places in index.html
        updatePage(pathToCheckout + "/index.html", page -> {
            Element mobileList = page.getElementById("full_available_list");
            for (Element snippet : mobileList.children()) {
                String horsePage = snippet.attr("href");
                if (hrefForHorsePage.equalsIgnoreCase(horsePage)) {
                    snippet.remove();
                    break;
                }
            }

            Element recentAdds = page.select(".recent_adds").getFirst().getElementsByTag("ul").getFirst();
            for (Element li : recentAdds.children()) {
                String horsePage = li.getElementsByTag("a").attr("href");
                if (hrefForHorsePage.equalsIgnoreCase(horsePage)) {
                    li.remove();
                    break;
                }
            }
        });

        // add to placed.html
        updatePage(pathToCheckout + "/placed.html", page -> {
            Element main = page.getElementsByTag("main").getFirst();
            main.children().get(1).after(snippetToMove.toString()); // h1 & year nav
        });

        // add optional notes or "PLACED" to top of horse page
        String notes = optionalDetails.isBlank() ? "PLACED" : optionalDetails;
        updatePage(pathToCheckout + "/" + hrefForHorsePage, page -> {
            Element title = page.getElementsByTag("h1").getFirst();
            title.after("<p>" + notes + "</p>");
        });
    }
}
