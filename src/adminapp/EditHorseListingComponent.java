package adminapp;
import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;


public class EditHorseListingComponent extends JButton {
        
    public static interface EditCallback {
        public void call(String detailPage);
    }

    public EditHorseListingComponent(ListedHorseLoader.ListedHorse horse, EditCallback edit) {
        String title = HorseListingHelper.simplifyWhitespace(horse.title());
        if (!horse.active()) {
            title = "(Inactive) " + title;
        }
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(new LineBorder(Color.DARK_GRAY, 1, true));
        JLabel icon = HorseListingHelper.loadIcon(horse.thumbnail());
        add(icon);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(CreateListingFrontend.DEFAULT_FONT);
        add(titleLabel);
        addActionListener(e -> edit.call(horse.page()));
        setFocusable(false);
    }
}
