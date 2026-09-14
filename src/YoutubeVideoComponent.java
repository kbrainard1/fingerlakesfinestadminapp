import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;


public class YoutubeVideoComponent extends JComponent {
    
    protected static final int CLOSE_SIZE = 15;
    
    public static final int WIDTH = 250;
    public static final int HEIGHT = 180;
    
    private YoutubeConnector.YoutubeResult video;

    public YoutubeVideoComponent(YoutubeConnector.YoutubeResult video) throws IOException {
       this.video = video;
       setBackground(Color.WHITE);
       setLayout(new BorderLayout());
       JLabel title = new JLabel(video.title());
       title.setFont(CreateListingFrontend.DEFAULT_FONT);
       title.setHorizontalAlignment(SwingConstants.CENTER);
       title.setForeground(Color.BLUE);
       title.setBackground(Color.WHITE);
       add(title, BorderLayout.NORTH);
       URL url = URI.create(video.thumb().getUrl()).toURL();
       Image image = ImageIO.read(url).getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH);
       JLabel videoThumb = new JLabel(new ImageIcon(image)) {
           @Override
           public void paintComponent(Graphics g) {
               super.paintComponent(g);
               
               int triangleHeight = 15;
               int circleWidth = 25;
               
               g.setColor(Color.LIGHT_GRAY);
               g.fillOval((getWidth() - circleWidth) / 2 , (getHeight() - circleWidth) / 2, circleWidth, circleWidth);
               g.setColor(Color.BLACK);
               
               int[] xPoints = new int[] {getWidth() / 2 - triangleHeight / 2 + 2, getWidth() / 2 - triangleHeight / 2 + 2, getWidth() / 2 + triangleHeight / 2 + 2};
               int[] yPoints = new int[] {getHeight() / 2 - triangleHeight / 2, getHeight() / 2 + triangleHeight / 2, getHeight() / 2};
               g.fillPolygon(xPoints, yPoints, 3);
           }
       };
       videoThumb.setBorder(new MatteBorder(10, 2, 10, 2, Color.BLACK));
       add(videoThumb);
       addMouseListener(new MouseListener() {

           @Override
           public void mouseReleased(MouseEvent e) {}

           @Override
           public void mousePressed(MouseEvent e) {}

           @Override
           public void mouseExited(MouseEvent e) {}

           @Override
           public void mouseEntered(MouseEvent e) {}

           @Override
           public void mouseClicked(MouseEvent e) {
               if (e.getY() < CLOSE_SIZE && e.getX() < getWidth()
                       && e.getX() > getWidth() - CLOSE_SIZE) {
                   Container parent =  getParent();
                   parent.remove(YoutubeVideoComponent.this); 
                   parent.setPreferredSize(new Dimension(parent.getComponentCount() * WIDTH, HEIGHT + 20));
                   parent.revalidate();
                   parent.repaint();
               } else {
                   try {
                       Desktop.getDesktop().browse(URI.create("https://www.youtube.com/watch?v=" + video.videoId()));
                   } catch (IOException e1) {
                       throw new RuntimeException(e1);
                   }
               }
           }
       });
       setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
       setPreferredSize(new Dimension(WIDTH, HEIGHT + 40));
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawClose(g);
    }
    
    private void drawClose(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(getWidth() - CLOSE_SIZE, 0, CLOSE_SIZE, CLOSE_SIZE);
        g.setColor(Color.RED);
        g.setFont(new Font("Courier", Font.BOLD, 24));
        g.drawString("\u00D7", getWidth() - CLOSE_SIZE, CLOSE_SIZE);
    }

    public String getVideoId() {
       return video.videoId();
    }
}
