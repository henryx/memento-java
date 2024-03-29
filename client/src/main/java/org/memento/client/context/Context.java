/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.client.context;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.memento.PathName;
import org.memento.client.Main;
import org.memento.client.context.commands.CommandFile;
import org.memento.json.FileAcl;
import org.memento.json.FileAttrs;
import org.memento.json.commands.CommandSystem;

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

        cmd = new CommandFile(this.connection);
        path = new File(directory);

        cmd.setFile(path);
        cmd.setAcl(acl);

        if (path.isDirectory()) {
            Main.LOGGER.fine("Context - extract metadata");
            cmd.walk();
        } else {
            throw new IllegalArgumentException("Directory cannot be read: " + path.toString());
        }
    }

    private void cmdSendFile(String fileName) throws FileNotFoundException, IOException {
        CommandFile cmd;
        File srcFile;

        cmd = new CommandFile(this.connection);
        srcFile = new File(fileName);

        if (!srcFile.exists()) {
            throw new FileNotFoundException("File not exist");
        }

        if (srcFile.isDirectory()) {
            throw new IllegalArgumentException(srcFile + " is not a file");
        }

        cmd.setFile(srcFile);
        cmd.sendFile();
    }

    private void cmdReceiveFile(String fileName, String type) throws IOException {
        CommandFile cmd;
        File destFile;
        File bakFile;
        File parent;
        HashMap<String, String> result;
        JSONSerializer serializer;
        PrintWriter out;

        destFile = new File(fileName);
        parent = new File(destFile.getParent());
        cmd = new CommandFile(this.connection);
        result = new HashMap<>();
        serializer = new JSONSerializer();
        out = new PrintWriter(this.connection.getOutputStream(), true);

        if (destFile.exists()) {
            bakFile = new File(fileName + "." + Calendar.getInstance().getTimeInMillis());
            Files.move(destFile.toPath(), bakFile.toPath());
        }

        if (!parent.exists()) {
            destFile.mkdirs();
        }

        if (type.equals("file")) {

            cmd.setFile(destFile);

            result.put("context", "restore");
            result.put("result", "ok");

            out.println(serializer.serialize(result));
            cmd.receiveFile();
        } else {
            destFile.mkdir();
        }
    }

    private void cmdSetAcls(String name, ArrayList<FileAcl> acls) throws IOException {
        PathName path;

        path = new PathName(new File(name));
        path.setAcl(acls);
    }

    private void cmdSetAttrs(String attributes) throws IOException {
        FileAttrs attrs;
        PathName path;

        attrs = new JSONDeserializer<FileAttrs>().deserialize(attributes, FileAttrs.class);
        path = new PathName(new File(attrs.getName()));

        path.setAttrs(attrs);
    }

    public boolean parseFile(org.memento.json.commands.CommandFile command) throws IOException {
        String[] paths;
        Context error;
        HashMap errMsg;
        boolean exit;

        exit = false;
        switch (command.getName()) {
            case "list":
                Main.LOGGER.fine("Context - Received list files command");
                try {
                    paths = command.getDirectory();
                    if (paths.length == 0) {
                        throw new ClassCastException("List not definied");
                    }

                    for (String item : paths) {
                        this.cmdListFile(item, command.getAcl());
                    }
                } catch (ClassCastException ex) {
                    // TODO: manage exception
                }
                break;
            case "get":
                Main.LOGGER.fine("Context - Received get file command");
                this.cmdSendFile(command.getFilename());
                break;
            case "put":
                Main.LOGGER.fine("Context - Received put file command");
                this.cmdReceiveFile(command.getFilename(), command.getAttrs().getType());
                this.cmdSetAttrs(new JSONSerializer().serialize(command.getAttrs()));

                if (command.getAcl()) {
                    this.cmdSetAcls(command.getFilename(), command.getAttrs().getAcl());
                }
                break;
            default:
                Main.LOGGER.fine("Context - no file command received");
                errMsg = new HashMap();
                error = new Context(this.connection);

                errMsg.put("message", "Command not found");

                error.parseError(errMsg);
                break;
        }

        return exit;
    }

    public boolean parseSystem(CommandSystem command) {
        BufferedReader bre;
        HashMap result;
        JSONSerializer serializer;
        PrintWriter out;
        Process p;
        String line;
        String message;
        String[] cmd;
        boolean exit;
        int status;

        message = "";
        exit = false;
        cmd = new String[3];

        switch (command.getName()) {
            case "exit":
                Main.LOGGER.fine("Context - Exit command");
                exit = true;
                break;
            case "exec":
                // FIXME: it doesn't work if STDERR or STDOUT are too big.
                //        Current workaround is redirect STDOUT and STDERR to
                //        /dev/null (linux) or NUL (windows)
                
                Main.LOGGER.fine("Context - Exec command");
                if (System.getProperty("os.name").startsWith("Windows")) {
                    cmd[0] = "cmd.exe";
                    cmd[1] = "/C";
                } else {
                    cmd[0] = "/bin/sh";
                    cmd[1] = "-c";
                }
                cmd[2] = command.getValue();

                try {
                    p = new ProcessBuilder().inheritIO().command(cmd).start();
                    status = p.waitFor();
                    
                    Main.LOGGER.fine("Context - command executed");

                    if (status != 0) {
                        message = "Error when executing external command:\n";

                        bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                        while ((line = bre.readLine()) != null) {
                            message = message + "\t" + line + "\n";
                        }
                        bre.close();
                    }
                } catch (InterruptedException | IOException ex) {
                    message = "Error when executing external command: " + ex.getMessage();
                }
                break;
            case "version":
                Main.LOGGER.fine("Context - version command");
                result = new HashMap();
                serializer = new JSONSerializer();

                result.put("result", "ok");
                result.put("version", Main.VERSION);

                try {
                    out = new PrintWriter(this.connection.getOutputStream(), true);
                    out.println(serializer.serialize(result));
                } catch (IOException ex) {
                    message = "Error when sending client version: " + ex.getMessage();
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
        HashMap result;
        JSONSerializer serializer;
        PrintWriter out;
        boolean exit;

        out = new PrintWriter(connection.getOutputStream(), true);
        result = command;
        exit = false;
        serializer = new JSONSerializer();

        result.put("result", "error");
        out.println(serializer.serialize(result));

        return exit;
    }
}
