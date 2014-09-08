/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

/**
 *
 * @author enrico
 */
public class DBConnection implements AutoCloseable {

    private Connection conn;
    private boolean autocommit;

    public DBConnection(HashMap<String, String> params, boolean autocommit) throws SQLException, ClassNotFoundException {
        this.autocommit = autocommit;

        this.openConnection(params);

        if (!this.checkSchemaExist()) {
            this.createSchema();
        }
    }

    private void openConnection(HashMap<String, String> params) throws SQLException, ClassNotFoundException {
        String url;

        url = "jdbc:firebirdsql:" + params.get("host") + "/" + params.get("port") + ":" + params.get("dbname");

        Class.forName("org.firebirdsql.jdbc.FBDriver");
        this.conn = DriverManager.getConnection(url, params.get("user"), params.get("password"));
        this.conn.setAutoCommit(this.autocommit);
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

    private void createSchema() throws SQLException {
        Statement stmt;
        String[] data;
        String[] index;
        String[] tables;
        String[] domains;

        domains = new String[]{
            "CREATE DOMAIN BOOLEAN AS SMALLINT CHECK (value is null or value in (0, 1))"
        };

        index = new String[]{
            "CREATE INDEX idx_attrs_1 ON attrs(area, grace, dataset)",
            "CREATE INDEX idx_attrs_2 ON attrs(area, grace, dataset, hash)",
            "CREATE INDEX idx_attrs_3 ON attrs(area, grace, dataset, type)",
            "CREATE INDEX idx_acls_1 ON acls(area, grace, dataset)"
        };
        tables = new String[]{
            "CREATE TABLE status ("
            + "grace VARCHAR(5),"
            + " actual INTEGER,"
            + " last_run TIMESTAMP)",
            "CREATE TABLE attrs ("
            + "area VARCHAR(30),"
            + " grace VARCHAR(5),"
            + " dataset INTEGER,"
            + " element VARCHAR(1024),"
            + " os VARCHAR(32),"
            + " username VARCHAR(50),"
            + " groupname VARCHAR(50),"
            + " type VARCHAR(9),"
            + " link VARCHAR(1024),"
            + " hash VARCHAR(32),"
            + " perms VARCHAR(32),"
            + " mtime BIGINT,"
            + " ctime BIGINT,"
            + "compressed BOOLEAN)",
            "CREATE TABLE acls ("
            + "area VARCHAR(30),"
            + " grace VARCHAR(5),"
            + " dataset INTEGER,"
            + " element VARCHAR(1024),"
            + " name VARCHAR(50),"
            + " type VARCHAR(5),"
            + " perms VARCHAR(3))"
        };
        data = new String[]{
            "INSERT INTO status VALUES('hour', 0, CURRENT_TIMESTAMP)",
            "INSERT INTO status VALUES('day', 0, CURRENT_TIMESTAMP)",
            "INSERT INTO status VALUES('week', 0, CURRENT_TIMESTAMP)",
            "INSERT INTO status VALUES('month', 0, CURRENT_TIMESTAMP)"
        };

        stmt = this.conn.createStatement();

        for (String item : domains) {
            stmt.executeUpdate(item);
        }

        for (String item : tables) {
            stmt.executeUpdate(item);
        }

        for (String item : index) {
            stmt.executeUpdate(item);
        }

        for (String item : data) {
            stmt.executeUpdate(item);
        }

        stmt.close();

        // For security, force commit
        if (!this.conn.getAutoCommit()) {
            this.conn.commit();
        }
    }

    public Connection getConnection() throws SQLException, ClassNotFoundException {
        return this.conn;
    }

    @Override
    public void close() throws SQLException {
        this.conn.close();
    }
}
