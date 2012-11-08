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
import org.memento.server.Main;

/**
 *
 * @author enrico
 */
public class FileStorage extends CommonStorage {

    public FileStorage(Wini cfg) {
        super(cfg);
    }

    private void addFile(FileAttrs json) throws IOException {
        File source;
        File dest;

        dest = this.getFile(this.returnStructure(false) + json.getName(), json.getOs());
        if (!json.getPreviousDataset()) {
            this.getRemoteFile(json.getName(), dest);
        } else {
            source = this.getFile(this.returnStructure(true) + json.getName(), json.getOs());
            Files.createLink(dest.toPath(), source.toPath());
        }
    }

    private void getRemoteFile(String source, File dest) throws IOException {
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
        outFile = null;
        conn = null;

        try {
            conn = new Socket(this.cfg.get(this.section, "host"),
                    Integer.parseInt(this.cfg.get(this.section, "port")));

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

            if (outFile instanceof FileOutputStream) {
                outFile.close();
            }
        }
    }

    private File getFile(String aPath, String os) {
        File result;
        String pathCleaned;

        if (os.startsWith("windows")) {
            pathCleaned = aPath
                    .replace("\\", System.getProperty("file.separator"))
                    .replace(":", "");
            result = new File(pathCleaned);
        } else {
            result = new File(aPath);
        }

        return result;
    }

    /**
     * @param section the section to set
     */
    @Override
    public void setSection(String section) {
        File directory;

        this.section = section;
        directory = new File(this.returnStructure(false));

        if (!directory.exists()) {
            Main.logger.debug("Creating directory " + directory.getAbsolutePath());
            directory.mkdirs();
        }
    }

    public void add(FileAttrs json) throws IOException {
        File path;

        switch (json.getType()) {
            case "directory":
                path = getFile(this.returnStructure(false) + json.getName(), json.getOs());
                path.mkdirs();
                break;
            case "file":
                this.addFile(json);
                break;
            case "symlink":
                path = getFile(this.returnStructure(false) + json.getName(), json.getOs());
                Files.createSymbolicLink(path.toPath(), Paths.get(json.getLinkTo()));
                break;
        }
    }
}
