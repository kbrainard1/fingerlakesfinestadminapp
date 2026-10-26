package wysiwyg;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import adminapp.CreateListingFrontend;

public class EditorButton extends JButton {

    public static Color normalBg = CreateListingFrontend.ADMIN_BACKGROUND;
    public static Color hoverBg = new Color(160, 180, 250);
    public static Color clickBg = new Color(120, 140, 250);

    public EditorButton(Action action, String text) {
        super(action);
        setText(text);
        setFont(CreateListingFrontend.DEFAULT_FONT);
        setFocusable(false);
        setUI(new BasicButtonUI());
        setOpaque(false);

        setBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2),new CompoundBorder(new LineBorder(Color.BLACK, 1, false), 
                new EmptyBorder(5, 5, 5, 5))));
    }
    
    public EditorButton(String text) {
        super(text);
        setFont(CreateListingFrontend.DEFAULT_FONT);
        setFocusable(false);
        setUI(new BasicButtonUI());
        setOpaque(false);

        setBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2),new CompoundBorder(new LineBorder(Color.BLACK, 1, false), 
                new EmptyBorder(5, 5, 5, 5))));
    }
    
    @Override
    public void paintComponent(Graphics g) {
        if (getModel().isPressed()) {
           g.setColor(clickBg);
        } else if (getModel().isRollover()) {
            g.setColor(hoverBg);
        } else {
           g.setColor(normalBg);
        }
        g.fillRect(2,2, getWidth() - 4, getHeight() - 4);

        super.paintComponent(g);
    }
}
