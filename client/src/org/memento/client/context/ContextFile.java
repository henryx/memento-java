/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client.context;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import org.memento.client.context.commands.CommandFile;

/**
 *
 * @author enrico
 */
public class ContextFile extends AbstractContext {

    public ContextFile(Socket connection) {
        this.connection = connection;
    }

    private void cmdListFile(String directory, Boolean acl) throws FileNotFoundException, IOException {
        CommandFile cmd;
        Path path;
        PrintWriter out;

        out = new PrintWriter(this.connection.getOutputStream(), true);

        try {
            cmd = new CommandFile();
            path = Paths.get(directory);

            cmd.setDirectory(directory);
            cmd.setAcl(acl);
            cmd.setWriter(out);

            if (Files.isReadable(path)) {
                Files.walkFileTree(path, cmd);
            } else {
                throw new IllegalArgumentException("Directory cannot be read: " + path.toString());
            }
        } finally {
            out.close();
        }
    }

    private void cmdGetFile(String fileName) throws FileNotFoundException, IOException {
        BufferedInputStream buff;
        BufferedOutputStream outStream;
        File data;
        FileInputStream fis;
        byte[] buffer;
        int read;

        data = new File(fileName);
        buffer = new byte[8192];

        if (!data.exists()) {
            throw new FileNotFoundException("File not exist");
        }

        if (data.isDirectory()) {
            throw new IllegalArgumentException(fileName + " is not a file");
        }

        fis = new FileInputStream(data);
        buff = new BufferedInputStream(fis);
        outStream = new BufferedOutputStream(this.connection.getOutputStream());

        while ((read = buff.read(buffer)) != -1) {
            outStream.write(buffer, 0, read);
            outStream.flush();
        }

        data = null;
        fis.close();
        buff.close();
        outStream.close();
    }

    private void cmdPutFile(String toString) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Boolean parse(HashMap command) throws IOException {
        ArrayList paths;
        Boolean exit;
        ContextError error;
        HashMap errMsg;

        exit = Boolean.FALSE;
        switch (command.get("name").toString()) {
            case "list":
                try {
                    paths = (ArrayList) command.get("directory");
                    if (paths.isEmpty()) {
                        throw new ClassCastException("List not definied");
                    }

                    for (int item = 0; item < paths.size(); item++) {
                        this.cmdListFile(paths.get(item).toString(), (Boolean) command.get("acl"));
                    }
                } catch (ClassCastException ex) {
                    // TODO: manage exception
                }
                break;
            case "get":
                this.cmdGetFile(command.get("filename").toString());
                break;
            case "put":
                this.cmdPutFile(command.get("filename").toString());
                break;
            default:
                errMsg = new HashMap();
                error = new ContextError(this.connection);

                errMsg.put("message", "Command not found");

                error.parse(errMsg);
                break;
        }

        return exit;
    }
}
