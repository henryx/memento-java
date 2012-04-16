/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       BackupSYNC
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.server.storage;

import java.sql.*;
import java.util.HashMap;

/**
 *
 * @author enrico
 */
public class DBConnection {

    private static DBConnection instance;
    private HashMap<String, Connection> connections;

    private DBConnection() {
        this.connections = new HashMap<>();
    }

    private void openConnection(String area, String dbLocation) throws SQLException, ClassNotFoundException {
        Connection conn;
        String url;

        url = "jdbc:derby://localhost:1527/" + dbLocation + "/.memento";

        Class.forName("org.apache.derby.jdbc.ClientDriver");
        conn = DriverManager.getConnection(url + ";create=true");

        this.connections.put(area, conn);
    }

    private Boolean checkSchemaExist(String area) throws SQLException {
        DatabaseMetaData dbmd;
        ResultSet res;
        int counter;

        counter = 0;
        dbmd = this.connections.get(area).getMetaData();

        res = dbmd.getTables(null, null, null, new String[]{"TABLE"});
        while (res.next()) {
            counter++;
        }

        if (counter > 0) {
            return true;
        } else {
            return false;
        }
    }

    private void createSchema(String area) {
        Statement stmt;
        String[] queries;

        queries = new String[]{
            "CREATE TABLE accounts(account_name VARCHAR(30),"
                      + "account_type VARCHAR(70), account_number INTEGER,"
                      + "CONSTRAINT pk_accounts PRIMARY KEY(account_name))",
            "CREATE TABLE attributes(attribute_type VARCHAR(30),attribute_value VARCHAR(70),"
                      + "CONSTRAINT pk_attributes PRIMARY KEY(attribute_type, attribute_value))",
            "CREATE TABLE payments(account_name VARCHAR(30),transaction_date DATE,"
                      + "category VARCHAR(70),payment_type VARCHAR(70),amount NUMERIC(10,2),"
                      + "note TEXT)"
        };

        try {
            stmt = this.connections.get(area).createStatement();
            
            for (String item : queries) {
                stmt.executeUpdate(item);
            }
            
        } catch (SQLException ex) {
        }
    }

    public Connection getConnection(String area, String dbLocation) throws SQLException, ClassNotFoundException {

        if (!this.connections.containsKey(area)) {
            this.openConnection(area, dbLocation);
        }

        if (!this.checkSchemaExist(area)) {
            this.createSchema(area);
        }

        return this.connections.get(area);
    }

    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }

        return instance;
    }

    public Iterable<String> getAreaList() {
        // TODO: implement this
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
}
