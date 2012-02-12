/*
Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
Project       BackupSYNC
Description   A backup system
License       GPL version 2 (see GPL.txt for details)
 */

package org.memento.json;

/**
 *
 * @author enrico
 */
public class Context {
    private String context;
    CommandFile command;

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
    public CommandFile getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(CommandFile command) {
        this.command = command;
    }
}
