/*
Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
Project       BackupSYNC
Description   A backup system
License       GPL version 2 (see GPL.txt for details)
 */

package org.application.backupsync.json;

import java.util.HashMap;

/**
 *
 * @author enrico
 */
public class Context {
    private String context;
    private HashMap<String, String> command;

    /**
     * @return the context
     */
    public String getContext() {
        return context;
    }

    /**
     * @param context the context to set
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * @return the command
     */
    public HashMap<String, String> getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(HashMap<String, String> command) {
        this.command = command;
    }
}
