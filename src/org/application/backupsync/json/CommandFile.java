/*
Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
Project       BackupSYNC
Description   A backup system
License       GPL version 2 (see GPL.txt for details)
 */

package org.application.backupsync.json;

import java.util.ArrayList;

/**
 *
 * @author enrico
 */
public class CommandFile {
    private String name;
    
    // For list commands
    private ArrayList<String> directory;
    private Boolean acl;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the directory
     */
    public ArrayList<String> getDirectory() {
        return directory;
    }

    /**
     * @param directory the directory to set
     */
    public void setDirectory(ArrayList<String> directory) {
        this.directory = directory;
    }

    /**
     * @return the acl
     */
    public Boolean getAcl() {
        return acl;
    }

    /**
     * @param acl the acl to set
     */
    public void setAcl(Boolean acl) {
        this.acl = acl;
    }
}
