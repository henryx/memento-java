/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client.context.commands;

import flexjson.JSONSerializer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import org.memento.PathName;
import org.memento.json.FileAttrs;

/**
 *
 * @author enrico
 */
public class CommandFile {
    private String directory;
    private Boolean acl;
    private PrintWriter writer;

    private FileAttrs compute(PathName aPath) throws IllegalArgumentException, FileNotFoundException, IOException {
        FileAttrs result;

        result = aPath.getAttrs();

        result.setName(aPath.getAbsolutePath());
        result.setOs(System.getProperty("os.name").toLowerCase());

        if (aPath.isDirectory()) {
            result.setType("directory");
        } else if (aPath.isSymlink()) {
            result.setType("symlink");
        } else {
            result.setType("file");
            try {
                result.setHash(aPath.hash());
            } catch (NoSuchAlgorithmException | IOException ex) {
                result.setHash("");
            }
        }

        if (this.acl) {
            result.setAcl(aPath.getAcl());
        }

        return result;
    }

    /**
     * @return the directory
     */
    public String getDirectory() {
        return this.directory;
    }

    /**
     * @param directory the directory to set
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * @return the acl
     */
    public Boolean getAcl() {
        return this.acl;
    }

    /**
     * @param acl the acl to set
     */
    public void setAcl(Boolean acl) {
        this.acl = acl;
    }

    /**
     * @return the writer
     */
    public PrintWriter getWriter() {
        return this.writer;
    }

    /**
     * @param writer the writer to set
     */
    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public void walk(File path) throws IllegalArgumentException, FileNotFoundException, IOException {
        File[] filesAndDirs;
        FileAttrs data;
        List<File> filesDirs;
        JSONSerializer serializer;
        
        filesAndDirs = path.listFiles();
        filesDirs = Arrays.asList(filesAndDirs);
        serializer = new JSONSerializer();

        data = this.compute(new PathName(path.toPath()));
        this.writer.println(serializer.deepSerialize(data));

        for (File file : filesDirs) {
            data = this.compute(new PathName(file.toPath()));
            this.writer.println(serializer.deepSerialize(data));

            if (file.isDirectory()) {
                this.walk(file);
            }
        }
    }
}