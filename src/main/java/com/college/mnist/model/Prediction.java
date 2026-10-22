package com.college.mnist.model;

/**
 * Prediction is the answer of one classification: which digit was recognised
 * and how sure the network is about it.
 *
 * This class demonstrates ENCAPSULATION in the clearest possible way:
 *   - both fields are private, so no outside class can change them;
 *   - the values can only be supplied once, through the constructor;
 *   - the only access path is the two getters.
 * A Prediction object is therefore immutable - once created it cannot be
 * corrupted by another part of the program.
 */
public class Prediction {

    /** The recognised digit, 0 to 9. */
    private final int digit;

    /** Confidence as a percentage, 0.0 to 100.0. */
    private final double confidence;

    /**
     * Creates one prediction.
     *
     * @param digit      recognised digit, 0 to 9
     * @param confidence confidence in percent, e.g. 98.6
     */
    public Prediction(int digit, double confidence) {
        this.digit = digit;
        this.confidence = confidence;
    }

    /** @return the recognised digit, 0 to 9. */
    public int getDigit() {
        return digit;
    }

    /** @return the confidence as a percentage, 0.0 to 100.0. */
    public double getConfidence() {
        return confidence;
    }
}
