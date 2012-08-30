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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Wini;
import org.memento.json.Context;
import org.memento.json.FileAttrs;
import org.memento.json.commands.CommandFile;
import org.memento.server.management.Operation;
import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

/**
 *
 * @author enrico
 */
public class FileOperation extends Operation {

    private Integer dataset;
    private DbStorage dbstore;
    private FileStorage fsstore;
    private String grace;
    private String section;
    private Wini cfg;

    public FileOperation(Wini cfg) {
        super();
        this.cfg = cfg;
    }

    private void sendCommand(Context command) throws UnknownHostException, IOException, SQLException, ClassNotFoundException {
        ArrayList<FileAttrs> files;
        ArrayList<FileAttrs> symlinks;
        BufferedReader in;
        FileAttrs inJSON;
        JSONSerializer serializer;
        PrintWriter out;
        Socket conn;
        String line;

        serializer = new JSONSerializer();

        in = null;
        out = null;
        conn = null;

        files = new ArrayList<>();
        symlinks = new ArrayList<>();

        try {
            if (!this.cfg.get(this.section, "pre_command").equals("")) {
                execCommand(this.cfg.get(this.section, "pre_command"));
            }
            conn = new Socket(this.cfg.get(section, "host"), Integer.parseInt(this.cfg.get(section, "port")));

            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            out = new PrintWriter(conn.getOutputStream(), true);

            out.println(serializer.exclude("*.class").deepSerialize(command));
            out.flush();

            while (Boolean.TRUE) {
                line = in.readLine();

                if (line == null) {
                    break;
                }

                inJSON = new JSONDeserializer<FileAttrs>().deserialize(line);
                
                if (inJSON.getType().equals("directory")) {
                    //this.fsstore.add(inJSON);
                    //this.dbstore.add(inJSON);
                } else if (inJSON.getType().equals("file")) {
                    if(this.dbstore.isItemExist(inJSON)) {
                        inJSON.setPreviousDataset(Boolean.TRUE);
                    } else {
                        inJSON.setPreviousDataset(Boolean.FALSE);
                    }                    
                    files.add(inJSON);
                } else if (inJSON.getType().equals("symlink")) {
                    symlinks.add(inJSON);
                }
            }

            for (FileAttrs item : files) {
                this.fsstore.add(item);
                this.dbstore.add(item);
            }

            for (FileAttrs item : symlinks) {
                this.fsstore.add(item);
                this.dbstore.add(item);
            }
            
            if (!this.cfg.get(this.section, "post_command").equals("")) {
                execCommand(this.cfg.get(this.section, "post_command"));
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(FileOperation.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (ConnectException ex) {
            Logger.getLogger(FileOperation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileOperation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException | ClassNotFoundException ex) {
                Logger.getLogger(FileOperation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
