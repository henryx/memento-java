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
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Wini;
import org.memento.server.operation.FileOperation;
import org.memento.server.storage.DBConnection;
import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

public class Manager {

    private String grace;
    private boolean reload;
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

    // FIXME: Ugly. This is not a good place for getting last dataset from database
    private Integer getLastDataset() {
        Connection conn;
        Integer result;
        PreparedStatement pstmt;
        ResultSet res;
        final String SELECT = "SELECT actual FROM status WHERE grace = ?";
        
        try {
            conn = DBConnection.getInstance().getConnection("system", this.cfg.get("general", "repository"));
            
            pstmt = conn.prepareStatement(SELECT);
            pstmt.setString(1, this.grace);
            
            res = pstmt.executeQuery();
            
            res.next();
            result = res.getInt(1);
            
            res.close();
            pstmt.close();
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
            result = 0;
        }
        
        return result;
    }

    // FIXME: Ugly. This is not a good place for getting last dataset from database
    private void setLastDataset(Integer dataset) {
        Connection conn;
        PreparedStatement pstmt;
        final String UPDATE = "UPDATE status SET actual = ? WHERE grace = ?";
        
        try {
            conn = DBConnection.getInstance().getConnection("system", this.cfg.get("general", "repository"));
            
            pstmt = conn.prepareStatement(UPDATE);
            pstmt.setInt(1, dataset);
            pstmt.setString(2, this.grace);
            
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
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
    
    public void sync() throws IOException {
        Integer dataset;
        Operation operation;
        FileStorage fsStorage;
        DbStorage dbStorage;
        
        fsStorage = new FileStorage(this.cfg);
        dbStorage = new DbStorage(this.cfg);
        
        fsStorage.setGrace(this.grace);
        dbStorage.setGrace(this.grace);
        
        dataset = this.getLastDataset();
        
        if (!this.reload) {
            if (dataset >= Integer.decode(this.cfg.get("dataset", this.grace))) {
                dataset = 1;
            } else {
                dataset = dataset + 1;
            }
        }
        
        fsStorage.setDataset(dataset);
        dbStorage.setDataset(dataset);
        
        this.remove(dataset);
        
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
        
        this.setLastDataset(dataset);
    }
    
    public void remove(Integer dataset) throws IOException {
        class Remove {
            public void remove(File aDir) {
                if (aDir.exists()) {
                    for (File child : aDir.listFiles()) {
                        if (child.isDirectory()) {
                            this.remove(child);
                        } else {
                            child.delete();
                        }
                    }
                }
                aDir.delete();
            }
        }
        
        File directory;
        String sep;
        
        sep = System.getProperty("file.separator");
        
        directory = new File(this.cfg.get("general", "repository")
                + sep
                + this.grace
                + sep
                + dataset);
        
        new Remove().remove(directory);
    }
}
