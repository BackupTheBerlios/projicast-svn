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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to store configuration for a ProjiCast server session,
 * such as a Player, a password, FileManagers and so on.
 */
public class Configuration
{
    public enum FileType { VIDEO, IMAGE }
    
    private Player player;
    private Charset charset;
    private String password;
    private FileManager videoManager;
    private FileManager imageManager;
    private List<Integer> slideshowPlaylist;
    
    /**
     * Constructs a new Configuration.
     * 
     * @param player             the player to use
     * @param charset            the charset to use
     * @param password           the password to expect from clients
     * @param videoManager       the FileManager to use for managing video files
     * @param imageManager       the FileManager to use for managing image files
     * @param slideshowPlaylist  the playlist to use for slideshows
     */
    public Configuration(Player player, Charset charset, String password,
            FileManager videoManager, FileManager imageManager, List<Integer> slideshowPlaylist)
    {
        this.player = player;
        this.charset = charset;
        this.password = password;
        this.videoManager = videoManager;
        this.imageManager = imageManager;
        this.slideshowPlaylist = slideshowPlaylist;
    }

    /**
     * @return Returns the charset.
     */
    public Charset getCharset()
    {
        return charset;
    }
    

    /**
     * @return Returns the imageManager.
     */
    public FileManager getImageManager()
    {
        return imageManager;
    }
    

    /**
     * @return Returns the password.
     */
    public String getPassword()
    {
        return password;
    }
    

    /**
     * @return Returns the player.
     */
    public Player getPlayer()
    {
        return player;
    }    

    /**
     * @return Returns the videoManager.
     */
    public FileManager getVideoManager()
    {
        return videoManager;
    }

    public List<Integer> getSlideshowPlaylist()
    {
        return slideshowPlaylist;
    }

    public void setSlideshowPlaylist(List<Integer> slideshowPlaylist)
    {
        this.slideshowPlaylist = slideshowPlaylist;
    }
    
    /**
     * Returns the playlist as files from the image manager.
     */
    public List<File> playlistAsFiles()
    {
        List<File> plist = new ArrayList<File>();
        for(int id : slideshowPlaylist)
        {
            File file = imageManager.getFile(id);
            if(file != null)
            {
                plist.add(file);
            }
        }
        return plist;
    }
    
    /**
     * Saves managers and playlists to file.
     */
    public void save()
    {
        File savefile = new File("files.dat");
        ObjectOutputStream out = null;
        try
        {
            out = new ObjectOutputStream(new FileOutputStream(savefile));
            out.writeObject(videoManager);
            out.writeObject(imageManager);
            out.writeObject(slideshowPlaylist);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                out.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Loads managers and playlists if file exists.
     */
    public void load()
    {
        File savefile = new File("files.dat");
        if(savefile.exists())
        {
            ObjectInputStream in = null;
            try
            {
                in = new ObjectInputStream(new FileInputStream(savefile));
                videoManager = (FileManager)in.readObject();
                imageManager = (FileManager)in.readObject();
                slideshowPlaylist = (List<Integer>)in.readObject();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            catch(ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    in.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
