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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * ProjiCast Client class.
 */
public class Client
{
    private enum State { NOT_CONNECTED, AUTHING, CONNECTED, DEAD }
    
    private State state = State.NOT_CONNECTED;
    
    private String host;
    private int port;
    private String password;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    private Map<Integer, ServerFile> videoFiles;
    private Map<Integer, ServerFile> imageFiles;
    private List<ServerFile> playlist;
    
    
    
    /**
     * Creates a new Client for connecting to the specified host and port.
     * 
     * @param host      the host to connect to
     * @param port      the port to connect to
     * @param password  the password to login with
     */
    public Client(String host, int port, String password)
    {
        this.host = host;
        this.port = port;
        this.password = password;
        
        videoFiles = new HashMap<Integer, ServerFile>();
        imageFiles = new HashMap<Integer, ServerFile>();
        playlist = new ArrayList<ServerFile>();
    }
    
    /**
     * @return Returns the host.
     */
    public String getHost()
    {
        return host;
    }
    
    /**
     * @return Returns the password.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @return Returns the port.
     */
    public int getPort()
    {
        return port;
    }
    
    /**
     * Connects this Client to the server.
     * 
     * @throws IOException                on connection error
     * @throws UnknownHostException       if the host is unknown
     * @throws InvalidAuthException       if the password is wrong
     */
    public void connect() throws IOException, UnknownHostException, ProjiCastException
    {
        if(state != State.NOT_CONNECTED)
        {
            throw new IllegalStateException("You must be unconnected to connect!");
        }
        
        try
        {
            socket = new Socket(host, port);
            socket.setSoTimeout(10000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            state = State.AUTHING;
            
            String command;
            String[] split;
            
            out.println("PROJICAST");
            out.flush();
            command = in.readLine();
            split = command.split(" ");
            if((split.length == 3) && split[0].equals("PROJICAST") && split[1].equals("AUTH"))
            {
                String auth = Security.digest(password + split[2]);
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
            
            receiveLists();
            
            state = State.CONNECTED; //connected
        }
        catch(NumberFormatException e)
        {
            close();
            throw new MalformedAnswerException("Unexpected answer, invalid integer.");
        }
        catch(UnsupportedEncodingException e)
        {
            System.err.println("UTF-8 not supported by system!");
            close();
        }
        catch(NullPointerException e)
        {
            throw new MalformedAnswerException("Null read from stream.");
        }
    }
    
    /**
     * Returns an array of all video files on the server.
     * 
     * @return an array of all video files on the server
     */
    public ServerFile[] getVideoFiles()
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        synchronized(videoFiles)
        {
            return videoFiles.values().toArray(new ServerFile[0]);
        }
    }
    
    /**
     * Returns an array of all image files on the server.
     * 
     * @return an array of all image files on the server
     */
    public ServerFile[] getImageFiles()
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        synchronized(imageFiles)
        {
            return imageFiles.values().toArray(new ServerFile[0]);
        }
    }
    
    /**
     * Gets the video file specified by the specified ID.
     * 
     * @param id  the id of the file to get
     *
     * @return the file specified by the id
     */
    public ServerFile getVideoFile(int id)
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        synchronized(videoFiles)
        {
            return videoFiles.get(id);
        }
    }
    
    /**
     * Gets the image file specified by the specified ID.
     * 
     * @param id  the id of the file to get
     *
     * @return the file specified by the id
     */
    public ServerFile getImageFile(int id)
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        synchronized(imageFiles)
        {
            return imageFiles.get(id);
        }
    }
    
    /**
     * Refreshes the file list from the server.
     * 
     * @throws MalformedAnswerException if the server sends something unexpected
     */
    public synchronized void refreshFiles() throws ProjiCastException,IOException
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        out.println("REFRESH");
        out.flush();
        String command = null;
        
        receiveLists();
    }
    
    /**
     * Starts the slideshow.
     */
    public synchronized void slideshow() throws ProjiCastException, IOException
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        out.println("SLIDESHOW");
        out.flush();
        String command = in.readLine();
        if(!command.equals("SLIDESHOW OK"))
        {
            close();
            throw new MalformedAnswerException("Unexpected answer: " + command);
        }
    }
    
    /**
     * Stops all playing/showing.
     */
    public synchronized void stop() throws ProjiCastException, IOException
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        out.println("STOP");
        out.flush();
        String command = in.readLine();
        if(!command.equals("STOP OK"))
        {
            close();
            throw new MalformedAnswerException("Unexpected answer: " + command);
        }
    }
    
    /**
     * Plays a video file on the server.
     * 
     * @param file  the file to play (ServerFile)
     * 
     * @throws FileNotOnServerException  if the specified file has been removed from the server or
     *                                   somtehing like that
     */
    public synchronized void playVideo(ServerFile file) throws ProjiCastException, IOException
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        if(file.getType() != ServerFile.Type.VIDEO)
        {
            throw new IllegalArgumentException("File must be of type VIDEO");
        }
        out.println("PLAY " + file.getID());
        out.flush();
        String command = in.readLine();
        if(command.equals("ERROR File not on server"))
        {
            throw new FileNotOnServerException("File not on server!");
        }
        else if(!command.equals("PLAY OK"))
        {
            close();
            throw new MalformedAnswerException("Unexpected answer: " + command);
        }
    }
    
    /**
     * Displays an image file on the server.
     * 
     * @param file  the file to display (ServerFile)
     * 
     * @throws FileNotOnServerException  if the specified file has been removed from the server or
     *                                   somtehing like that
     */
    public synchronized void displayImage(ServerFile file) throws ProjiCastException, IOException
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        if(file.getType() != ServerFile.Type.IMAGE)
        {
            throw new IllegalArgumentException("File must be of type IMAGE");
        }
        out.println("IMAGE " + file.getID());
        out.flush();
        String command = in.readLine();
        if(command.equals("ERROR File not on server"))
        {
            throw new FileNotOnServerException("File not on server!");
        }
        else if(!command.equals("IMAGE OK"))
        {
            close();
            throw new MalformedAnswerException("Unexpected answer: " + command);
        }
    }
    
    /**
     * Displays the specified text on the server.
     * 
     * @param text  the text to display
     */
    public synchronized void displayText(String text) throws ProjiCastException, IOException
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected!");
        }
        
        out.println("TEXT " + URLEncoder.encode(text, "UTF-8"));
        out.flush();
        String command = in.readLine();
        if(!command.equals("TEXT OK"))
        {
            close();
            throw new MalformedAnswerException("Unexpected answer: " + command);
        }
    }
    
    /**
     * Deletes the specified file from the server.
     * 
     * @param file  the file to delete (ServerFile)
     * 
     * @throws FileNotOnServerException  if the specified file has been removed from the server or
     *                                   somtehing like that
     */
    public synchronized void deleteFile(ServerFile file) throws ProjiCastException, IOException
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        String type = null;
        switch(file.getType())
        {
            case VIDEO:
                type = "VIDEO";
                break;
            case IMAGE:
                type = "IMAGE";
                break;
        }
        
        out.println("DELETE " + type + " " + file.getID());
        out.flush();
        String command = in.readLine();
        if(command.equals("ERROR File not on server"))
        {
            throw new FileNotOnServerException("File not on server!");
        }
        else if(!command.equals("DELETE OK"))
        {
            close();
            throw new MalformedAnswerException("Unexpected answer: " + command);
        }
    }
    
    /**
     * Sets the slideshow playlist on the server.
     * 
     * @throws InvalidArgumentException  if any of the files aren't of the type IMAGE
     *                                   or if they don't exist
     */
    public synchronized void setSlideshowPlaylist(List<ServerFile> pl) throws ProjiCastException, IOException
    {
        if(state != State.CONNECTED)
        {
            throw new IllegalStateException("Must be connected");
        }
        
        //Check all files are valid
        for(ServerFile file : pl)
        {
            if((file.getType() != ServerFile.Type.IMAGE) || !imageFiles.containsValue(file))
            {
                throw new IllegalArgumentException("Invalid ServerFile.");
            }
        }
        
        out.println("PLIST BEGIN");
        out.flush();
        
        String command = in.readLine();
        if(!command.equals("PLIST READY"))
        {
            throw new MalformedAnswerException("Unexpected answer: " + command);
        }
        
        for(ServerFile file : pl)
        {
            out.println("PLIST " + file.getID());
            out.flush();
        }
        
        out.println("PLIST END");
        out.flush();
        
        command = in.readLine();
        if(!command.equals("PLIST OK"))
        {
            throw new MalformedAnswerException("Unexpected answer: " + command);
        }
        
        playlist = pl;
    }
    
    /**
     * Returns the slideshow playlist.
     */
    public List<ServerFile> getSlideshowPlaylist()
    {
        return playlist;
    }
    
    /**
     * Close connection to server.
     */
    public synchronized void close()
    {
        if((state != State.CONNECTED) && (state != State.AUTHING))
        {
            throw new IllegalStateException("Must be connected");
        }
        try
        {
            in.close();
            out.close();
            socket.close();
            state = State.DEAD;
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private synchronized void receiveLists() throws ProjiCastException, IOException
    {
        String command = null;
        
        try
        {
            command = in.readLine();
            
            if(command.equals("LIST BEGIN"))
            {
                videoFiles.clear();
                imageFiles.clear();
                playlist = new ArrayList<ServerFile>(); //TODO good?
                for(;;)
                {
                    command = in.readLine();
                    String[] split = command.split(" ");
                    if(split.length == 3)
                    {
                        if(split[0].equals("VIDEO"))
                        {
                            Integer id = new Integer(split[1]);
                            String name = URLDecoder.decode(split[2], "UTF-8");
                            synchronized(videoFiles)
                            {
                                videoFiles.put(id, new ServerFile(name, ServerFile.Type.VIDEO, id));
                            }
                        }
                        else if(split[0].equals("IMAGE"))
                        {
                            Integer id = new Integer(split[1]);
                            String name = URLDecoder.decode(split[2], "UTF-8");
                            synchronized(imageFiles)
                            {
                                imageFiles.put(id, new ServerFile(name, ServerFile.Type.IMAGE, id));
                            }
                        }
                        else
                        {
                            close();
                            throw new MalformedAnswerException("Unexpected answer: " + command);
                        }
                    }
                    else if((split.length == 2) && split[0].equals("PLIST"))
                    {
                        ServerFile file = imageFiles.get(new Integer(split[1]));
                        if(file != null)
                        {
                            playlist.add(file);
                        }
                    }
                    else if(command.equals("LIST END"))
                    {
                        break;
                    }
                    else
                    {
                        close();
                        throw new MalformedAnswerException("Unexpected answer: " + command);
                    }
                }
            }
        }
        catch (NumberFormatException e)
        {
            throw new MalformedAnswerException("Invalid integers in command: " + command);
        }
        catch (UnsupportedEncodingException e)
        {
            close();
            System.err.println(
                    "System does not support UTF-8 which is required for ProjiCast Client. Disconnected.");
        }
    }
}