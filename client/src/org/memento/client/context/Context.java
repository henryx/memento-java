/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client.context;

import flexjson.JSONSerializer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.memento.client.context.commands.CommandFile;

/**
 *
 * @author enrico
 */

public class Context {
    private Socket connection;

    public Context(Socket connection) {
        this.connection = connection;
    }
    
    private void cmdListFile(String directory, boolean acl) throws FileNotFoundException, IOException {
        CommandFile cmd;
        File path;
        PrintWriter out;

        out = new PrintWriter(this.connection.getOutputStream(), true);

        cmd = new CommandFile();
        path = new File(directory);

        cmd.setDirectory(directory);
        cmd.setAcl(acl);
        cmd.setWriter(out);

        if (path.isDirectory()) {
            cmd.walk(path);
        } else {
            throw new IllegalArgumentException("Directory cannot be read: " + path.toString());
        }
    }

    private void cmdGetFile(String fileName) throws FileNotFoundException, IOException {
        BufferedInputStream buff;
        BufferedOutputStream outStream;
        File data;
        FileInputStream fis;
        byte[] buffer;
        int read;

        data = new File(fileName);
        buffer = new byte[8192];

        if (!data.exists()) {
            throw new FileNotFoundException("File not exist");
        }

        if (data.isDirectory()) {
            throw new IllegalArgumentException(fileName + " is not a file");
        }

        fis = new FileInputStream(data);
        buff = new BufferedInputStream(fis);
        outStream = new BufferedOutputStream(this.connection.getOutputStream());

        while ((read = buff.read(buffer)) != -1) {
            outStream.write(buffer, 0, read);
            outStream.flush();
        }

        data = null;
        fis.close();
        buff.close();
        outStream.close();
    }

    private void cmdPutFile(String toString) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean parseFile(HashMap command) throws IOException {
        ArrayList paths;
        boolean exit;
        Context error;
        HashMap errMsg;

        exit = false;
        switch (command.get("name").toString()) {
            case "list":
                try {
                    paths = (ArrayList) command.get("directory");
                    if (paths.isEmpty()) {
                        throw new ClassCastException("List not definied");
                    }

                    for (int item = 0; item < paths.size(); item++) {
                        this.cmdListFile(paths.get(item).toString(), (boolean) command.get("acl"));
                    }
                } catch (ClassCastException ex) {
                    // TODO: manage exception
                }
                break;
            case "get":
                this.cmdGetFile(command.get("filename").toString());
                break;
            case "put":
                this.cmdPutFile(command.get("filename").toString());
                break;
            default:
                errMsg = new HashMap();
                error = new Context(this.connection);

                errMsg.put("message", "Command not found");

                error.parseError(errMsg);
                break;
        }

        return exit;
    }
    
    public boolean parseSystem(HashMap command) {
        HashMap result;
        Process p;
        String message;
        boolean exit;
        int status;

        message = "";

        exit = false;
        switch (command.get("name").toString()) {
            case "exit":
                exit = true;
                break;
            case "exec":
                try {
                    p = Runtime.getRuntime().exec(command.get("value").toString());
                    status = p.waitFor();

                    if (status != 0) {
                        message = "Error when executing external command: " + status;
                    }
                } catch (InterruptedException | IOException ex) {
                    message = "Error when executing external command: " + ex.getMessage();
                }

                break;
            default:
                message = "Command not found";
                break;
        }

        try {
            if (!message.isEmpty()) {
                result = new HashMap();

                result.put("message", message);
                this.parseError(result);
            }
        } catch (IOException ex) {
            Logger.getLogger(Context.class.getName()).log(Level.SEVERE, null, ex);
        }

        return exit;
    }

    public boolean parseError(HashMap command) throws IOException {
        boolean exit;
        HashMap result;
        JSONSerializer serializer;
        PrintWriter out;

        out = new PrintWriter(connection.getOutputStream(), true);
        result = command;
        exit = false;
        serializer = new JSONSerializer();

        result.put("result", "error");
        out.println(serializer.exclude("*.class").serialize(result));

        return exit;
    }
}
