import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

public class AddHorseFromFb extends AddHorseShared {
    private JTextArea fbPost;
    private JLabel errorMessage;

    public AddHorseFromFb() {
        fbPost = new JTextArea(100, 100);
        fbPost.setBackground(new Color(230, 230, 230));
        fbPost.setBorder(new LineBorder(Color.DARK_GRAY));
        fbPost.setMargin(new Insets(10, 10, 10, 10));
        JLabel fbPostLabel = new JLabel("Text of Facebook post:");
        JLabel fbPostLabel2 = new JLabel("Don't add any newlines!");
        JLabel fbPostLabel3 = new JLabel("Trust the process, do not format the text.");
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new GridLayout(3, 1, 5, 5));
        labelPanel.setBackground(Color.WHITE);
        labelPanel.setOpaque(true);
        JPanel labelPanelOuter = new JPanel();
        labelPanelOuter.setLayout(new FlowLayout());
        labelPanelOuter.add(labelPanel);
        labelPanelOuter.setBackground(Color.WHITE);
        labelPanelOuter.setOpaque(true);
        
        for (JLabel label : new JLabel[]{fbPostLabel, fbPostLabel2, fbPostLabel3}) {
            label.setBackground(Color.WHITE);
            label.setOpaque(true);
            label.setForeground(Color.DARK_GRAY);
            label.setFont(CreateListingFrontend.DEFAULT_FONT);
            labelPanel.add(label);
        }

        setLayout(new BorderLayout());
        add(labelPanelOuter, BorderLayout.WEST);
        add(fbPost, BorderLayout.CENTER);
        JLabel comp = new JLabel("  ");
        comp.setOpaque(true);
        comp.setBackground(Color.WHITE);
        add(comp, BorderLayout.EAST);

        add(photosPanel, BorderLayout.SOUTH);

        JButton createListing = new CustomButton("Done - Add Horse To Website");
        errorMessage = new JLabel();
        errorMessage.setForeground(CreateListingFrontend.ERROR_COLOR);
        createListing.addActionListener(e -> {
            if (photosPreview.getComponentCount() == 0) {
                errorMessage.setText("Must have at least one photo!");
                return;
            }
            if (fbPost.getText().isBlank()) {
                errorMessage.setText("Must have some text from the FB post!");
                return;
            }

            CreateListingFrontend.showSpinner();

            CreateListingFrontend.threadPool.submit(() -> {
                try {
                    List<String> fbPostData = Arrays.asList(fbPost.getText().split("\n"));
                    List<String> imageFiles = prepImageFiles();
                    
                    CreateListing.createListingPage(fbPostData, "profile.jpg", imageFiles);
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                } finally {
                    CreateListingFrontend.hideSpinner();
                }
            });
        });
        JPanel wrapButton = CreateListingFrontend.wrapButton(createListing);
        wrapButton.add(errorMessage);
        add(wrapButton, BorderLayout.NORTH);
    }
    
}
