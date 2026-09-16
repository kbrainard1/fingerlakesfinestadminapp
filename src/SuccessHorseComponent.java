import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SuccessHorseComponent extends JPanel {
    
    public static interface FacebookCallback {
        public String getButtonText();
        public void postToFb();
    }

    public SuccessHorseComponent(String horsePage, FacebookCallback fb) {
        setLayout(new BorderLayout());
        JLabel success = new JLabel("Success!");
        success.setFont(CreateListingFrontend.DEFAULT_FONT.deriveFont(24f));
        success.setForeground(CreateListingFrontend.SUCCESS_COLOR);
        add(CreateListingFrontend.wrapButton(success), BorderLayout.NORTH);
        
        JLabel status = new JLabel();
        status.setFont(CreateListingFrontend.DEFAULT_FONT);
        
        JButton preview = new CustomButton("Preview changes in browser");
        preview.addActionListener(e -> {
            status.setText("");
            GithubConnector.cloneStaging();
            
            try {
                // otherwise the embedded youtube videos don't have a valid referrer
                Desktop.getDesktop().browse(new URI("http://127.0.0.1:8080/" + horsePage));
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        });
        
        
        
        JButton deploy = new CustomButton("Looks good, deploy to website!");
        deploy.addActionListener(e -> {
            status.setText("");
            GithubConnector.mergeStaging();
            status.setForeground(CreateListingFrontend.SUCCESS_COLOR);
            status.setText("Successfully Deployed Site");
        });
        
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        JPanel wrapButton = CreateListingFrontend.wrapButton(preview);
        wrapButton.add(deploy);
        
        
        JButton facebookAction = new CustomButton(fb.getButtonText());
        facebookAction.addActionListener(e -> {
            status.setText("");
            try {
                fb.postToFb();
                status.setForeground(CreateListingFrontend.SUCCESS_COLOR);
                status.setText("Successfully posted to Facebook");
            } catch (Exception e1) {
                status.setForeground(CreateListingFrontend.ERROR_COLOR);
                status.setText("Unexpected error posting to Facebook " + e1.getMessage());
            }
        });
        wrapButton.add(facebookAction);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < 5; i++) {
            // squish the flow layouts
            panel.add(CreateListingFrontend.wrapButton(new JLabel(" ")));
        }
        panel.add(wrapButton);
        panel.add(CreateListingFrontend.wrapButton(status));
        for (int i = 0; i < 15; i++) {
            // squish the flow layouts
            panel.add(CreateListingFrontend.wrapButton(new JLabel(" ")));
        }
        
        add(panel, BorderLayout.CENTER);
        
        setOpaque(true);
        setBackground(Color.WHITE);
    }
}
