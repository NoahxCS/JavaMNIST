package com.college.mnist.ui;

import com.college.mnist.util.ImageProcessor;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * DrawingCanvas is the 280x280 area where the user draws a digit with the mouse
 * or with a finger on a touch screen.
 *
 * INHERITANCE: it extends JPanel, so it already is a Swing component and can be
 * added to a window. Only the behaviour that is specific to drawing is added:
 *   - a black background image of 280x280 pixels that keeps the strokes,
 *   - drawStroke() which paints a thick white line between two mouse positions,
 *   - getImage() so the rest of the program can read the picture,
 *   - clear() to wipe the canvas.
 *
 * The mouse events themselves are handled by CanvasMouseHandler, which is
 * registered here. That separation is the Swing event delegation model.
 */
public class DrawingCanvas extends JPanel {

    /** Required by Swing: identifies the serialised form of a component. */
    private static final long serialVersionUID = 1L;

    /** Side of the canvas in pixels (280 = 28 * 10, so 10x10 blocks divide evenly). */
    public static final int CANVAS_SIZE = 280;

    /** Brush thickness in pixels. */
    public static final int BRUSH_SIZE = 20;

    /** The image that stores the strokes. */
    private final BufferedImage image;

    /** False while a prediction is running, so the drawing cannot be changed. */
    private boolean drawingEnabled = true;

    /**
     * Creates an empty black canvas and connects the mouse handler.
     */
    public DrawingCanvas() {
        image = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);

        setPreferredSize(new Dimension(CANVAS_SIZE, CANVAS_SIZE));
        setBackground(Color.BLACK);
        setOpaque(true);

        clear();

        // Event delegation: the canvas publishes the events, the handler reacts.
        CanvasMouseHandler handler = new CanvasMouseHandler(this);
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    /**
     * Paints the stored image. Swing calls this method whenever the component
     * needs to be redrawn (first shown, uncovered, resized ...).
     *
     * The picture is drawn over the whole (square) canvas, which is the same
     * scaling drawStroke() uses, so what the user sees is what is stored.
     *
     * @param graphics the graphics context supplied by Swing
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }

    /**
     * Draws one brush stroke between two points. Mouse events do not arrive at
     * every single pixel, so joining the previous point with the current one by
     * a thick line gives a smooth stroke instead of separate dots.
     *
     * The points are component coordinates and are converted to picture
     * coordinates first, so the stroke always appears exactly under the pointer
     * even if the canvas is shown at a different size (for example on a scaled
     * display). The Graphics is also scaled with the same factor.
     *
     * @param fromX x of the previous mouse position
     * @param fromY y of the previous mouse position
     * @param toX   x of the current mouse position
     * @param toY   y of the current mouse position
     */
    public void drawStroke(int fromX, int fromY, int toX, int toY) {
        double factor = getScaleFactor();

        Graphics2D graphics = image.createGraphics();
        graphics.scale(factor, factor);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(BRUSH_SIZE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(fromX, fromY, toX, toY);

        graphics.dispose();     // always release the graphics context of an image
        repaint();              // ask Swing to show the new pixels
    }

    /**
     * Converts one coordinate of the canvas into a coordinate of the picture.
     *
     * @param componentCoordinate x or y measured on the component
     * @return the matching coordinate inside the BufferedImage
     */
    public int toImageCoordinate(int componentCoordinate) {
        return (int) Math.round(componentCoordinate / getScaleFactor());
    }

    /**
     * @return how many pixels of the picture one pixel of the component shows,
     *         1.0 when the canvas is displayed at its natural size
     */
    public double getScaleFactor() {
        // A square canvas is intended, so the width is used for both directions.
        if (getWidth() <= 0 || getHeight() <= 0) {
            return 1.0;
        }
        return (double) getWidth() / (double) image.getWidth();
    }

    /**
     * Clears the canvas back to black.
     */
    public void clear() {
        Graphics graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        graphics.dispose();
        repaint();
    }

    /**
     * @return the 280x280 image with the drawing
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Convenience method that shrinks the drawing to the 784 values the network
     * needs. It simply delegates to ImageProcessor, so the averaging loops live
     * in the util package and not in the GUI.
     *
     * @return 784 normalised pixel values in 0..1
     */
    public double[] getNormalisedPixels() {
        return ImageProcessor.downsample(image);
    }

    /** @return true while the user is allowed to draw. */
    public boolean isDrawingEnabled() {
        return drawingEnabled;
    }

    /**
     * Enables or disables drawing.
     *
     * @param drawingEnabled false to lock the canvas, for example while the
     *                       network is working
     */
    public void setDrawingEnabled(boolean drawingEnabled) {
        this.drawingEnabled = drawingEnabled;
    }
}