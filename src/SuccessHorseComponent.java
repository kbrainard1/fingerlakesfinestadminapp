import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
        success.setFont(CreateListingFrontend.DEFAULT_FONT.deriveFont(36f));
        success.setVerticalAlignment(JLabel.BOTTOM);
        success.setPreferredSize(new Dimension(200, 100));
        success.setForeground(StatusLabel.SUCCESS_COLOR);
        add(CreateListingFrontend.wrapButton(success), BorderLayout.NORTH);
        
        StatusLabel status = new StatusLabel();
        
        JButton preview = new CustomButton("Preview Webpage");
        preview.addActionListener(e -> {
            status.reset();
            GithubConnector.updateFromRemote();
            
            try {
                // otherwise the embedded youtube videos don't have a valid referrer
                Desktop.getDesktop().browse(new URI("http://127.0.0.1:8080/" + horsePage));
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        });
        
        JButton back = new CustomButton("Edit Horse Info");
        back.addActionListener(e -> {
            CreateListingFrontend.swapInComponent(new EditHorseComponent(horsePage));
        });
        
        JButton deploy = new CustomButton("Publish to Site");
        deploy.addActionListener(e -> {
            CreateListingFrontend.runWithSpinner(status, () -> {
                GithubConnector.mergeStaging();
                status.setSuccess("Successfully Published Site");
            });
        });
        
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        JPanel wrapButton = CreateListingFrontend.wrapButton(preview, new FlowLayout(FlowLayout.CENTER, 20, 5));
        wrapButton.add(back);
        wrapButton.add(deploy);
        
        
        JButton facebookAction = new CustomButton(fb.getButtonText());
        facebookAction.addActionListener(e -> {
            CreateListingFrontend.runWithSpinner(status, () -> {
                fb.postToFb();
                status.setSuccess("Successfully posted to Facebook");
            });
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
