/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.management;

/**
 *
 * @author enrico
 */
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.ini4j.Wini;
import org.memento.server.Main;
import org.memento.server.operation.FileOperation;
import org.memento.server.net.DBConnection;
import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

public class Manager {

    private HashMap<String, String> connData;
    private String grace;
    private Wini cfg;
    private boolean reload;

    public Manager(Wini cfg) throws IOException {
        this.cfg = cfg;

        this.connData = new HashMap<>();
        this.connData.put("host", this.cfg.get("database", "host"));
        this.connData.put("port", this.cfg.get("database", "port"));
        this.connData.put("dbname", this.cfg.get("database", "dbname"));
        this.connData.put("user", this.cfg.get("database", "user"));
        this.connData.put("password", this.cfg.get("database", "password"));
    }

    private Operation compute(String section) {
        String type;

        type = this.cfg.get(section, "type");
        switch (type) {
            case "file":
                FileOperation result = new FileOperation(this.cfg);
                return result;
            default:
                // TODO: write specific system operations
                throw new UnsupportedOperationException("type method not supported");
        }
    }

    // FIXME: Ugly. This is not a good place for getting last dataset from database
    private Integer getLastDataset() {

        Integer result;
        PreparedStatement pstmt;
        ResultSet res;
        final String SELECT = "SELECT actual FROM status WHERE grace = ?";

        try (DBConnection dbm = new DBConnection(this.connData, true)) {
            pstmt = dbm.getConnection().prepareStatement(SELECT);
            pstmt.setString(1, this.grace);

            res = pstmt.executeQuery();

            res.next();
            result = res.getInt(1);

            res.close();
            pstmt.close();
        } catch (SQLException | ClassNotFoundException ex) {
            Main.logger.debug("Problems to retrieve last dataset processed: ", ex);
            result = 0;
        }

        return result;
    }

    // FIXME: Ugly. This is not a good place for setting last dataset into database
    private void setLastDataset(Integer dataset) {
        PreparedStatement pstmt;
        final String UPDATE = "UPDATE status SET actual = ?, last_run = CURRENT_TIMESTAMP WHERE grace = ?";

        try (DBConnection dbm = new DBConnection(this.connData, true)) {
            pstmt = dbm.getConnection().prepareStatement(UPDATE);
            pstmt.setInt(1, dataset);
            pstmt.setString(2, this.grace);

            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException | ClassNotFoundException ex) {
            Main.logger.error("Problems whe setting last dataset processed: " + ex.getMessage());
            Main.logger.debug("Problems whe setting last dataset processed", ex);
        }
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

    public void setReload(boolean reload) {
        this.reload = reload;
    }

    public boolean getReload() {
        return this.reload;
    }

    public void exec(String mode) throws IOException, SQLException, ClassNotFoundException {
        Integer dataset;
        Operation operation;
        FileStorage fsStorage;
        DbStorage dbStorage;
        ExecutorService pool;

        // NOTE: maximum concurrent threads is hardcoded for convenience
        pool = Executors.newFixedThreadPool(5);

        if (mode.equals("sync")) {
            dataset = this.getLastDataset();
            if (!this.reload) {
                if (dataset >= Integer.decode(this.cfg.get("dataset", this.grace))) {
                    dataset = 1;
                } else {
                    dataset = dataset + 1;
                }
            }
            this.remove(dataset);
        } else {
            dataset = Integer.decode(this.cfg.get("general", "dataset"));
        }

        Main.logger.info("Dataset processed: " + dataset);

        for (String section : this.cfg.keySet()) {
            if (!(section.equals("general") || section.equals("dataset") || section.equals("database"))) {
                Main.logger.info("About to execute section " + section);

                fsStorage = new FileStorage(this.cfg);
                dbStorage = new DbStorage(this.cfg);

                fsStorage.setOperationType(mode);
                dbStorage.setOperationType(mode);

                fsStorage.setGrace(this.grace);
                dbStorage.setGrace(this.grace);

                fsStorage.setDataset(dataset);
                dbStorage.setDataset(dataset);

                fsStorage.setSection(section);
                dbStorage.setSection(section);

                operation = this.compute(section);
                operation.setOperationType(mode);

                operation.setGrace(this.grace);
                operation.setDataset(dataset);
                operation.setSection(section);

                operation.setDbStore(dbStorage);
                operation.setFsStore(fsStorage);

                pool.execute(operation);
            }
        }

        pool.shutdown();
        try {
            // NOTE: timeout is hardcoded for convenience
            pool.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException ex) {
            Main.logger.debug("Problems when awaiting thread pool: ", ex);
        }

        if (mode.equals("sync")) {
            this.setLastDataset(dataset);
        }
    }

    public void remove(Integer dataset) throws IOException {
        class Remove {

            public void remove(File aDir) throws IOException {
                if (aDir.exists()) {
                    for (File child : aDir.listFiles()) {
                        if (child.isDirectory()) {
                            this.remove(child);
                        } else {
                            Files.delete(child.toPath());
                        }
                    }
                }
                Files.delete(aDir.toPath());
            }
        }

        File directory;
        PreparedStatement pstmt;
        String sep;
        String[] delete;

        sep = System.getProperty("file.separator");
        delete = new String[]{
            "DELETE FROM attrs WHERE dataset = ? AND grace = ?",
            "DELETE FROM acls WHERE dataset = ? AND grace = ?"
        };

        try (DBConnection dbm = new DBConnection(this.connData, true);) {

            for (String item : delete) {
                pstmt = dbm.getConnection().prepareStatement(item);
                pstmt.setInt(1, dataset);
                pstmt.setString(2, this.grace);
                pstmt.execute();

                pstmt.close();
            }
        } catch (SQLException | ClassNotFoundException ex) {
            Main.logger.error("Could not remove dataset: " + ex.getMessage());
            Main.logger.debug("Could not remove dataset: ", ex);
        }

        directory = new File(this.cfg.get("general", "repository")
                + sep
                + this.grace
                + sep
                + dataset);

        if (directory.exists()) {
            Main.logger.debug("About to remove " + directory.getAbsolutePath());
            try {
                new Remove().remove(directory);
            } catch (DirectoryNotEmptyException ex) {
                Main.logger.error("Directory " + directory + " is not empty: " + ex.getMessage());
                Main.logger.debug("Directory " + directory + " is not empty", ex);
            } catch (FileSystemException ex) {
                Main.logger.error("Problems when remove old dataset: " + ex.getMessage());
                Main.logger.debug("Problems when remove old dataset", ex);
            }

            Main.logger.debug("Directory " + directory.getAbsolutePath() + " removed");
        }
    }
}
