/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
    Description   A backup system
    License       GPL version 2 (see GPL.txt for details)
 */

package org.memento.server.management;

import org.memento.json.FileAttrs;

/**
 *
 * @author enrico
 */
public interface Storage extends Properties {
    public void add(FileAttrs json);
}
