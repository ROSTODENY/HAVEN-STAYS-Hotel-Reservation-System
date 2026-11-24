package haven.theme;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Rounded Swing button with anti-aliased background and hover/pressed/disabled states. */
public class ModernButton extends JButton {
    private Color buttonColor;
    private Color hoverColor;
    private Color pressedColor;
    private Color textColor = Color.WHITE;
    private int cornerRadius = 14;

    public ModernButton(String text) { this(text, HavenColors.DEEP_TEAL); }

    public ModernButton(String text, Color buttonColor) {
        super(text);
        setButtonColor(buttonColor);
        setFont(new Font("SansSerif", Font.BOLD, 13));
        setForeground(textColor);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setBorder(new EmptyBorder(11, 18, 11, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);
        getAccessibleContext().setAccessibleName(text);
        getAccessibleContext().setAccessibleDescription("Action button: " + text);
    }

    public void setButtonColor(Color color) {
        buttonColor = color == null ? HavenColors.DEEP_TEAL : color;
        hoverColor = blend(buttonColor, Color.WHITE, .12f);
        pressedColor = buttonColor.darker();
        repaint();
    }
    public Color getButtonColor() { return buttonColor; }
    public void setHoverColor(Color color) { hoverColor = color == null ? buttonColor.brighter() : color; repaint(); }
    public void setPressedColor(Color color) { pressedColor = color == null ? buttonColor.darker() : color; repaint(); }
    public void setTextColor(Color color) { textColor = color == null ? Color.WHITE : color; setForeground(textColor); repaint(); }
    public void setCornerRadius(int radius) { cornerRadius = Math.max(0, radius); repaint(); }
    public int getCornerRadius() { return cornerRadius; }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Color stateColor = buttonColor;
            if (!isEnabled()) stateColor = new Color(buttonColor.getRed(), buttonColor.getGreen(), buttonColor.getBlue(), 105);
            else if (getModel().isPressed()) stateColor = pressedColor;
            else if (getModel().isRollover()) stateColor = hoverColor;
            g2.setColor(stateColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
        } finally { g2.dispose(); }
        super.paintComponent(graphics);
    }

    private static Color blend(Color first, Color second, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        return new Color(Math.round(first.getRed() * (1 - t) + second.getRed() * t),
                Math.round(first.getGreen() * (1 - t) + second.getGreen() * t),
                Math.round(first.getBlue() * (1 - t) + second.getBlue() * t));
    }
}
