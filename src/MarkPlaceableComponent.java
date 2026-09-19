import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


public class MarkPlaceableComponent {
    
    protected JPanel horseListings;
    
    public MarkPlaceableComponent() {
        horseListings = new JPanel();
    }
    
    public JComponent getComponent(HorseListingComponent.ChosenHorseCallback buttonListener) {
        JPanel addMargins = new JPanel();
        addMargins.setBackground(Color.WHITE);
        addMargins.setLayout(new BorderLayout());
        addMargins.add(createSpacer(), BorderLayout.WEST);
        horseListings.setLayout(new GridLayout(0, 1));
        JScrollPane comp = new JScrollPane(horseListings,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        addMargins.add(comp, BorderLayout.CENTER);
        addMargins.add(createSpacer(), BorderLayout.EAST);
        loadHorses(buttonListener);
        return addMargins;
    }

    public static JLabel createSpacer() {
        return new JLabel("                          ");
    }

    public void loadHorses(HorseListingComponent.ChosenHorseCallback buttonListener) {
        horseListings.removeAll();
        AvailableHorsesLoader.loadHorses().forEach(horse -> 
        horseListings.add(new HorseListingComponent(horse.page(), horse.title(), horse.thumbnail(), buttonListener)));
    }
}
