import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kohsuke.github.GHContent;

public class MarkPlaced {

    public static interface DocumentUpdater {
        public void update(Document page);
    }

    public static void updatePage(String fileName, DocumentUpdater update) throws IOException {
        Document page = Jsoup.parse(getString(GithubConnector.getRetriably(fileName)));
        update.update(page);
        page.outputSettings(page.outputSettings().prettyPrint(false));
        try (BufferedWriter out = new BufferedWriter(new FileWriter("temp.html"))) {
//            out.write("<!DOCTYPE html>");
//            out.newLine();
//            for (Element child : page.children()) {
//                prettyPrint(out, child, 0);
//            }
            out.write(page.outerHtml());
            out.newLine();
        }
        GithubConnector.commitChange(fileName, "temp.html");
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

//    private static void prettyPrint(BufferedWriter out, Element elem, int indent)  throws IOException {
//        StringBuilder indentStr = new StringBuilder();
//        for (int i = 0; i < 4*indent; i++) {
//            indentStr.append(" ");
//        }
//        out.write(indentStr + "<" + elem.tag().toString() + elem.attributes() + ">");
//        out.write(elem.wholeOwnText());
//        if (elem.tagName().equals("script")) {
//            out.write(elem.data());
//        }
//        if (elem.childrenSize() > 0) {
//            out.newLine();
//        }
//        
//        for (Element child : elem.children()) {
//            prettyPrint(out, child, indent + 1);
//        }
//        // not everything has a close tag
//        String closeTag = "</" + elem.tagName() + ">";
//        if (elem.outerHtml().contains(closeTag)) {
//            if (elem.childrenSize() > 0) {
//                out.write(indentStr.toString());
//            }
//            out.write(closeTag);
//        }
//        out.newLine();
//    }

    public static void markPlaced(String hrefForHorsePage, String optionalDetails) throws IOException {
        // remove from available.html
        StringBuilder snippetToMove = new StringBuilder();
        AtomicBoolean removed = new AtomicBoolean(false);
        updatePage("available.html", page -> {
            Elements snippets = page.select(".available_snippet_div");
            for (Element snippet : snippets) {
                String horsePage = snippet.select(".available_title").getFirst().attr("href");
                if (horsePage.equalsIgnoreCase(hrefForHorsePage)) {
                    snippetToMove.append(snippet.outerHtml());
                    snippet.remove();
                    removed.set(true);
                    break;
                }
            }
        });
        
        if (!removed.get()) {
            // horse already marked as placed (semantic merge conflict)
            // reload (which is triggered after this) should fix the stale UI
            return;
        }

        // remove from both places in index.html
        updatePage("index.html", page -> {
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
        updatePage("placed.html", page -> {
            Element main = page.getElementsByTag("main").getFirst();
            main.children().get(1).after(snippetToMove.toString()); // h1 & year nav
        });

        // add optional notes or "PLACED" to top of horse page
        String notes = optionalDetails.isBlank() ? "PLACED" : optionalDetails;
        updatePage(hrefForHorsePage, page -> {
            Element title = page.getElementsByTag("h1").getFirst();
            title.after("<p>" + notes + "</p>");
        });
    }
}
