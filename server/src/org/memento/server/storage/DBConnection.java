/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.server.storage;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author enrico
 */
public class DBConnection {

    private static DBConnection instance;
    private static HashMap<String, Connection> connections;

    private DBConnection() {
        DBConnection.connections = new HashMap<>();
    }

    private void openConnection(String area, String dbLocation) throws SQLException, ClassNotFoundException {
        Connection conn;
        String url;

        url = "jdbc:derby://localhost:1527/" + dbLocation + "/.memento";

        Class.forName("org.apache.derby.jdbc.ClientDriver");
        conn = DriverManager.getConnection(url + ";create=true");

        DBConnection.connections.put(area, conn);
    }

    private Boolean checkSchemaExist(String area) throws SQLException {
        DatabaseMetaData dbmd;
        ResultSet res;
        int counter;

        counter = 0;
        dbmd = DBConnection.connections.get(area).getMetaData();

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
            stmt = DBConnection.connections.get(area).createStatement();
            
            for (String item : queries) {
                stmt.executeUpdate(item);
            }            
        } catch (SQLException ex) {
            // NOTE: add code for exception management
        }
    }

    public Connection getConnection(String area, String dbLocation) throws SQLException, ClassNotFoundException {

        if (!DBConnection.connections.containsKey(area)) {
            this.openConnection(area, dbLocation);
        }

        if (!this.checkSchemaExist(area)) {
            this.createSchema(area);
        }

        return DBConnection.connections.get(area);
    }

    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }

        return instance;
    }

    public ArrayList<String> getAreaList() {
        Iterator it;
        ArrayList<String> keys;


        keys = new ArrayList<>();

        it = DBConnection.connections.keySet().iterator();

        while (it.hasNext()) {
            keys.add(it.next().toString());
        }

        return keys;
    }

    public void closeConnection(String area) throws SQLException {
        Connection conn;

        if (DBConnection.connections.containsKey(area)) {
            conn = DBConnection.connections.get(area);
            conn.close();
        }
    }
}
