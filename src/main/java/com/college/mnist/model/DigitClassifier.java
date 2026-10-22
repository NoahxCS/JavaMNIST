package com.college.mnist.model;

import com.college.mnist.exception.PredictionException;

/**
 * DigitClassifier is an interface: it describes WHAT a digit classifier can do
 * and says nothing about HOW it is done.
 *
 * This is ABSTRACTION. The GUI (MainFrame) declares its field as
 * {@code DigitClassifier} and never mentions Deeplearning4j at all. Because the
 * GUI talks to the interface, any other implementation - a random test stub, a
 * future CNN, a remote service - could be dropped in without changing one line
 * of GUI code. That substitution is POLYMORPHISM through an interface.
 */
public interface DigitClassifier {

    /**
     * Classifies one handwritten digit.
     *
     * @param pixels 784 normalised pixel values (0.0 = black, 1.0 = white),
     *               row major order, 28 rows of 28 columns
     * @return the recognised digit together with its confidence
     * @throws PredictionException if the model is missing or the network fails
     */
    Prediction classify(double[] pixels) throws PredictionException;
}
