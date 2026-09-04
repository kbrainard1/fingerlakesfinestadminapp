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

import javax.swing.JLabel;
import javax.swing.border.LineBorder;

public class CustomPhoto extends JLabel {

    protected static final int CLOSE_SIZE = 15;
    private Image img;
    private String fileName;
    private Color border = null;
    
    public CustomPhoto(Image img, String fileName) {
        this.img = img;
        this.fileName = fileName;
        setPreferredSize(new Dimension(100, 100));
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
       
        g.drawImage(img, 0, 0, null);
        if (firstPhoto) {
            int textHeight = 17;
            g.setColor(Color.WHITE);
            g.fillRect(0, getHeight() - textHeight, img.getWidth(null), textHeight);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            
            g.drawString("Profile Photo", 12, getHeight() - 7); // border = 5, spacing = 2
        }
        drawBorder(g);
        drawClose(g);
        super.paintComponent(g);
    }
    
    private int getEffectiveWidth() {
        return Math.min(getWidth(), img.getWidth(null));
    }
    
    private void drawClose(Graphics g) {
        g.setColor(Color.RED);
        g.drawLine(getEffectiveWidth() - CLOSE_SIZE + 1, 1, getEffectiveWidth() - 1, CLOSE_SIZE - 1);
        g.drawLine(getEffectiveWidth() - CLOSE_SIZE + 1, CLOSE_SIZE - 1, getEffectiveWidth() - 1, 1);
        g.drawRect(getEffectiveWidth() - CLOSE_SIZE,0, CLOSE_SIZE, CLOSE_SIZE);
    }

    private void drawBorder(Graphics g) {
        if (border != null) {
            int width = getEffectiveWidth();
            int height = img.getHeight(null);
            new LineBorder(border, 5).paintBorder(this, g, 0, 0, width, height);
        }
    }

    private boolean isFirstPhoto() {
        return getParent().getComponent(0) == this;
    }

    public String getFileName() {
        return fileName;
    }

    public Image getImage() {
        return img;
    }

}
