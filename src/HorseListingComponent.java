import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class HorseListingComponent extends HorseListingBase {

    private JTextField details;

    public HorseListingComponent(String detailPage, String title, String thumbnailFile) {
        super(detailPage, title, thumbnailFile);
        details = new JTextField("");
        details.setPreferredSize(new Dimension(200, 20));
        JLabel detailLabel = new JLabel("Optional Placement Notes");
        detailLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        JPanel labeledDetails = new JPanel();
        labeledDetails.setLayout(new GridLayout(3, 1));
        labeledDetails.add(detailLabel);
        labeledDetails.add(details);
        labeledDetails.add(new JLabel()); // for visual centering
        add(labeledDetails);
    }

    public String getDetails() {
        return details.getText();
    }

}
