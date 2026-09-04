import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.kohsuke.github.GHRepository;

public class HorseListingComponent extends JComponent {

    private String detailPage;
    private String title;
    private JCheckBox checked;
    private JTextField details;

    public HorseListingComponent(String detailPage, String title, String thumbnailFile, GHRepository repo) {
        this.detailPage = detailPage;
        this.title = simplifyWhitespace(title);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        checked = new JCheckBox();
        add(checked);
        details = new JTextField("");
        details.setPreferredSize(new Dimension(200, 20));
        JLabel icon;
        try (InputStream in = repo.getFileContent("staging/" + thumbnailFile).read()) {
            icon = new JLabel(new ImageIcon(ImageIO.read(in)
                    .getScaledInstance(100, 100, Image.SCALE_DEFAULT)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        icon.setPreferredSize(new Dimension(100, 100));
        add(icon);
        JLabel titleLabel = new JLabel(this.title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        add(titleLabel);
        JLabel detailLabel = new JLabel("Optional Placement Notes");
        detailLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        JPanel labeledDetails = new JPanel();
        labeledDetails.setLayout(new GridLayout(3, 1));
        labeledDetails.add(detailLabel);
        labeledDetails.add(details);
        labeledDetails.add(new JLabel()); // for visual centering
        add(labeledDetails);
    }

    private String simplifyWhitespace(String s) {
        s = s.trim();
        s = s.replaceAll("\\s\\s+", " ");

        return s;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details.getText();
    }

    public String getHref() {
        return detailPage;
    }

    public boolean isSelected() {
        return checked.getModel().isSelected();
    }

}
