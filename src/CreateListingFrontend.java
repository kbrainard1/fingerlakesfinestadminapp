import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

public class CreateListingFrontend {
    
    // TODO: 
    // Wire this up to the GH API so no local checkout is needed
    // Figure out how to ship this
    // Warn on unsaved changes (new & close)
    // Add one other mode: enter horse info manually
    //

    static  JLabel spinnerLayer;
    static JFrame outerFrame;
    static JPanel mainLayer;
    static JLayeredPane layeredPane;

    public static final Font DEFAULT_FONT = Font.decode("Arial");
    public static final Color ERROR_COLOR = new Color(160, 0, 0);
    public static final Color ADMIN_BACKGROUND = new Color(240, 240, 240);
    public static final int OVERALL_WIDTH = 700;

    public static void main(String[] args) throws Exception {
        threadPool.submit(() -> GithubConnector.init());
        
        UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        UIManager.put("Button.focus", new ColorUIResource(new Color(0, 0, 0, 0)));
        UIManager.put("ToggleButton.focus", new ColorUIResource(new Color(0, 0, 0, 0)));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                outerFrame = new JFrame("FLF Admin Panel");
                try {
                    outerFrame.setIconImage(ImageIO.read(new File("icon.png")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                outerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                outerFrame.setFont(DEFAULT_FONT);
                
                JPanel wrapped = createTopLevelMenu();

                mainLayer = new JPanel();
                mainLayer.setLayout(new BorderLayout());
                mainLayer.add(wrapped, BorderLayout.NORTH);
                JPanel fillerPanel = new JPanel();
                fillerPanel.setPreferredSize(new Dimension(OVERALL_WIDTH, OVERALL_WIDTH));
                mainLayer.add(fillerPanel, BorderLayout.CENTER);
                mainLayer.setBounds(0, 0, OVERALL_WIDTH, OVERALL_WIDTH);

                createSpinnerLayer();
               
                layeredPane = new JLayeredPane();
                layeredPane.setLayout(new BorderLayout());
                mainLayer.putClientProperty(JLayeredPane.LAYER_PROPERTY, JLayeredPane.DEFAULT_LAYER);
                layeredPane.add(mainLayer);
                outerFrame.getContentPane().add(layeredPane);

                outerFrame.pack();
                outerFrame.setVisible(true);
            }
        });
    }
    
    protected static void createSpinnerLayer() {
        spinnerLayer = new JLabel("Working...") {
            @Override
            public void paintComponent(Graphics g) {
                g.setColor(CustomButton.normalBg);
                g.fillRoundRect(getWidth() / 5, getHeight() / 3, 3 * getWidth() / 5, getHeight() / 3, getWidth() / 10, getHeight() / 10);
               
                super.paintComponent(g);
            }
        };
        spinnerLayer.putClientProperty(JLayeredPane.LAYER_PROPERTY, JLayeredPane.MODAL_LAYER);
        spinnerLayer.setVerticalAlignment(JLabel.CENTER);
        spinnerLayer.setHorizontalAlignment(JLabel.CENTER);
        spinnerLayer.setFont(new Font("Arial", Font.BOLD, 72));
        spinnerLayer.setPreferredSize(new Dimension(OVERALL_WIDTH, OVERALL_WIDTH));
        spinnerLayer.setBounds(0, 0, OVERALL_WIDTH, OVERALL_WIDTH);
        spinnerLayer.addMouseListener(new MouseListener() {
            // No clicking for you! The UI is busy
            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseClicked(MouseEvent e) {}
        });
    }

    private static JPanel createTopLevelMenu() {
        JButton createListing = new CustomButton("Add New Available Horse");
        createListing.addActionListener(e -> {
            AddHorseComponent comp = new AddHorseComponent();
            swapInComponent(comp);
        });
        JButton markPlaced = new CustomButton("Mark Horse As Placed");
        markPlaced.addActionListener(e -> {
            MarkPlacedComponent comp = new MarkPlacedComponent();
            swapInComponent(comp);
        });
        JButton deploy = new CustomButton("Preview Site & Deploy");
        deploy.addActionListener(e -> {
            PreviewDeployComponent comp = new PreviewDeployComponent();
            swapInComponent(comp);
        });
        
        JPanel wrapped = wrapButton(createListing);
        wrapped.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        wrapped.setBackground(ADMIN_BACKGROUND);
        wrapped.add(markPlaced);
        wrapped.add(deploy);
        return wrapped;
    }
    
    protected static void swapInComponent(JComponent comp) {
        mainLayer.remove(((BorderLayout)mainLayer.getLayout()).getLayoutComponent(BorderLayout.CENTER));
        mainLayer.add(comp);
        mainLayer.revalidate();
        mainLayer.repaint();
    }

    public static JPanel wrapButton(JComponent button) {
        return wrapButton(button, new FlowLayout());
    }

    public static JPanel wrapButton(JComponent button, LayoutManager layout) {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setLayout(layout);
        panel.add(button);
        return panel;
    }
    
    public static void showSpinner() {        
        layeredPane.add(spinnerLayer);
        spinnerLayer.requestFocus();
        layeredPane.revalidate();
        layeredPane.repaint();
        
        spinnerShowing.set(true);
        threadPool.submit(() -> {
            while (spinnerShowing.get()) {
                long timeBox = (System.currentTimeMillis() / 1000) % 3;
                String text = "Working";
                for (int i = 0; i <= timeBox; i++) {
                    text += ".";
                }
                spinnerLayer.setText(text);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    
    public static void hideSpinner() {
        layeredPane.remove(spinnerLayer);
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private static AtomicBoolean spinnerShowing = new AtomicBoolean(false);
    public static ExecutorService threadPool = Executors.newCachedThreadPool();
}
