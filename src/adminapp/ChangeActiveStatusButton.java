package adminapp;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import adminapp.GithubConnector.CommitType;
import adminapp.ListedHorseLoader.ListedHorse;

public class ChangeActiveStatusButton extends CustomButton {

    private static final String MARK_INACTIVE = "Mark Inactive";
    private static final String MARK_ACTIVE = "Mark Available";
    private boolean currentlyActive;
    
    public ChangeActiveStatusButton(ListedHorse horse) {
        super(horse.active() ? MARK_INACTIVE : MARK_ACTIVE);
        currentlyActive = horse.active();
        addActionListener(e -> {
            try {
                if (currentlyActive) {
                    MarkPlaced.removeFromAvailable(horse.page());
                    addToInactive(horse);
                } else {
                    removeFromInactive(horse.page());
                    CreateListing.updateMetadata(horse.title(), horse.thumbnail(), horse.page(), horse.snippet());
                }
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
            currentlyActive = !currentlyActive;
            setText(currentlyActive ? MARK_INACTIVE : MARK_ACTIVE);
            CreateListingFrontend.runWithSpinner(() -> {
                GithubConnector.commitAndPush(CommitType.EDIT_HORSE);
                GithubConnector.mergeStaging();
            });
        });
    }

    private void removeFromInactive(String href) throws IOException {
        StringBuilder snippetToMove = new StringBuilder();
        AtomicBoolean removed = new AtomicBoolean(false);
        MarkPlaced.updatePage("inactive.html", page -> {
            Elements snippets = page.select(".available_snippet_div");
            for (Element snippet : snippets) {
                String horsePage = snippet.select(".available_title").get(0).attr("href");
                if (horsePage.equalsIgnoreCase(href)) {
                    snippetToMove.append(snippet.outerHtml());
                    snippet.remove();
                    removed.set(true);
                    break;
                }
            }
        });
    }

    private void addToInactive(ListedHorse horse) throws IOException {
        MarkPlaced.updatePage("inactive.html", page -> {
            Element titleElem = page.getElementsByTag("h1").get(0);
            String toAdd = 
                    "    <div class=\"available_snippet_div\">" +
                    "        <a class=\"available_title\" href=\"" + horse.page() + "\">" + horse.title() + "</a>" +
                    "        <div class=\"available_snippet_inner_div\">" +
                    "        <img class=\"snippet_thumb\" src=\"" + horse.thumbnail() + "\">" + 
                    "        <p>" + horse.snippet() + 
                    "        <a href=\"" + horse.page() + "\">Continue Reading...</a></p> " + 
                    "        </div>" +
                    "    </div>";
            titleElem.after(toAdd);
        });
    }

}
