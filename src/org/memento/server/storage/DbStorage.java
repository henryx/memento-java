/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.memento.server.storage;

import org.ini4j.Wini;
import org.memento.json.FileAttrs;
import org.memento.server.management.Storage;

/**
 *
 * @author ebianchi
 */
public class DbStorage implements Storage {

    private Integer dataset;
    private String grace;
    private String section;
    private Wini cfg;

    public DbStorage(Wini cfg) {
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

    @Override
    public void add(FileAttrs json) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
