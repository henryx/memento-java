/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Wini;
import org.memento.json.FileAttrs;

/**
 *
 * @author ebianchi
 */
public class DbStorage extends CommonStorage {

    private Connection conn;
    private Connection oldConn;

    public DbStorage(Wini cfg) {
        super(cfg);
    }

    private void addDosAttrs(FileAttrs json) throws SQLException {
        // TODO: Add more attributes returned by Windows client
        PreparedStatement insert;

        insert = conn.prepareStatement("INSERT INTO attrs"
                + "(element, element_os, element_mtime, element_ctime, element_hash)"
                + " VALUES (?, ?, ?, ?, ?, ?)");

        insert.setString(1, json.getName());
        insert.setString(2, json.getOs());
        insert.setLong(3, json.getMtime());
        insert.setLong(4, json.getCtime());
        insert.setString(5, json.getHash());

        insert.executeUpdate();
        insert.close();
    }

    private void addPosixAttrs(FileAttrs json) throws SQLException {
        PreparedStatement insert;

        insert = conn.prepareStatement("INSERT INTO attrs"
                + "(element, element_os, element_user, element_group, element_type,"
                + " element_link, element_mtime, element_ctime, element_hash, element_perm)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        insert.setString(1, json.getName());
        insert.setString(2, json.getOs());
        insert.setString(3, json.getPosixOwner());
        insert.setString(4, json.getPosixGroup());
        insert.setString(5, json.getType());
        if (json.getPosixSymlink()) {
            insert.setString(6, json.getLinkTo());
        } else {
            insert.setNull(6, Types.VARCHAR);
        }
        insert.setLong(7, json.getMtime());
        insert.setLong(8, json.getCtime());
        insert.setString(9, json.getHash());
        insert.setString(10, json.getPosixPermission());

        insert.executeUpdate();
        insert.close();
    }

    @Override
    protected void finalize() throws Throwable {
        if (this.conn instanceof Connection && !this.conn.isClosed()) {
            this.conn.commit();
            this.conn.close();
        }

        if (this.oldConn instanceof Connection && !this.conn.isClosed()) {
            this.oldConn.rollback();
            this.oldConn.close();
        }

        super.finalize();
    }

    public void add(FileAttrs json) throws SQLException, ClassNotFoundException {
        if (!json.getOs().startsWith("windows")) {
            this.addPosixAttrs(json);
        } else {
            this.addDosAttrs(json);
        }
    }
    
    public ArrayList<FileAttrs> listItems(String itemType) throws SQLException {
        ArrayList<FileAttrs> result;
        FileAttrs json;
        PreparedStatement query;
        final ResultSet res;


        query = this.conn.prepareStatement("SELECT element,"
                + " element_os,"
                + " element_hash,"
                + " element_link,"
                + " element_mtime,"
                + " element_ctime FROM attrs WHERE element_type = ?");

        query.setString(1, itemType);
        res = query.executeQuery();

        result = new ArrayList<> ();

        while (res.next()) {
            json = new FileAttrs();
            json.setName(res.getString(1));
            json.setOs(res.getString(2));
            json.setHash(res.getString(3));
            json.setLinkTo(res.getString(4));
            json.setMtime(res.getLong(5));
            json.setCtime(res.getLong(6));
            json.setType(itemType);
            result.add(json);
        }

        return result;
    }

    public Boolean isItemExist(FileAttrs json) {
        ResultSet res;

        if (!(this.oldConn instanceof Connection)) {
            return Boolean.FALSE;
        }

        res = null;

        try (PreparedStatement select = this.oldConn.prepareStatement("SELECT count(*) FROM attrs"
                        + " WHERE element = ? AND element_hash = ?")) {

            select.setString(1, json.getName());
            select.setString(2, json.getHash());

            res = select.executeQuery();
            res.next();

            if (res.getInt(1) > 0) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }

        } catch (SQLException ex) {
            Logger.getLogger(CommonStorage.class.getName()).log(Level.SEVERE, null, ex);
            return Boolean.FALSE;
        } finally {
            try {
                if (res instanceof ResultSet && !res.isClosed()) {
                    res.close();
                }
            } catch (SQLException ex) {
            }
        }
    }

    /**
     * @param section the section to set
     */
    @Override
    public void setSection(String section) {
        this.section = section;

        try {
            this.conn = DBConnection.getInstance().getConnection("cur_" + this.section,
                    this.returnStructure(Boolean.FALSE));

            if (new File(this.returnStructure(Boolean.TRUE)).exists()) {
                this.oldConn = DBConnection.getInstance().getConnection("old_" + this.section,
                        this.returnStructure(Boolean.TRUE));
            }
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(DbStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
