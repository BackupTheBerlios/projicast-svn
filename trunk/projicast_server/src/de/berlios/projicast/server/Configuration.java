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

import java.nio.charset.Charset;

/**
 * Class used to store configuration for a ProjiCast server session,
 * such as a Player, a password, FileManagers and so on.
 */
public class Configuration
{
    public enum FileType { VIDEO, IMAGE, SLIDESHOW }
    
    private Player player;
    private Charset charset;
    private String password;
    private FileManager videoManager;
    private FileManager imageManager;
    private FileManager slideshowManager;
    
    /**
     * Constructs a new Configuration.
     * 
     * @param player            the player to use
     * @param charset           the charset to use
     * @param password          the password to expect from clients
     * @param videoManager      the FileManager to use for managing video files
     * @param imageManager      the FileManager to use for managing image files
     * @param slideshowManager  the FileManager to use for managing slideshow files
     */
    public Configuration(Player player, Charset charset, String password,
            FileManager videoManager, FileManager imageManager, FileManager slideshowManager)
    {
        this.player = player;
        this.charset = charset;
        this.password = password;
        this.videoManager = videoManager;
        this.imageManager = imageManager;
        this.slideshowManager = slideshowManager;
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
     * @return Returns the slideshowManager.
     */
    public FileManager getSlideshowManager()
    {
        return slideshowManager;
    }
    

    /**
     * @return Returns the videoManager.
     */
    public FileManager getVideoManager()
    {
        return videoManager;
    }
}
