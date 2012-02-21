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
public interface Operation extends Properties {

    public void setDbStore(Storage dbstore);
    public Storage getDbStore();
    public void setFsStore(Storage fsstore);
    public Storage getFsStore();

    public void run();
}
