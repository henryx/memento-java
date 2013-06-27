/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.server.storage;

import org.ini4j.Wini;
import org.memento.server.management.Properties;

/**
 *
 * @author enrico
 */
public abstract class CommonStorage implements Properties {
    private Integer dataset;
    private String grace;
    private String operationType;
    protected String section;
    protected Wini cfg;

    public CommonStorage(Wini cfg) {
        this.cfg = cfg;
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

    protected String returnStructure(Boolean old) {
        Integer curDataset;
        String sep;

        sep = System.getProperty("file.separator");

        if (old) {
            if ((this.dataset - 1) <= 0) {
                curDataset = Integer.decode(this.cfg.get("dataset", this.grace));
            } else {
                curDataset = this.dataset - 1;
            }
        } else {
            curDataset = this.dataset;
        }

        return this.cfg.get("general", "repository")
                + sep
                + this.grace
                + sep
                + curDataset
                + sep
                + this.section
                + sep;
    }

    /**
     * @return the operation
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
}
