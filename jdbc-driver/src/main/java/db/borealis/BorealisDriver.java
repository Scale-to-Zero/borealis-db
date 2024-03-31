package db.borealis;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class BorealisDriver implements Driver {
    private static final String URL_PREFIX = "jdbc:borealis:";
    private static Logger logger = Logger.getLogger(BorealisDriver.class.getName());

    static {
        try {
            DriverManager.registerDriver(new BorealisDriver());
            logger.info("BorealisDriver registered successfully");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register BorealisDriver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (acceptsURL(url)) {
            String[] urlParts = url.substring(URL_PREFIX.length()).split(":");
            String profileName = urlParts[0];
            String regionName = urlParts[1];
            String functionName = urlParts[2];
            return new BorealisConnection(profileName, regionName, functionName);
        }
        return null;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}