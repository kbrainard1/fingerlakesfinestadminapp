import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;

public class CustomButton extends JButton {

    public static Color normalBg = new Color(200, 220, 250);
    private static Color hoverBg = new Color(160, 180, 250);
    private static Color borderColor = new Color(25, 25, 128);

    public interface UnscaledBorderPainter {
        void paintUnscaledBorder(Graphics g,
                int w, int h,
                double scaleFactor);
    }

    private static class RoundedBorder extends LineBorder {

        public RoundedBorder(Color color, int thickness) {
            super(color, thickness, true);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            paintBorder(g,
                    x, y,
                    width, height,
                    (g2,w,h,s) -> paintUnscaledBorder(g2, w, h, s, true, borderColor));
        }

        // I don't want to talk about it (oooh, here's some critical drawing logic, but it's in some locked-down package that
        // you shouldn't rely on because sun graphics or something)
        public static void paintBorder(Graphics g, int x, int y, int width, int height, UnscaledBorderPainter painter) {

            // Step 1: Reset Transform
            AffineTransform at = null;
            Stroke oldStroke = null;
            boolean resetTransform = false;
            double scaleFactor = 1;

            int xtranslation = x;
            int ytranslation = y;
            int w = width;
            int h = height;

            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D) g;
                at = g2d.getTransform();
                oldStroke = g2d.getStroke();
                scaleFactor = Math.min(at.getScaleX(), at.getScaleY());

                // if m01 or m10 is non-zero, then there is a rotation or shear,
                // or if scale=1, skip resetting the transform in these cases.
                resetTransform = ((at.getShearX() == 0) && (at.getShearY() == 0))
                        && ((at.getScaleX() > 1) || (at.getScaleY() > 1));

                if (resetTransform) {
                    /* Deactivate the HiDPI scaling transform,
                     * so we can do paint operations in the device
                     * pixel coordinate system instead of the logical coordinate system.
                     */
                    g2d.setTransform(new AffineTransform());
                    double xx = at.getScaleX() * x + at.getTranslateX();
                    double yy = at.getScaleY() * y + at.getTranslateY();
                    xtranslation = clipRound(xx);
                    ytranslation = clipRound(yy);
                    width = clipRound(at.getScaleX() * w + xx) - xtranslation;
                    height = clipRound(at.getScaleY() * h + yy) - ytranslation;
                }
            }

            g.translate(xtranslation, ytranslation);

            // Step 2: Call respective paintBorder with transformed values
            painter.paintUnscaledBorder(g, width, height, scaleFactor);

            // Step 3: Restore previous stroke & transform
            g.translate(-xtranslation, -ytranslation);
            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(oldStroke);
                if (resetTransform) {
                    g2d.setTransform(at);
                }
            }
        }
        
        public static int clipRound(final double coordinate) {
            final double newv = coordinate - 0.5;
            if (newv < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            if (newv > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) Math.ceil(newv);
        }

        public static void paintUnscaledBorder(Graphics g,
                int w, int h,
                double scaleFactor,
                boolean pathNotInner,
                Color colorToUse) {
            if ((g instanceof Graphics2D)) {
                Graphics2D g2d = (Graphics2D) g;

                Color oldColor = g2d.getColor();
                g2d.setColor(colorToUse);

                Shape outer;
                Shape inner;

                int offs = (int) scaleFactor;
                int size = offs + offs;
                float arc = 10;
                outer = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
                inner = new RoundRectangle2D.Float(offs, offs, w - size, h - size, arc, arc);

                if (pathNotInner) {
                    Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
                    path.append(outer, false);
                    path.append(inner, false);
                    g2d.fill(path);
                } else {
                    g2d.fill(inner);
                }

                g2d.setColor(oldColor);
            }
        }
    }

    public CustomButton(String text) {
        super(text);
        setFont(CreateListingFrontend.DEFAULT_FONT);
        setFocusable(false);
        setUI(new BasicButtonUI());
        setOpaque(false);

        setBorder(new CompoundBorder(new RoundedBorder(borderColor, 1), 
                new EmptyBorder(10, 10, 10, 10)));
    }

    @Override
    public void paintComponent(Graphics g) {
        if (getModel().isRollover()) {
            RoundedBorder.paintBorder(g,
                    0, 0,
                    getWidth(), getHeight(),
                    (g2,w,h,s) -> RoundedBorder.paintUnscaledBorder(g2,w,h,s,false,hoverBg));
        } else {
            RoundedBorder.paintBorder(g,
                    0, 0,
                    getWidth(), getHeight(),
                    (g2,w,h,s) -> RoundedBorder.paintUnscaledBorder(g2,w,h,s,false,normalBg));
        }

        super.paintComponent(g);
    }
}
