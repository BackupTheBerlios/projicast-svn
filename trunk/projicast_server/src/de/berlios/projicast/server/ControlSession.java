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
import java.util.Map;


/**
 * Class for collecting char data read from a buffer and assembling strings from 
 * it when complete lines are available.
 */
public class ControlSession extends BasicInputHandler
{
    enum State { NEW, AUTH_STRING_SENT, LOGGED_IN }
    
    private Configuration configuration;
    private State state = State.NEW;
    private String authString;
    
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
        String command = getLine();
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
    private void process(String command)
    {
        try
        {
            String[] split = command.split(" ");
            if(command.equals("REFRESH"))
            {
                configuration.getVideoManager().refresh();
                configuration.getImageManager().refresh();
                configuration.getSlideshowManager().refresh();
                sendFileList();
            }
            else if(command.equals("SLIDESHOW"))
            {
                configuration.getPlayer().slideshow();
                writeCommand("SLIDESHOW OK");
            }
            else if(command.equals("STOP"))
            {
                configuration.getPlayer().stop();
                writeCommand("STOP OK");
            }
            else if((split.length == 2) && split[0].equals("PLAY"))
            {
                playVideo(Integer.parseInt(split[1]));
            }
            else if((split.length == 2) && split[0].equals("IMAGE"))
            {
                displayImage(Integer.parseInt(split[1]));
            }
            else if((split.length == 3) && split[0].equals("DELETE"))
            {
                if(split[1].equals("VIDEO"))
                {
                    deleteFile(Integer.parseInt(split[2]), Configuration.FileType.VIDEO);
                }
                else if(split[1].equals("IMAGE"))
                {
                    deleteFile(Integer.parseInt(split[2]), Configuration.FileType.IMAGE);
                }
                else if(split[1].equals("SLDSHW"))
                {
                    deleteFile(Integer.parseInt(split[2]), Configuration.FileType.SLIDESHOW);
                }
                else
                {
                    close();
                }
            }
            else
            {
                close();
            }
        }
        catch (NumberFormatException e)
        {
            close();
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
            case SLIDESHOW:
                manager = configuration.getSlideshowManager();
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
            
            files = configuration.getSlideshowManager().getFileList();
            for(Map.Entry<Integer, File> entry : files.entrySet())
            {
                encodedName = URLEncoder.encode(entry.getValue().getName(), configuration.getCharset().name());
                writeCommand("SLDSHW " + entry.getKey() + " " + encodedName);
            }
            
            writeCommand("LIST END");
        }
        catch(UnsupportedEncodingException e)
        {
            //should never happen
            close();
        }
    }
}
