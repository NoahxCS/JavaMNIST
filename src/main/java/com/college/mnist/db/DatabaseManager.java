package com.college.mnist.db;

import com.college.mnist.exception.PredictionException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DatabaseManager is the only class that talks to SQLite. It owns the JDBC
 * Connection and offers four small operations on the history table.
 *
 * SINGLETON PATTERN
 * -----------------
 * A database is a shared resource: opening several connections to the same file
 * would be wasteful and could lock the file. The Singleton pattern guarantees
 * exactly one DatabaseManager (and therefore one Connection) in the whole
 * program:
 *   - the constructor is private, so "new DatabaseManager()" is impossible;
 *   - the single instance lives in the static field instance;
 *   - getInstance() creates it the first time it is called and returns the same
 *     object afterwards.
 *
 * All SQL uses PreparedStatement, which prevents SQL injection and lets SQLite
 * reuse the compiled statement. Every method uses try/catch and closes its
 * resources in a finally block or with try-with-resources.
 */
public class DatabaseManager {

    /** Name of the SQLite file; created automatically on first run. */
    public static final String DATABASE_FILE = "digits.db";

    /** The one and only instance of this class. */
    private static DatabaseManager instance = null;

    /** The single JDBC connection used by the whole application. */
    private Connection connection;

    /** Pre-compiled insert statement, reused for every prediction. */
    private PreparedStatement insertStatement;

    /** Pre-compiled select statement for the newest rows. */
    private PreparedStatement lastDigitsStatement;

    /** Pre-compiled select statement for the complete table. */
    private PreparedStatement allHistoryStatement;

    /**
     * Private constructor: loads the driver, opens the connection, creates the
     * table when it does not exist and prepares the statements.
     *
     * @throws PredictionException if SQLite cannot be opened
     */
    private DatabaseManager() throws PredictionException {
        try {
            // The driver registers itself here; without this line JDBC cannot
            // understand the jdbc:sqlite: URL.
            Class.forName("org.sqlite.JDBC");

            // A relative path means the file is created next to the project.
            connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_FILE);
            createTable();
            prepareStatements();

        } catch (ClassNotFoundException driverMissing) {
            throw new PredictionException(
                    "The SQLite JDBC driver is missing from the project.", driverMissing);
        } catch (SQLException sqlProblem) {
            throw new PredictionException(
                    "The database " + DATABASE_FILE + " could not be opened.", sqlProblem);
        }
    }

    /**
     * Returns the single instance, creating it on the first call.
     *
     * @return the shared DatabaseManager
     * @throws PredictionException if the database cannot be opened
     */
    public static synchronized DatabaseManager getInstance() throws PredictionException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Creates the history table when the database file is new.
     *
     * @throws SQLException if the CREATE TABLE statement fails
     */
    private void createTable() throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS history ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "predicted_digit INTEGER, "
                    + "confidence REAL, "
                    + "created_at TEXT)");
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * Compiles the three statements used by the GUI once, at start up.
     *
     * @throws SQLException if a statement cannot be prepared
     */
    private void prepareStatements() throws SQLException {
        insertStatement = connection.prepareStatement(
                "INSERT INTO history (predicted_digit, confidence, created_at) VALUES (?, ?, ?)");

        lastDigitsStatement = connection.prepareStatement(
                "SELECT id, predicted_digit, confidence, created_at FROM history "
                + "ORDER BY id DESC LIMIT ?");

        allHistoryStatement = connection.prepareStatement(
                "SELECT id, predicted_digit, confidence, created_at FROM history "
                + "ORDER BY id DESC");
    }

    /**
     * Saves one prediction. Called by the GUI after every successful prediction.
     *
     * @param digit      predicted digit, 0 to 9
     * @param confidence confidence in percent
     * @throws PredictionException if the row cannot be inserted
     */
    public void insertPrediction(int digit, double confidence) throws PredictionException {
        try {
            insertStatement.setInt(1, digit);
            insertStatement.setDouble(2, confidence);
            insertStatement.setString(3, nowAsText());
            insertStatement.executeUpdate();
        } catch (SQLException sqlProblem) {
            throw new PredictionException("The prediction could not be saved.", sqlProblem);
        }
    }

    /**
     * Returns the newest predictions, newest first.
     *
     * @param n how many rows are wanted
     * @return list with at most n records (empty when the table is empty)
     * @throws PredictionException if the query fails
     */
    public List<HistoryRecord> getLastDigits(int n) throws PredictionException {
        List<HistoryRecord> records = new ArrayList<HistoryRecord>();
        ResultSet resultSet = null;
        try {
            lastDigitsStatement.setInt(1, n);
            resultSet = lastDigitsStatement.executeQuery();
            while (resultSet.next()) {
                records.add(readRecord(resultSet));
            }
        } catch (SQLException sqlProblem) {
            throw new PredictionException("The recent history could not be read.", sqlProblem);
        } finally {
            closeQuietly(resultSet);
        }
        return records;
    }

    /**
     * Returns the complete history table, newest first. Used by the expandable
     * table in the bottom panel.
     *
     * @return every row of the history table
     * @throws PredictionException if the query fails
     */
    public List<HistoryRecord> getAllHistory() throws PredictionException {
        List<HistoryRecord> records = new ArrayList<HistoryRecord>();
        ResultSet resultSet = null;
        try {
            resultSet = allHistoryStatement.executeQuery();
            while (resultSet.next()) {
                records.add(readRecord(resultSet));
            }
        } catch (SQLException sqlProblem) {
            throw new PredictionException("The history could not be read.", sqlProblem);
        } finally {
            closeQuietly(resultSet);
        }
        return records;
    }

    /**
     * Deletes every row of the history table.
     *
     * There is no button for this operation in the GUI; it exists so the class
     * offers the complete set of CRUD operations (Create, Read, Update through
     * re-insert, Delete) and it is used by the test class and in the report.
     *
     * @throws PredictionException if the rows cannot be deleted
     */
    public void clearHistory() throws PredictionException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM history");
        } catch (SQLException sqlProblem) {
            throw new PredictionException("The history could not be cleared.", sqlProblem);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                    // nothing useful can be done if closing the statement fails
                }
            }
        }
    }

    /**
     * Counts the rows currently stored. Used by the test class.
     *
     * @return number of rows in the history table
     * @throws PredictionException if the query fails
     */
    public int getHistoryCount() throws PredictionException {
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM history");
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException sqlProblem) {
            throw new PredictionException("The history could not be counted.", sqlProblem);
        } finally {
            closeQuietly(resultSet);
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                    // nothing useful can be done if closing the statement fails
                }
            }
        }
    }

    /**
     * Turns the current row of a ResultSet into a HistoryRecord object. Reading
     * the row into an object here is what keeps JDBC out of the GUI classes.
     *
     * @param resultSet result set positioned on a row
     * @return the row as an object
     * @throws SQLException if a column cannot be read
     */
    private HistoryRecord readRecord(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        int digit = resultSet.getInt("predicted_digit");
        double confidence = resultSet.getDouble("confidence");
        String createdAt = resultSet.getString("created_at");
        return new HistoryRecord(id, digit, confidence, createdAt);
    }

    /**
     * @return the current date and time as text, for example 2026-10-22 17:45:03
     */
    private String nowAsText() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }

    /**
     * Closes a result set without letting a failure hide the real error.
     *
     * @param resultSet result set to close, may be null
     */
    private void closeQuietly(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
                // closing a read-only result set cannot change the outcome
            }
        }
    }

    /**
     * Closes the connection when the program ends. Called by Main after the
     * window has been closed.
     */
    public void close() {
        try {
            if (insertStatement != null) {
                insertStatement.close();
            }
            if (lastDigitsStatement != null) {
                lastDigitsStatement.close();
            }
            if (allHistoryStatement != null) {
                allHistoryStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException sqlProblem) {
            System.out.println("Warning: the database was not closed cleanly.");
        } finally {
            instance = null;
        }
    }

    /**
     * Helper kept for completeness: formats a Timestamp as text. It documents the
     * exact format written into created_at.
     *
     * @param timestamp value to format
     * @return text in yyyy-MM-dd HH:mm:ss form
     */
    public static String formatTimestamp(Timestamp timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date(timestamp.getTime()));
    }
}
