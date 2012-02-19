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

import java.io.IOException;
import org.ini4j.Wini;
import org.memento.server.storage.FileStorage;

public class Manager {
    private String grace;
    private Wini cfg;
    
    public Manager(Wini cfg) throws IOException {
        this.cfg = cfg;
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
        Storage fsStorage;
        Storage dbStorage;
        
        fsStorage = new FileStorage(cfg);
    }
}
