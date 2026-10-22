package com.college.mnist.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * PreviewPanel shows what the network actually receives: the 28x28 image that
 * ImageProcessor produced from the 280x280 canvas.
 *
 * INHERITANCE: like DrawingCanvas it extends JPanel and overrides
 * paintComponent(). The stored image is only 28x28 pixels; it is drawn with
 * nearest neighbour scaling, so each small pixel becomes a visible 10x10 square
 * instead of a blurred smear. Seeing this preview is the easiest way to check
 * that the digit is centred and thick enough before predicting.
 */
public class PreviewPanel extends JPanel {

    /** Required by Swing: identifies the serialised form of a component. */
    private static final long serialVersionUID = 1L;

    /** The 28x28 image to show. */
    private BufferedImage previewImage;

    /** Display size in pixels. */
    private final int displaySize;

    /**
     * Creates an empty (black) preview.
     *
     * @param displaySize width and height of the panel in pixels, e.g. 140
     */
    public PreviewPanel(int displaySize) {
        this.displaySize = displaySize;

        setPreferredSize(new Dimension(displaySize, displaySize));
        setBackground(Color.BLACK);
        setOpaque(true);

        previewImage = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
        clearPreview();
    }

    /**
     * Replaces the shown image.
     *
     * @param smallImage a 28x28 image, normally produced from the canvas
     */
    public void setPreviewImage(BufferedImage smallImage) {
        if (smallImage != null) {
            previewImage = smallImage;
        }
        repaint();
    }

    /**
     * Fills the preview with black, used at start up and by Clear.
     */
    public void clearPreview() {
        Graphics graphics = previewImage.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, previewImage.getWidth(), previewImage.getHeight());
        graphics.dispose();
        repaint();
    }

    /**
     * Draws the 28x28 image enlarged to the panel size.
     *
     * @param graphics the graphics context supplied by Swing
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.drawImage(previewImage, 0, 0, displaySize, displaySize, null);
    }
}