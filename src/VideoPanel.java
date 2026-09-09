import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class VideoPanel extends JPanel {
    
    JPanel youtubeResults = new JPanel();

    public VideoPanel(String horseName) {
        try {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            List<YoutubeConnector.YoutubeResult> videos = YoutubeConnector.getMatchingVideos(horseName);
            for (YoutubeConnector.YoutubeResult video : videos) {
                youtubeResults.add(new YoutubeVideoComponent(video));
            }
            
            JLabel customLinkLabel = new JLabel("Enter additional youtube video links: ");
            customLinkLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
            JTextField customLink = new JTextField(50);
            JButton customLinkAdd = new CustomButton("Add");
            
            customLinkAdd.addActionListener(e -> {
                String youtubeLink = customLink.getText();
                if (youtubeLink != null && !youtubeLink.isBlank()) {
                    try {
                        YoutubeConnector.YoutubeResult result = YoutubeConnector.lookupByLink(youtubeLink);
                        youtubeResults.add(new YoutubeVideoComponent(result));
                        revalidate();
                        repaint();
                    } catch (Exception e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
            
            JPanel customAdd = CreateListingFrontend.wrapButton(customLinkLabel, new FlowLayout(FlowLayout.LEFT));
            customAdd.add(customLink);
            customAdd.add(customLinkAdd);
          
            
            JScrollPane videoWrapper = new JScrollPane(youtubeResults,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            youtubeResults.setLayout(new GridLayout(1, 0, 3, 3));
            youtubeResults.setPreferredSize(new Dimension(YoutubeVideoComponent.WIDTH * youtubeResults.getComponentCount(), YoutubeVideoComponent.HEIGHT + 40));
            add(videoWrapper);
            add(customAdd, BorderLayout.SOUTH);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }

    public List<String> getYoutubeLinks() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < youtubeResults.getComponentCount(); i++) {
            result.add("https://www.youtube.com/watch?v=" + ((YoutubeVideoComponent)youtubeResults.getComponent(i)).getVideoId());
        }
        return result;
    }
}
