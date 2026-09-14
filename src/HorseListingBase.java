import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;

public abstract class HorseListingBase extends JComponent {

    private String detailPage;
    private String title;
    protected JToggleButton checked;

    public HorseListingBase(String detailPage, String title, String thumbnailFile) {
        this.detailPage = detailPage;
        this.title = simplifyWhitespace(title);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        checked = new JCheckBox();
        add(checked);
        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(100, 100));
        loadIcon(thumbnailFile, icon);
        add(icon);
        JLabel titleLabel = new JLabel(this.title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        add(titleLabel);
    }

    private void loadIcon(String thumbnailFile, JLabel icon) {
        CreateListingFrontend.threadPool.submit(() -> {
            try (InputStream in = GithubConnector.getRetriably(thumbnailFile).read()) {
                icon.setIcon(new ImageIcon(ImageIO.read(in)
                        .getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String simplifyWhitespace(String s) {
        s = s.trim();
        s = s.replaceAll("\\s\\s+", " ");

        return s;
    }

    public String getTitle() {
        return title;
    }

    public String getHref() {
        return detailPage;
    }

    public boolean isSelected() {
        return checked.getModel().isSelected();
    }

}
