package adminapp;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;

public class StatusLabel extends JLabel {
    
    public static final Color SUCCESS_COLOR = new Color(0, 180, 0);
    public static final Color ERROR_COLOR = new Color(160, 0, 0);
    
    public StatusLabel() {
        setFont(CreateListingFrontend.DEFAULT_FONT.deriveFont(18f).deriveFont(Font.BOLD));
    }

    public void setSuccess(String message) {
        setForeground(SUCCESS_COLOR);
        setText(message);
    }
    
    public void setError(String message) {
        setForeground(ERROR_COLOR);
        setText(message);
    }
    
    public void setError(Exception e) {
        setForeground(ERROR_COLOR);
        String text = e.getMessage();
        if (text.startsWith("java.lang.RuntimeException:")) {
            text = text.substring(text.indexOf(":") + 1);
        }
        setText(text);
    }
    
    public void reset() {
        setText("");
    }
}
