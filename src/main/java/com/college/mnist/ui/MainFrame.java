package com.college.mnist.ui;

import com.college.mnist.db.DatabaseManager;
import com.college.mnist.exception.PredictionException;
import com.college.mnist.model.DigitClassifier;
import com.college.mnist.model.Prediction;
import com.college.mnist.util.ImageProcessor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * MainFrame is the single window of the application. It builds the layout, wires
 * the two buttons and runs the predict flow.
 *
 * The window is kept deliberately simple and uses only BorderLayout, FlowLayout
 * and BoxLayout:
 *
 *   CENTER - DrawingCanvas   (280x280, the user draws here)
 *   EAST   - PreviewPanel    (the 28x28 input, enlarged)
 *            the result label
 *            the Predict and Clear buttons
 *   SOUTH  - HistoryPanel    (recent digits strip + expandable table)
 *
 * OOP points shown by this class:
 *   - POLYMORPHISM - the field classifier is typed as the interface
 *     DigitClassifier, so this class never imports Deeplearning4j;
 *   - INHERITANCE - MainFrame extends JFrame;
 *   - EVENT DELEGATION - the buttons get small NAMED listener classes
 *     (PredictListener, ClearListener, HistoryToggleListener) instead of
 *     anonymous classes or lambdas, so each button action has a clear name.
 */
public class MainFrame extends JFrame {

    /** Required by Swing: identifies the serialised form of a frame. */
    private static final long serialVersionUID = 1L;

    /** The classifier used for predictions. Interface type = polymorphism. */
    private final DigitClassifier classifier;

    /** The shared database object. */
    private final DatabaseManager databaseManager;

    /** Where the user draws. */
    private final DrawingCanvas canvas;

    /** Enlarged view of the 28x28 network input. */
    private final PreviewPanel previewPanel;

    /** Shows the predicted digit and the confidence. */
    private final JLabel resultLabel;

    /** Predict button. */
    private final JButton predictButton;

    /** Clear button. */
    private final JButton clearButton;

    /** Bottom strip with the recent digits and the expandable table. */
    private final HistoryPanel historyPanel;

    /**
     * Creates the window, the components and the listeners, but does not show
     * it. Main calls setVisible(true).
     *
     * @param databaseManager the shared database object
     * @param classifier      the loaded classifier
     */
    public MainFrame(DatabaseManager databaseManager, DigitClassifier classifier) {
        this.databaseManager = databaseManager;
        this.classifier = classifier;

        // ----- window basics ---------------------------------------------------
        setTitle("MNIST Digit Classifier");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // ----- centre: the drawing canvas --------------------------------------
        // The canvas is wrapped in a panel that centres it and gives it a small
        // margin. That keeps the drawing area an exact 280x280 square: a plain
        // BorderLayout CENTER would stretch it, and then the brush would not sit
        // under the mouse pointer any more.
        canvas = new DrawingCanvas();

        JPanel canvasHolder = new JPanel(new GridBagLayout());
        canvasHolder.add(canvas, new GridBagConstraints());
        canvasHolder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(canvasHolder, BorderLayout.CENTER);

        // ----- right: preview, result and buttons ------------------------------
        previewPanel = new PreviewPanel(280);

        resultLabel = new JLabel("Digit: -   Confidence: -", SwingConstants.CENTER);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        resultLabel.setForeground(Color.BLACK);

        predictButton = new JButton("Predict");
        predictButton.setPreferredSize(new Dimension(120, 32));
        clearButton = new JButton("Clear");
        clearButton.setPreferredSize(new Dimension(120, 32));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.add(predictButton);
        buttonPanel.add(clearButton);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(previewPanel, BorderLayout.CENTER);

        JPanel bottomOfRight = new JPanel(new BorderLayout(5, 5));
        bottomOfRight.add(resultLabel, BorderLayout.NORTH);
        bottomOfRight.add(buttonPanel, BorderLayout.SOUTH);
        rightPanel.add(bottomOfRight, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);

        // ----- bottom: the history strip and the table -------------------------
        historyPanel = new HistoryPanel(databaseManager, this);
        add(historyPanel, BorderLayout.SOUTH);

        // ----- listeners (named classes, one per button) -----------------------
        predictButton.addActionListener(new PredictListener());
        clearButton.addActionListener(new ClearListener());
        historyPanel.getToggleButton().addActionListener(new HistoryToggleListener());

        // pack() asks every component for its preferred size and sizes the
        // window so that nothing is cut off. The table is therefore scrolled,
        // not clipped, and expanding it later is just another pack().
        pack();
        setLocationRelativeTo(null);    // centre the window on the screen
    }

    /**
     * Button listener for Predict: runs the whole predict flow.
     *
     * It is a named inner class, so the ActionListener interface is implemented
     * by a class with a meaningful name instead of a lambda.
     */
    private class PredictListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            // Lock the canvas and the button while the network is working, and
            // always unlock them again with finally - even if an error happens.
            canvas.setDrawingEnabled(false);
            predictButton.setEnabled(false);
            try {
                predictCurrentDrawing();
            } catch (PredictionException problem) {
                showMessage(problem.getMessage());
            } finally {
                canvas.setDrawingEnabled(true);
                predictButton.setEnabled(true);
            }
        }
    }

    /**
     * Button listener for Clear: wipes the canvas, the preview and the result
     * label. The history in the database is not touched.
     */
    private class ClearListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            canvas.clear();
            previewPanel.clearPreview();
            resultLabel.setText("Digit: -   Confidence: -");
            canvas.requestFocusInWindow();
        }
    }

    /**
     * Button listener for "Show all history" / "Hide history": expands or
     * collapses the table and re-packs the window.
     */
    private class HistoryToggleListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            boolean showTable = historyPanel.isExpanded() == false;

            historyPanel.setExpanded(showTable);
            historyPanel.refresh();

            // pack() asks every component for its preferred size again, so the
            // window grows when the table appears and shrinks when it is hidden.
            pack();
            validate();
        }
    }

    /**
     * The predict flow: read the canvas, shrink it to 28x28, show the preview,
     * run the network, show the digit and the confidence, save the row and
     * refresh the history views.
     *
     * @throws PredictionException if the drawing is too small to use, the model
     *                             cannot predict, or the database fails
     */
    private void predictCurrentDrawing() throws PredictionException {
        // 1. Turn the drawing into the 784 values the network expects.
        double[] pixels = canvas.getNormalisedPixels();

        // 2. Build the 28x28 image for the preview from the same values.
        BufferedImage smallImage = createSmallImage(pixels);
        previewPanel.setPreviewImage(smallImage);

        // 3. Refuse to guess on an empty canvas instead of showing a random digit.
        if (isCanvasEmpty(pixels)) {
            showMessage("The canvas is empty. Please draw a digit (white on black) first.");
            return;
        }

        // 4. Ask the classifier. Because the field has the interface type, the
        //    actual work is done by Dl4jDigitClassifier (polymorphism).
        Prediction prediction = classifier.classify(pixels);

        // 5. Show the result: only the digit and the confidence.
        resultLabel.setText("Digit: " + prediction.getDigit()
                + "   Confidence: " + String.format("%.1f", prediction.getConfidence()) + "%");

        // 6. Save it, then reload both history views from the database.
        databaseManager.insertPrediction(prediction.getDigit(), prediction.getConfidence());
        historyPanel.refresh();
    }

    /**
     * Rebuilds a 28x28 greyscale image from the normalised pixel values, so the
     * preview shows exactly what was classified.
     *
     * @param pixels 784 values in 0..1
     * @return a 28x28 image
     */
    private BufferedImage createSmallImage(double[] pixels) {
        BufferedImage smallImage = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);

        for (int row = 0; row < 28; row++) {
            for (int column = 0; column < 28; column++) {
                int brightness = (int) Math.round(pixels[row * 28 + column] * 255.0);
                if (brightness < 0) {
                    brightness = 0;
                }
                if (brightness > 255) {
                    brightness = 255;       // keep the value inside one colour channel
                }
                int grey = (brightness << 16) | (brightness << 8) | brightness;
                smallImage.setRGB(column, row, grey);
            }
        }
        return smallImage;
    }

    /**
     * Checks whether the canvas contains any real stroke.
     *
     * @param pixels 784 values in 0..1
     * @return true when nothing worth classifying was drawn
     */
    private boolean isCanvasEmpty(double[] pixels) {
        double total = 0;
        for (int index = 0; index < pixels.length; index++) {
            total = total + pixels[index];
        }
        // 784 pixels; a total below about 0.1 means (almost) everything is black.
        return total < 0.1;
    }

    /**
     * Shows an error message in a small dialog.
     *
     * @param message text to display
     */
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "MNIST Digit Classifier",
                JOptionPane.WARNING_MESSAGE);
    }
}