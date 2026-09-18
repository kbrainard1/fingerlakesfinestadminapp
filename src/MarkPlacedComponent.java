import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

public class MarkPlacedComponent extends JPanel {
    
    private AvailableHorsesComponent<HorseListingComponent> horseListings;
    
    public MarkPlacedComponent() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        StatusLabel success = new StatusLabel();
       
        JButton markPlaced = new CustomButton("Mark As Placed");
        markPlaced.addActionListener(e -> {

            CreateListingFrontend.runWithSpinner(success, () -> {
                boolean successForAll = true;
                for (HorseListingComponent comp : horseListings.getSelected()) {
                    MarkPlaced.markPlaced(comp.getHref(), comp.getDetails());
                    successForAll &= updateFbPost(comp.getTitle(), comp.getDetails());
                }
                GithubConnector.mergeStaging();
                if (successForAll) {
                    success.setSuccess("Success!");
                } else {
                    success.setError("Not all Facebook posts updated");
                }
                horseListings.loadHorses();
            });

        });
        setLayout(new BorderLayout());
        JPanel placedHeader = CreateListingFrontend.wrapButton(markPlaced);
        placedHeader.add(success);
        add(placedHeader, BorderLayout.NORTH);
        
        horseListings = new AvailableHorsesComponent<>((detailPage, title, thumbnailFile) -> new HorseListingComponent(detailPage, title, thumbnailFile));
        add(horseListings.getComponent(), BorderLayout.CENTER);
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
