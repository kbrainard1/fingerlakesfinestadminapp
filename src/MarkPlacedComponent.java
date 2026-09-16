import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MarkPlacedComponent extends JPanel {
    
    private AvailableHorsesComponent<HorseListingComponent> horseListings;
    
    public MarkPlacedComponent() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        JLabel success = new JLabel();
       
        JButton markPlaced = new CustomButton("Mark As Placed");
        markPlaced.addActionListener(e -> {
            success.setText("");
            CreateListingFrontend.showSpinner();

            CreateListingFrontend.threadPool.submit(() -> {
                try {
                    boolean successForAll = true;
                    for (HorseListingComponent comp : horseListings.getSelected()) {
                            MarkPlaced.markPlaced(comp.getHref(), comp.getDetails());
                            successForAll &= updateFbPost(comp.getTitle(), comp.getDetails());
                    }
                    if (successForAll) {
                        success.setForeground(CreateListingFrontend.SUCCESS_COLOR);
                        success.setText("Success!");
                    } else {
                        success.setForeground(CreateListingFrontend.ERROR_COLOR);
                        success.setText("Not all Facebook posts updated");
                    }
                    horseListings.loadHorses();
                } catch (Exception e1) {
                    success.setForeground(CreateListingFrontend.ERROR_COLOR);
                    success.setText("Unexpected error: " + e1.getMessage());
                    throw new RuntimeException(e1);
                } finally {
                    CreateListingFrontend.hideSpinner();
                }
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
                String currentText = post.contents();
                currentText = currentText.substring(currentText.indexOf("\n"));
                String newText = title + "\n" + details + "\n" + currentText;
                FbConnector.updatePostText(newText, post.id());
            }
            return true;
        } catch (Exception e) {
           return false;
        }  
    }
}
