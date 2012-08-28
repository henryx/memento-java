/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.json.commands;

/**
 *
 * @author enrico
 */
public class CommandFile extends Command {

    // For list command:
    private String[] directory;
    private boolean acl;
    
    // For get/put command:
    private String filename;

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
    public boolean getAcl() {
        return acl;
    }

    /**
     * @param acl the acl to set
     */
    public void setAcl(boolean acl) {
        this.acl = acl;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }
}
