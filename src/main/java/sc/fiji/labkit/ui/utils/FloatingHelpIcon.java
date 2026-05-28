package sc.fiji.labkit.ui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A borderless, fully transparent top-level window that paints a rounded,
 * semi-transparent badge containing any single character (default "?").
 * <p>
 * Call attachTo(component) to wire it to a JComponent automatically.
 */
public class FloatingHelpIcon {

	private static final int SIZE = 20;          // badge diameter in px
	private static final int OFFSET = 10;        // distance from cursor tip

	private final JWindow window;
	private boolean shouldShow = false;

	public FloatingHelpIcon() {
		this("?");
	}

	public FloatingHelpIcon(String symbol) {
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

	/**
	 * Moves the badge to just south-east of the given screen coordinate.
	 */
	private void moveTo(Point screenPt) {
		window.setLocation(screenPt.x + OFFSET, screenPt.y + OFFSET);
	}

	private void show() {
		window.setVisible(true);
	}

	private void hide() {
		window.setVisible(false);
	}

	/**
	 * Enables showing of the icon over the components
	 * to which it is attached.
	 */
	public void enableIcon() {
		shouldShow = true;
	}

	/**
	 * Disables showing of the icon over the components
	 * to which it is attached.
	 */
	public void disableIcon() {
		shouldShow = false;
	}

	public boolean isIconEnabled() {
		return shouldShow;
	}

	/**
	 * Attaches this badge to {@code target}: the badge appears when the mouse
	 * enters, follows as the mouse moves, and disappears on exit.
	 */
	public void attachTo(JComponent target) {
		target.addMouseListener(adapter);
		target.addMouseMotionListener(adapter);
	}

	/**
	 * Attaches this badge to {@code target}, keeping it visible when the cursor
	 * moves into any inner/child component of {@code target}. A {@code mouseExited}
	 * event only hides the badge when the cursor has truly left the bounds of
	 * {@code target}, preventing the brief blink that would otherwise occur at
	 * child-component boundaries (unless the child has this badge attached as well).
	 */
	public void attachToKeepOverInnerOf(JComponent target) {
		target.addMouseListener(persistentAdapter);
		target.addMouseMotionListener(persistentAdapter);
	}

	private final MouseAdapter adapter = new MouseAdapter() {
		@Override
		public void mouseEntered(MouseEvent e) {
			if (shouldShow) {
				moveTo(e.getLocationOnScreen());
				show();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (shouldShow)
				moveTo(e.getLocationOnScreen());
		}

		@Override
		public void mouseExited(MouseEvent e) {
			//always hide (consider disabling showing in the middle of a mouse-over episode)
			hide();
		}

		// Also hide while the button is held down and the cursor drifts out
		@Override
		public void mouseDragged(MouseEvent e) {
			if (shouldShow)
				moveTo(e.getLocationOnScreen());
		}
	};

	private final MouseAdapter persistentAdapter = new MouseAdapter() {
		@Override
		public void mouseEntered(MouseEvent e) {
			if (shouldShow) {
				moveTo(e.getLocationOnScreen());
				show();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (shouldShow)
				moveTo(e.getLocationOnScreen());
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// Only hide when the cursor has truly left the component's area;
			// a non-null getMousePosition(true) means it moved into a child.
			if (((JComponent) e.getComponent()).getMousePosition(true) == null)
				hide();
		}

		// Also hide while the button is held down and the cursor drifts out
		@Override
		public void mouseDragged(MouseEvent e) {
			if (shouldShow)
				moveTo(e.getLocationOnScreen());
		}
	};
}
