/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
    Description   A backup system
    License       GPL version 2 (see GPL.txt for details)
 */

package org.memento.client.context;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

/**
 *
 * @author enrico
 */

public class ContextSystem extends AbstractContext {

    public ContextSystem(Socket connection) {
        this.connection = connection;
    }

    @Override
    public Boolean parse(HashMap command) throws IOException {
        Boolean exit;
        ContextError error;
        HashMap result;

        switch (command.get("name").toString()) {
            case "exit":
                exit = Boolean.TRUE;
                break;
            default:
                result = new HashMap();
                error = new ContextError(this.connection);

                result.put("message", "Command not found");
                error.parse(result);
                exit = Boolean.FALSE;
                break;
        }
        
        return exit;
    }
}
