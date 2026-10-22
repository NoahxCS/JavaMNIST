package com.college.mnist;

import com.college.mnist.db.DatabaseManager;
import com.college.mnist.db.HistoryRecord;
import com.college.mnist.exception.PredictionException;
import com.college.mnist.model.DigitClassifier;
import com.college.mnist.model.Dl4jDigitClassifier;
import com.college.mnist.model.Prediction;
import com.college.mnist.util.ImageProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * TestCases runs the eight sample test cases that are listed in
 * DOCUMENTATION.md. It is a plain console program (no JUnit), so it can be run
 * with a single Maven command and the output can be pasted into the report:
 *
 *     mvn exec:java -Dexec.mainClass=com.college.mnist.TestCases
 *
 * Each test method prints one line in the format
 *     Id | Description | Expected output | Pass/Fail
 * and increments a counter when it passes. At the end the summary line shows how
 * many cases passed.
 *
 * Cases 1 to 4 and 8 need no trained model. Cases 5, 6 and 7 need mnist_model.zip
 * and are reported as FAIL when the model has not been trained yet.
 */
public class TestCases {

    /** Counts the test cases that passed. */
    private static int passedCount = 0;

    /** Counts the test cases that were executed. */
    private static int totalCount = 0;

    /**
     * Runs all eight test cases.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        System.out.println("=== MNIST Digit Classifier - test cases ===");
        System.out.println("Format: Id | Description | Expected output | Pass/Fail");
        System.out.println();

        testImageProcessorDownsampling();
        testPredictionEncapsulation();
        testSingleton();
        testDatabaseInsertAndRead();
        testDbErrorHandling();
        testModelFileMissing();
        testModelLoadAndPredict();
        testClearHistory();

        System.out.println();
        System.out.println("Summary: " + passedCount + " of " + totalCount + " test cases passed.");
    }

    /**
     * Test 1 - ImageProcessor.downsample() must turn a 280x280 image into 784
     * values in the range 0..1 and keep the white stroke visible.
     */
    private static void testImageProcessorDownsampling() {
        String id = "TC-01";
        String description = "ImageProcessor.downsample() on a 280x280 image with a white square";
        String expected = "784 values, all in 0..1, and a white block still present";

        BufferedImage image = new BufferedImage(280, 280, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 280, 280);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(100, 100, 80, 80);    // an 8x8 block of the small image
        graphics.dispose();

        double[] pixels = ImageProcessor.downsample(image);

        boolean correctSize = pixels.length == 784;
        boolean inRange = true;
        for (int index = 0; index < pixels.length; index++) {
            if (pixels[index] < 0.0 || pixels[index] > 1.0) {
                inRange = false;
            }
        }

        // The middle of the white square is small-image pixel (14, 14).
        double middleValue = pixels[14 * 28 + 14];
        boolean whiteKept = middleValue > 0.9;

        boolean pass = correctSize && inRange && whiteKept;
        report(id, description, expected, pass,
                "784 values, middle value " + String.format("%.3f", middleValue));
    }

    /**
     * Test 2 - a Prediction object must return exactly the values given to its
     * constructor (encapsulation through getters).
     */
    private static void testPredictionEncapsulation() {
        String id = "TC-02";
        String description = "Prediction stores digit and confidence and returns them unchanged";
        String expected = "digit = 7 and confidence = 98.6";

        Prediction prediction = new Prediction(7, 98.6);
        boolean pass = prediction.getDigit() == 7
                && Math.abs(prediction.getConfidence() - 98.6) < 0.0001;

        report(id, description, expected, pass,
                "digit = " + prediction.getDigit()
                + ", confidence = " + prediction.getConfidence());
    }

    /**
     * Test 3 - DatabaseManager must be a Singleton: two calls to getInstance()
     * return the same object.
     */
    private static void testSingleton() {
        String id = "TC-03";
        String description = "DatabaseManager.getInstance() called twice returns one object";
        String expected = "both references are the same object (singleton)";

        try {
            DatabaseManager first = DatabaseManager.getInstance();
            DatabaseManager second = DatabaseManager.getInstance();
            boolean pass = first == second;

            report(id, description, expected, pass,
                    pass ? "same instance" : "two different instances");

        } catch (PredictionException databaseProblem) {
            report(id, description, expected, false, databaseProblem.getMessage());
        }
    }

    /**
     * Test 4 - insertPrediction() followed by getLastDigits() must return the
     * stored values, newest first.
     */
    private static void testDatabaseInsertAndRead() {
        String id = "TC-04";
        String description = "insertPrediction() then getLastDigits(2)";
        String expected = "2 rows, newest first: digit 3 then digit 1";

        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.clearHistory();
            databaseManager.insertPrediction(1, 91.5);
            databaseManager.insertPrediction(3, 88.0);

            List<HistoryRecord> records = databaseManager.getLastDigits(2);
            boolean pass = records.size() == 2
                    && records.get(0).getDigit() == 3
                    && records.get(1).getDigit() == 1;

            String actual = records.size() + " rows, first digit " + records.get(0).getDigit()
                    + ", second digit " + records.get(1).getDigit();
            report(id, description, expected, pass, actual);

        } catch (PredictionException databaseProblem) {
            report(id, description, expected, false, databaseProblem.getMessage());
        }
    }

    /**
     * Test 5 - getLastDigits() with a valid request must never hand back a broken
     * object, and asking for an impossible number of rows must still be handled
     * without an exception (database error handling with try/catch).
     */
    private static void testDbErrorHandling() {
        String id = "TC-05";
        String description = "getLastDigits(3) on an empty table and getHistoryCount()";
        String expected = "empty list, no exception, count = 0";

        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.clearHistory();

            List<HistoryRecord> records = databaseManager.getLastDigits(3);
            int count = databaseManager.getHistoryCount();
            boolean pass = records.isEmpty() && count == 0;

            report(id, description, expected, pass,
                    records.size() + " rows, count = " + count);

        } catch (PredictionException databaseProblem) {
            report(id, description, expected, false, databaseProblem.getMessage());
        }
    }

    /**
     * Test 6 - loading a model that does not exist must raise the custom
     * PredictionException with the "run ModelTrainer" message, not a crash.
     */
    private static void testModelFileMissing() {
        String id = "TC-06";
        String description = "Dl4jDigitClassifier.loadFromDisk() with no mnist_model.zip";
        String expected = "PredictionException with the message 'run ModelTrainer'";

        java.io.File modelFile = new java.io.File(Dl4jDigitClassifier.MODEL_FILE);
        if (modelFile.exists()) {
            report(id, description, expected, true,
                    "model file present, so the missing-file path cannot be shown now");
            return;
        }

        try {
            Dl4jDigitClassifier.loadFromDisk();
            report(id, description, expected, false, "no exception was raised");

        } catch (PredictionException expectedProblem) {
            boolean pass = expectedProblem.getMessage().contains("ModelTrainer");
            report(id, description, expected, pass, "PredictionException raised");
        }
    }

    /**
     * Test 7 - with a trained model present, classify() must return a digit
     * between 0 and 9 with a confidence between 0 and 100 percent.
     */
    private static void testModelLoadAndPredict() {
        String id = "TC-07";
        String description = "Dl4jDigitClassifier.classify() on a simple drawn stroke";
        String expected = "a digit 0..9 with confidence 0..100 %";

        java.io.File modelFile = new java.io.File(Dl4jDigitClassifier.MODEL_FILE);
        if (modelFile.exists() == false) {
            report(id, description, expected, false,
                    "mnist_model.zip not found - run ModelTrainer first");
            return;
        }

        try {
            DigitClassifier classifier = Dl4jDigitClassifier.loadFromDisk();

            // Draw a thick vertical stroke in the middle: a very rough "1".
            BufferedImage image = new BufferedImage(280, 280, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, 280, 280);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(130, 40, 22, 200);
            graphics.dispose();

            double[] pixels = ImageProcessor.downsample(image);
            Prediction prediction = classifier.classify(pixels);

            boolean digitOk = prediction.getDigit() >= 0 && prediction.getDigit() <= 9;
            boolean confidenceOk = prediction.getConfidence() >= 0.0
                    && prediction.getConfidence() <= 100.0;
            boolean pass = digitOk && confidenceOk;

            report(id, description, expected, pass,
                    "digit " + prediction.getDigit()
                    + ", confidence " + String.format("%.1f", prediction.getConfidence()) + " %");

        } catch (PredictionException problem) {
            report(id, description, expected, false, problem.getMessage());
        }
    }

    /**
     * Test 8 - clearHistory() must remove every row (the delete operation of
     * CRUD, used by the report and by these test cases).
     */
    private static void testClearHistory() {
        String id = "TC-08";
        String description = "clearHistory() removes all rows inserted before";
        String expected = "count = 0 after clearHistory()";

        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.clearHistory();
            databaseManager.insertPrediction(5, 77.7);
            int before = databaseManager.getHistoryCount();

            databaseManager.clearHistory();
            int after = databaseManager.getHistoryCount();

            boolean pass = before == 1 && after == 0;
            report(id, description, expected, pass,
                    "count before = " + before + ", after = " + after);

        } catch (PredictionException databaseProblem) {
            report(id, description, expected, false, databaseProblem.getMessage());
        }
    }

    /**
     * Prints one result line and counts the outcome.
     *
     * @param id          test case id, e.g. TC-01
     * @param description what the case checks
     * @param expected    the expected output
     * @param pass        true when the case passed
     * @param actual      what really happened
     */
    private static void report(String id, String description, String expected,
                               boolean pass, String actual) {
        totalCount++;
        if (pass) {
            passedCount++;
        }
        String outcome = pass ? "PASS" : "FAIL";
        System.out.println(id + " | " + description + " | " + expected + " | " + outcome);
        System.out.println("      actual: " + actual);
    }
}