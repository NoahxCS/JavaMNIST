package com.college.mnist.model;

import com.college.mnist.exception.PredictionException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;

/**
 * Dl4jDigitClassifier is the concrete implementation of the DigitClassifier
 * interface. It is the only class in the project that knows about
 * Deeplearning4j - everything else only knows the interface.
 *
 * Responsibilities:
 *   1. load the trained model file (mnist_model.zip) once, in the constructor,
 *      so clicking Predict is fast;
 *   2. convert the 784 doubles coming from the GUI into an ND4J tensor;
 *   3. run the network and turn the 10 softmax outputs into a Prediction.
 *
 * Note the {@code implements} keyword: this is INTERFACE IMPLEMENTATION, and
 * because MainFrame keeps its reference typed as DigitClassifier, calling
 * classify() here is runtime POLYMORPHISM.
 */
public class Dl4jDigitClassifier implements DigitClassifier {

    /** Location of the model produced by ModelTrainer. */
    public static final String MODEL_FILE = "mnist_model.zip";

    /** The loaded neural network. */
    private final MultiLayerNetwork network;

    /**
     * Private constructor - outside code must use loadFromDisk() so that the
     * "file exists" and "file is a valid model" checks always happen first.
     *
     * @param network an already loaded network
     */
    private Dl4jDigitClassifier(MultiLayerNetwork network) {
        this.network = network;
    }

    /**
     * Loads the saved model from mnist_model.zip.
     *
     * @return a ready to use classifier
     * @throws PredictionException if the file is missing or cannot be read,
     *                             with a message telling the user what to do
     */
    public static Dl4jDigitClassifier loadFromDisk() throws PredictionException {
        File modelFile = new File(MODEL_FILE);

        if (modelFile.exists() == false) {
            throw new PredictionException(
                    "Model file " + MODEL_FILE + " was not found.\n\n"
                    + "Please run ModelTrainer first (class com.college.mnist.model.ModelTrainer)\n"
                    + "to train the network once. Training needs internet for the MNIST download.");
        }

        MultiLayerNetwork loadedNetwork = null;
        try {
            loadedNetwork = ModelSerializer.restoreMultiLayerNetwork(modelFile);
        } catch (IOException ioProblem) {
            throw new PredictionException(
                    "The model file " + MODEL_FILE + " could not be read.", ioProblem);
        }

        if (loadedNetwork == null) {
            throw new PredictionException("The model file " + MODEL_FILE + " is not a valid network.");
        }
        return new Dl4jDigitClassifier(loadedNetwork);
    }

    /**
     * Runs the network on one image.
     *
     * @param pixels 784 values in 0..1, 28 rows of 28 columns, white digit on black
     * @return the digit with the highest probability and that probability as a percentage
     * @throws PredictionException if the array has the wrong size or the network fails
     */
    @Override
    public Prediction classify(double[] pixels) throws PredictionException {

        if (pixels == null || pixels.length != 784) {
            throw new PredictionException("Expected 784 pixel values but received "
                    + (pixels == null ? "null" : String.valueOf(pixels.length)) + ".");
        }

        // 1. Feed the picture into the network and read the 10 output probabilities.
        INDArray probabilities = null;
        try {
            // reshape turns the flat 784 values into the 1 x 784 matrix the network wants.
            INDArray input = Nd4j.create(pixels, new int[] { 1, 784 });
            probabilities = network.output(input);
        } catch (RuntimeException networkProblem) {
            throw new PredictionException("The neural network could not process the image.",
                    networkProblem);
        }

        // 2. Find the highest probability with a plain loop - no streams, no lambdas.
        int bestDigit = 0;
        double bestProbability = probabilities.getDouble(0, 0);
        for (int digit = 1; digit < 10; digit++) {
            double currentProbability = probabilities.getDouble(0, digit);
            if (currentProbability > bestProbability) {
                bestProbability = currentProbability;
                bestDigit = digit;
            }
        }

        // 3. Confidence is simply the winning probability as a percentage.
        double confidencePercent = bestProbability * 100.0;
        return new Prediction(bestDigit, confidencePercent);
    }
}
