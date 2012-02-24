/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
    Description   A backup system
    License       GPL version 2 (see GPL.txt for details)
 */

package org.memento.server.management;

import java.util.HashMap;

/**
 *
 * @author enrico
 */
public interface Storage extends Properties {
    public void add(HashMap json);
}
