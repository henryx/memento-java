/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       BackupSYNC
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.storage;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.ini4j.Wini;
import org.memento.json.FileAttrs;
import org.memento.server.management.Properties;

/**
 *
 * @author enrico
 */
public class FileStorage implements Properties {

    private Integer dataset;
    private String grace;
    private String section;
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
        String structure;
        String sep;

        sep = System.getProperty("file.separator");

        this.section = section;
        structure = this.cfg.get("general", "repository")
                + sep
                + this.grace
                + sep
                + this.dataset
                + sep
                + this.section;

        directory = new File(structure);

        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public void add(FileAttrs json, Socket sock) {
        // TODO: implement this
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
