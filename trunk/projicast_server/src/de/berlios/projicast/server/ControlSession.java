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

package de.berlios.projicast.server;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Class for collecting char data read from a buffer and assembling strings from 
 * it when complete lines are available.
 */
public class ControlSession extends BasicInputHandler
{
    enum State { NEW, AUTH_STRING_SENT, LOGGED_IN }
    enum SubState { IDLE, RECEIVING_PLAYLIST }
    
    private Configuration configuration;
    private State state = State.NEW;
    private SubState subState = SubState.IDLE;
    private String authString;
    
    private List<Integer> tmpPlaylist;
    
    /**
     * Creates a new Session with the specified Charset and password.
     * 
     * @param ch        the channel associated with the session
     * @param charset   the charset to use for decoding
     * @param password  the password to expect from the client
     */
    public ControlSession(SocketChannel ch, Configuration configuration)
    {
        super(ch, configuration.getCharset().newDecoder(), configuration.getCharset().newEncoder());
        this.ch = ch;
        this.configuration = configuration;
    }
    
    public void input(ByteBuffer ibuf)
    {
        try
        {
            cInput(ibuf);
        }
        catch(CharacterCodingException e)
        {
            System.err.println("Error while decoding characters: " + e.getMessage());
            close();
        }
        String command = null;
        
        //Until the are no commands left in the buffer, run them
        do
        {
            command = getLine();
            if(command != null)
            {
                if(state != State.LOGGED_IN)
                {
                    auth(command);
                }
                else
                {
                    process(command);
                }
            }
        } while(command != null);
    }
    
    /**
     * Authenticates with client.
     * 
     * @param command  the command received from the client
     */
    private void auth(String command)
    {
        switch(state)
        {
            case NEW:
                if(command.equals("PROJICAST"))
                {
                    authString = Security.generate(128);
                    writeCommand("PROJICAST AUTH " + authString);
                    state = State.AUTH_STRING_SENT;
                }
                else
                {
                    close();
                }
                break;
                
            case AUTH_STRING_SENT:
                String[] split = command.split(" ");
                if((split.length == 2) && split[0].equals("AUTH"))
                {
                    String expect = Security.digest(configuration.getPassword() + authString);
                    if(split[1].equals(expect))
                    {
                        writeCommand("AUTH OK");
                        sendFileList();
                        state = State.LOGGED_IN;
                    }
                    else
                    {
                        writeCommand("ERROR Invalid auth");
                        close();
                    }
                }
                break;
        }
    }
    
    /**
     * Process a command.
     * 
     * @param command  the command
     */
    private void process(String command) //TODO this method is ugly, make it nice
    {
        try
        {
            
            String[] split = command.split(" ");
            if(subState == SubState.IDLE) 
            {
                /*
                 * The follwing commands are only allowed in IDLE state
                 */
                if(command.equals("REFRESH"))
                {
                    configuration.getVideoManager().refresh();
                    configuration.getImageManager().refresh();
                    sendFileList();
                    configuration.save();
                    return;
                }
                else if(command.equals("SLIDESHOW"))
                {
                    configuration.getPlayer().slideshow(configuration.playlistAsFiles());
                    writeCommand("SLIDESHOW OK");
                    return;
                }
                else if(command.equals("STOP"))
                {
                    configuration.getPlayer().stop();
                    writeCommand("STOP OK");
                    return;
                }
                else if((split.length == 2) && split[0].equals("PLAY"))
                {
                    playVideo(Integer.parseInt(split[1]));
                    return;
                }
                else if((split.length == 2) && split[0].equals("IMAGE"))
                {
                    displayImage(Integer.parseInt(split[1]));
                    return;
                }
                else if((split.length == 3) && split[0].equals("DELETE"))
                {
                    if(split[1].equals("VIDEO"))
                    {
                        deleteFile(Integer.parseInt(split[2]), Configuration.FileType.VIDEO);
                        return;
                    }
                    else if(split[1].equals("IMAGE"))
                    {
                        deleteFile(Integer.parseInt(split[2]), Configuration.FileType.IMAGE);
                        return;
                    }
                    else
                    {
                        close();
                    }
                }
            }
            
            /*
             * The playlist command may be allowed depending on the parameter, but that's
             * not up to the process method to decide so we send it to the processPlaylist
             * method.
             */
            if((split.length == 2) && split[0].equals("PLIST"))
            {
                processPlaylist(split);
                return;
            }
            
            close(); //We should only get here if the command is invalid
        }
        catch(NumberFormatException e)
        {
            close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Plays the video file with the specified ID and replies thereafter.
     * 
     * @param id  the ID of the file to play
     */
    private void playVideo(int id)
    {
        FileManager manager = configuration.getVideoManager();
        manager.refresh();
        File file = manager.getFile(id);
        if(file != null)
        {
            configuration.getPlayer().playVideo(file);
            writeCommand("PLAY OK");
        }
        else
        {
            writeCommand("ERROR File not on server");
        }
    }
    
    /**
     * Displays the image with the specified ID and replies thereafter.
     * 
     * @param id  the ID of the file to display
     */
    private void displayImage(int id)
    {
        FileManager manager = configuration.getImageManager();
        manager.refresh();
        File file = manager.getFile(id);
        if(file != null)
        {
            try
            {
                configuration.getPlayer().displayImage(file);
            }
            catch(IOException e)
            {
                configuration.getImageManager().refresh();
            }
            writeCommand("IMAGE OK");
        }
        else
        {
            writeCommand("ERROR File not on server");
        }
    }
    
    /**
     * Deletes the file with the specified ID and type.
     * 
     * @param id    the id of the file to delete
     * @param type  the type of the file to delete
     */
    private void deleteFile(int id, Configuration.FileType type)
    {
        FileManager manager = null;
        switch(type)
        {
            case VIDEO:
                manager = configuration.getVideoManager();
                break;
            case IMAGE:
                manager = configuration.getImageManager();
                break;
        }
        File file = manager.getFile(id);
        if(file != null)
        {
            file.delete();
            manager.refresh();
            writeCommand("DELETE OK");
        }
        else
        {
            writeCommand("ERROR File not on server");
        }
    }
    
    /**
     * Sends list of files avaialable for playing to client.
     */
    private void sendFileList()
    {
        try
        {
            writeCommand("LIST BEGIN");
            Map<Integer, File> files = configuration.getVideoManager().getFileList();
            String encodedName;
            for(Map.Entry<Integer, File> entry : files.entrySet())
            {
                encodedName = URLEncoder.encode(entry.getValue().getName(), configuration.getCharset().name());
                writeCommand("VIDEO " + entry.getKey() + " " + encodedName);
            }
            
            files = configuration.getImageManager().getFileList();
            for(Map.Entry<Integer, File> entry : files.entrySet())
            {
                encodedName = URLEncoder.encode(entry.getValue().getName(), configuration.getCharset().name());
                writeCommand("IMAGE " + entry.getKey() + " " + encodedName);
            }
            
            List<Integer> plist = configuration.getSlideshowPlaylist();
            for(Integer item : plist)
            {
                writeCommand("PLIST " + item.toString());
            }
            
            writeCommand("LIST END");
        }
        catch(UnsupportedEncodingException e)
        {
            //should never happen
            close();
        }
    }
    
    /**
     * Processes a set-playlist command.
     */
    private void processPlaylist(String[] split)
    {
        try
        {
            if((subState == SubState.IDLE) && split[1].equals("BEGIN"))
            {
                subState = SubState.RECEIVING_PLAYLIST;
                tmpPlaylist = new ArrayList<Integer>();
                writeCommand("PLIST READY");
            }
            else if(subState == SubState.RECEIVING_PLAYLIST)
            {
                if(split[1].equals("END"))
                {
                    configuration.setSlideshowPlaylist(tmpPlaylist);
                    tmpPlaylist = null;
                    subState = SubState.IDLE;
                    writeCommand("PLIST OK");
                    configuration.save();
                }
                else
                {
                    tmpPlaylist.add(new Integer(split[1]));
                }
            }
        }
        catch(NumberFormatException e)
        {
            close();
        }
    }
}
