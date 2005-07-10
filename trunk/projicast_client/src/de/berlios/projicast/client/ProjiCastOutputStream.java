/*
 * Copyright (c) 2005 Emil Eriksson <shadewind[at]gmail[dot]com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package de.berlios.projicast.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;

/**
 * OutputStream class for sending a file to a ProjiCast server.
 */
public class ProjiCastOutputStream extends OutputStream
{
    private enum State { NOT_CONNECTED, AUTHING, READY_TO_SEND, DEAD }
    
    private State state = State.NOT_CONNECTED;
    private String host;
    private int port;
    private String name;
    private long size;
    private ServerFile.Type type;
    private String password;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private OutputStream bout;
    
    /**
     * Constructs a new ProjiCastOutputStream.
     * 
     * @param host      the host to connect to
     * @param port      the port to connect to
     * @param name      the file name of the file
     * @param size      the amount of data to send
     * @param type      the type of file to send
     * @param password  the password to log in with
     */
    public ProjiCastOutputStream(String host, int port, String name, long size, ServerFile.Type type,
            String password) throws IOException,ProjiCastException
    {
        this.host = host;
        this.port = port;
        this.name = name;
        this.size = size;
        this.type = type;
        this.password = password;
        auth();
    }
    
    /**
     * Authenticate with server.
     */
    private void auth() throws IOException,ProjiCastException
    {
        try
        {
            socket = new Socket(host, port);
            socket.setSoTimeout(10000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            state = State.AUTHING;
            String command;
            String[] split;
            out.println("PROJICAST TRANSFER");
            out.flush();
            command = in.readLine();
            split = command.split(" ");
            if((split.length == 4) && split[0].equals("PROJICAST") && split[1].equals("TRANSFER") && split[2].equals("AUTH"))
            {
                String auth = Security.digest(password + split[3]);
                out.println("AUTH " + auth);
                out.flush();
            }
            else
            {
                close();
                throw new MalformedAnswerException("Unexpected answer: " + command);
            }
            command = in.readLine();
            if(!command.equals("AUTH OK"))
            {
                close();
                split = command.split(" ");
                if(split[0].equals("ERROR"))
                {
                    throw new InvalidAuthException("Wrong password!");
                }
                else
                {
                    throw new MalformedAnswerException("Unexpected answer: " + command);
                }
            }
            String strType = null;
            switch(type)
            {
                case VIDEO:
                    strType = "VIDEO";
                    break;
                case IMAGE:
                    strType = "IMAGE";
                    break;
            }
            out.println("FILE " + strType + " " + size + " " + URLEncoder.encode(name, "UTF-8"));
            out.flush();
            command = in.readLine();
            if(!command.equals("FILE OK"))
            {
                    throw new MalformedAnswerException("Unexpected answer: " + command);
            }
            bout = socket.getOutputStream();
            state = State.READY_TO_SEND;
        }
        catch(NullPointerException e)
        {
            throw new MalformedAnswerException("Null read from stream.");
        }
    }
    
    public void write(int b) throws IOException
    {
            bout.write(b);
    }
    
    public void write(byte[] buf, int off,int len) throws IOException
    {
        bout.write(buf, off, len);
    }
    
    public void close() throws IOException
    {
            out.close();
            in.close();
            socket.close();
    }
}
