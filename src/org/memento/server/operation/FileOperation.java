/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       BackupSYNC
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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Wini;
import org.memento.json.Context;
import org.memento.json.commands.CommandFile;
import org.memento.server.management.Operation;
import org.memento.server.management.Storage;

/**
 *
 * @author enrico
 */
public class FileOperation implements Operation {

    private Integer dataset;
    private Storage dbstore;
    private Storage fsstore;
    private String grace;
    private String section;
    private Wini cfg;

    public FileOperation(Wini cfg) {
        this.cfg = cfg;
    }

    private void sendCommand(Context command) throws UnknownHostException, IOException {
        BufferedReader in;
        HashMap inJSON;
        JSONSerializer serializer;
        PrintWriter out;
        Socket conn;
        String line;

        serializer = new JSONSerializer();
        in = null;
        out = null;
        conn = null;

        try {
            conn = new Socket(this.cfg.get(section, "host"), Integer.parseInt(this.cfg.get(section, "port")));

            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            out = new PrintWriter(conn.getOutputStream(), true);

            out.println(serializer.exclude("*.class").deepSerialize(command));

            while (!(line = in.readLine()).equals("")) {
                inJSON = new JSONDeserializer<HashMap>().deserialize(line);
                this.dbstore.add(inJSON);
                this.fsstore.add(inJSON);
            }
        } finally {
            if (in instanceof BufferedReader) {
                in.close();
            }

            if (out instanceof PrintWriter) {
                out.close();
            }

            if (conn instanceof Socket && !conn.isClosed()) {
                conn.close();
            }
        }
    }

    /**
     * @return the dataset
     */
    @Override
    public Integer getDataset() {
        return dataset;
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
        return grace;
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
        return section;
    }

    /**
     * @param section the section to set
     */
    @Override
    public void setSection(String section) {
        this.section = section;
    }

    @Override
    public void setDbStore(Storage dbstore) {
        this.dbstore = dbstore;
    }

    @Override
    public Storage getDbStore() {
        return this.dbstore;
    }

    @Override
    public void setFsStore(Storage fsstore) {
        this.fsstore = fsstore;
    }

    @Override
    public Storage getFsStore() {
        return this.fsstore;
    }

    @Override
    public void run() {
        Context context;
        CommandFile command;

        context = new Context();
        command = new CommandFile();

        context.setContext("file");
        command.setName("list");
        command.setDirectory(this.cfg.get(section, "path").split(","));
        command.setAcl(Boolean.parseBoolean(this.cfg.get(section, "acl")));
        context.setCommand(command);

        try {
            this.sendCommand(context);
        } catch (UnknownHostException ex) {
            Logger.getLogger(FileOperation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileOperation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
