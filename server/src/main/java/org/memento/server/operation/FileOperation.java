/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.operation;

import flexjson.JSONDeserializer;
import flexjson.JSONException;
import flexjson.JSONSerializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.FileSystemException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import org.ini4j.Wini;
import org.memento.json.Context;
import org.memento.json.FileAttrs;
import org.memento.json.commands.CommandFile;
import org.memento.json.commands.CommandSystem;
import org.memento.server.Main;
import org.memento.server.management.Operation;
import org.memento.server.net.Client;

/**
 *
 * @author enrico
 */
public class FileOperation extends Operation {

    private Wini cfg;

    public FileOperation(Wini cfg) {
        this.cfg = cfg;
    }

    private void backupFile(String context, BufferedReader in) throws ClassNotFoundException, SQLException, UnknownHostException, IOException {
        FileAttrs item;
        Iterator<FileAttrs> items;
        String line;

        Main.logger.debug("About to read all files metadata");
        while (true) {
            line = in.readLine();

            if (line == null) {
                this.dbstore.commit();
                break;
            }

            if (context.equals("file")) {
                try {
                    item = new JSONDeserializer<FileAttrs>().deserialize(line);

                    if (Boolean.parseBoolean(cfg.get(this.section, "compress"))) {
                        item.setCompressed(Boolean.TRUE);
                    } else {
                        item.setCompressed(Boolean.FALSE);
                    }

                    this.dbstore.add(item);
                } catch (ClassCastException | JSONException ex) {
                    if (ex instanceof ClassCastException) {

                        HashMap err = new JSONDeserializer<HashMap>().deserialize(line);
                        if (err.containsKey("result") && err.get("result").equals("error")) {
                            Main.logger.error("Metadata parsing error for host's item: " + this.section + ": " + err.get("message"));
                        }
                        Main.logger.debug("Metadata parsing error for host's item: " + this.section + ": " + line, ex);
                    }

                    if (ex instanceof JSONException) {
                        throw new SocketException("Socket read error for host's item: " + this.section + ": " + ex);
                    }
                } catch (SQLException ex) {
                    // NOTE: this skip insert for file
                    Main.logger.debug("Error saving metadata for item: " + this.section + ": " + line, ex);
                }
            }
        }

        if (context.equals("file")) {
            Main.logger.debug("Al files metadata readed");

            Main.logger.debug("About to create directory structure");
            items = this.dbstore.listItems("directory", false);
            while (items.hasNext()) {
                item = items.next();
                this.fsstore.get(item);
            }
            Main.logger.debug("Directory structure created");

            Main.logger.debug("About to download files from remote server");
            items = this.dbstore.listItems("file", false);
            while (items.hasNext()) {
                item = items.next();
                if (this.dbstore.isItemExist(item)) {
                    item.setPreviousDataset(Boolean.TRUE);
                } else {
                    item.setPreviousDataset(Boolean.FALSE);
                }

                if (Boolean.parseBoolean(cfg.get(this.section, "compress"))) {
                    item.setCompressed(Boolean.TRUE);
                } else {
                    item.setCompressed(Boolean.FALSE);
                }

                try {
                    this.fsstore.get(item);
                } catch (FileSystemException ex) {
                    Main.logger.error("File error for host: " + this.section + ": " + ex.getMessage());
                    Main.logger.debug("File error for host: " + this.section, ex);
                    this.dbstore.remove(item.getName());
                    this.dbstore.commit();
                }
            }
            Main.logger.debug("Files downloaded");

            Main.logger.debug("About to create symlinks");
            items = this.dbstore.listItems("symlink", false);
            while (items.hasNext()) {
                item = items.next();
                this.fsstore.get(item);
            }
            Main.logger.debug("Symlinks created");
        }
    }

    private void sendCommand(Context command) throws UnknownHostException, IOException, SQLException, ClassNotFoundException, SocketTimeoutException {
        JSONSerializer serializer;

        serializer = new JSONSerializer();
        try (Client client = new Client(this.section, this.cfg);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.socket().getInputStream()));
                PrintWriter out = new PrintWriter(client.socket().getOutputStream(), true)) {

            Main.logger.debug("Connection for "
                    + this.cfg.get(this.section, "host")
                    + ":" + this.cfg.get(this.section, "port")
                    + " is opened");

            out.println(serializer.deepSerialize(command));
            out.flush();

            Main.logger.debug("About to parse " + command.getContext() + " command");
            this.backupFile(command.getContext(), in);
        }
    }

    private void sync() throws ClassNotFoundException, ConnectException, SQLException, IOException, UnknownHostException {
        Context context;
        CommandFile cfile;

        context = new Context();
        cfile = new CommandFile();

        context.setContext("file");
        cfile.setName("list");
        cfile.setDirectory(this.cfg.get(this.section, "path").split(","));
        cfile.setAcl(Boolean.parseBoolean(this.cfg.get(this.section, "acl")));
        context.setCommand(cfile);
        this.sendCommand(context);
    }

    private void restore() throws UnknownHostException, IOException, SQLException, ClassNotFoundException {
        CommandFile cfile;
        Iterator<FileAttrs> items;
        FileAttrs itemAttrs;
        String[] sections;
        
        cfile = new CommandFile();
        cfile.setName("put");
        cfile.setAcl(Boolean.parseBoolean(this.cfg.get(this.section, "acl")));

        if (!this.cfg.get(this.section, "path").equals("")) {
            Main.logger.info("Restoring path " + this.cfg.get(this.section, "path"));
            cfile.setFilename(this.cfg.get(this.section, "path"));
            cfile.setAttrs(this.dbstore.getFileAttrs(cfile.getFilename(), cfile.getAcl()));
            this.fsstore.put(cfile);
        } else {
            Main.logger.info("No path selected, make a full restore");
            sections = new String[]{"directory", "file", "symlink"};
            for (String item : sections) {
                items = this.dbstore.listItems(item, Boolean.parseBoolean(this.cfg.get(this.section, "acl")));
                while (items.hasNext()) {
                    itemAttrs = items.next();
                    
                    cfile.setFilename(itemAttrs.getName());
                    cfile.setAttrs(itemAttrs);
                    this.fsstore.put(cfile);
                }
            }
        }
    }

    @Override
    public void run() {
        Context context;
        CommandSystem csystem;

        context = new Context();
        try {
            if (!this.cfg.get(this.section, "pre_command").equals("")) {
                csystem = new CommandSystem();

                context.setContext("system");
                csystem.setName("exec");
                csystem.setValue(this.cfg.get(this.section, "pre_command"));
                context.setCommand(csystem);

                this.sendCommand(context);
                Main.logger.debug("Pre command executed");
            }

            if (this.getOperationType().equals("sync")) {
                this.sync();
            } else {
                this.restore();
            }

            if (!this.cfg.get(this.section, "post_command").equals("")) {
                csystem = new CommandSystem();

                context.setContext("system");
                csystem.setName("exec");
                csystem.setValue(this.cfg.get(this.section, "post_command"));
                context.setCommand(csystem);

                this.sendCommand(context);
                Main.logger.debug("Post command executed");
            }
        } catch (UnknownHostException ex) {
            Main.logger.error("Host not found: " + this.section + ": " + ex.getMessage());
            Main.logger.debug("Host not found: " + this.section, ex);
        } catch (ConnectException ex) {
            Main.logger.error("Connect error for host: " + this.section + ": " + ex.getMessage());
            Main.logger.debug("Connect error for host: " + this.section, ex);
        } catch (SocketTimeoutException ex) {
            Main.logger.error("Connect timeout for host: " + this.section + ": " + ex.getMessage());
            Main.logger.debug("Connect timeout for host: " + this.section, ex);
        } catch (IOException ex) {
            Main.logger.error("I/O error for host: " + this.section + ": " + ex.getMessage());
            Main.logger.debug("I/O error for host: " + this.section, ex);
        } catch (SQLException | ClassNotFoundException ex) {
            Main.logger.error("SQL error for host: " + this.section + ": " + ex.getMessage());
            Main.logger.debug("SQL error for host: " + this.section, ex);
        }
    }
}
