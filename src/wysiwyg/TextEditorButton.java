package wysiwyg;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import adminapp.CreateListingFrontend;

public class TextEditorButton extends EditorButton {

    private boolean enabled = false;

    public TextEditorButton(String text) {
        super(text);
        setBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2),new CompoundBorder(new LineBorder(CreateListingFrontend.ADMIN_BACKGROUND, 1, false), 
                new EmptyBorder(5, 5, 5, 5))));
    }
    
    public void toggle(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            setBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2),new CompoundBorder(new LineBorder(Color.BLACK, 1, false), 
                    new EmptyBorder(5, 5, 5, 5))));
        } else {
            setBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2),new CompoundBorder(new LineBorder(CreateListingFrontend.ADMIN_BACKGROUND, 1, false), 
                    new EmptyBorder(5, 5, 5, 5))));
        }
        repaint();
    }
    
    @Override
    public void paintComponent(Graphics g) {
        if (enabled) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(CreateListingFrontend.ADMIN_BACKGROUND);
        }
        g.fillRect(2,2, getWidth() - 4, getHeight() - 4);
        super.paintComponent(g);
    }
}
