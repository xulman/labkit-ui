package sc.fiji.labkit.ui.utils;

import javax.swing.*;
import java.awt.*;

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

    private FloatingHelpIcon icon = new FloatingHelpIcon();

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
        button.addActionListener(l -> {
            if (icon.isIconEnabled())
                icon.disableIcon();
            else
                icon.enableIcon();
        });
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(button, gbc);

        // One line to attach the badge to the button.
        // Swap "?" for "i", "!", or any other symbol you like.
        icon.attachTo(button);

        add(panel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FloatingHintDemo::new);
    }
}
