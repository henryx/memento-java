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

            out.println(serializer.exclude("*.class").serialize(result));
            cmd.receiveFile();
        } else {
            destFile.mkdir();
        }
    }
    
    private void cmdSetAcls(String name, ArrayList<HashMap> acls) throws IOException {
        ArrayList<FileAcl> items;
        FileAcl acl;
        PathName path;

        items = new ArrayList<>();
        for (HashMap item : acls) {
            acl = new FileAcl();
            acl.setName(item.get("name").toString());
            acl.setAclType(item.get("aclType").toString());
            acl.setAttrs(item.get("attrs").toString());
            
            items.add(acl);
        }

        path = new PathName(new File(name));
        path.setAcl(items);
    }

    private void cmdSetAttrs(String attributes) throws IOException {
        FileAttrs attrs;
        PathName path;

        attrs = new JSONDeserializer<FileAttrs>().deserialize(attributes, FileAttrs.class);
        path = new PathName(new File(attrs.getName()));

        path.setAttrs(attrs);
    }

    public boolean parseFile(HashMap command) throws IOException {
        ArrayList paths;
        Context error;
        HashMap errMsg;
        boolean exit;

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
                this.cmdSendFile(command.get("filename").toString());
                break;
            case "put":
                this.cmdReceiveFile(command.get("filename").toString(), ((HashMap)command.get("attrs")).get("type").toString());
                this.cmdSetAttrs(new JSONSerializer().serialize(command.get("attrs")));

                if (Boolean.parseBoolean(command.get("acl").toString())) {
                    if (((HashMap)command.get("attrs")).get("acl") instanceof ArrayList) {
                        this.cmdSetAcls(command.get("filename").toString(),
                                (ArrayList<HashMap>)((HashMap)command.get("attrs")).get("acl"));
                    }
                }                
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

        switch (command.get("name").toString()) {
            case "exit":
                exit = true;
                break;
            case "exec":
                if (System.getProperty("os.name").startsWith("Windows")) {
                    cmd[0] = "cmd.exe";
                    cmd[1] = "/C";
                } else {
                    cmd[0] = "/bin/sh";
                    cmd[1] = "-c";
                }
                cmd[2] = command.get("value").toString();

                try {
                    p = Runtime.getRuntime().exec(cmd);
                    status = p.waitFor();

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
                result = new HashMap();
                serializer = new JSONSerializer();

                result.put("result", "ok");
                result.put("version", Main.VERSION);

                try {
                    out = new PrintWriter(this.connection.getOutputStream(), true);
                    out.println(serializer.exclude("*.class").serialize(result));
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
        out.println(serializer.exclude("*.class").serialize(result));

        return exit;
    }
}
