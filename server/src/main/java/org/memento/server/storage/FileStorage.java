/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.storage;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import org.ini4j.Wini;
import org.memento.json.Context;
import org.memento.json.FileAttrs;
import org.memento.json.commands.CommandFile;
import org.memento.server.Main;
import org.memento.server.net.Client;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 *
 * @author enrico
 */
public class FileStorage extends CommonStorage {

    public FileStorage(Wini cfg) {
        super(cfg);
    }

    private void compress(File source, File dest) throws FileNotFoundException, IOException {
        byte[] buf = new byte[1024 * 1024];
        int bytesRead = 0;

        try (FileInputStream in = new FileInputStream(source);
                FileOutputStream out = new FileOutputStream(dest);
                XZOutputStream compressed = new XZOutputStream(out, new LZMA2Options());) {

            while ((bytesRead = in.read(buf, 0, buf.length)) != -1) {
                compressed.write(buf, 0, bytesRead);
                compressed.flush();
            }
            compressed.finish();
        }
    }

    private void getFile(FileAttrs json) throws UnknownHostException, IOException {
        File retrieved;
        File source;
        File dest;

        retrieved = this.fileFromOS(this.returnStructure(false) + json.getName(), json.getOs());

        if (!json.getPreviousDataset()) {
            this.getRemoteFile(json.getName(), retrieved);

            if (json.isCompressed()) {
                dest = this.fileFromOS(this.returnStructure(false) + json.getName() + ".compressed", json.getOs());
                this.compress(retrieved, dest);
                retrieved.delete();
            }
        } else {
            if (json.isCompressed()) {
                source = this.fileFromOS(this.returnStructure(true) + json.getName() + ".compressed", json.getOs());
                dest = this.fileFromOS(this.returnStructure(false) + json.getName() + ".compressed", json.getOs());
            } else {
                source = this.fileFromOS(this.returnStructure(true) + json.getName(), json.getOs());
                dest = this.fileFromOS(this.returnStructure(false) + json.getName(), json.getOs());
            }

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

        try (Client client = new Client(this.section, this.cfg);
                InputStream in = client.socket().getInputStream();
                PrintWriter out = new PrintWriter(client.socket().getOutputStream(), true);
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
                path = this.fileFromOS(this.returnStructure(false) + json.getName(), json.getOs());
                path.mkdirs();
                break;
            case "file":
                this.getFile(json);
                break;
            case "symlink":
                path = this.fileFromOS(this.returnStructure(false) + json.getName(), json.getOs());
                Files.createSymbolicLink(path.toPath(), Paths.get(json.getLinkTo()));
                break;
        }
    }
    
    public void put(CommandFile json) throws UnknownHostException, IOException {
        Context context;
        File source;
        JSONSerializer serializer;
        HashMap<String, String> response;
        int read;
        byte[] buffer = new byte[8192];

        context = new Context();
        serializer = new JSONSerializer();

        try (Client client = new Client(this.section, this.cfg);
                PrintWriter out = new PrintWriter(client.socket().getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.socket().getInputStream()));) {

            context.setContext("file");
            context.setCommand(json);

            out.println(serializer.exclude("*.class").deepSerialize(context));
            out.flush();

            response = new JSONDeserializer<HashMap>().deserialize(in.readLine());
            if (json.getAttrs().getType().equals("file")) {
                if (response.get("context").equals("restore") && response.get("result").equals("ok")) {
                    source = this.fileFromOS(this.returnStructure(false) + json.getName(), json.getAttrs().getOs());
                    try (BufferedInputStream buff = new BufferedInputStream(new FileInputStream(source));
                            BufferedOutputStream outStream = new BufferedOutputStream(client.socket().getOutputStream());) {

                        while ((read = buff.read(buffer)) != -1) {
                            outStream.write(buffer, 0, read);
                            outStream.flush();
                        }
                    }
                }
            }
        }
    }
}
