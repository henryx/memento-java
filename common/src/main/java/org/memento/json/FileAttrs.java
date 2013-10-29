/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.json;

import java.util.ArrayList;

/**
 *
 * @author enrico
 */
public class FileAttrs {

    private String name;
    // Common attributes
    private long ctime;
    private long mtime;
    private long size;
    private String type;
    private String hash;

    // System type
    private String os;

    // Posix attributes
    private boolean posixSymlink;
    private String posixOwner;
    private String posixGroup;
    private String posixPermission;

    // Dos attributes
    private boolean dosArchive;
    private boolean dosHidden;
    private boolean dosReadonly;
    private boolean dosSystem;

    // ACL
    private ArrayList<FileAcl> acl;

    // Other
    private String linkTo;
    private boolean previousDataset;
    private boolean compressed;
    
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

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
    public String getPosixPermission() {
        return posixPermission;
    }

    /**
     * @param posixAttrs the posixAttrs to set
     */
    public void setPosixPermission(String posixPermission) {
        this.posixPermission = posixPermission;
    }

    /**
     * @return the dosArchive
     */
    public boolean getDosArchive() {
        return dosArchive;
    }

    /**
     * @param dosArchive the dosArchive to set
     */
    public void setDosArchive(boolean dosArchive) {
        this.dosArchive = dosArchive;
    }

    /**
     * @return the dosHidden
     */
    public boolean getDosHidden() {
        return dosHidden;
    }

    /**
     * @param dosHidden the dosHidden to set
     */
    public void setDosHidden(boolean dosHidden) {
        this.dosHidden = dosHidden;
    }

    /**
     * @return the dosReadonly
     */
    public boolean getDosReadonly() {
        return dosReadonly;
    }

    /**
     * @param dosReadonly the dosReadonly to set
     */
    public void setDosReadonly(boolean dosReadonly) {
        this.dosReadonly = dosReadonly;
    }

    /**
     * @return the dosSystem
     */
    public boolean getDosSystem() {
        return dosSystem;
    }

    /**
     * @param dosSystem the dosSystem to set
     */
    public void setDosSystem(boolean dosSystem) {
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

    /**
     * @return the posixSymlink
     */
    public boolean getPosixSymlink() {
        return posixSymlink;
    }

    /**
     * @param posixSymlink the posixSymlink to set
     */
    public void setPosixSymlink(boolean posixSymlink) {
        this.posixSymlink = posixSymlink;
    }

    /**
     * @return the linkTo
     */
    public String getLinkTo() {
        return linkTo;
    }

    /**
     * @param linkTo the linkTo to set
     */
    public void setLinkTo(String linkTo) {
        this.linkTo = linkTo;
    }

    /**
     * @return the system
     */
    public String getOs() {
        return os;
    }

    /**
     * @param os the system to set
     */
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * @return the previousDataset
     */
    public boolean getPreviousDataset() {
        return previousDataset;
    }

    /**
     * @param previousDataset the previousDataset to set
     */
    public void setPreviousDataset(boolean previousDataset) {
        this.previousDataset = previousDataset;
    }

    /**
     * @return the compressed
     */
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * @param compressed the compressed to set
     */
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }
}
