import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class DataFieldComponent extends JPanel {
    private JTextField data;
    private JLabel error = new JLabel("");
    private JLabel nameLabel;

    public DataFieldComponent(String name) {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        
        // Close enough to a mono-spaced tab
        StringBuilder label = new StringBuilder("        ").append(name);
        nameLabel = new JLabel(label.toString());
        while (nameLabel.getPreferredSize().getWidth() < 100) {
            label.append(" ");
            nameLabel = new JLabel(label.toString());
        }
        
        
        nameLabel.setFont(CreateListingFrontend.DEFAULT_FONT.deriveFont(Font.ITALIC));
        nameLabel.setForeground(Color.DARK_GRAY);
        add(nameLabel);
        data = new JTextField(20);
        data.setFont(CreateListingFrontend.DEFAULT_FONT);
        data.setEnabled(false);
        add(data);
        add(error);
        error.setForeground(Color.RED);
        data.addKeyListener(new KeyListener() {
            
            @Override
            public void keyTyped(KeyEvent e) {
                error.setText("");
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
            
            @Override
            public void keyPressed(KeyEvent e) {}
        });
    }
    
    public void enable() {
        nameLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        nameLabel.setForeground(Color.BLACK);
        data.setEnabled(true);
    }
    
    public void disable() {
        nameLabel.setFont(CreateListingFrontend.DEFAULT_FONT.deriveFont(Font.ITALIC));
        nameLabel.setForeground(Color.DARK_GRAY);
        data.setEnabled(false);
    }

    public String getData() {
        return data.getText();
    }

    public void setText(String textData) {
        data.setText(textData);
    }

    public void setError(String errorMsg) {
        error.setText(errorMsg);
    }

    public void focus() {
       data.requestFocusInWindow();
    }

    public void onEnter(Runnable r) {
        data.addActionListener(e -> r.run());
    }
}
