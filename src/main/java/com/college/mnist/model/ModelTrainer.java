package com.college.mnist.model;

import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;

/**
 * ModelTrainer is a console program that:
 *   1. downloads the MNIST dataset on the first run (internet needed, cached afterwards),
 *   2. trains 784 -> 128 (ReLU) -> 10 (softmax) with Adam for a few epochs,
 *   3. prints the test accuracy,
 *   4. saves the trained network to mnist_model.zip.
 *
 * It has its own main() and is run exactly once. The Swing application never
 * trains; it only loads the file this class produces. Keeping training and
 * prediction in two separate programs is what makes the desktop app open
 * immediately instead of spending minutes on training first.
 *
 * The data comes from DL4J's own MnistDataSetIterator, so no CSV parsing code is
 * needed. That reader already returns every image as 784 values between 0 and 1
 * (white digit on black background), which is exactly the format
 * ImageProcessor.downsample() produces from the drawing canvas - so training and
 * prediction feed the network data on the same scale.
 */
public class ModelTrainer {

    /** One batch of training examples handled at a time. */
    private static final int BATCH_SIZE = 64;

    /** How many times the whole training set (60 000 images) is shown to the network. */
    private static final int EPOCHS = 5;

    /** Adam learning rate. */
    private static final double LEARNING_RATE = 0.001;

    /** Number of test images used for the accuracy report. */
    private static final int TEST_EXAMPLES = 10000;

    /** Where the trained network is written. */
    private static final String MODEL_FILE = Dl4jDigitClassifier.MODEL_FILE;

    /**
     * DL4J's base folder. It is "~/.deeplearning4j" unless overridden with the
     * system property below, and DL4J stores the downloaded MNIST files in
     * <base>/data/MNIST. Overriding it is useful on a machine where the home
     * folder cannot be written, for example:
     * {@code -Dorg.deeplearning4j.resources.directory=target/dl4j-data}
     */
    private static final String DL4J_BASE_FOLDER =
            System.getProperty("org.deeplearning4j.resources.directory",
                    System.getProperty("user.home") + File.separator + ".deeplearning4j");

    /**
     * Creates the folder in which DL4J keeps the MNIST files.
     *
     * DL4J downloads the dataset into this folder but refuses to start when the
     * folder is missing, and it does not create the folder itself - hence this
     * small check, which runs before the first iterator is built.
     *
     * @throws IOException if the folder cannot be created
     */
    private static void prepareMnistFolder() throws IOException {
        File folder = new File(DL4J_BASE_FOLDER, "data" + File.separator + "MNIST");
        if (folder.exists() == false) {
            boolean created = folder.mkdirs();
            if (created == false && folder.exists() == false) {
                throw new IOException("Could not create the MNIST folder "
                        + folder.getAbsolutePath());
            }
            System.out.println("Created MNIST cache folder: " + folder.getAbsolutePath());
        }
    }

    /**
     * Console entry point.
     *
     * @param args one optional argument: the number of training epochs to run
     *             instead of the default of EPOCHS. Handy for a quick test run,
     *             for example {@code ModelTrainer 1}.
     */
    public static void main(String[] args) {
        int epochs = EPOCHS;
        if (args.length > 0) {
            try {
                epochs = Integer.parseInt(args[0]);
            } catch (NumberFormatException badArgument) {
                System.out.println("The epoch argument was not a number, using " + EPOCHS + ".");
            }
        }

        System.out.println("=== MNIST model training ===");
        System.out.println("Epochs: " + epochs + ", batch size: " + BATCH_SIZE);
        System.out.println("The first run downloads MNIST (about 15 MB) and caches it.");

        try {
            // Step 0: DL4J needs its cache folder to exist before the first download.
            prepareMnistFolder();

            // Step 1: build the network.
            MultiLayerNetwork network = createNetwork();
            System.out.println(network.summary());

            // Step 2: train.
            train(network, epochs);

            // Step 3: measure accuracy on the 10 000 test images.
            double accuracy = evaluate(network);

            // Step 4: save the model for the GUI.
            saveModel(network);

            System.out.println();
            System.out.println("Test accuracy: " + String.format("%.2f", accuracy) + " %");
            System.out.println("Model saved to " + MODEL_FILE);
            System.out.println("You can now start the application: com.college.mnist.Main");

        } catch (IOException ioProblem) {
            System.out.println("Training failed: " + ioProblem.getMessage());
            System.out.println("The MNIST download needs an internet connection the first time.");
        } catch (RuntimeException libraryProblem) {
            // DL4J reports its own problems as unchecked exceptions; printing the
            // message keeps the console output readable instead of a raw stack trace.
            System.out.println("Training failed inside DL4J: " + libraryProblem.getMessage());
        }
    }

    /**
     * Defines the network: 784 inputs (one per pixel), one hidden ReLU layer with
     * 128 units and a softmax output layer with 10 units (one per digit).
     *
     * @return the compiled, ready to train network
     */
    private static MultiLayerNetwork createNetwork() {
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .updater(new Adam(LEARNING_RATE))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(784)               // 28 x 28 pixels
                        .nOut(128)              // 128 hidden units
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(128)
                        .nOut(10)               // digits 0 to 9
                        .activation(Activation.SOFTMAX)
                        .build())
                .build();

        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();
        return network;
    }

    /**
     * Runs the training loop and prints the score every 100 mini batches.
     *
     * @param network the network to train
     * @param epochs  how many times the whole training set is shown
     * @throws IOException if the MNIST data cannot be downloaded or read
     */
    private static void train(MultiLayerNetwork network, int epochs) throws IOException {
        System.out.println("Training: " + epochs + " epochs, batch size " + BATCH_SIZE);

        for (int epoch = 1; epoch <= epochs; epoch++) {
            // A fresh iterator per epoch, so each epoch sees the data once again
            // from the start (the fetcher shuffles the order every time).
            // Arguments: batch size, examples per epoch, train (not test).
            DataSetIterator iterator = new MnistDataSetIterator(BATCH_SIZE, 60000, true);

            int batchNumber = 0;
            while (iterator.hasNext()) {
                DataSet batch = iterator.next();
                network.fit(batch);
                batchNumber++;

                if (batchNumber % 100 == 0) {
                    System.out.println("  epoch " + epoch + ", batch " + batchNumber
                            + ", score " + String.format("%.4f", network.score()));
                }
            }
            System.out.println("Epoch " + epoch + " of " + epochs + " finished.");
        }
    }

    /**
     * Measures the accuracy on the test set, which the network has never seen
     * during training.
     *
     * @param network trained network
     * @return accuracy in percent, for example 97.42
     * @throws IOException if the MNIST data cannot be read
     */
    private static double evaluate(MultiLayerNetwork network) throws IOException {
        System.out.println("Evaluating on " + TEST_EXAMPLES + " test images...");

        DataSetIterator testIterator = new MnistDataSetIterator(BATCH_SIZE, TEST_EXAMPLES, false);
        Evaluation evaluation = new Evaluation(10);

        while (testIterator.hasNext()) {
            DataSet testBatch = testIterator.next();
            evaluation.eval(testBatch.getLabels(), network.output(testBatch.getFeatures()));
        }
        return evaluation.accuracy() * 100.0;
    }

    /**
     * Saves the trained network to mnist_model.zip. The third argument "true"
     * stores the updater state (the state of Adam) together with the weights.
     *
     * @param network trained network
     * @throws IOException if the file cannot be written
     */
    private static void saveModel(MultiLayerNetwork network) throws IOException {
        ModelSerializer.writeModel(network, new File(MODEL_FILE), true);
    }
}
