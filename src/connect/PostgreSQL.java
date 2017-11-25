package connect;

import java.sql.SQLException;

public class PostgreSQL extends Connector {

    public PostgreSQL(String url, String user, String password) throws SQLException, ClassNotFoundException {
        super("org.postgresql.Driver", "postgresql", url, user, password);
    }
}