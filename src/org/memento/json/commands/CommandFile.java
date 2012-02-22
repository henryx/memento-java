/*
Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
Project       BackupSYNC
Description   A backup system
License       GPL version 2 (see GPL.txt for details)
 */

package org.memento.json.commands;

/**
 *
 * @author enrico
 */
public class CommandFile extends Command {

    // For list commands
    private String[] directory;
    private Boolean acl;

    /**
     * @return the directory
     */
    public String[] getDirectory() {
        return directory;
    }

    /**
     * @param directory the directory to set
     */
    public void setDirectory(String[] directory) {
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
