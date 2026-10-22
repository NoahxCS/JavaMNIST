package com.college.mnist.exception;

/**
 * Custom checked exception used everywhere this project can fail in a way the
 * user must know about: the model could not be loaded, the network could not
 * predict, or the database could not be read/written.
 *
 * It is a checked exception (it extends Exception), so the compiler forces the
 * caller to either catch it or declare it - which is exactly the behaviour we
 * want in the GUI, where every failure has to become a message dialog.
 */
public class PredictionException extends Exception {

    /** Required by the Exception contract: identifies the serialised form. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a human readable message.
     *
     * @param message text shown to the user or written to the log
     */
    public PredictionException(String message) {
        super(message);
    }

    /**
     * Creates the exception and keeps the original cause, so debugging is still
     * possible (e.g. the SQLException inside a "database error" message).
     *
     * @param message text shown to the user or written to the log
     * @param cause   the original exception that caused this failure
     */
    public PredictionException(String message, Throwable cause) {
        super(message, cause);
    }
}
