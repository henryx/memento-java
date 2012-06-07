/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.server.management;

/**
 *
 * @author enrico
 */

import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Wini;
import org.memento.server.operation.FileOperation;
import org.memento.server.storage.DBConnection;
import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

public class Manager {
    private String grace;
    private Wini cfg;

    public Manager(Wini cfg) throws IOException {
        this.cfg = cfg;
    }

    private Operation compute(String name) {
        String type;

        type = this.cfg.get(name, "type");
        switch (type) {
            case "file":
                FileOperation result = new FileOperation(this.cfg);
                return result;
            default:
                throw new UnsupportedOperationException("type method not supported");
        }
    }
    
    // Ugly. This is not a good place for getting last dataset from database
    private Integer getLastDataset() {
        Connection conn;
        Integer result;
        PreparedStatement pstmt;
        ResultSet res;

        try {
            conn = DBConnection.getInstance().getConnection("system", this.cfg.get("general", "repository"));
            pstmt = conn.prepareStatement("SELECT actual FROM status WHERE grace = ?");
            pstmt.setString(1, this.grace);

            res = pstmt.executeQuery();

            res.next();
            result = res.getInt(1);
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
            result = 0;
        }

        return result;
    }

    /**
     * @return the mode
     */
    public String getGrace() {
        return grace;
    }

    /**
     * @param grace the mode to set
     */
    public void setGrace(String grace) {
        this.grace = grace;
    }

    public void go() throws IOException {
        Integer dataset;
        Operation operation;
        FileStorage fsStorage;
        DbStorage dbStorage;

        fsStorage = new FileStorage(this.cfg);
        dbStorage = new DbStorage(this.cfg);

        fsStorage.setGrace(this.grace);
        dbStorage.setGrace(this.grace);

        dataset = this.getLastDataset();

        if (dataset >= Integer.decode(this.cfg.get("dataset", this.grace))) {
            dataset = 1;
        } else {
            dataset = dataset + 1;
        }

        fsStorage.setDataset(dataset);
        dbStorage.setDataset(dataset);

        for (String section : this.cfg.keySet()) {
            if (!(section.equals("general") || section.equals("dataset"))) {
                fsStorage.setSection(section);
                dbStorage.setSection(section);

                operation = this.compute(section);

                operation.setGrace(this.grace);
                operation.setDataset(dataset);
                operation.setSection(section);

                operation.setDbStore(dbStorage);
                operation.setFsStore(fsStorage);

                operation.run();
            }
        }
    }
}
