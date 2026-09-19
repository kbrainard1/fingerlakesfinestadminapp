import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;


public class EditHorseListingComponent extends JButton {
    
    private JLabel titleLabel;
    
    public static interface EditCallback {
        public void call(String detailPage);
    }

    public EditHorseListingComponent(String detailPage, String title, String thumbnailFile, EditCallback edit) {
        title = HorseListingHelper.simplifyWhitespace(title);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(new LineBorder(Color.DARK_GRAY, 1, true));
        JLabel icon = HorseListingHelper.loadIcon(thumbnailFile);
        add(icon);
        titleLabel = new JLabel(title);
        titleLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        add(titleLabel);
        addActionListener(e -> edit.call(detailPage));
        setFocusable(false);
    }
}
