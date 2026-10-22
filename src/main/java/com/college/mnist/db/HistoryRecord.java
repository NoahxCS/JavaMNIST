package com.college.mnist.db;

/**
 * HistoryRecord represents one row of the history table in digits.db
 * (columns: id, predicted_digit, confidence, created_at).
 *
 * Like Prediction, this class shows ENCAPSULATION: the four fields are private
 * and reachable only through getters. Objects of this class travel between the
 * database layer and the GUI, so the GUI never has to touch a ResultSet.
 */
public class HistoryRecord {

    /** Primary key of the row, as created by SQLite (AUTOINCREMENT). */
    private final int id;

    /** The digit that was predicted, 0 to 9. */
    private final int digit;

    /** Confidence of that prediction in percent, e.g. 98.6. */
    private final double confidence;

    /** Timestamp text exactly as stored in the database. */
    private final String createdAt;

    /**
     * Creates one history row.
     *
     * @param id        database id
     * @param digit     predicted digit, 0 to 9
     * @param confidence confidence in percent
     * @param createdAt timestamp text from the database
     */
    public HistoryRecord(int id, int digit, double confidence, String createdAt) {
        this.id = id;
        this.digit = digit;
        this.confidence = confidence;
        this.createdAt = createdAt;
    }

    /** @return the database id of this row. */
    public int getId() {
        return id;
    }

    /** @return the predicted digit, 0 to 9. */
    public int getDigit() {
        return digit;
    }

    /** @return the confidence in percent. */
    public double getConfidence() {
        return confidence;
    }

    /** @return the timestamp text stored in the database. */
    public String getCreatedAt() {
        return createdAt;
    }
}
