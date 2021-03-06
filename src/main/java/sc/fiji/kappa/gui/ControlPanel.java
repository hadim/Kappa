/*
 * #%L
 * A Fiji plugin for Curvature Analysis.
 * %%
 * Copyright (C) 2016 - 2020 Gary Brouhard
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package sc.fiji.kappa.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import sc.fiji.kappa.curve.Curve;

public class ControlPanel extends JPanel {
	// Scaling Constants
	// MAX_AVG_PIXEL_DIM is a maximal average of an image's height and width. It's
	// used to determine
	// how much an image can be zoomed into (without running into heap issues)

	public static final int MAX_AVG_PIXEL_DIM = 2000;
	public static final int MIN_SCALE = 25;
	public static final int MAX_SCALE = 800;
	public static final int DEFAULT_SCALE = 100;
	public static final int DIGITS_MAX_SCALE = Integer.toString(MAX_SCALE).length();
	public static double[] SCALE_INCREMENTS = { 1 / 4.0, 1 / 2.0, 2 / 3.0, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0,
			8.0 };

	// Keyframe marker constants
	final int SLIDER_OFFSET_OSX = 13;
	final int SLIDER_OFFSET_WIN = 7;
	final int KEYFRAME_MARKER_X_SIZE = 1;
	final int KEYFRAME_MARKER_Y_OFFSET_OSX = 24;
	final int KEYFRAME_MARKER_Y_OFFSET = 25;
	final int KEYFRAME_MARKER_Y_SIZE = 7;

	// Control Panel Sliders
	private JSlider scaleSlider;
	private JSlider currentLayerSlider;

	// Buttons for the image channels to be displayed.
	private JToggleButton[] channelButtons = new JToggleButton[3];
	final static String[] CHANNEL_TOOLTIPS = { "Display Red Channel", "Display Green Channel", "Display Blue Channel" };
	final static String[] CHANNEL_FILENAMES = { "red.jpg", "green.jpg", "blue.jpg" };

	private static final long serialVersionUID = 1L;
	private JLabel layerLabel, scaleLabel;

	private KappaFrame frame;

	// Constructs a new InputPanel object
	public ControlPanel(KappaFrame frame) {

		this.frame = frame;

		setBackground(KappaFrame.PANEL_COLOR);
		setPreferredSize(new Dimension(0, 55));
		setBorder(BorderFactory.createLineBorder(Color.GRAY));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

		// Create the slider and a label for it
		setCurrentLayerSlider(new JSlider(SwingConstants.HORIZONTAL, 1, 200, frame.getINIT_LAYER()));
		layerLabel = new JLabel("");
		layerLabel.setPreferredSize(new Dimension(65, Short.MAX_VALUE));
		getCurrentLayerSlider().addChangeListener(new LayerChanger());
		getCurrentLayerSlider().setPaintTicks(false);
		getCurrentLayerSlider().setEnabled(false);

		// Add the slider and the label to the control panel
		this.add(Box.createRigidArea(new Dimension(15, 0)));
		this.add(getCurrentLayerSlider());
		this.add(Box.createRigidArea(new Dimension(8, 0)));
		this.add(layerLabel);

		// Creates the toggle buttons for the image channels
		for (int i = 0; i < channelButtons.length; i++) {
			ImageIcon channelIcon = new ImageIcon(Panel.class.getResource("/icons/" + CHANNEL_FILENAMES[i]));
			channelButtons[i] = new JToggleButton(channelIcon);
			channelButtons[i].setEnabled(false);
			channelButtons[i].setToolTipText(CHANNEL_TOOLTIPS[i]);
			channelButtons[i].addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					frame.setDisplayedChannels(channelButtons[0].isSelected(), channelButtons[1].isSelected(),
							channelButtons[2].isSelected());
				}
			});
		}

		// Adds another slider for the scale factor
		setScaleSlider(new JSlider(SwingConstants.HORIZONTAL, MIN_SCALE, MAX_SCALE, DEFAULT_SCALE));
		getScaleSlider().setMajorTickSpacing(100);
		getScaleSlider().setMinorTickSpacing(50);
		getScaleSlider().setPaintTicks(true);
		getScaleSlider().setMaximumSize(new Dimension(200, Short.MAX_VALUE));
		getScaleSlider().addChangeListener(new ScaleChanger());
		scaleLabel = new JLabel(frame.formatNumber(DEFAULT_SCALE, DIGITS_MAX_SCALE));
		scaleLabel.setPreferredSize(new Dimension(40, Short.MAX_VALUE));
		getScaleSlider().setEnabled(false);

		this.add(Box.createHorizontalGlue());
		this.add(Box.createHorizontalGlue());
		this.add(new JLabel("SCALE: "));
		this.add(getScaleSlider());
		this.add(Box.createRigidArea(new Dimension(4, 0)));
		this.add(scaleLabel);
		addSpacer(25);
		this.add(new JLabel("CHANNELS:"));
		this.add(Box.createRigidArea(new Dimension(8, 0)));
		for (JToggleButton b : channelButtons) {
			this.add(b);
		}
		this.add(Box.createRigidArea(new Dimension(8, 0)));
	}

	/**
	 * Repaint the control panel
	 *
	 * @param g
	 *            The Graphics context
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Draws keyframe markers. Position affected by Swing UI style, which differs
		// between OS X and Windows
		Rectangle bounds = getCurrentLayerSlider().getBounds();
		if (frame.getCurves().getNoSelected() == 1) {
			g.setColor(Color.RED);
			Curve currentCurve = frame.getCurves().getSelected()[0];
			int[] keyframeLayers = currentCurve.getKeyframeLayers();
			for (int frameLayer : keyframeLayers) {
				int centerX;
				if (System.getProperty("os.name").equals("Mac OS X")) {
					centerX = bounds.x + SLIDER_OFFSET_OSX + (int) ((frameLayer - 1.0)
							/ (getCurrentLayerSlider().getMaximum() - 1) * (bounds.width - 2 * SLIDER_OFFSET_OSX));
				} else {
					centerX = bounds.x + SLIDER_OFFSET_WIN + (int) ((frameLayer - 1.0)
							/ (getCurrentLayerSlider().getMaximum() - 1) * (bounds.width - 2 * SLIDER_OFFSET_WIN));
				}
				g.fillRect(centerX - KEYFRAME_MARKER_X_SIZE, bounds.y + KEYFRAME_MARKER_Y_OFFSET,
						2 * KEYFRAME_MARKER_X_SIZE, KEYFRAME_MARKER_Y_SIZE);
			}
		}
	}

	private void addSpacer(int spaceSize) {
		this.add(Box.createRigidArea(new Dimension(spaceSize, 0)));
		JSeparator spacer = new JSeparator(SwingConstants.VERTICAL);
		spacer.setMaximumSize(new Dimension(10, 35));
		this.add(spacer);
		this.add(Box.createRigidArea(new Dimension(spaceSize, 0)));
	}

	// Modifies the TIFF layer in response to the value on the slider
	private class LayerChanger implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent ce) {
			layerLabel.setText(frame.formatNumber(getCurrentLayerSlider().getValue(), frame.getMaxLayerDigits()) + " / "
					+ frame.getMaxLayer());
			frame.setLayer(getCurrentLayerSlider().getValue(), getScaleSlider().getValue() / 100.0);

			JSlider source = (JSlider) ce.getSource();
			if (!source.getValueIsAdjusting()) {
				frame.getInfoPanel().updateHistograms();
			}

		}
	}

	private class ScaleChanger implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent ce) {
			scaleLabel.setText(frame.formatNumber(getScaleSlider().getValue(), DIGITS_MAX_SCALE) + "%");
			frame.setScaledImage(getScaleSlider().getValue() / 100.0);
			frame.drawImageOverlay();
		}
	}

	public JToggleButton[] getChannelButtons() {
		return channelButtons;
	}

	public JSlider getScaleSlider() {
		return scaleSlider;
	}

	public void setScaleSlider(JSlider scaleSlider) {
		this.scaleSlider = scaleSlider;
	}

	public JSlider getCurrentLayerSlider() {
		return currentLayerSlider;
	}

	public void setCurrentLayerSlider(JSlider currentLayerSlider) {
		this.currentLayerSlider = currentLayerSlider;
	}

}
