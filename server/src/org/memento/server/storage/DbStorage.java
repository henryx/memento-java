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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Wini;
import org.memento.json.FileAttrs;

/**
 *
 * @author ebianchi
 */
public class DbStorage extends CommonStorage {

    private PreparedStatement insPosix;
    private PreparedStatement selExist;

    public DbStorage(Wini cfg) {
        super(cfg);
    }

    private void addDosAttrs(FileAttrs json) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void addPosixAttrs(FileAttrs json) throws SQLException {
        this.insPosix.setString(1, json.getName());
        this.insPosix.setString(2, json.getPosixOwner());
        this.insPosix.setString(3, json.getPosixGroup());
        this.insPosix.setString(4, json.getType());
        this.insPosix.setLong(5, json.getMtime());
        this.insPosix.setLong(6, json.getCtime());
        this.insPosix.setString(7, json.getHash());
        this.insPosix.setString(8, json.getPosixPermission());

        this.insPosix.executeUpdate();
    }

    @Override
    protected void finalize() throws Throwable {
        if (this.insPosix instanceof PreparedStatement) {
            this.insPosix.close();
        }

        if (this.selExist instanceof PreparedStatement) {
            this.selExist.close();
        }

        super.finalize();
    }

    public void add(FileAttrs json) throws SQLException {
        if (json.getOs().startsWith("windows")) {
            this.addDosAttrs(json);
        } else {
            this.addPosixAttrs(json);
        }
    }

    public Boolean isItemExist(FileAttrs json) {
        ResultSet res;

        res = null;

        if (!(this.selExist instanceof PreparedStatement)) {
            return Boolean.FALSE;
        }

        try {
            this.selExist.setString(1, json.getName());
            this.selExist.setString(2, json.getHash());

            res = this.selExist.executeQuery();
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
            if (res instanceof ResultSet) {
                try {
                    res.close();
                } catch (SQLException ex) {
                }
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
            Connection conn;
            Connection oldConn;

            conn = DBConnection.getInstance().getConnection("cur_" + this.section,
                    this.returnStructure(Boolean.FALSE));

            this.insPosix = conn.prepareStatement("INSERT INTO attrs"
                    + "(element, element_user, element_group, element_type,"
                    + " element_mtime, element_ctime, element_hash, element_perm)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

            if (new File(this.returnStructure(Boolean.TRUE)).exists()) {
                oldConn = DBConnection.getInstance().getConnection("old_" + this.section,
                        this.returnStructure(Boolean.TRUE));
                this.selExist = oldConn.prepareStatement("SELECT count(*) FROM attrs"
                        + " WHERE element = ? AND element_hash = ?");
            }
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(DbStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
