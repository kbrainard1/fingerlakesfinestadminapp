import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class UndoLabel extends JPanel {
    
    public UndoLabel(String title, Runnable callback) {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBackground(new Color(255, 251, 223));
   
        String justName = title.substring(0, title.indexOf(","));
        if (justName.contains("20")) {
            justName = justName.substring(justName.indexOf("2"));
        }
        JLabel label = new JLabel(justName + " marked as placed.     ");
        label.setFont(CreateListingFrontend.DEFAULT_FONT);
        add(label);
        JButton undoButton = new CustomButton("Undo");
        add(undoButton);
        undoButton.addActionListener(e -> {
            GithubConnector.revertLast(GithubConnector.CommitType.MARK_PLACED);
            callback.run();
        });
        
    }

}
