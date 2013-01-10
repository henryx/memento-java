/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.operation;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Iterator;
import org.ini4j.Wini;
import org.memento.json.Context;
import org.memento.json.FileAttrs;
import org.memento.json.commands.CommandFile;
import org.memento.json.commands.CommandSystem;
import org.memento.server.Main;
import org.memento.server.management.Operation;
import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

/**
 *
 * @author enrico
 */
public class FileOperation implements Operation {

    private Integer dataset;
    private DbStorage dbstore;
    private FileStorage fsstore;
    private String grace;
    private String operationType;
    private String section;
    private Wini cfg;

    public FileOperation(Wini cfg) {
        this.cfg = cfg;
    }

    private void backupFile(String context, BufferedReader in) throws ClassNotFoundException, SQLException, UnknownHostException, IOException {
        FileAttrs inJSON;
        FileAttrs item;
        Iterator<FileAttrs> items;
        String line;

        Main.logger.debug("About to read all files metadata");
        while (true) {
            line = in.readLine();

            if (line == null) {
                break;
            }

            if (context.equals("file")) {
                inJSON = new JSONDeserializer<FileAttrs>().deserialize(line);
                this.dbstore.add(inJSON);
            }
        }

        if (context.equals("file")) {
            Main.logger.debug("Al files metadata readed");

            Main.logger.debug("About to create directory structure");
            items = this.dbstore.listItems("directory");
            while (items.hasNext()) {
                item = items.next();
                this.fsstore.get(item);
            }
            Main.logger.debug("Directory structure created");

            Main.logger.debug("About to download files from remote server");
            items = this.dbstore.listItems("file");
            while (items.hasNext()) {
                item = items.next();
                if (this.dbstore.isItemExist(item)) {
                    item.setPreviousDataset(Boolean.TRUE);
                } else {
                    item.setPreviousDataset(Boolean.FALSE);
                }
                this.fsstore.get(item);
            }
            Main.logger.debug("Files downloaded");

            Main.logger.debug("About to create symlinks");
            items = this.dbstore.listItems("symlink");
            while (items.hasNext()) {
                item = items.next();
                this.fsstore.get(item);
            }
            Main.logger.debug("Symlinks created");
        }
    }

    private void sendCommand(Context command) throws UnknownHostException, IOException, SQLException, ClassNotFoundException {
        JSONSerializer serializer;

        serializer = new JSONSerializer();

        try (Socket conn = new Socket(this.cfg.get(this.section, "host"), Integer.parseInt(this.cfg.get(this.section, "port")));
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                PrintWriter out = new PrintWriter(conn.getOutputStream(), true)) {

            Main.logger.debug("Connection for "
                    + this.cfg.get(this.section, "host")
                    + ":" + this.cfg.get(this.section, "port")
                    + " is opened");

            out.println(serializer.exclude("*.class").deepSerialize(command));
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
        Context context;
        CommandFile cfile;

        context = new Context();
        cfile = new CommandFile();

        context.setContext(this.cfg.get(this.section, "type"));
        cfile.setName("put");

        if (!Boolean.getBoolean(this.cfg.get(this.section, "full"))) {
            cfile.setFilename(this.cfg.get(this.section, "path"));
        } // TODO: add code for a full restore

        cfile.setAcl(Boolean.parseBoolean(this.cfg.get(this.section, "acl")));
        context.setCommand(cfile);

        if (context.getContext().equals("file")) {
            this.fsstore.put(this.dbstore.getFile(((CommandFile) context.getCommand()).getFilename()));
        }
    }

    /**
     * @return the dataset
     */
    @Override
    public Integer getDataset() {
        return this.dataset;
    }

    /**
     * @param dataset the dataset to set
     */
    @Override
    public void setDataset(Integer dataset) {
        this.dataset = dataset;
    }

    /**
     * @return the grace
     */
    @Override
    public String getGrace() {
        return this.grace;
    }

    /**
     * @param grace the grace to set
     */
    @Override
    public void setGrace(String grace) {
        this.grace = grace;
    }

    /**
     * @return the section
     */
    @Override
    public String getSection() {
        return this.section;
    }

    /**
     * @param section the section to set
     */
    @Override
    public void setSection(String section) {
        this.section = section;
    }

    @Override
    public void setDbStore(DbStorage dbstore) {
        this.dbstore = dbstore;
    }

    @Override
    public DbStorage getDbStore() {
        return this.dbstore;
    }

    @Override
    public void setFsStore(FileStorage fsstore) {
        this.fsstore = fsstore;
    }

    @Override
    public FileStorage getFsStore() {
        return this.fsstore;
    }

    @Override
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    @Override
    public String getOperationType() {
        return this.operationType;
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
            Main.logger.error("Host not found: " + this.section);
            Main.logger.debug("Host not found: " + this.section, ex);
        } catch (ConnectException ex) {
            Main.logger.error("Connect error for host: " + this.section);
            Main.logger.debug("Connect error for host: " + this.section, ex);
        } catch (IOException ex) {
            Main.logger.error("I/O error for host: " + this.section);
            Main.logger.debug("I/O error for host: " + this.section, ex);
        } catch (SQLException | ClassNotFoundException ex) {
            Main.logger.error("SQL error for host: " + this.section);
            Main.logger.debug("SQL error for host: " + this.section, ex);
        }
    }
}
