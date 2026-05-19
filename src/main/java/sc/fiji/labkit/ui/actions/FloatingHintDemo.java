package sc.fiji.labkit.ui.actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Demonstrates a small semi-transparent floating badge ("?") that:
 *  - appears just south-east of the mouse pointer when it enters a control,
 *  - follows the mouse while it stays over the control,
 *  - disappears as soon as the mouse leaves.
 *
 * The OS cursor is left completely untouched.
 * No external libraries required — pure AWT/Swing.
 */
public class FloatingHintDemo extends JFrame {

    // -------------------------------------------------------------------------
    // Floating badge window
    // -------------------------------------------------------------------------

    /**
     * A borderless, fully transparent top-level window that paints a rounded,
     * semi-transparent badge containing any single character (default "?").
     *
     * Call attach(component) to wire it to a JComponent automatically.
     */
    static class FloatingBadge {

        private static final int  SIZE   = 20;          // badge diameter in px
        private static final int  OFFSET = 10;          // distance from cursor tip

        private final JWindow window;

        FloatingBadge(String symbol) {
            window = new JWindow();

            // Transparent window background — essential for the rounded look.
            window.setBackground(new Color(0, 0, 0, 0));
            window.setSize(SIZE, SIZE);
            window.setAlwaysOnTop(true);
            window.setFocusableWindowState(false);  // never steal keyboard focus

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);

                    // Semi-transparent filled circle
                    g2.setColor(new Color(255, 220, 60, 200));   // amber, ~80 % opaque
                    g2.fillOval(1, 1, SIZE - 3, SIZE - 3);

                    // Thin border
                    g2.setColor(new Color(160, 100, 0, 220));
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawOval(1, 1, SIZE - 3, SIZE - 3);

                    // Symbol
                    g2.setColor(new Color(80, 40, 0, 255));
                    g2.setFont(new Font("SansSerif", Font.BOLD, 15));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (SIZE - fm.stringWidth(symbol)) / 2;
                    int ty = (SIZE - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(symbol, tx, ty);

                    g2.dispose();
                }
            };
            panel.setOpaque(false);
            window.setContentPane(panel);
        }

        /** Moves the badge to just south-east of the given screen coordinate. */
        void moveTo(Point screenPt) {
            window.setLocation(screenPt.x + OFFSET, screenPt.y + OFFSET);
        }

        void show() { window.setVisible(true); }
        void hide() { window.setVisible(false); }

        // ---------------------------------------------------------------------
        // Wiring — attach to any JComponent with one call
        // ---------------------------------------------------------------------

        /**
         * Attaches this badge to {@code target}: the badge appears when the mouse
         * enters, follows as the mouse moves, and disappears on exit.
         */
        void attach(JComponent target) {
            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    moveTo(e.getLocationOnScreen());
                    show();
                }
                @Override
                public void mouseMoved(MouseEvent e) {
                    moveTo(e.getLocationOnScreen());
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    hide();
                }
                // Also hide while the button is held down and the cursor drifts out
                @Override
                public void mouseDragged(MouseEvent e) {
                    moveTo(e.getLocationOnScreen());
                }
            };
            target.addMouseListener(adapter);
            target.addMouseMotionListener(adapter);
        }
    }

    // -------------------------------------------------------------------------
    // Demo frame
    // -------------------------------------------------------------------------

    public FloatingHintDemo() {
        setTitle("Floating Hint Badge Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 280);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("Hi, this is a floating hint badge testbed");
        title.setFont(new Font("SansSerif", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 28, 0);
        panel.add(title, gbc);

        JButton button = new JButton("Hover over me for the hint");
        button.setPreferredSize(new Dimension(240, 40));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(button, gbc);

        // One line to attach the badge to the button.
        // Swap "?" for "i", "!", or any other symbol you like.
        new FloatingBadge("?").attach(button);

        add(panel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FloatingHintDemo::new);
    }
}
