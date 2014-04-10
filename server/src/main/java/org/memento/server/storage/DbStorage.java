/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.ini4j.Wini;
import org.memento.json.FileAcl;
import org.memento.json.FileAttrs;
import org.memento.server.Main;

/**
 *
 * @author ebianchi
 */
public class DbStorage extends CommonStorage {

    private Connection conn;

    public DbStorage(Wini cfg) throws SQLException, ClassNotFoundException {
        super(cfg);

        HashMap<String, String> connData;

        connData = new HashMap<>();
        connData.put("host", this.cfg.get("database", "host"));
        connData.put("port", this.cfg.get("database", "port"));
        connData.put("dbname", this.cfg.get("database", "dbname"));
        connData.put("user", this.cfg.get("database", "user"));
        connData.put("password", this.cfg.get("database", "password"));

        this.conn = new DBConnection(connData, false).getConnection();
    }
    
    private void addPosixAcl(String element, ArrayList<FileAcl> acls) throws SQLException {
        PreparedStatement insert;

        insert = this.conn.prepareStatement("INSERT INTO acls"
                + "(area, grace, dataset, element, name, type, perms)"
                + " VALUES(?, ?, ?, ?, ?, ?, ?)");

        for (FileAcl acl : acls) {
            insert.setString(1, this.getSection());
            insert.setString(2, this.getGrace());
            insert.setInt(3, this.getDataset());
            insert.setString(4, element);
            insert.setString(5, acl.getName());
            insert.setString(6, acl.getAclType());
            insert.setString(7, acl.getAttrs());

            insert.executeUpdate();
        }

        insert.close();
    }

    private void addDosAttrs(FileAttrs json) throws SQLException {
        // TODO: Add more attributes returned by Windows client
        PreparedStatement insert;

        insert = this.conn.prepareStatement("INSERT INTO attrs"
                + "(area, grace, dataset, element, os, type, mtime, ctime, hash, compressed)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        insert.setString(1, this.getSection());
        insert.setString(2, this.getGrace());
        insert.setInt(3, this.getDataset());
        insert.setString(4, json.getName());
        insert.setString(5, json.getOs());
        insert.setString(6, json.getType());
        insert.setLong(7, json.getMtime());
        insert.setLong(8, json.getCtime());
        insert.setString(9, json.getHash());
        insert.setBoolean(10, json.isCompressed());

        insert.executeUpdate();
        insert.close();
    }

    private void addPosixAttrs(FileAttrs json) throws SQLException {
        PreparedStatement insert;

        insert = this.conn.prepareStatement("INSERT INTO attrs"
                + "(area, grace, dataset, element, os, username, groupname, type,"
                + " link, mtime, ctime, hash, perms, compressed)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        insert.setString(1, this.getSection());
        insert.setString(2, this.getGrace());
        insert.setInt(3, this.getDataset());
        insert.setString(4, json.getName());
        insert.setString(5, json.getOs());
        insert.setString(6, json.getPosixOwner());
        insert.setString(7, json.getPosixGroup());
        insert.setString(8, json.getType());
        if (json.getPosixSymlink()) {
            insert.setString(9, json.getLinkTo());
        } else {
            insert.setNull(9, Types.VARCHAR);
        }
        insert.setLong(10, json.getMtime());
        insert.setLong(11, json.getCtime());
        insert.setString(12, json.getHash());
        insert.setString(13, json.getPosixPermission());
        insert.setBoolean(14, json.isCompressed());

        insert.executeUpdate();
        insert.close();
    }

    public void add(FileAttrs json) throws SQLException, ClassNotFoundException {
        if (!json.getOs().startsWith("windows")) {
            this.addPosixAttrs(json);
            if (json.getAcl() != null && !json.getAcl().isEmpty()) {
                this.addPosixAcl(json.getName(), json.getAcl());
            }
        } else {
            this.addDosAttrs(json);
        }
    }

    public Iterator<FileAttrs> listItems(String itemType) throws SQLException {
        class DbItems implements Iterator<FileAttrs> {

            private ResultSet res;
            private String type;

            @Override
            public boolean hasNext() {
                try {
                    if (this.res.next()) {
                        return true;
                    } else {
                        this.res.close();
                        return false;
                    }
                } catch (SQLException ex) {
                    Main.logger.debug("Iterator error", ex);
                    return false;
                }
            }

            @Override
            public FileAttrs next() {
                FileAttrs json;
                json = new FileAttrs();

                try {
                    json.setName(this.res.getString(1));
                    json.setOs(this.res.getString(2));
                    json.setHash(this.res.getString(3));
                    json.setLinkTo(this.res.getString(4));
                    json.setMtime(this.res.getLong(5));
                    json.setCtime(this.res.getLong(6));
                    json.setType(this.type);

                    return json;
                } catch (SQLException ex) {
                    Main.logger.debug("Iterator error", ex);
                    return null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void setResult(ResultSet res) {
                this.res = res;
            }

            public void setType(String type) {
                this.type = type;
            }
        }

        DbItems result;
        PreparedStatement query;
        ResultSet res;

        query = this.conn.prepareStatement("SELECT element,"
                + " os,"
                + " hash,"
                + " link,"
                + " mtime,"
                + " ctime FROM attrs WHERE type = ?"
                + " AND area = ? AND grace = ? AND dataset = ?");

        query.setString(1, itemType);
        query.setString(2, this.getSection());
        query.setString(3, this.getGrace());
        query.setInt(4, this.getDataset());
        res = query.executeQuery();

        result = new DbItems();

        result.setType(itemType);
        result.setResult(res);

        return result;
    }

    public Boolean isItemExist(FileAttrs json) {
        ResultSet res;
        int previousDataset;
        
        if (this.getDataset() -1 <= 0) {
            previousDataset = Integer.decode(this.cfg.get("dataset", this.getGrace()));
        } else {
            previousDataset = this.getDataset() -1;
        }

        res = null;
        try (PreparedStatement select = this.conn.prepareStatement("SELECT count(*) FROM attrs"
                + " WHERE element = ? AND hash = ?"
                + " AND area = ? AND grace = ? AND dataset = ?")) {

            select.setString(1, json.getName());
            select.setString(2, json.getHash());
            select.setString(3, this.getSection());
            select.setString(4, this.getGrace());
            select.setInt(5, previousDataset);

            res = select.executeQuery();
            res.next();

            if (res.getInt(1) > 0) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }

        } catch (SQLException ex) {
            Main.logger.debug("Query error", ex);
            return Boolean.FALSE;
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException ex) {
            }
        }
    }

    public FileAttrs getFile(String name, boolean acl) throws SQLException {
        FileAttrs result;

        result = new FileAttrs();
        try (PreparedStatement query = this.conn.prepareStatement("SELECT element,"
                + " os,"
                + " hash,"
                + " link,"
                + " type,"
                + " mtime,"
                + " ctime,"
                + " perms,"
                + " username,"
                + " groupname FROM attrs WHERE element = ?"
                + " AND area = ? AND grace = ? AND dataset = ?");) {

            query.setString(1, name);
            query.setString(2, this.getSection());
            query.setString(3, this.getGrace());
            query.setInt(4, this.getDataset());
            
            try (ResultSet res = query.executeQuery();) {
                res.next();

                result.setName(res.getString(1));
                result.setOs(res.getString(2));
                result.setHash(res.getString(3));
                if (!(res.getString(4) == null || res.getString(4).equals(""))) {
                    result.setLinkTo(res.getString(4));
                }
                result.setType(res.getString(5));
                result.setMtime(res.getLong(6));
                result.setCtime(res.getLong(7));

                if (result.getOs().equals("linux")) {
                    result.setPosixPermission(res.getString(8));
                    result.setPosixOwner(res.getString(9));
                    result.setPosixGroup(res.getString(10));
                }
            }
        }

        if (acl) {
            // TODO: Add code for ACL's extraction
        }

        return result;
    }

    public void commit() throws SQLException {
        this.conn.commit();
    }

    public void remove(String name) throws SQLException {
        PreparedStatement delete;

        for (String table : new String[]{"attrs", "acls"}) {
            delete = this.conn.prepareStatement("DELETE FROM "
                    + table
                    + " WHERE element = ?"
                    + " AND area = ? AND grace = ? AND dataset = ?");
            delete.setString(1, name);
            delete.setString(2, this.getSection());
            delete.setString(3, this.getGrace());
            delete.setInt(4, this.getDataset());

            delete.executeUpdate();
            delete.close();
        }
    }
}