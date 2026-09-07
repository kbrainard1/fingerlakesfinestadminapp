import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

public class AddHorseComponent extends JPanel {
    private JTextArea fbPost;
    private JPanel photosPreview;
    private JLabel errorMessage;


    public AddHorseComponent() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
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

        JFileChooser photos = new JFileChooser();
        photos.setMultiSelectionEnabled(true);
        JButton selectPhotos = new CustomButton("Select All Photos");

        selectPhotos.addActionListener(e -> {
            int result = photos.showDialog(this, "Select Horse Photos");
            if (result == JFileChooser.APPROVE_OPTION) {
                for (File f : photos.getSelectedFiles()) {
                    try {
                        Image img = ImageIO.read(f).getScaledInstance(100, 100, Image.SCALE_DEFAULT);
                        JComponent comp = new CustomPhoto(img, f.getCanonicalPath());
                        photosPreview.add(comp);
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }

                repaint();
                revalidate();
            }
        });

        setLayout(new BorderLayout());
        add(labelPanelOuter, BorderLayout.WEST);
        add(fbPost, BorderLayout.CENTER);
        JLabel comp = new JLabel("  ");
        comp.setOpaque(true);
        comp.setBackground(Color.WHITE);
        add(comp, BorderLayout.EAST);

        JPanel photosPanel = new JPanel();
        photosPreview = new JPanel();
        photosPanel.setLayout(new BorderLayout(3, 3));
        photosPanel.add(CreateListingFrontend.wrapButton(selectPhotos, new FlowLayout(FlowLayout.LEFT)), BorderLayout.NORTH);

        photosPreview.setLayout(new GridLayout(1, 7, 3, 0));
        photosPreview.setPreferredSize(new Dimension(500, 100));
        photosPanel.add(photosPreview, BorderLayout.CENTER);

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
                    // TODO: connect to GH
                    List<String> fbPostData = Arrays.asList(fbPost.getText().split("\n"));
                    List<String> imageFiles = new ArrayList<>();
                    for (int i = 0; i < photosPreview.getComponentCount(); i++) {
                        imageFiles.add(((CustomPhoto)photosPreview.getComponent(i)).getFileName());
                    }
                    
                    // Make a small thumbnail
                    BufferedImage rawImage = ImageIO.read(new File(((CustomPhoto)photosPreview.getComponent(0)).getFileName()));
                    BufferedImage buffered = new BufferedImage(rawImage.getWidth() / 2, rawImage.getHeight() / 2, 
                            BufferedImage.TYPE_INT_RGB);
                    buffered.getGraphics().drawImage(rawImage.getScaledInstance(rawImage.getWidth() / 2, 
                            rawImage.getHeight() / 2, Image.SCALE_DEFAULT), 0, 0 , null);
                    ImageIO.write(buffered, "jpg", new File("profile.jpg"));

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
