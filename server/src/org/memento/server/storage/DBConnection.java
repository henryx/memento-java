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
        String sep;
        String url;

        sep = System.getProperty("file.separator");

        url = "jdbc:sqlite:" + dbLocation + sep + ".store.db";

        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection(url);

        conn.setAutoCommit(Boolean.FALSE);
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

    private void createSchema(String area) throws SQLException {
        Statement stmt;
        String[] data;
        String[] index;
        String[] tables;

        if (!area.equals("system")) {
            data = new String[]{}; // In non-system area, data is empty
            index = new String[]{
                "CREATE INDEX idx_store_1 ON attrs(element,"
                + " element_hash)"
            };
            tables = new String[]{
                "CREATE TABLE attrs (element VARCHAR(1024),"
                    + " element_os VARCHAR(32),"
                    + " element_user VARCHAR(50),"
                    + " element_group VARCHAR(50),"
                    + " element_type VARCHAR(9),"
                    + " element_link VARCHAR(1024),"
                    + " element_hash VARCHAR(32),"
                    + " element_perm VARCHAR(32),"
                    + " element_mtime INTEGER,"
                    + " element_ctime INTEGER)",
                "CREATE TABLE acls (element VARCHAR(1024),"
                    + " id VARCHAR(50),"
                    + " id_type VARCHAR(1),"
                    + " perms VARCHAR(3))"
            };
        } else {
            data = new String[]{
                "INSERT INTO status VALUES('hour', 0, current_timestamp)",
                "INSERT INTO status VALUES('day', 0, current_timestamp)",
                "INSERT INTO status VALUES('week', 0, current_timestamp)",
                "INSERT INTO status VALUES('month', 0, current_timestamp)"
            };

            index = new String[]{}; // In system area, index is empty

            tables = new String[]{
                "CREATE TABLE status (grace VARCHAR(5),"
                + " actual INTEGER,"
                + " last_run TIMESTAMP)"
            };
        }

        stmt = DBConnection.connections.get(area).createStatement();

        for (String item : tables) {
            stmt.executeUpdate(item);
        }

        for (String item : index) {
            stmt.executeUpdate(item);
        }

        for (String item : data) {
            stmt.executeUpdate(item);
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

    public void closeConnection(String area, Boolean commit) throws SQLException {
        Connection conn;

        if (DBConnection.connections.containsKey(area)) {
            conn = DBConnection.connections.get(area);
            if (commit) {
                conn.commit();
            }
            conn.close();
        }
    }
}