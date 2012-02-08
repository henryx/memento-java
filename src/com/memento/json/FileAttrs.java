/*
Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
Project       BackupSYNC
Description   A backup system
License       GPL version 2 (see GPL.txt for details)
 */

package com.memento.json;

import java.util.ArrayList;

/**
 *
 * @author enrico
 */
public class FileAttrs {
    
    // Common attributes
    private long ctime;
    private long mtime;
    private long size;
    private String type;
    private String hash;

    // Posix attributes
    private String posixOwner;
    private String posixGroup;
    private String posixAttrs;
    
    // Dos attributes
    private Boolean dosArchive;
    private Boolean dosHidden;
    private Boolean dosReadonly;
    private Boolean dosSystem;
    
    // ACL
    private ArrayList<FileAcl> acl;

    /**
     * @return the ctime
     */
    public long getCtime() {
        return ctime;
    }

    /**
     * @param ctime the ctime to set
     */
    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    /**
     * @return the mtime
     */
    public long getMtime() {
        return mtime;
    }

    /**
     * @param mtime the mtime to set
     */
    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * @param hash the hash to set
     */
    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * @return the posixOwner
     */
    public String getPosixOwner() {
        return posixOwner;
    }

    /**
     * @param posixOwner the posixOwner to set
     */
    public void setPosixOwner(String posixOwner) {
        this.posixOwner = posixOwner;
    }

    /**
     * @return the posixGroup
     */
    public String getPosixGroup() {
        return posixGroup;
    }

    /**
     * @param posixGroup the posixGroup to set
     */
    public void setPosixGroup(String posixGroup) {
        this.posixGroup = posixGroup;
    }

    /**
     * @return the posixAttrs
     */
    public String getPosixAttrs() {
        return posixAttrs;
    }

    /**
     * @param posixAttrs the posixAttrs to set
     */
    public void setPosixAttrs(String posixAttrs) {
        this.posixAttrs = posixAttrs;
    }

    /**
     * @return the dosArchive
     */
    public Boolean getDosArchive() {
        return dosArchive;
    }

    /**
     * @param dosArchive the dosArchive to set
     */
    public void setDosArchive(Boolean dosArchive) {
        this.dosArchive = dosArchive;
    }

    /**
     * @return the dosHidden
     */
    public Boolean getDosHidden() {
        return dosHidden;
    }

    /**
     * @param dosHidden the dosHidden to set
     */
    public void setDosHidden(Boolean dosHidden) {
        this.dosHidden = dosHidden;
    }

    /**
     * @return the dosReadonly
     */
    public Boolean getDosReadonly() {
        return dosReadonly;
    }

    /**
     * @param dosReadonly the dosReadonly to set
     */
    public void setDosReadonly(Boolean dosReadonly) {
        this.dosReadonly = dosReadonly;
    }

    /**
     * @return the dosSystem
     */
    public Boolean getDosSystem() {
        return dosSystem;
    }

    /**
     * @param dosSystem the dosSystem to set
     */
    public void setDosSystem(Boolean dosSystem) {
        this.dosSystem = dosSystem;
    }

    /**
     * @return the acl
     */
    public ArrayList<FileAcl> getAcl() {
        return acl;
    }

    /**
     * @param acl the acl to set
     */
    public void setAcl(ArrayList<FileAcl> acl) {
        this.acl = acl;
    }
}
