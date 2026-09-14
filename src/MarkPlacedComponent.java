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
        success.setForeground(CreateListingFrontend.SUCCESS_COLOR);
        JButton markPlaced = new CustomButton("Mark As Placed");
        markPlaced.addActionListener(e -> {
            success.setText("");
            CreateListingFrontend.showSpinner();

            CreateListingFrontend.threadPool.submit(() -> {
                try {
                    for (HorseListingComponent comp : horseListings.getSelected()) {
                            MarkPlaced.markPlaced(comp.getHref(), comp.getDetails());
                            updateFbPost(comp.getHref(), comp.getDetails());
                    }
                    success.setText("Success!");
                    horseListings.loadHorses();
                } catch (Exception e1) {
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

    private void updateFbPost(String href, String details) {
        // TODO Auto-generated method stub
        
    }
}
