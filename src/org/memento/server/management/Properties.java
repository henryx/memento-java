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
public interface Properties {

    public void setDataset(Integer dataset);
    public Integer getDataset();

    public void setGrace(String grace);
    public String getGrace();

    public void setSection(String section);
    public String getSection();
    
}
