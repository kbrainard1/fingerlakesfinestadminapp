import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.ColorUIResource;

import com.google.common.base.Supplier;

import javafx.embed.swing.JFXPanel;

public class CreateListingFrontend {
    
    // TODO: Warn on unsaved changes (new & close)

    static  JLabel spinnerLayer;
    static Component nonSpinnerLayer;
    static JFrame outerFrame;
    static JPanel mainLayer;
    static JLayeredPane layeredPane;

    public static final Font DEFAULT_FONT = Font.decode("Arial");
    
    public static final Color ADMIN_BACKGROUND = new Color(240, 240, 240);
    public static final int OVERALL_WIDTH = 850;
    

    public static void main(String[] args) throws Exception {
        threadPool.submit(() -> {
            try {
                SimpleServer.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        
        // initialize JavaFX Toolkit
        new JFXPanel();
       
        
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
                outerFrame.addWindowListener(new WindowListener() {
                    
                    @Override
                    public void windowOpened(WindowEvent e) {}
                    
                    @Override
                    public void windowIconified(WindowEvent e) {}
                    
                    @Override
                    public void windowDeiconified(WindowEvent e) {}
                    
                    @Override
                    public void windowDeactivated(WindowEvent e) {}
                    
                    @Override
                    public void windowClosing(WindowEvent e) {}
                    
                    @Override
                    public void windowClosed(WindowEvent e) {
                        GithubConnector.waitForPush();
                    }
                    
                    @Override
                    public void windowActivated(WindowEvent e) {}
                });

                outerFrame.setFont(DEFAULT_FONT);
                
                

                mainLayer = new JPanel();
                mainLayer.setLayout(new BorderLayout());
                
                JPanel fillerPanel = new JPanel();
                fillerPanel.setPreferredSize(new Dimension(OVERALL_WIDTH, OVERALL_WIDTH));
                fillerPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.BLACK));
                fillerPanel.setBackground(Color.WHITE);
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
                doLogin();
            }
        });
    }
    
    protected static void doLogin() {
        CreateListingFrontend.threadPool.submit(() -> {
            
            GithubConnector.login((url, userCode) -> {
                JLabel message1 = new JLabel("GitHub authorization needed!");
                JLabel message2 = new JLabel("Go to " + url + " and enter the following code:");
                message1.setFont(DEFAULT_FONT.deriveFont(24f));
                message2.setFont(DEFAULT_FONT.deriveFont(24f));
                JTextField code = new JTextField(userCode);
                code.setFont(DEFAULT_FONT.deriveFont(36f));
                code.setEditable(false);
                JButton openSite = new CustomButton("Open Link In Browser");
                openSite.addActionListener(e -> {
                    try {
                        Desktop.getDesktop().browse(URI.create(url));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                });
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                for (int i = 0; i < 10; i++) {
                    // squish the flow layouts
                    panel.add(wrapButton(new JLabel(" ")));
                }
                panel.add(wrapButton(message1));
                panel.add(wrapButton(message2));
                panel.add(wrapButton(code));
                panel.add(wrapButton(openSite));
                for (int i = 0; i < 10; i++) {
                    // squish the flow layouts
                    panel.add(wrapButton(new JLabel(" ")));
                }
                swapInComponent(panel);
            });
            
            FbConnector.doLogin();
            
           
           
           
            
            JPanel wrapped = createTopLevelMenu();
            mainLayer.add(wrapped, BorderLayout.NORTH);
            JPanel fillerPanel = new JPanel();
            fillerPanel.setPreferredSize(new Dimension(OVERALL_WIDTH, OVERALL_WIDTH));
            fillerPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.BLACK));
            fillerPanel.setBackground(Color.WHITE);
            swapInComponent(fillerPanel);
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
        JButton createListingFromFb = new CustomButton("Add Horse From FB Post");
        createListingFromFb.addActionListener(e -> {
            addComp(() -> new AddHorseFromFb());
        });
        JButton createListing = new CustomButton("Add New Available Horse");
        createListing.addActionListener(e -> {
            addComp(() -> new AddHorseDetailed());
        });
        JButton markPlaced = new CustomButton("Mark Horse As Placed");
        markPlaced.addActionListener(e -> {
            addComp(() -> new MarkPlacedComponent());
        });
        JButton editListing = new CustomButton("Edit Existing Horse's Info");
        editListing.addActionListener(e -> {
            addComp(() -> new EditHorseComponent());
        });
        JButton deploy = new CustomButton("Publish Changes To Site");
        deploy.addActionListener(e -> {
           addComp(() -> new PreviewDeployComponent());
        });
        
        JPanel wrapped = wrapButton(createListingFromFb);
        wrapped.add(createListing);
        wrapped.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        wrapped.setBackground(ADMIN_BACKGROUND);
        wrapped.add(markPlaced);
        wrapped.add(editListing);
        wrapped.add(deploy);
        return wrapped;
    }
    
    private static void addComp(Supplier<JComponent> comp) {
        if (!GithubConnector.isLocalCheckoutDone()) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            for (int i = 0; i < 10; i++) {
                // squish the flow layouts
                panel.add(wrapButton(new JLabel(" ")));
            }
            JLabel message = new JLabel("Setting up data...");
            message.setFont(DEFAULT_FONT.deriveFont(24f));
            panel.add(wrapButton(message));
            for (int i = 0; i < 10; i++) {
                // squish the flow layouts
                panel.add(wrapButton(new JLabel(" ")));
            }
            swapInComponent(panel);
            // will return once clone is done
            GithubConnector.initialClone(); 
        }
        swapInComponent(comp.get());
        
    }

    public static void swapInComponent(JComponent comp) {
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
    
    public static interface RunnableException {
        public void run() throws Exception;
    }
    
    public static void runWithSpinner(RunnableException r) {
        runWithSpinner(new StatusLabel(), r);
    }
    
    public static void runWithSpinner(StatusLabel status, RunnableException r) {
        status.reset();
        showSpinner();
       
        threadPool.submit(() -> {
            try {
               r.run();
            } catch (Exception e) {
                status.setError(e);
            } finally {
                hideSpinner();
            }
        });
    }
    
    private static void showSpinner() {
        nonSpinnerLayer = layeredPane.getComponent(0);
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
    
    private static void hideSpinner() {
        layeredPane.remove(spinnerLayer);
        // pretty sure it's a swing bug that this is getting unset
        layeredPane.getLayout().addLayoutComponent("Center", nonSpinnerLayer);
        layeredPane.revalidate();
        layeredPane.repaint();
        outerFrame.getContentPane().requestFocus();
    }

    private static AtomicBoolean spinnerShowing = new AtomicBoolean(false);
    public static ExecutorService threadPool = Executors.newCachedThreadPool();
}
