package db.borealis;

import java.sql.SQLException;
import java.sql.Statement;

public class BorealisConnection extends AbstractConnection {
    final String regionName;
    final String functionName;
    final String profileName;

    BorealisConnection(String profileName, String regionName, String functionName) {
        this.regionName = regionName;
        this.functionName = functionName;
        this.profileName = profileName;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new BorealisStatement(this);
    }

}