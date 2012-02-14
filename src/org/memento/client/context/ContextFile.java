/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
    Description   A backup system
    License       GPL version 2 (see GPL.txt for details)
 */

package org.memento.client.context;

import flexjson.JSONSerializer;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import org.memento.client.context.commands.CommandFile;
import org.memento.json.FileAttrs;

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
    
    private void cmdGetFile(String fileName) throws FileNotFoundException, IOException {
        BufferedInputStream buff;
        BufferedOutputStream outStream;
        File data;
        byte[] buffer;
        int read;

        data = new File(fileName);
        buffer = new byte[1024];

        if (!data.exists()) {
            throw new FileNotFoundException("File not exist");
        }
        
        if (data.isDirectory()) {
            throw new IllegalArgumentException(fileName + " is not a file");
        }

        buff = new BufferedInputStream(new FileInputStream(data));
        outStream = new BufferedOutputStream(this.connection.getOutputStream());
        
        while ((read = buff.read(buffer)) != -1) {
            outStream.write(buffer, 0, read);
            outStream.flush();
        }

        outStream.close();
        buff.close();
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
