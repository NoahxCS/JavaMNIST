package com.college.mnist.ui;

import com.college.mnist.db.DatabaseManager;
import com.college.mnist.db.HistoryRecord;
import com.college.mnist.exception.PredictionException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;

/**
 * HistoryPanel is the strip at the bottom of the window. It has two parts:
 *
 *   1. ALWAYS VISIBLE - a label showing the last three predicted digits, newest
 *      first, for example "Recent: 7  3  1", and next to it the toggle button
 *      "Show all history".
 *
 *   2. COLLAPSED BY DEFAULT - a scrollable, read-only JTable inside a
 *      JScrollPane, backed by a DefaultTableModel, showing the complete database
 *      table with the columns id, digit, confidence, time. The toggle button
 *      shows or hides it, and the window is re-packed so it grows and shrinks
 *      with the table.
 *
 * INHERITANCE: extends JPanel. ASSOCIATION: it holds a DatabaseManager reference
 * and asks it for HistoryRecord objects - it never writes SQL itself.
 */
public class HistoryPanel extends JPanel {

    /** Required by Swing: identifies the serialised form of a component. */
    private static final long serialVersionUID = 1L;

    /** How many digits the always visible strip shows. */
    private static final int STRIP_SIZE = 3;

    /** The database object used to read the history. */
    private final DatabaseManager databaseManager;

    /** The window that has to be re-packed when the table is shown or hidden. */
    private final Window ownerWindow;

    /** Label with the last three digits. */
    private final JLabel recentLabel;

    /** Button that expands and collapses the table. */
    private final JButton toggleButton;

    /** Model behind the history table. */
    private final DefaultTableModel tableModel;

    /** The history table itself. */
    private final JTable historyTable;

    /** Scroll pane holding the table; only added to the panel when expanded. */
    private final JScrollPane tableScrollPane;

    /** Container that switches between "only the strip" and "strip + table". */
    private final JPanel tableArea;

    /** True while the full history table is shown. */
    private boolean expanded;

    /**
     * Creates the panel and loads the current history from the database.
     *
     * @param databaseManager the shared database object
     * @param ownerWindow     window that is re-packed when the table is toggled
     */
    public HistoryPanel(DatabaseManager databaseManager, Window ownerWindow) {
        this.databaseManager = databaseManager;
        this.ownerWindow = ownerWindow;
        this.expanded = false;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // ----- part 1: the always visible strip -------------------------------
        recentLabel = new JLabel("Recent:");
        toggleButton = new JButton("Show all history");

        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        strip.add(recentLabel);
        strip.add(toggleButton);
        add(strip, BorderLayout.NORTH);

        // ----- part 2: the expandable table -----------------------------------
        String[] columnNames = { "id", "digit", "confidence", "time" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;

            /** Keeps every cell read-only; the table only displays data. */
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(tableModel);
        historyTable.setAutoCreateRowSorter(false);
        historyTable.setRowHeight(22);

        tableScrollPane = new JScrollPane(historyTable);
        tableScrollPane.setPreferredSize(new Dimension(600, 220));

        // A one-component holder: adding the scroll pane shows the table,
        // removing it collapses the panel again.
        tableArea = new JPanel(new BorderLayout());
        tableArea.setLayout(new BoxLayout(tableArea, BoxLayout.Y_AXIS));
        add(tableArea, BorderLayout.CENTER);

        // Load whatever is already in the database.
        refresh();
    }

    /**
     * Reloads the strip and the table from the database. The GUI calls this
     * after every prediction, so both views always show the stored data and
     * never an in-memory copy.
     */
    public void refresh() {
        loadRecentDigits();
        loadFullTable();
    }

    /**
     * Reads the newest three rows and builds the "Recent: 7  3  1" text.
     */
    private void loadRecentDigits() {
        try {
            List<HistoryRecord> records = databaseManager.getLastDigits(STRIP_SIZE);

            StringBuffer text = new StringBuffer("Recent:");
            for (int index = 0; index < records.size(); index++) {
                HistoryRecord record = records.get(index);
                text.append("  ");
                text.append(record.getDigit());
            }
            recentLabel.setText(text.toString());

        } catch (PredictionException databaseProblem) {
            recentLabel.setText("Recent:  (database unavailable)");
        }
    }

    /**
     * Reads the complete history table and refills the DefaultTableModel.
     */
    private void loadFullTable() {
        try {
            List<HistoryRecord> records = databaseManager.getAllHistory();

            // Build the rows first, then hand them to the model in one call.
            Object[][] rows = new Object[records.size()][4];
            for (int index = 0; index < records.size(); index++) {
                HistoryRecord record = records.get(index);
                rows[index][0] = Integer.valueOf(record.getId());
                rows[index][1] = Integer.valueOf(record.getDigit());
                rows[index][2] = String.format("%.1f %%", record.getConfidence());
                rows[index][3] = record.getCreatedAt();
            }

            String[] columnNames = { "id", "digit", "confidence", "time" };
            tableModel.setDataVector(rows, columnNames);

        } catch (PredictionException databaseProblem) {
            tableModel.setRowCount(0);
        }
    }

    /**
     * Shows or hides the complete history table and re-packs the window so it
     * grows and shrinks together with the table.
     *
     * @param expanded true to show the table
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;

        tableArea.removeAll();
        if (expanded) {
            tableArea.add(tableScrollPane);
        }
        if (expanded) {
            toggleButton.setText("Hide history");
        } else {
            toggleButton.setText("Show all history");
        }

        tableArea.revalidate();
        tableArea.repaint();

        // pack() recalculates the preferred size of the whole window.
        ownerWindow.pack();
    }

    /** @return true while the complete history table is visible. */
    public boolean isExpanded() {
        return expanded;
    }

    /** @return the button that expands and collapses the table. */
    public JButton getToggleButton() {
        return toggleButton;
    }

    /** @return the table that shows the history rows. */
    public JTable getHistoryTable() {
        return historyTable;
    }
}