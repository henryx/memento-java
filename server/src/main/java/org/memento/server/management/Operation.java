/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.management;

import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

/**
 *
 * @author enrico
 */
public abstract class Operation extends Thread implements Properties {

    private Integer dataset;
    private String grace;
    private String operationType;
    protected DbStorage dbstore;
    protected FileStorage fsstore;
    protected String section;
    
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

    public void setDbStore(DbStorage dbstore) {
        this.dbstore = dbstore;
    }

    public DbStorage getDbStore() {
        return this.dbstore;
    }

    public void setFsStore(FileStorage fsstore) {
        this.fsstore = fsstore;
    }

    public FileStorage getFsStore() {
        return this.fsstore;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationType() {
        return this.operationType;
    }

    @Override
    public abstract void run();
}
