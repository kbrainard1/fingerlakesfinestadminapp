import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MarkPlaced {

    public static interface DocumentUpdater {
        public void update(Document page);
    }

    public static void updatePage(String fileName, DocumentUpdater update) throws IOException {
        Document page = GithubConnector.getHtmlFile(fileName);
        update.update(page);
        writePage(page);
        GithubConnector.editFile(fileName, "temp.html");
    }
    
    public static void writePage(Document page) throws IOException {
        page.outputSettings(page.outputSettings().prettyPrint(false));
        try (BufferedWriter out = new BufferedWriter(new FileWriter("temp.html"))) {
            out.write("<!DOCTYPE html>");
            out.newLine();
            for (Element child : page.children()) {
                prettyPrint(out, child, 0);
            }
            // We want pretty printing *except* don't change &lt to <
            // Newlines and reformatting is a nightmare, so we largely reinvent the wheel
           // out.write(page.outerHtml());
        }
    }

    private static void prettyPrint(BufferedWriter out, Element elem, int indent)  throws IOException {
        StringBuilder indentStr = new StringBuilder();
        for (int i = 0; i < 4*indent; i++) {
            indentStr.append(" ");
        }
        out.write(indentStr + "<" + elem.tag().toString() + elem.attributes() + ">");

        if (elem.childrenSize() > 0) {
            out.write(elem.ownText());
            out.newLine();
        } else {
            out.write(elem.html());
        }
        
        for (Element child : elem.children()) {
            prettyPrint(out, child, indent + 1);
        }
        // not everything has a close tag
        String closeTag = "</" + elem.tagName() + ">";
        if (elem.outerHtml().contains(closeTag)) {
            if (elem.childrenSize() > 0) {
                out.write(indentStr.toString());
            }
            out.write(closeTag);
        }
        out.newLine();
    }

    public static void markPlaced(String hrefForHorsePage, String optionalDetails) throws IOException {
        // remove from available.html
        StringBuilder snippetToMove = new StringBuilder();
        AtomicBoolean removed = new AtomicBoolean(false);
        updatePage("available.html", page -> {
            Elements snippets = page.select(".available_snippet_div");
            for (Element snippet : snippets) {
                String horsePage = snippet.select(".available_title").get(0).attr("href");
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

            Element recentAdds = page.select(".recent_adds").get(0).getElementsByTag("ul").get(0);
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
            Element main = page.getElementsByTag("main").get(0);
            main.children().get(1).after(snippetToMove.toString()); // h1 & year nav
        });

        // add optional notes or "PLACED" to top of horse page
        String notes = optionalDetails.isBlank() ? "PLACED" : optionalDetails;
        updatePage(hrefForHorsePage, page -> {
            Element title = page.getElementsByTag("h1").get(0);
            title.after("<p>" + notes + "</p>");
        });
        GithubConnector.commitAndPush(GithubConnector.CommitType.MARK_PLACED);
    }
}
