/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.storage;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
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

    private void getFile(FileAttrs json) throws UnknownHostException, IOException {
        File source;
        File dest;

        dest = this.fileFromOS(this.returnStructure(false) + json.getName(), json.getOs());
        if (!json.getPreviousDataset()) {
            this.getRemoteFile(json.getName(), dest);
        } else {
            source = this.fileFromOS(this.returnStructure(true) + json.getName(), json.getOs());
            Files.createLink(dest.toPath(), source.toPath());
        }
    }

    private void getRemoteFile(String source, File dest) throws UnknownHostException, IOException {
        Context context;
        CommandFile command;
        JSONSerializer serializer;

        byte[] buf = new byte[8192];
        int bytesRead = 0;

        context = new Context();
        command = new CommandFile();
        serializer = new JSONSerializer();

        try (Socket conn = new Socket(this.cfg.get(this.section, "host"),
                        Integer.parseInt(this.cfg.get(this.section, "port")));
                InputStream in = conn.getInputStream();
                PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
                FileOutputStream outFile = new FileOutputStream(dest);) {

            context.setContext("file");
            command.setName("get");
            command.setFilename(source);
            context.setCommand(command);

            out.println(serializer.exclude("*.class").deepSerialize(context));
            out.flush();

            while ((bytesRead = in.read(buf, 0, buf.length)) != -1) {
                outFile.write(buf, 0, bytesRead);
            }
            outFile.flush();
        }
    }

    private File fileFromOS(String aPath, String os) {
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

    private void putFile(FileAttrs json) throws UnknownHostException, IOException {
        Context context;
        CommandFile command;
        File source;
        JSONSerializer serializer;
        HashMap<String, String> response;
        int read;
        byte[] buffer = new byte[8192];

        context = new Context();
        command = new CommandFile();
        serializer = new JSONSerializer();

        source = this.fileFromOS(this.returnStructure(false) + json.getName(), json.getOs());
        try (Socket conn = new Socket(this.cfg.get(this.section, "host"),
                        Integer.parseInt(this.cfg.get(this.section, "port")));
                PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                BufferedInputStream buff = new BufferedInputStream(new FileInputStream(source));
                BufferedOutputStream outStream = new BufferedOutputStream(conn.getOutputStream());) {

            command.setName("put");
            command.setFilename(json.getName());
            command.setAttrs(json);

            context.setContext("file");
            context.setCommand(command);

            out.println(serializer.exclude("*.class").deepSerialize(context));
            out.flush();

            response = new JSONDeserializer<HashMap>().deserialize(in.readLine());
            if (response.get("context").equals("restore") && response.get("result").equals("ok")) {
                while ((read = buff.read(buffer)) != -1) {
                    outStream.write(buffer, 0, read);
                    outStream.flush();
                }
            }
        }
    }

    /**
     * @param section the section to set
     */
    @Override
    public void setSection(String section) {
        File directory;

        this.section = section;
        directory = new File(this.returnStructure(false));

        if (!directory.exists() && this.getOperationType().equals("sync")) {
            Main.logger.debug("Creating directory " + directory.getAbsolutePath());
            directory.mkdirs();
        }
    }

    public void get(FileAttrs json) throws UnknownHostException, IOException {
        File path;

        switch (json.getType()) {
            case "directory":
                path = fileFromOS(this.returnStructure(false) + json.getName(), json.getOs());
                path.mkdirs();
                break;
            case "file":
                this.getFile(json);
                break;
            case "symlink":
                path = fileFromOS(this.returnStructure(false) + json.getName(), json.getOs());
                Files.createSymbolicLink(path.toPath(), Paths.get(json.getLinkTo()));
                break;
        }
    }

    public void put(FileAttrs json) throws UnknownHostException, IOException {
        switch (json.getType()) {
            case "directory":
                // TODO: need to create directories?
                break;
            case "file":
                this.putFile(json);
                break;
            case "symlink":
                // TODO: need to create symlinks?
                break;
        }
    }
}