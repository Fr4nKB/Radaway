package it.unipi.iot.coordinator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBDriver {
    private static DBDriver instance = null;
    private static final String databaseIp;
    private static final String databasePort;
    private static final String databaseUsername;
    private static final String databasePassword;
    private static final String databaseName;

    static {
        databaseIp = "127.0.0.1";
        databasePort = "3306";
        databaseUsername = "iot";
        databasePassword = "iotUbuntu24!";
        databaseName = "radaway";
    }

    private DBDriver() {
        try {
            executeSQLScript("radaway.sql");
            System.out.println("DATABASE READY");
        }
        catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeSQLScript(String fileName) throws IOException, SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
        		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            StringBuilder sql = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sql.append(line);
                if (line.endsWith(";")) {
                    statement.execute(sql.toString());
                    sql = new StringBuilder();
                }
            }
        }
    }

    // singleton
    public static DBDriver getInstance() {
        if(instance == null)
            instance = new DBDriver();

        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://"+ databaseIp + ":" + databasePort +
                        "/" + databaseName,
                databaseUsername, databasePassword);
    }

    public void registerActuator(String ipv6, String type, String sensorTypesJson) {
        try(Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("REPLACE INTO `actuator` (`ipv6`, `type`, `sensor_types`) VALUES (?, ?, ?)")) {
            statement.setString(1, ipv6);
            statement.setString(2, type);
            statement.setString(3, sensorTypesJson);
            statement.executeUpdate();
        }
        catch(final SQLException e) {
            System.err.println("Skipping replace...");
        }
    }

    public void insertActuatorStatus(String type, String ipv6, int value) {
        try(Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + type + "` (`ipv6`, `value`) VALUES (?, ?)")) {
            statement.setString(1, ipv6);
            statement.setInt(2, value);
            statement.executeUpdate();
        }
        catch (final SQLException e) {
            System.err.println("Skipping insert...");
        }
    }

    public void insertSensorSample(String type, int value) {
        try(Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + type + "` (`value`) VALUES (?)")) {
            statement.setInt(1, value);
            statement.executeUpdate();
        } catch (final SQLException e) {
            return;
        }
    }

    public List<Integer> getValuesFromLastSeconds(String type, int secondsAgoInit, int secondsAgoEnd) {
        List<Integer> values = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT `value` FROM `" + type + "` WHERE `timestamp` >= ? AND `timestamp` < ?")) {

            long currentTime = System.currentTimeMillis();
            long startTime = currentTime - (secondsAgoInit * 1000);
            long endTime = currentTime - (secondsAgoEnd * 1000);

            // convert milliseconds to SQL timestamp
            java.sql.Timestamp startTimestamp = new java.sql.Timestamp(startTime);
            java.sql.Timestamp endTimestamp = new java.sql.Timestamp(endTime);

            statement.setTimestamp(1, endTimestamp);
            statement.setTimestamp(2, startTimestamp);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                values.add(resultSet.getInt("value"));
            }
        } catch (final SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving values...");
        }

        return values;
    }

    public String getIpFromActuatorType(String type) {
        String ipv6 = null;

        try(Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT `ipv6` FROM `actuator` WHERE `type` = ? LIMIT 1")) {
            statement.setString(1, type);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                ipv6 = resultSet.getString("ipv6");
            }
        }
        catch (final SQLException e) {
            System.err.println("Error retrieving ip....");
        }

        return ipv6;
    }

    public Map<String, String> getActuatorInfoFromSensorType(String sensorType) {
        Map<String, String> result = new HashMap<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `ipv6`, `type` FROM `actuator` WHERE JSON_CONTAINS(`sensor_types`, ?, '$') LIMIT 1")) {
            statement.setString(1, sensorType);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                result.put("ipv6", resultSet.getString("ipv6"));
                result.put("type", resultSet.getString("type"));
            }
        } catch (final SQLException e) {
            System.err.println("Error retrieving ip and type: " + e.getMessage());
        }

        return result;
    }
}
