package db.borealis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;

public class BorealisResultSet extends AbstractResultSet {

    private JsonArray rows;
    private List<String> columnNames = new ArrayList<>();
    private int currentRow;

    BorealisResultSet(JsonArray names, JsonArray rows) {

        names.forEach(name -> {
            columnNames.add(name.getAsString());
        });

        this.rows = rows;

        currentRow = -1;

        if (rows.size() == 0) {
            return;
        }

    }

    @Override
    public boolean next() throws SQLException {
        if (currentRow < rows.size() - 1) {
            currentRow++;
            return true;
        }
        return false;
    }

    @Override
    public void close() throws SQLException {
        rows = null;
        columnNames = null;
        currentRow = Integer.MAX_VALUE;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return currentRow == rows.size();
    }

    private void checkColumnIndex(int columnIndex) throws SQLException {
        if (columnIndex < 1 || columnIndex > columnNames.size()) {
            throw new SQLException("Invalid column index");
        }
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return rows.get(currentRow).getAsJsonArray().get(columnIndex - 1).getAsString();
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return rows.get(currentRow).getAsJsonArray().get(columnIndex - 1).getAsInt();
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(columnNames.indexOf(columnLabel) + 1);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(columnNames.indexOf(columnLabel) + 1);
    }
}