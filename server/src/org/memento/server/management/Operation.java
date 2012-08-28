/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.server.management;

import java.io.IOException;
import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

/**
 *
 * @author enrico
 */
public abstract class Operation implements Properties {

    public abstract void setDbStore(DbStorage dbstore);
    public abstract DbStorage getDbStore();
    public abstract void setFsStore(FileStorage fsstore);
    public abstract FileStorage getFsStore();

    public void preCommand(String command) throws IOException, InterruptedException {
        Process p;
        
        p = Runtime.getRuntime().exec(command);
        p.wait();
    }
    
    public void postCommand(String command) throws IOException, InterruptedException {
        Process p;
        
        p = Runtime.getRuntime().exec(command);
        p.wait();
    }
    
    public abstract void run();
}
