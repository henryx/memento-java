/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.storage;

import java.sql.*;

/**
 *
 * @author enrico
 */
public class DBConnection {

    private Connection conn;

    public DBConnection() {
    }

    private Boolean checkSchemaExist() throws SQLException {
        DatabaseMetaData dbmd;
        ResultSet res;
        int counter;

        counter = 0;
        dbmd = this.conn.getMetaData();

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

    private void createSchema(Boolean system) {
        Statement stmt;
        String[] data;
        String[] index;
        String[] tables;

        if (!system) {
            data = new String[]{}; // In non-system area, data is empty
            index = new String[]{
                "CREATE INDEX idx_store_1 ON attrs(element_mtime,"
                + " element_ctime)"
            };
            tables = new String[]{
                "CREATE TABLE attrs (element VARCHAR(1024),"
                + " element_user VARCHAR(50),"
                + " element_group VARCHAR(50),"
                + " element_type CHAR(1),"
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

        try {
            stmt = this.conn.createStatement();

            for (String item : tables) {
                stmt.executeUpdate(item);
            }

            for (String item : index) {
                stmt.executeUpdate(item);
            }

            for (String item : data) {
                stmt.executeUpdate(item);
            }
        } catch (SQLException ex) {
            // NOTE: add code for exception management
        }
    }

    public Connection open(String dbname, Boolean system) throws ClassNotFoundException, SQLException {
        String url;

        url = "jdbc:sqlite:" + dbname;
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection(url);

        if (!this.checkSchemaExist()) {
            this.createSchema(system);
        }

        this.conn.setAutoCommit(Boolean.FALSE);
        return this.conn;
    }

    public void close() throws SQLException {
        this.conn.commit();
        this.conn.close();
    }
}
