import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

public class PreviewDeployComponent extends JPanel {

    PreviewDeployComponent() {
        JButton preview = new CustomButton("Preview website in browser");
        preview.addActionListener(e -> {
            GithubConnector.cloneStaging();
            
            try {
                URI uri = new File("staging/index.html").toURI();
                Desktop.getDesktop().browse(uri);
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        });
        
        JButton deploy = new CustomButton("Looks good, deploy to actual site");
        deploy.addActionListener(e -> {
            GithubConnector.mergeStaging();
        });
        
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        JPanel wrapButton = CreateListingFrontend.wrapButton(preview);
        wrapButton.add(deploy);
        
        add(wrapButton, BorderLayout.CENTER);
        setOpaque(true);
        setBackground(Color.WHITE);
    }
}
