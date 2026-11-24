package haven.theme;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Reusable rounded card with anti-aliased Java2D painting and a subtle shadow. */
public class RoundedPanel extends JPanel {
    private int cornerRadius;
    private Color fillColor;
    private Color outlineColor;
    private boolean shadowEnabled = true;

    public RoundedPanel() { this(16, HavenColors.WHITE); }

    public RoundedPanel(int cornerRadius, Color fillColor) {
        this(cornerRadius, fillColor, HavenColors.BORDER);
    }

    public RoundedPanel(int cornerRadius, Color fillColor, Color outlineColor) {
        this.cornerRadius = Math.max(0, cornerRadius);
        this.fillColor = fillColor == null ? HavenColors.WHITE : fillColor;
        this.outlineColor = outlineColor;
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 16, 16));
    }

    public void setCornerRadius(int radius) { cornerRadius = Math.max(0, radius); repaint(); }
    public int getCornerRadius() { return cornerRadius; }
    public void setFillColor(Color color) { fillColor = color == null ? HavenColors.WHITE : color; repaint(); }
    public Color getFillColor() { return fillColor; }
    public void setOutlineColor(Color color) { outlineColor = color; repaint(); }
    public void setShadowEnabled(boolean enabled) { shadowEnabled = enabled; repaint(); }
    public boolean isShadowEnabled() { return shadowEnabled; }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            float shadowOffset = shadowEnabled ? 5f : 0f;
            float cardWidth = Math.max(0f, getWidth() - shadowOffset - 1f);
            float cardHeight = Math.max(0f, getHeight() - shadowOffset - 1f);
            float arc = cornerRadius;

            if (shadowEnabled) {
                g2.setColor(new Color(15, 30, 46, 13));
                g2.fill(new RoundRectangle2D.Float(3f, 5f, cardWidth, cardHeight, arc, arc));
                g2.setColor(new Color(15, 30, 46, 8));
                g2.fill(new RoundRectangle2D.Float(1.5f, 3f, cardWidth, cardHeight, arc, arc));
            }

            g2.setColor(fillColor);
            g2.fill(new RoundRectangle2D.Float(0f, 0f, cardWidth, cardHeight, arc, arc));
            if (outlineColor != null) {
                g2.setColor(outlineColor);
                g2.draw(new RoundRectangle2D.Float(.5f, .5f,
                        Math.max(0f, cardWidth - 1f), Math.max(0f, cardHeight - 1f), arc, arc));
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }
}
