import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

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
                            // TODO: connect to GH
                            MarkPlaced.markPlaced("", comp.getHref(), comp.getDetails());
                            // Eventually: also update the FB post
                        }
                    }
                    loadHorses();
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                } finally {
                    CreateListingFrontend.hideSpinner();
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
        
        loadHorses();
    }

    private void loadHorses() {
        try {
            horseListings.removeAll();
            GitHub github = new GitHubBuilder().withOAuthToken("my_personal_token").build();
            GHRepository repo = github.getRepository(CreateListingFrontend.GITHUB_REPO);
            GHContent fileContent = repo.getFileContent("staging/available.html");
            List<String> lines = readFile(fileContent);
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
                    horseListings.add(new HorseListingComponent(page, title, thumb, repo));
                }
            }
            
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }

    private List<String> readFile(GHContent fileContent) throws IOException {
       List<String> result = new ArrayList<>();
       try (BufferedReader buff = new BufferedReader(new InputStreamReader(fileContent.read()))) {
           String line;
           while ((line = buff.readLine()) != null) {
               result.add(line);
           }
       }
       return result;
    }
}
