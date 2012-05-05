/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import org.memento.json.FileAcl;
import org.memento.json.FileAttrs;

/**
 *
 * @author enrico
 */
public class PathName {

    private Path path;

    public PathName(Path aPath) {
        this.path = aPath;
    }

    private ArrayList<FileAcl> aclFromWindows() throws IOException, InterruptedException {
        // TODO: write code for Windows ACL extraction
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private ArrayList<FileAcl> aclFromLinux() throws IOException, InterruptedException {
        // NOTE: Need to manage case when external command fail
        BufferedReader bri;
        //BufferedReader bre;
        FileAcl acl;
        ArrayList<FileAcl> result;
        Process p;
        String[] command;
        String line;

        result = new ArrayList<>();

        command = new String[]{"getfacl", this.path.toString()};
        p = Runtime.getRuntime().exec(command);
        bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
        //bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        while ((line = bri.readLine()) != null) {
            if (!line.startsWith("#") && !line.contains("::") && !line.isEmpty()) {
                acl = new FileAcl();

                if (line.startsWith("user")) {
                    acl.setAclType(FileAcl.OWNER);
                } else if (line.startsWith("group")) {
                    acl.setAclType(FileAcl.GROUP);
                }

                acl.setName(line.split(":")[1]);
                acl.setAttrs(line.split(":")[2]);

                result.add(acl);
            }
        }
        bri.close();

        /*
        while ((line = bre.readLine()) != null) {
            System.out.println(line);
        }
        bre.close();
        */

        p.waitFor();
        //p.exitValue()

        return result;
    }

    public String hash() throws NoSuchAlgorithmException, IOException {
        FileInputStream fis;
        MessageDigest md;
        String hex;
        StringBuffer hexString;
        byte[] dataBytes;
        int nread;

        md = MessageDigest.getInstance("MD5");
        fis = new FileInputStream(this.path.toFile());
        dataBytes = new byte[65536];
        hexString = new StringBuffer();

        try {
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        } finally {
            fis.close();
        }

        byte[] mdbytes = md.digest();

        for (int i = 0; i < mdbytes.length; i++) {
            hex = Integer.toHexString(0xff & mdbytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        fis.close();

        return hexString.toString();
    }

    public FileAttrs getAttrs() throws IOException {
        FileAttrs result;
        BasicFileAttributes attr;
        DosFileAttributes dosAttr;
        PosixFileAttributes posixAttr;

        result = new FileAttrs();
        attr = Files.readAttributes(this.path, BasicFileAttributes.class);

        result.setCtime(attr.creationTime().toMillis());
        result.setMtime(attr.lastModifiedTime().toMillis());
        //result.append("symlink", attr.isSymbolicLink()); //Redundant
        result.setSize(attr.size());

        if (System.getProperty("os.name").startsWith("Windows")) {
            dosAttr = Files.readAttributes(this.path, DosFileAttributes.class);

            result.setDosArchive(dosAttr.isArchive());
            result.setDosHidden(dosAttr.isHidden());
            result.setDosReadonly(dosAttr.isReadOnly());
            result.setDosSystem(dosAttr.isSystem());
        } else {
            posixAttr = Files.readAttributes(this.path, PosixFileAttributes.class);

            result.setPosixSymlink(posixAttr.isSymbolicLink());
            
            if (result.getPosixSymlink()) {
                result.setLinkTo(Files.readSymbolicLink(path).toString());
            }
            
            result.setPosixOwner(posixAttr.owner().getName());
            result.setPosixGroup(posixAttr.group().getName());
            result.setPosixPermission(PosixFilePermissions.toString(posixAttr.permissions()));
        }

        return result;
    }

    public ArrayList<FileAcl> getAcl() throws IOException {
        ArrayList<FileAcl> result;

        try {
            if (System.getProperty("os.name").startsWith("Windows")) {
                result = aclFromWindows();
            } else {
                result = aclFromLinux();
            }
        } catch (InterruptedException ex) {
            result = null;
        }

        return result;
    }

    public Boolean isDirectory() throws IllegalArgumentException, FileNotFoundException {
        return Files.isDirectory(this.path);
    }
    
    public Boolean isSymlink() {
        return Files.isSymbolicLink(this.path);
    }
    
    public String getAbsolutePath() {
        return this.path.toString();
    }
}
