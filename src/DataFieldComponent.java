import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class DataFieldComponent extends JPanel {
    private JTextField data;
    private JLabel error = new JLabel("");

    public DataFieldComponent(String name) {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel nameLabel = new JLabel("        " + name);
        nameLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        add(nameLabel);
        data = new JTextField(20);
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

    public String getData() {
        return data.getText();
    }

    public void setText(String textData) {
        data.setText(textData);
    }

    public void setError(String errorMsg) {
        error.setText(errorMsg);
    }
}
