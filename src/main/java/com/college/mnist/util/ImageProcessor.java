package com.college.mnist.util;

import java.awt.image.BufferedImage;

/**
 * ImageProcessor holds the pure image mathematics of the project.
 *
 * It is a utility class: it has no fields, only static methods, because
 * downsampling an image does not depend on any object state. Keeping these
 * loops out of the GUI classes means the GUI only deals with events and the
 * maths is in one small, testable place.
 */
public final class ImageProcessor {

    /**
     * Private constructor so nobody can create an ImageProcessor object.
     * (A utility class is only a container for static methods.)
     */
    private ImageProcessor() {
    }

    /**
     * Converts a 280x280 drawing into the 28x28 input the network expects.
     *
     * Every one of the 28x28 output pixels is the average of a 10x10 block of
     * the input image (280 / 28 = 10). Averaging keeps more information than
     * simply picking one pixel out of each block, so the shrunk digit still
     * looks like the drawn digit.
     *
     * The result is already normalised to 0.0 .. 1.0 by dividing the average
     * brightness by 255, which is the same scaling the MNIST training data uses.
     *
     * @param image the canvas image, normally 280x280
     * @return 784 values in row major order (28 rows of 28 columns), range 0..1
     */
    public static double[] downsample(BufferedImage image) {
        double[] pixels = new double[28 * 28];

        int blockWidth = image.getWidth() / 28;
        int blockHeight = image.getHeight() / 28;
        if (blockWidth < 1) {
            blockWidth = 1;
        }
        if (blockHeight < 1) {
            blockHeight = 1;
        }

        // Outer loops: one iteration per output pixel of the small image.
        for (int row = 0; row < 28; row++) {
            for (int col = 0; col < 28; col++) {

                // Inner loops: walk the 10x10 block that belongs to this pixel.
                double sum = 0;
                for (int y = 0; y < blockHeight; y++) {
                    for (int x = 0; x < blockWidth; x++) {
                        int sourceX = col * blockWidth + x;
                        int sourceY = row * blockHeight + y;

                        int argb = image.getRGB(sourceX, sourceY);

                        // Red is enough: the canvas is grey/white on black, so R = G = B.
                        int red = (argb >> 16) & 0xFF;
                        sum = sum + red;
                    }
                }

                double average = sum / (blockWidth * blockHeight);
                pixels[row * 28 + col] = average / 255.0;
            }
        }
        return pixels;
    }

    /**
     * Returns the raw 0..255 brightness of an image, one value per pixel.
     * Used by the test class to show that a shrunk image really contains the
     * drawn strokes; the classifier itself uses downsample().
     *
     * @param image any image
     * @return width*height brightness values in row major order
     */
    public static double[] toPixelArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[] pixels = new double[width * height];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int argb = image.getRGB(col, row);
                int red = (argb >> 16) & 0xFF;
                pixels[row * width + col] = red;
            }
        }
        return pixels;
    }
}
