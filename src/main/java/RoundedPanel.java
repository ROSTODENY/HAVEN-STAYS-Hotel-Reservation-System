import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Reusable anti-aliased rounded card panel for Swing. */
public class RoundedPanel extends JPanel {
    private final int radius;
    private final Color fill;
    private final Color outline;

    public RoundedPanel(int radius, Color fill) { this(radius, fill, ThemePalette.BORDER); }
    public RoundedPanel(int radius, Color fill, Color outline) {
        this.radius = radius;
        this.fill = fill;
        this.outline = outline;
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 14, 16));
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int w = getWidth(), h = getHeight();
        g.setColor(new Color(15, 30, 46, 14));
        g.fillRoundRect(2, 3, Math.max(0, w - 5), Math.max(0, h - 5), radius, radius);
        g.setColor(fill);
        g.fillRoundRect(0, 0, Math.max(0, w - 4), Math.max(0, h - 4), radius, radius);
        if (outline != null) {
            g.setColor(outline);
            g.drawRoundRect(0, 0, Math.max(0, w - 5), Math.max(0, h - 5), radius, radius);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
