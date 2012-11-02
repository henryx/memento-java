/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.management;

import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

/**
 *
 * @author enrico
 */
public interface Operation extends Properties, Runnable {

    public void setDbStore(DbStorage dbstore);
    public DbStorage getDbStore();
    public void setFsStore(FileStorage fsstore);
    public FileStorage getFsStore();

    @Override
    public void run();
}
