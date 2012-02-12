/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
    Description   A backup system
    License       GPL version 2 (see GPL.txt for details)
 */

package org.application.backupsync.client.context;

import flexjson.JSONSerializer;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import org.application.backupsync.client.context.commands.CommandFile;
import org.application.backupsync.json.FileAttrs;

/**
 *
 * @author enrico
 */

public class ContextFile extends AbstractContext {
    public ContextFile(Socket connection) {
        this.connection = connection;
    }
    
    private void cmdListFile(String directory, Boolean acl) throws IOException {
        HashMap<String, FileAttrs> result;
        JSONSerializer serializer;
        PrintWriter out;

        serializer = new JSONSerializer();
        out = new PrintWriter(this.connection.getOutputStream(), true);

        result = new CommandFile(directory, acl).get();
        out.println(serializer.exclude("*.class").deepSerialize(result));
    }
    
    private void cmdGetFile(String fileName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Boolean parse(HashMap command) throws IOException {
        ArrayList paths;
        Boolean exit;
        ContextError error;
        HashMap errMsg;

        exit = Boolean.FALSE;
        switch (command.get("name").toString()) {
            case "list":
                try {
                    paths = (ArrayList) command.get("directory");
                    if (paths.isEmpty()) {
                        throw new ClassCastException("List not definied");
                    }

                    for (int item = 0; item < paths.size(); item++) {
                        this.cmdListFile(paths.get(item).toString(), (Boolean) command.get("acl"));
                    }
                } catch (ClassCastException ex) {
                    // TODO: manage exception
                }
                break;
            case "get":
                this.cmdGetFile(command.get("file").toString());
                break;
            default:
                errMsg = new HashMap();
                error = new ContextError(this.connection);

                errMsg.put("message", "Command not found");

                error.parse(errMsg);
                break;
        }

        return exit;
    }
}
