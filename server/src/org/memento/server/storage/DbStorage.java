/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.server.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.ini4j.Wini;
import org.memento.json.FileAttrs;

/**
 *
 * @author ebianchi
 */
public class DbStorage extends CommonStorage {

    private Connection conn;

    public DbStorage(Wini cfg) {
        super(cfg);
    }

    private void addDosAttrs(FileAttrs json) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void addPosixAttrs(FileAttrs json) throws SQLException {
        PreparedStatement pstmt;
        final String INSERT = "INSERT INTO attrs"
                + "(element, element_user, element_group, element_type,"
                + " element_mtime, element_ctime, element_hash, element_perm)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        pstmt = this.conn.prepareStatement(INSERT);
        pstmt.setString(1, json.getName());
        pstmt.setString(2, json.getPosixOwner());
        pstmt.setString(3, json.getPosixGroup());
        pstmt.setString(4, json.getType());
        pstmt.setLong(5, json.getMtime());
        pstmt.setLong(6, json.getCtime());
        pstmt.setString(7, json.getHash());
        pstmt.setString(8, json.getPosixPermission());

        pstmt.executeUpdate();

        pstmt.close();
    }

    public void add(FileAttrs json) throws SQLException, ClassNotFoundException {
        this.conn = DBConnection.getInstance().getConnection("cur_" + this.section,
                this.returnStructure(Boolean.FALSE));

        if (json.getOs().indexOf("windows") >= 0) {
            this.addDosAttrs(json);
        } else {
            this.addPosixAttrs(json);
        }
    }
}
