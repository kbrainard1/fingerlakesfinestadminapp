import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class MarkPlacedComponent extends JPanel {
    
    private MarkPlaceableComponent horseListings;
    private HorseListingComponent.ChosenHorseCallback buttonListener;
    private JPanel placedHeader;
    
    public MarkPlacedComponent() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        StatusLabel success = new StatusLabel();
        
        buttonListener = (href, title, details) -> {

            CreateListingFrontend.runWithSpinner(success, () -> {
                MarkPlaced.markPlaced(href, details);
                boolean updatedFb = updateFbPost(title, details);
                GithubConnector.mergeStaging();
                if (updatedFb) {
                    success.setSuccess("");
                } else {
                    success.setError("Facebook post failed to update");
                }
                for (int i = 0; i < placedHeader.getComponentCount(); i++) {
                    if (placedHeader.getComponent(i) instanceof UndoLabel) {
                        placedHeader.remove(i);
                        break;
                    }
                }
                placedHeader.add(new UndoLabel(title, () -> {
                    for (int i = 0; i < placedHeader.getComponentCount(); i++) {
                        if (placedHeader.getComponent(i) instanceof UndoLabel) {
                            placedHeader.remove(i);
                            break;
                        }
                    }
                    success.reset();
                    horseListings.loadHorses(buttonListener);
                    revalidate();
                    repaint();
                }));
                horseListings.loadHorses(buttonListener);
                revalidate();
                repaint();
            });

        };
        setLayout(new BorderLayout());
        placedHeader = CreateListingFrontend.wrapButton(success);
        add(placedHeader, BorderLayout.NORTH);
        
        horseListings = new MarkPlaceableComponent();
        add(horseListings.getComponent(buttonListener), BorderLayout.CENTER);
    }

    private boolean updateFbPost(String title, String details) {
        try {
            String horseName = title.substring(0, title.indexOf(","));
            FbConnector.FbPost post = FbConnector.findPost(horseName);
            if (post != null) {
                String newText = details.toUpperCase() + " - " + post.contents();
                FbConnector.updatePostText(newText, post.id());
            }
            return true;
        } catch (Exception e) {
           return false;
        }  
    }
}
