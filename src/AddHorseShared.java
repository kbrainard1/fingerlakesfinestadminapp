import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public abstract class AddHorseShared extends JPanel {

    protected PhotosPanel photosPanel;

    public AddHorseShared() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        photosPanel = new PhotosPanel();
    }
}
