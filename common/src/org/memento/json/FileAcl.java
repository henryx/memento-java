/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.json;

/**
 *
 * @author enrico
 */
public class FileAcl {
    public final static String OWNER = "owner";
    public final static String GROUP = "group";
    
    private String name;
    private String aclType;
    private String attrs;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the aclType
     */
    public String getAclType() {
        return aclType;
    }

    /**
     * @param aclType the aclType to set
     */
    public void setAclType(String aclType) {
        this.aclType = aclType;
    }

    /**
     * @return the attrs
     */
    public String getAttrs() {
        return attrs;
    }

    /**
     * @param attrs the attrs to set
     */
    public void setAttrs(String attrs) {
        this.attrs = attrs;
    }
}
