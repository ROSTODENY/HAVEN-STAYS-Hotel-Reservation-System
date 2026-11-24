import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Reusable rounded Swing button with anti-aliasing and hover feedback. */
public class RoundedButton extends JButton {
    private final Color baseColor;
    public RoundedButton(String text, Color color) {
        super(text);
        baseColor = color;
        setForeground(Color.WHITE);
        setBackground(color);
        setFont(new Font("SansSerif", Font.BOLD, 11));
        setFocusPainted(false);
        setBorder(new EmptyBorder(11, 16, 11, 16));
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(text);
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = getModel().isPressed() ? baseColor.darker().darker()
                : getModel().isRollover() ? baseColor.brighter() : baseColor;
        g.setColor(color);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g.dispose();
        super.paintComponent(graphics);
    }
}
