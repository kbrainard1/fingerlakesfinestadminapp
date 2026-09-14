import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileSystemView;

import javafx.application.Platform;
import javafx.stage.FileChooser;

public class PhotosPanel {

    protected JPanel photosPreview;
    protected JPanel photosPanel;

    public PhotosPanel() {
        JButton selectPhotos = new CustomButton("Select All Photos");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle( "Select Horse Photos");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
                );
        
        try {
            FileSystemView view = FileSystemView.getFileSystemView();
            fileChooser.setInitialDirectory(view.getHomeDirectory());
            
            // Windows, man. I have *opinions*
            String currentFolder = view.getHomeDirectory().getCanonicalFile().getCanonicalPath();
            currentFolder = currentFolder.substring(0, currentFolder.lastIndexOf(File.separator) + 1) + "Downloads";
            File downloads = new File(currentFolder);
            if (downloads.exists()) {
                fileChooser.setInitialDirectory(downloads);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        
        selectPhotos.addActionListener(e -> {

            Platform.runLater(() -> {
                List<File> result = fileChooser.showOpenMultipleDialog(null);
                if (result != null) {
                    CreateListingFrontend.showSpinner();
                    CreateListingFrontend.threadPool.submit(() -> {
                        try {
                            addFiles(result);
                        } finally {
                            CreateListingFrontend.hideSpinner();
                        }
                    });
                }
            });
        });

        photosPanel = new JPanel();
        photosPreview = new JPanel();
        photosPanel.setLayout(new BorderLayout(3, 3));
        photosPanel.add(CreateListingFrontend.wrapButton(selectPhotos, new FlowLayout(FlowLayout.LEFT)), BorderLayout.NORTH);

        JScrollPane photoWrapper = new JScrollPane(photosPreview,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        photosPreview.setLayout(new GridLayout(1, 0, 3, 3));
        photosPreview.setPreferredSize(new Dimension(CustomPhoto.WIDTH, CustomPhoto.HEIGHT + 10));
        photosPanel.add(photoWrapper, BorderLayout.CENTER);
    }

    public void addFiles(List<File> files) {
        for (File f : files) {
            try {
                Image img = ImageIO.read(f).getScaledInstance(CustomPhoto.WIDTH, CustomPhoto.HEIGHT, Image.SCALE_SMOOTH);
                JComponent comp = new CustomPhoto(img, f.getCanonicalPath());
                photosPreview.add(comp);
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
        photosPreview.setPreferredSize(new Dimension(CustomPhoto.WIDTH * photosPreview.getComponentCount(), 
                CustomPhoto.HEIGHT + 20));

        photosPanel.repaint();
        photosPanel.revalidate();
    }
    
    public JComponent getPhotosComponent() {
        return photosPanel;
    }

    public List<String> prepImageFiles() throws IOException {
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
        return imageFiles;
    }

    public boolean hasPhoto() {
        return photosPreview.getComponentCount() > 0;
    }
}
