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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class ProjiCastClient extends JFrame
{   
    private Client client;
    private JList videoList;
    private JList imageList;
    private JList slideshowList;
    private JTextArea textArea;
    
    private ServerFileListModel videoModel;
    private ServerFileListModel imageModel;
    private DefaultListModel slideshowModel;
    
    /**
     * Constructs a new ProjiCastClient using the specified Client.
     * 
     * @param client  the Client to use
     */
    public ProjiCastClient(Client client)
    {
        this.client = client;
        setTitle("ProjiCast Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        videoModel = new ServerFileListModel(ServerFile.Type.VIDEO);
        imageModel = new ServerFileListModel(ServerFile.Type.IMAGE);
        slideshowModel = new DefaultListModel();
        createGUI();
        updateLists();
    }
    
    public void createGUI()
    {
        JScrollPane scrollPane;
        JButton button;
        Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(2,2,2,2);
        
        //========================
        gc.gridx = 0;
        gc.gridy = 0;
        pane.add(new JLabel("Video files:"), gc);
        
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.gridheight = 2;
        gc.fill = GridBagConstraints.BOTH;
        videoList = new JList(videoModel);
        scrollPane = new JScrollPane(videoList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        pane.add(scrollPane, gc);
        
        gc.gridx = 0;
        gc.gridy = 3;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        button = new JButton("Play video");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                playVideo();
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 0;
        gc.gridy = 4;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        button = new JButton("Upload video");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                uploadFile(ServerFile.Type.VIDEO);
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 0;
        gc.gridy = 5;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        button = new JButton("Delete video");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                deleteFile(ServerFile.Type.VIDEO);
            }
        });
        pane.add(button, gc);
        
        //===========================================
        gc.gridx = 1;
        gc.gridy = 0;
        pane.add(new JLabel("Image files:"), gc);
        
        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.gridheight = 2;
        gc.fill = GridBagConstraints.BOTH;
        imageList = new JList(imageModel);
        scrollPane = new JScrollPane(imageList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        pane.add(scrollPane, gc);
        
        gc.gridx = 1;
        gc.gridy = 3;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        button = new JButton("Display image");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                displayImage();
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 1;
        gc.gridy = 4;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        button = new JButton("Upload image");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                uploadFile(ServerFile.Type.IMAGE);
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 1;
        gc.gridy = 5;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        button = new JButton("Delete image");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                deleteFile(ServerFile.Type.IMAGE);
            }
        });
        pane.add(button, gc);
        
        //=========================================
        gc.gridx = 3;
        gc.gridy = 0;
        pane.add(new JLabel("Slideshow playlist:"), gc);
        
        gc.gridx = 3;
        gc.gridy = 1;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.gridheight = 1;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        slideshowList = new JList(slideshowModel);
        scrollPane = new JScrollPane(slideshowList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        pane.add(scrollPane, gc);
        
        gc.gridx = 3;
        gc.gridy = 3;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        gc.gridwidth = 2;
        button = new JButton("Play slideshow");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                slideshow();
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 3;
        gc.gridy = 4;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        gc.gridwidth = 2;
        button = new JButton("Set playlist on server");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                playlistSet();
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 3;
        gc.gridy = 5;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        gc.gridwidth = 2;
        button = new JButton("Delete entry");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                playlistRemove();
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 3;
        gc.gridy = 2;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        gc.gridwidth = 1;
        button = new JButton("Up");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                playlistUp();
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 4;
        gc.gridy = 2;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridheight = 1;
        gc.gridwidth = 1;
        button = new JButton("Down");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                playlistDown();
            }
        });
        pane.add(button, gc);
        
        gc.gridx = 2;
        gc.gridy = 1;
        button = new JButton(">");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                playlistAdd();
            }
        });
        pane.add(button, gc);
        //==========================================
        
        gc.gridx = 0;
        gc.gridy = 6;
        gc.gridwidth = 5;
        button = new JButton("STOP");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                stop();
            }
        });
        pane.add(button, gc);
        
        //=====================================
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new GridBagLayout());
        
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        button = new JButton("Refresh lists");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                refresh();
            }
        });
        panel.add(button, gc);
        
        gc.gridx = 5;
        gc.gridy = 1;
        gc.gridheight = 5;
        pane.add(panel, gc);
        
        //======================================
        
        textArea = new JTextArea();
        scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        gc.gridx = 0;
        gc.gridy = 7;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.gridwidth = 5;
        gc.gridheight = 1;
        pane.add(scrollPane, gc);
        
        button = new JButton("Display text");
        gc.gridx = 0;
        gc.gridy = 8;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridwidth = 5;
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                displayText();
            }
        });
        pane.add(button, gc);
        
        pack();
    }
    
    /**
     * Updates file lists.
     */
    public void updateLists()
    {
        videoModel.refresh();
        imageModel.refresh();
        
        slideshowModel.clear();
        for(ServerFile file : client.getSlideshowPlaylist())
        {
            slideshowModel.addElement(file);
        }
    }
    
    /**
     * Plays the video selected in the video list.
     */
    public void playVideo()
    {
        try
        {
            ServerFile file = (ServerFile)videoList.getSelectedValue();
            if(file != null)
            {
                client.playVideo(file);
            }
        }
        catch(FileNotOnServerException e)
        {
            JOptionPane.showMessageDialog(
                    this,
                    "It seems like the file you tried to play was removed from the server.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            refresh();
        }
        catch(MalformedAnswerException e)
        {
            unexpectedReply();
        }
        catch(ProjiCastException e)
        {
            //should never happen
            e.printStackTrace();
        }
        catch (IOException e)
        {
            inputOutputError(e);
        }
    }
    
    /**
     * Displays the image selected in the image list.
     */
    public void displayImage()
    {
        try
        {
            ServerFile file = (ServerFile)imageList.getSelectedValue();
            if(file != null)
            {
                client.displayImage(file);
            }
        }
        catch(FileNotOnServerException e)
        {
            JOptionPane.showMessageDialog(
                    this,
                    "It seems like the image you tried to display was removed from the server.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            refresh();
        }
        catch(MalformedAnswerException e)
        {
            unexpectedReply();
        }
        catch(ProjiCastException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            inputOutputError(e);
        }
    }
    
    /**
     * Displays the specified text on the server.
     */
    public void displayText()
    {
        try
        {
            client.displayText(textArea.getText());
        }
        catch(MalformedAnswerException e)
        {
            unexpectedReply();
        }
        catch(ProjiCastException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            inputOutputError(e);
        }
    }
    
    /**
     * Starts the slideshow.
     */
    public void slideshow()
    {
        try
        {
            client.slideshow();
        }
        catch(MalformedAnswerException e)
        {
            unexpectedReply();
        }
        catch (ProjiCastException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            inputOutputError(e);
        }
    }
    
    /**
     * Stops all playing
     */
    public void stop()
    {
        try
        {
            client.stop();
        }
        catch(MalformedAnswerException e)
        {
            unexpectedReply();
        }
        catch (ProjiCastException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            inputOutputError(e);
        }
    }
    
    /**
     * Refreshes file lists.
     */
    public void refresh()
    {
        try
        {
            client.refreshFiles();
            updateLists();
        }
        catch(MalformedAnswerException e)
        {
            unexpectedReply();
        }
        catch (ProjiCastException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            inputOutputError(e);
        }
    }
    
    /**
     * Uploads a file of the specified type.
     * 
     * @param type  the type of file to upload
     */
    public void uploadFile(ServerFile.Type type)
    {
        JFileChooser chooser = new JFileChooser();
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();
            FileTransferWindow dialog = new FileTransferWindow(this, client.getHost(), 30011, client.getPassword(), file, type);
            dialog.setVisible(true);
            dialog.start();
        }
    }
    
    /**
     * Deletes the file selected in the specified list.
     * 
     * @param type  the list
     */
    public void deleteFile(ServerFile.Type type)
    {
        try
        {
            JList list = null;
            switch(type)
            {
                case VIDEO:
                    list = videoList;
                    break;
                case IMAGE:
                    list = imageList;
                    break;
            }
            ServerFile file = (ServerFile)list.getSelectedValue();
            client.deleteFile(file);
        }
        catch(FileNotOnServerException e)
        {
            JOptionPane.showMessageDialog(
                    this,
                    "It seems like the file you tried to delete was removed from the server.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            refresh();
        }
        catch(MalformedAnswerException e)
        {
            unexpectedReply();
        }
        catch (ProjiCastException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            inputOutputError(e);
        }
        refresh();
    }
    
    /**
     * Moves the selected playlist entry one step up.
     */
    private void playlistUp()
    {
        ServerFile file = (ServerFile)slideshowList.getSelectedValue();
        int index = slideshowList.getSelectedIndex();
        if((file != null) && ((index - 1) >= 0))
        {
            slideshowModel.remove(index);
            slideshowModel.insertElementAt(file, index - 1);
            slideshowList.setSelectedIndex(index - 1);
        }
    }
    
    /**
     * Moves the selected playlist entry one step down.
     */
    private void playlistDown()
    {
        ServerFile file = (ServerFile)slideshowList.getSelectedValue();
        int index = slideshowList.getSelectedIndex();
        if((file != null) && ((index + 1) < slideshowModel.getSize()))
        {
            slideshowModel.remove(index);
            slideshowModel.insertElementAt(file, index + 1);
            slideshowList.setSelectedIndex(index + 1);
        }
    }
    
    /**
     * Adds the selected image file to the playlist.
     */
    private void playlistAdd()
    {
        ServerFile file = (ServerFile)imageList.getSelectedValue();
        slideshowModel.addElement(file);
    }
    
    /**
     * Removes the selected image from the playlist.
     */
    private void playlistRemove()
    {
        slideshowModel.remove(slideshowList.getSelectedIndex());
    }
    
    /**
     * Sets the playlist on the server.
     */
    private void playlistSet()
    {
        try
        {
            List<ServerFile> playlist = new ArrayList<ServerFile>();
            for(Object obj : slideshowModel.toArray())
            {
                playlist.add((ServerFile)obj);
            }
            client.setSlideshowPlaylist(playlist);
        }
        catch(MalformedAnswerException e)
        {
            unexpectedReply();
        }
        catch (ProjiCastException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            inputOutputError(e);
        }
    }
    
    //=============================
    
    /**
     * Called when the server replies unexpectedly.
     */
    private void unexpectedReply()
    {
        JOptionPane.showMessageDialog(
                this,
                "The server replied in an unexpedted way during the operation.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Called when an IOException is thrown because of an operation to the server.
     * 
     * @param e  the exception thrown
     */
    private void inputOutputError(IOException e)
    {
        JOptionPane.showMessageDialog(
                this,
                "An exception occurred when trying to carry out the operation: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
    
    public static void main(String[] args) throws Exception
    {
        ConnectDialog cDialog = new ConnectDialog();
        cDialog.setVisible(true);
        Client client = cDialog.getClient();
        if(client != null)
        {
            new ProjiCastClient(client).setVisible(true);
        }
    }
    
    /**
     * ListModel for ServerFiles.
     */
    private class ServerFileListModel extends AbstractListModel
    {
        private ServerFile.Type type;
        private int length;
        
        /**
         * Constructs a new ServerFileListModel.
         * 
         * @param type  the type for this ServerFileListModel
         */
        public ServerFileListModel(ServerFile.Type type)
        {
            this.type = type;
        }
        
        public Object getElementAt(int pos)
        {
            ServerFile[] files = null;
            switch(type)
            {
                case VIDEO:
                    files = client.getVideoFiles();
                    break;
                case IMAGE:
                    files = client.getImageFiles();
                    break;
            }
            return files[pos];
        }
        
        public int getSize()
        {
            ServerFile[] files = null;
            switch(type)
            {
                case VIDEO:
                    files = client.getVideoFiles();
                    break;
                case IMAGE:
                    files = client.getImageFiles();
                    break;
            }
            length = files.length;
            return files.length;
        }
        
        public void refresh()
        {
            fireContentsChanged(this, 0, (length - 1));
        }
    }
}
