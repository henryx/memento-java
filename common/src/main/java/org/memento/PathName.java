/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Set;
import org.memento.json.FileAcl;
import org.memento.json.FileAttrs;

/**
 *
 * @author enrico
 */
public class PathName {

    private File path;

    public PathName(File aPath) {
        this.path = aPath;
    }

    private ArrayList<FileAcl> aclFromWindows() throws IOException, InterruptedException {
        // TODO: write code for Windows ACL extraction
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private ArrayList<FileAcl> aclFromLinux() throws IOException, InterruptedException {
        // NOTE: Need to manage case when external command fail
        ArrayList<String> args;
        BufferedReader bri;
        //BufferedReader bre;
        FileAcl acl;
        ArrayList<FileAcl> result;
        Process p;
        String line;

        result = new ArrayList<>();

        args = new ArrayList<>();
        args.add("getfacl");
        args.add(this.path.getAbsolutePath());
            
        p = new ProcessBuilder(args).start();
        p.waitFor();
        
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

        //p.exitValue()

        return result;
    }

    private void aclToWindows(ArrayList<FileAcl> acls) throws IOException, InterruptedException {
        // TODO: write code for storing Windows ACL
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void aclToLinux(ArrayList<FileAcl> acls) throws IOException, InterruptedException {
        ArrayList<String> args;
        Process p;
        String acl;

        for (FileAcl item : acls) {
            if (item.getAclType().equals(FileAcl.OWNER)) {
                acl = "u:" + item.getName() + ":" + item.getAttrs();
            } else {
                acl = "g:" + item.getName() + ":" + item.getAttrs();
            }

            args = new ArrayList<>();
            args.add("setfacl");
            args.add("-m");
            args.add(acl);
            args.add(this.path.getAbsolutePath());
            
            p = new ProcessBuilder(args).start();
            p.waitFor();
        }
    }

    public String hash() throws NoSuchAlgorithmException, IOException {
        FileInputStream fis;
        MessageDigest md;
        String hex;
        StringBuffer hexString;
        byte[] dataBytes;
        int nread;

        md = MessageDigest.getInstance("MD5");
        fis = new FileInputStream(this.path);
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
        attr = Files.readAttributes(this.path.toPath(), BasicFileAttributes.class);

        result.setCtime(attr.creationTime().toMillis());
        result.setMtime(attr.lastModifiedTime().toMillis());
        //result.append("symlink", attr.isSymbolicLink()); //Redundant
        result.setSize(attr.size());

        if (System.getProperty("os.name").startsWith("Windows")) {
            dosAttr = Files.readAttributes(this.path.toPath(), DosFileAttributes.class);

            result.setDosArchive(dosAttr.isArchive());
            result.setDosHidden(dosAttr.isHidden());
            result.setDosReadonly(dosAttr.isReadOnly());
            result.setDosSystem(dosAttr.isSystem());
        } else {
            posixAttr = Files.readAttributes(this.path.toPath(), PosixFileAttributes.class);

            result.setPosixSymlink(this.isSymlink());

            if (result.getPosixSymlink()) {
                result.setLinkTo(Files.readSymbolicLink(this.path.toPath()).toString());
            }

            result.setPosixOwner(posixAttr.owner().getName());
            result.setPosixGroup(posixAttr.group().getName());
            result.setPosixPermission(PosixFilePermissions.toString(posixAttr.permissions()));
        }

        return result;
    }

    public void setAttrs(FileAttrs attrs) throws IOException {
        UserPrincipal posixOwner;
        GroupPrincipal posixGroup;
        Set<PosixFilePermission> posixPerms;
        UserPrincipalLookupService lookup;

        if (System.getProperty("os.name").startsWith("Windows")) {
            Files.setAttribute(this.path.toPath(), "dos:archive", attrs.getDosArchive());
            Files.setAttribute(this.path.toPath(), "dos:hidden", attrs.getDosHidden());
            Files.setAttribute(this.path.toPath(), "dos:readonly", attrs.getDosReadonly());
            Files.setAttribute(this.path.toPath(), "dos:system", attrs.getDosSystem());
        } else {
            lookup = this.path.toPath().getFileSystem().getUserPrincipalLookupService();

            posixOwner = lookup.lookupPrincipalByName(attrs.getPosixOwner());
            posixGroup = lookup.lookupPrincipalByGroupName(attrs.getPosixGroup());
            posixPerms = PosixFilePermissions.fromString(attrs.getPosixPermission());

            Files.setOwner(this.path.toPath(), posixOwner);
            Files.getFileAttributeView(this.path.toPath(), PosixFileAttributeView.class)
                    .setGroup(posixGroup);
            Files.setPosixFilePermissions(this.path.toPath(), posixPerms);
        }
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

    public void setAcl(ArrayList<FileAcl> acls) throws IOException {
        try {
            if (System.getProperty("os.name").startsWith("Windows")) {
                this.aclToWindows(acls);
            } else {
                this.aclToLinux(acls);
            }
        } catch (InterruptedException ex) {
        }
    }

    public boolean isDirectory() throws IllegalArgumentException, FileNotFoundException {
        return Files.isDirectory(this.path.toPath());
    }

    public boolean isSymlink() {
        return Files.isSymbolicLink(this.path.toPath());
    }

    public String getAbsolutePath() {
        return this.path.toString();
    }
}
