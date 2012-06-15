/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.server.storage;

import java.sql.Connection;
import java.sql.SQLException;
import org.ini4j.Wini;
import org.memento.json.FileAttrs;
import org.memento.server.management.Properties;

/**
 *
 * @author ebianchi
 */
public class DbStorage implements Properties {

    private Connection conn;
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

    public void add(FileAttrs json) throws SQLException, ClassNotFoundException {
        DBConnection dbc;
        String dbLocation;
        String sep;

        dbc = new DBConnection();
        sep = System.getProperty("file.separator");

        dbLocation = this.cfg.get("general", "repository")
                + sep
                + this.grace
                + sep
                + this.dataset
                + sep
                + this.section
                + sep
                + ".store.db";

        this.conn = dbc.open(dbLocation, Boolean.FALSE);
        // TODO: Add code for storing data into database

        dbc.close();
    }
}
