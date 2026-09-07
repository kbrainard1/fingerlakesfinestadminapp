import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.kohsuke.github.GHContent;

public class MarkPlacedComponent extends JPanel {
    
    private JPanel horseListings;
    
    public MarkPlacedComponent() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        JButton markPlaced = new CustomButton("Mark As Placed");
        markPlaced.addActionListener(e -> {
            CreateListingFrontend.showSpinner();

            CreateListingFrontend.threadPool.submit(() -> {
                try {
                    for (int i = 0; i < horseListings.getComponentCount(); i++) {
                        HorseListingComponent comp = ((HorseListingComponent)horseListings.getComponent(i));
                        
                        if (comp.isSelected()) {
                            MarkPlaced.markPlaced(comp.getHref(), comp.getDetails());
                            // Eventually: also update the FB post
                        }
                    }
                    loadHorses();
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                } finally {
                    
                }
            });

        });
        setLayout(new BorderLayout());
        add(CreateListingFrontend.wrapButton(markPlaced), BorderLayout.NORTH);
        
        horseListings = new JPanel();
        horseListings.setLayout(new GridLayout(0, 1));
        JScrollPane comp = new JScrollPane(horseListings,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(comp, BorderLayout.CENTER);
        
        CreateListingFrontend.showSpinner();
        CreateListingFrontend.threadPool.submit(() -> {
            loadHorses();
            CreateListingFrontend.hideSpinner();
        });
    }

    private void loadHorses() {
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
                    horseListings.add(new HorseListingComponent(page, title, thumb));
                }
            }
            
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }
}
