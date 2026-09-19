import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;


public class HorseListingComponent extends JComponent {
    
    public static interface ChosenHorseCallback {
        public void call(String href, String title, String details);
    }

    private String detailPage;
    private String title;
    private JTextField details;

    public HorseListingComponent(String detailPage, String title, String thumbnailFile, 
            ChosenHorseCallback callback) {
        this.detailPage = detailPage;
        this.title = HorseListingHelper.simplifyWhitespace(title);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(new LineBorder(Color.BLACK, 1, false));
        JLabel icon = HorseListingHelper.loadIcon(thumbnailFile);
        add(icon);
        JLabel titleLabel = new JLabel(this.title);
        titleLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        details = new JTextField("PLACED");
        details.setPreferredSize(new Dimension(300, 20));
        details.setForeground(Color.LIGHT_GRAY);
        details.setFont(CreateListingFrontend.DEFAULT_FONT.deriveFont(Font.ITALIC));
        details.addFocusListener(new FocusListener() {
            
            @Override
            public void focusLost(FocusEvent e) {
                if (details.getText().isBlank()) {
                    details.setText("PLACED");
                    details.setForeground(Color.LIGHT_GRAY);
                    details.setFont(CreateListingFrontend.DEFAULT_FONT.deriveFont(Font.ITALIC));
                }
            }
            
            @Override
            public void focusGained(FocusEvent e) {
                if (details.getFont().isItalic()) {
                    details.setText("");
                    details.setFont(CreateListingFrontend.DEFAULT_FONT);
                    details.setForeground(Color.BLACK);
                }
            }
        });
        
        String tooltip = "By default, the horse's page and facebook post will be updated "
                + "with \"PLACED\" unless you add other details here (ex: 'Moved to FLTAP')";
        JLabel detailLabel = new JLabel("Optional Placement Notes");
        detailLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        detailLabel.setToolTipText(tooltip);
        details.setToolTipText(tooltip);
        JPanel labeledDetails = new JPanel();
        labeledDetails.setLayout(new GridLayout(4, 1));
        labeledDetails.add(titleLabel);
        labeledDetails.add(new JLabel()); // spacer
        labeledDetails.add(detailLabel);
        labeledDetails.add(details);
        add(labeledDetails);
        JButton placed = new CustomButton("Mark as Placed");
        placed.addActionListener(e -> callback.call(getHref(), getTitle(), getDetails()));
        JPanel buttonPanel = CreateListingFrontend.wrapButton(new JLabel("      ")); // add spacer
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(placed);
        buttonPanel.add(new JLabel("  "));
        add(buttonPanel);
    }

    public String getDetails() {
        return details.getText();
    }

    public String getTitle() {
        return title;
    }

    public String getHref() {
        return detailPage;
    }
}
