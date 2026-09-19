import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class HorseListingHelper {

    public static JLabel loadIcon(String thumbnailFile) {
        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(100, 100));
        CreateListingFrontend.threadPool.submit(() -> {
            try {
                icon.setIcon(new ImageIcon(ImageIO.read(GithubConnector.getFile(thumbnailFile))
                        .getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return icon;
    }

    public static String simplifyWhitespace(String s) {
        s = s.trim();
        s = s.replaceAll("\\s\\s+", " ");

        return s;
    }
}
