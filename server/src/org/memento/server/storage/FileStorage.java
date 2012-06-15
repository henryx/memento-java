/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.server.storage;

import flexjson.JSONSerializer;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.ini4j.Wini;
import org.memento.json.Context;
import org.memento.json.FileAttrs;
import org.memento.json.commands.CommandFile;
import org.memento.server.management.Properties;

/**
 *
 * @author enrico
 */
public class FileStorage implements Properties {

    private Integer dataset;
    private String grace;
    private String section;
    private String structure;
    private Wini cfg;

    public FileStorage(Wini cfg) throws IOException {
        this.cfg = cfg;
        this.checkStructure();
    }

    private void checkStructure() throws IOException {
        File directory;
        String[] subdirectories;

        directory = new File(this.cfg.get("general", "repository"));
        subdirectories = new String[]{"hour", "day", "week", "month"};

        if (directory.isFile()) {
            throw new IllegalArgumentException(directory + " is a file");
        }

        if (!directory.exists()) {
            for (String subdirectory : subdirectories) {
                Files.createDirectories(Paths.get(directory.getAbsolutePath(), subdirectory));
            }
        }
    }

    private void getRemoteFile(String source, String dest) throws IOException {
        InputStream in;
        Context context;
        CommandFile command;
        FileOutputStream outFile;
        JSONSerializer serializer;
        PrintWriter out;
        Socket conn;
        byte[] buf = new byte[8192];
        int bytesRead = 0;

        context = new Context();
        command = new CommandFile();
        serializer = new JSONSerializer();

        in = null;
        out = null;
        conn = null;

        try {
            conn = new Socket(this.cfg.get(section, "host"), Integer.parseInt(this.cfg.get(section, "port")));

            in = conn.getInputStream();
            out = new PrintWriter(conn.getOutputStream(), true);
            outFile = new FileOutputStream(dest);

            context.setContext("file");
            command.setName("get");
            command.setFilename(source);
            context.setCommand(command);

            out.println(serializer.exclude("*.class").deepSerialize(context));
            out.flush();

            while ((bytesRead = in.read(buf, 0, buf.length)) != -1) {
                outFile.write(buf, 0, bytesRead);
            }
        } finally {
            if (conn instanceof Socket && !conn.isClosed()) {
                conn.close();
            }

            if (in instanceof InputStream) {
                in.close();
            }

            if (out instanceof PrintWriter) {
                out.flush();
                out.close();
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
        File directory;
        String sep;

        sep = System.getProperty("file.separator");

        this.section = section;
        this.structure = this.cfg.get("general", "repository")
                + sep
                + this.grace
                + sep
                + this.dataset
                + sep
                + this.section
                + sep;

        directory = new File(this.structure);

        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public void add(FileAttrs json) throws IOException {
        switch (json.getType()) {
            case "directory":
                Files.createDirectories(Paths.get(this.structure + json.getName()));
                break;
            case "file":
                this.getRemoteFile(json.getName(), this.structure + json.getName());
                break;
            case "symlink":
                // TODO: Create a symlink
                break;
        }
    }
}
