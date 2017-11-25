package connect;

import org.intellij.lang.annotations.Language;

import java.sql.*;

public class Connector {
    private Connection connection;

    public Connector(String driverClass, String driverUrl, String url, String user, String password)
            throws ClassNotFoundException, SQLException {

        Class.forName(driverClass);

        connection = DriverManager.getConnection(
                "jdbc:" + driverUrl + "://" + url,
                user,
                password);
    }

    public String[][] query(boolean result, @Language("SQL") String sql) throws SQLException {

        Statement statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);

        if (!result) {
            statement.executeUpdate(sql);
            statement.close();
            return new String[][]{{"SQL MODIFY"}};
        }

        ResultSet resultSet;

        resultSet = statement.executeQuery(sql);

        int col = resultSet.getMetaData().getColumnCount();
        resultSet.last();
        int row = resultSet.getRow();
        String[][] resultRecords = new String[row][col];
        resultSet.beforeFirst();

        int i = 0;
        while (resultSet.next()) {
            for (int j = 0; j < col; j++) {
                String val = resultSet.getString(j + 1);
                if (val != null)
                    resultRecords[i][j] = val;
            }
            i++;
        }

        resultSet.close();
        statement.close();

        return resultRecords;
    }

    public boolean isConnected() {
        return connection != null;
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}