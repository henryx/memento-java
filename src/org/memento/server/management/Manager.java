/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
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
import java.nio.file.Files;
import java.nio.file.Paths;
import org.ini4j.Wini;

public class Manager {
    private String mode;
    private Wini cfg;
    
    private void checkStructure() throws IOException {
        File directory;
        String[] subdirectories;
        
        directory = new File(this.cfg.get("general", "repository"));
        subdirectories = new String[] {"hour", "day", "week", "month"};
        
        if (directory.isFile()) {
            throw new IllegalArgumentException(directory + " is a file");
        }
        
        if (!directory.exists()) {
            for (String subdirectory: subdirectories) {
                Files.createDirectories(Paths.get(directory.getAbsolutePath(), subdirectory));
            }
            
        }
    }
    
    public Manager(Wini cfg) throws IOException {
        this.cfg = cfg;
        this.checkStructure();
    }

    /**
     * @return the mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public void go() {
        
    }
}
