import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.kohsuke.github.GHContent;

public class AvailableHorsesComponent<T extends HorseListingBase> {

    public static interface ComponentCreator<T> {
        T createComponent(String detailPage, String title, String thumbnailFile);
    }
    
    protected JPanel horseListings;
    private AvailableHorsesComponent.ComponentCreator<T> creator;
    
    public AvailableHorsesComponent(ComponentCreator<T> makeNewComp) {
        horseListings = new JPanel();
        this.creator = makeNewComp;
    }
    
    public JComponent getComponent() {
        horseListings.setLayout(new GridLayout(0, 1));
        JScrollPane comp = new JScrollPane(horseListings,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        loadHorses();
        return comp;
    }
    
    public List<T> getSelected() {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < horseListings.getComponentCount(); i++) {
            @SuppressWarnings("unchecked")
            T comp = (T)horseListings.getComponent(i);
            
            if (comp.isSelected()) {
                result.add(comp);
            }
        }
        return result;
    }

    public void loadHorses() {
        try {
            horseListings.removeAll();
           
            GHContent fileContent = GithubConnector.getRetriably("available.html");
            List<String> lines = GithubConnector.readFile(fileContent);
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
                    horseListings.add(creator.createComponent(page, title, thumb));
                }
            }
            
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }
}
