import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

// Add details for what to do if it doesn't look good
public class PreviewDeployComponent extends JPanel {

    PreviewDeployComponent() {
        JButton preview = new CustomButton("Preview website in browser");
        preview.addActionListener(e -> {
            GithubConnector.cloneStaging();
            
            try {
                // otherwise the embedded youtube videos don't have a valid referrer
                Desktop.getDesktop().browse(new URI("http://127.0.0.1:8080/index.html"));
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        });
        
        JLabel success = new JLabel();
        success.setForeground(CreateListingFrontend.SUCCESS_COLOR);
        JButton deploy = new CustomButton("Looks good, deploy site!");
        deploy.addActionListener(e -> {
            GithubConnector.mergeStaging();
            success.setText("Successfully Deployed Site");
        });
        
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        JPanel wrapButton = CreateListingFrontend.wrapButton(preview);
        wrapButton.add(deploy);
        wrapButton.add(success);
        
        
        add(wrapButton, BorderLayout.NORTH);
        setOpaque(true);
        setBackground(Color.WHITE);
    }
}
