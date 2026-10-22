package com.college.mnist;

import com.college.mnist.db.DatabaseManager;
import com.college.mnist.exception.PredictionException;
import com.college.mnist.model.DigitClassifier;
import com.college.mnist.model.Dl4jDigitClassifier;
import com.college.mnist.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Main starts the desktop application. It is intentionally short: create the
 * objects, hand them to the window, show the window.
 *
 * Start up order (this is also the object graph of the program):
 *   1. one DatabaseManager (Singleton) opens digits.db and creates the table;
 *   2. one Dl4jDigitClassifier loads mnist_model.zip;
 *   3. MainFrame receives both and builds the GUI;
 *   4. the window is shown on the Swing event dispatch thread.
 *
 * If the model file is missing, the user gets one clear message telling them to
 * run ModelTrainer first, and the program stops - the GUI never trains a model.
 */
public class Main {

    /**
     * Program entry point.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        // Ask Swing for the look of the operating system (nicer buttons on Windows).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The default look and feel is good enough if this fails.
        }

        try {
            // Step 1: the database (Singleton - only one connection exists).
            DatabaseManager databaseManager = DatabaseManager.getInstance();

            // Step 2: the classifier, loaded once and reused for every prediction.
            DigitClassifier classifier = Dl4jDigitClassifier.loadFromDisk();

            // Step 3: the user interface.
            MainFrame frame = new MainFrame(databaseManager, classifier);
            frame.setVisible(true);

        } catch (PredictionException problem) {
            // Missing model file or unusable database: explain it and stop.
            JOptionPane.showMessageDialog(null, problem.getMessage(),
                    "MNIST Digit Classifier", JOptionPane.ERROR_MESSAGE);
            System.out.println("Start up failed: " + problem.getMessage());
        }
    }
}