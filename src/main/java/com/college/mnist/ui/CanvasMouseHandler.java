package com.college.mnist.ui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * CanvasMouseHandler translates mouse (and touch) events into drawing commands
 * on a DrawingCanvas.
 *
 * It is a separate named class instead of an anonymous inner class or a lambda
 * so that the EVENT DELEGATION MODEL is easy to point at in the viva:
 *   - DrawingCanvas is the event source; it registers this handler with
 *     addMouseListener() and addMouseMotionListener();
 *   - the handler is the event listener; it implements the two listener
 *     interfaces;
 *   - Swing (the event dispatch thread) calls the methods below when the user
 *     presses, drags or releases the mouse.
 *
 * Only this class knows that a "click" has to be turned into a brush stroke,
 * so DrawingCanvas stays a small class that only stores and paints the image.
 */
public class CanvasMouseHandler implements MouseListener, MouseMotionListener {

    /** The canvas that is being drawn on. */
    private DrawingCanvas canvas;

    /** Previous x position, needed to join two points with a line. */
    private int previousX = -1;

    /** Previous y position, needed to join two points with a line. */
    private int previousY = -1;

    /**
     * Creates the handler for one canvas.
     *
     * @param canvas the canvas whose events are handled
     */
    public CanvasMouseHandler(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Starts a stroke: a single dot is painted immediately so that a short tap
     * also leaves a mark.
     *
     * @param event mouse pressed event
     */
    @Override
    public void mousePressed(MouseEvent event) {
        if (canvas.isDrawingEnabled() == false) {
            return;
        }
        // Convert the pointer position into picture coordinates, so the stroke
        // starts exactly where the user pressed.
        previousX = canvas.toImageCoordinate(event.getX());
        previousY = canvas.toImageCoordinate(event.getY());
        canvas.drawStroke(previousX, previousY, previousX, previousY);
    }

    /**
     * Continues a stroke while the button is held down.
     *
     * @param event mouse dragged event
     */
    @Override
    public void mouseDragged(MouseEvent event) {
        if (canvas.isDrawingEnabled() == false) {
            return;
        }
        int currentX = canvas.toImageCoordinate(event.getX());
        int currentY = canvas.toImageCoordinate(event.getY());
        canvas.drawStroke(previousX, previousY, currentX, currentY);
        previousX = currentX;
        previousY = currentY;
    }

    /**
     * Ends a stroke, so the next press starts a new one.
     *
     * @param event mouse released event
     */
    @Override
    public void mouseReleased(MouseEvent event) {
        previousX = -1;
        previousY = -1;
    }

    /** Not used, but required by the MouseListener interface. */
    @Override
    public void mouseClicked(MouseEvent event) {
        // A click is already handled by mousePressed, so nothing is done here.
    }

    /** Not used, but required by the MouseListener interface. */
    @Override
    public void mouseEntered(MouseEvent event) {
        // The brush does not change when the mouse enters the canvas.
    }

    /** Not used, but required by the MouseListener interface. */
    @Override
    public void mouseExited(MouseEvent event) {
        // Leaving the canvas does not interrupt a stroke.
    }

    /** Not used, but required by the MouseMotionListener interface. */
    @Override
    public void mouseMoved(MouseEvent event) {
        // Drawing only happens while the button is pressed.
    }
}
