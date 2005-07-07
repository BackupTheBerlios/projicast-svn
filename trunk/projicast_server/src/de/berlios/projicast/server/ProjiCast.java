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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import javax.imageio.ImageIO;

/**
 * Main class for projicast.
 * 
 * @author shadewind
 */
public class ProjiCast
{
    
    /*
     * Error codes:
     * 
     * 1 - no config file present
     * 2 - no password in config file
     * 3 - invalid control session port in config file
     * 4 - invalid file transfer session port
     * 5 - invalid transition effect
     * 6 - invalid transition effect delay
     * 7 - unable to create video dir
     * 8 - unable to create image dir
     * 9 - unable to create slideshow dir
     * 10 - invalid slideshow delay
     */
    public static void main(String[] args) throws Exception
    {
        Properties prop = new Properties();
        File configFile = new File("projicast.conf");
        if(!configFile.exists())
        {
            System.err.println("Config file projicast.conf does not exist. Exiting.");
            System.exit(1);
        }
        prop.load(new FileInputStream(configFile));
        
        String password = prop.getProperty("password");
        if(password == null)
        {
            System.err.println("No password specified in config file!");
            System.exit(2);
        }
        
        int port = 30010;
        int ftPort = 30011;
        try
        {
            port = Integer.parseInt(prop.getProperty("controlPort", "30010"));
        }
        catch(NumberFormatException e)
        {
            System.err.println("Invalid control session port specified!");
            System.exit(3);
        }
        try
        {
            ftPort = Integer.parseInt(prop.getProperty("fileTransferPort", "30011"));
        }
        catch(NumberFormatException e)
        {
            System.err.println("Invalid file transfer port specified!");
            System.exit(4);
        }
        
        String mplayerPath = prop.getProperty("mplayerPath", "/usr/bin/mplayer");
        
        File videoPath = new File(prop.getProperty("videoPath", "video"));
        File imagePath = new File(prop.getProperty("imagePath", "images"));
        File slideshowPath = new File(prop.getProperty("slideshowPath", "slideshow"));
        
        if(!videoPath.exists() && !videoPath.mkdirs())
        {
            System.err.println("Could not create the video storage directory. Please create it " +
                    "manually or specify another path in the configuration file.");
            System.exit(7);
        }
        
        if(!imagePath.exists() && !imagePath.mkdirs())
        {
            System.err.println("Could not create the image storage directory. Please create it " +
                    "manually or specify another path in the configuration file.");
            System.exit(8);
        }
        
        if(!slideshowPath.exists() && !slideshowPath.mkdirs())
        {
            System.err.println("Could not create the slideshow storage directory. Please create it " +
                    "manually or specify another path in the configuration file.");
            System.exit(9);
        }
        
        String strEffect = prop.getProperty("transitionEffect", "none");
        ImageDisplayer.TransitionEffect transEffect = ImageDisplayer.TransitionEffect.NONE;
        if(strEffect.equals("fade"))
        {
            transEffect = ImageDisplayer.TransitionEffect.FADE;
        }
        else if(strEffect.equals("blur"))
        {
            transEffect = ImageDisplayer.TransitionEffect.BLUR;
        }
        else if(strEffect.equals("blur_fade_combo"))
        {
            transEffect = ImageDisplayer.TransitionEffect.BLUR_FADE_COMBO;
        }
        else if(strEffect.equals("none"))
        {
            transEffect = ImageDisplayer.TransitionEffect.NONE;
        }
        else
        {
            System.err.println("Invalid transition effect specified in config file!");
            System.exit(5);
        }
        
        int tfd = 5;
        try
        {
            tfd = Integer.parseInt(prop.getProperty("transitionFrameDelay", "5"));
        }
        catch(NumberFormatException e)
        {
            System.err.println("Invalid transition frame delay specified in config file!");
            System.exit(6);
        }
        
        long slideshowDelay = 10000;
        try
        {
            slideshowDelay = Integer.parseInt(prop.getProperty("slideshowDelay", "10000"));
        }
        catch(NumberFormatException e)
        {
            System.err.println("Invalid slideshow delay specified in config file!");
            System.exit(10);
        }
        
        //Display the pretty little splash screen :)
        ImageDisplayer imageDisp = new ImageDisplayer(transEffect, tfd);
        imageDisp.fireUp();
        BufferedImage image = ImageIO.read(ProjiCast.class.getResourceAsStream("/splash.jpg"));
        imageDisp.displayImage(image);
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {}
        imageDisp.fadeToBlack();
        
        Player player = new Player(imageDisp, mplayerPath, slideshowPath, slideshowDelay);
        FileManager videoManager = new FileManager(videoPath);
        FileManager imageManager = new FileManager(imagePath);
        FileManager slideshowManager = new FileManager(slideshowPath);
        Configuration configuration = new Configuration(
                player, 
                Charset.forName("UTF-8"), 
                password, 
                videoManager, 
                imageManager,
                slideshowManager);
        
        ServerThread thread = new ServerThread(configuration, port, ftPort);
        thread.start();
    }
}