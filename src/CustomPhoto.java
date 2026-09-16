import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.border.LineBorder;

public class CustomPhoto extends JLabel {
    
    public static interface ImageLoader {
        Image loadImage() throws IOException;
    }

    protected static final int CLOSE_SIZE = 15;
    
    public static final int WIDTH = 150;
    public static final int HEIGHT = 150;
    
    private Image img;
    private String fileName;
    private Color border = null;
    
    public CustomPhoto(ImageLoader imgLoader, String fileName) {
        CreateListingFrontend.threadPool.submit(() -> {
            Image image;
            try {
                image = imgLoader.loadImage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.img = image;
            repaint();
        });
        this.fileName = fileName;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setVerticalAlignment(JLabel.BOTTOM);
        setHorizontalAlignment(JLabel.CENTER);
       
        addMouseMotionListener(new MouseMotionListener() {
            
            @Override
            public void mouseMoved(MouseEvent e) {
               // do nothing
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {                
                int absoluteX = getX() + e.getX();
                Component possibleSwap = getParent().getComponentAt(absoluteX, e.getY());
                Container parent = getParent(); //important to cache for removals
                if (possibleSwap != null && possibleSwap != CustomPhoto.this && possibleSwap != parent) {
                    if (possibleSwap.getX() < getX()) {
                        if (absoluteX < possibleSwap.getX() + possibleSwap.getWidth() / 2) {
                            // Example: we're index 2, swapIndex will be index 1
                            int swapIndex = findSwapIndex(possibleSwap);
                            parent.remove(CustomPhoto.this); // leaves prev index unchanged
                            parent.add(CustomPhoto.this, swapIndex); // add to position 1, shifting swap over 1
                            parent.revalidate();
                        }
                    } else {
                        if (absoluteX > possibleSwap.getX() + possibleSwap.getWidth() / 2) {
                            // Example: we're index 2, swapIndex will be index 3
                            int swapIndex = findSwapIndex(possibleSwap);
                            parent.remove(CustomPhoto.this); // this shifts to-swap down to 2
                            parent.add(CustomPhoto.this, swapIndex);
                            parent.revalidate();
                            parent.repaint();
                            revalidate();
                            repaint();
                        }
                    }
                }
            }

            private int findSwapIndex(Component possibleSwap) {
               for (int index = 0; index < getParent().getComponentCount(); index++) {
                   if (getParent().getComponent(index) == possibleSwap) {
                       return index;
                   }
               }
               return -1;
            }
        });
        addMouseListener(new MouseListener() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                border = null;
                repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                border = Color.BLUE;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getY() < CLOSE_SIZE && e.getX() < getEffectiveWidth()
                        && e.getX() > getEffectiveWidth() - CLOSE_SIZE) {
                    Container parent =  getParent();
                    parent.remove(CustomPhoto.this); 
                    parent.setPreferredSize(new Dimension(parent.getComponentCount() * WIDTH, HEIGHT + 20));
                    parent.revalidate();
                    parent.repaint();
                }
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        boolean firstPhoto = isFirstPhoto();
        if (firstPhoto) {
            if (border == null) { // rearranging takes precedence
               border = Color.BLACK;
            }
        } else {
            if (border == Color.BLACK) {
                border = null;
            }
        }
        
        if (img != null) {
            g.drawImage(img, 0, 0, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getEffectiveWidth(), getEffectiveHeight());
        }
        if (firstPhoto) {
            int textHeight = 17;
            g.setColor(Color.WHITE);
            g.fillRect(0, getEffectiveHeight() - textHeight, WIDTH, textHeight);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            
            g.drawString("Profile Photo", 12, getEffectiveHeight() - 7); // border = 5, spacing = 2
        }
        drawBorder(g);
        drawClose(g);
        super.paintComponent(g);
    }
    
    private int getEffectiveWidth() {
        return Math.min(getWidth(), WIDTH);
    }
    
    private int getEffectiveHeight() {
        return Math.min(getHeight(), HEIGHT);
    }
    
    private void drawClose(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(getEffectiveWidth() - CLOSE_SIZE, 0, CLOSE_SIZE, CLOSE_SIZE);
        g.setColor(Color.RED);
        g.setFont(new Font("Courier", Font.BOLD, 24));
        g.drawString("\u00D7", getEffectiveWidth() - CLOSE_SIZE, CLOSE_SIZE);
    }

    private void drawBorder(Graphics g) {
        if (border != null) {
            int width = getEffectiveWidth();
            int height = getEffectiveHeight();
            new LineBorder(border, 5).paintBorder(this, g, 0, 0, width, height);
        }
    }

    private boolean isFirstPhoto() {
        return getParent().getComponent(0) == this;
    }

    public String getFileName() {
        return fileName;
    }

}
