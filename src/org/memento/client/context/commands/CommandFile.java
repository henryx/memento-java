/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client.context.commands;

import flexjson.JSONSerializer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import org.memento.PathName;
import org.memento.json.FileAttrs;

/**
 *
 * @author enrico
 */
public class CommandFile implements FileVisitor<Path> {
    private String directory;
    private Boolean acl;
    private PrintWriter writer;
    
    private FileAttrs compute(PathName aPath) throws IllegalArgumentException, FileNotFoundException, IOException {
        FileAttrs result;

        result = aPath.getAttrs();
        result.setName(aPath.getAbsolutePath());

        if (aPath.isDirectory()) {
            result.setType("directory");
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

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws FileNotFoundException, IOException {
        FileAttrs data;
        JSONSerializer serializer;

        serializer = new JSONSerializer();
        data = this.compute(new PathName(file));

        this.writer.println(serializer.deepSerialize(data));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws FileNotFoundException, IOException {
        FileAttrs data;
        JSONSerializer serializer;

        serializer = new JSONSerializer();
        data = this.compute(new PathName(dir));

        this.writer.println(serializer.deepSerialize(data));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path t, BasicFileAttributes bfa) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path t, IOException ioe) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    /**
     * @return the directory
     */
    public String getDirectory() {
        return directory;
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
        return acl;
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
        return writer;
    }

    /**
     * @param writer the writer to set
     */
    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }
}