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
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;


/**
 * Window showing file transfer progress.
 */
public class FileTransferWindow extends JFrame
{
    private ProjiCastClient client;
    private String host;
    private int port;
    private String password;
    private File file;
    private ServerFile.Type type;
    
    private JLabel statusLabel;
    private JProgressBar bar;
    
    private TransferThread thread;
    
    
    /**
     * Constructs a new FileTransferWindow.
     * 
     * @param client    the parent client of this transfer window
     * @param host      the host to connect to
     * @param port      the port to connect to
     * @param password  the password to log in with
     * @param file      the file to send
     * @param type      the file type
     */
    public FileTransferWindow(ProjiCastClient client, String host, int port, String password, File file, ServerFile.Type type)
    {
        this.client = client;
        this.host = host;
        this.port = port;
        this.password = password;
        this.file = file;
        this.type = type;
        
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new CloseListener());
        Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());
        
        GridBagConstraints gc = new GridBagConstraints();
        
        gc.insets = new Insets(10,10,10,10);
        
        gc.gridx = 0;
        gc.gridy = 0;
        statusLabel = new JLabel("Initiating...");
        pane.add(statusLabel, gc);
        
        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        bar = new JProgressBar(0, 100);
        bar.setPreferredSize(new Dimension(300, 20));
        bar.setStringPainted(true);
        pane.add(bar, gc);
        
        gc.gridx = 0;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.NONE;
        JButton but = new JButton("Cancel");
        but.addActionListener(new ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent arg0)
            {
                thread.cancel();
                dispose();
            }
        });
        pane.add(but, gc);
        
        pack();
    }
    
    /**
     * Starts the file transfer.
     */
    public void start()
    {
        thread = new TransferThread();
        thread.start();
    }
    
    /**
     * Listener for window closing.
     */
    class CloseListener extends WindowAdapter
    {
        public void windowClosing(WindowEvent arg0)
        {
            thread.cancel();
        }
    }
    
    /**
     * Thread for handling transfer.
     */
    class TransferThread extends Thread
    {
        private long lastCheck = 0;
        private long nextCheck = 0;
        private long speed = 0;
        private float percentage;

        private ProjiCastOutputStream out;
        private long written = 0;
        private long size;
        private DecimalFormat kbForm;
        private DecimalFormat mbForm;
        private boolean cancel = false;
        
        /**
         * Cancels the file transfer.
         */
        public void cancel()
        {
            cancel = true;
        }
        
        public void run()
        {
            FileInputStream in = null;
            try
            {
                statusLabel.setText("Connecting to server...");
                out = new ProjiCastOutputStream(host, port, file.getName(), file.length(), type, password);
                
                
                mbForm = new DecimalFormat("0.0");
                kbForm = new DecimalFormat("0");
                
                in = new FileInputStream(file);
                byte[] buf = new byte[8 * 4096];
                int n;
                
                size = file.length();
                while((written < size) && !cancel)
                {
                    n = in.read(buf);
                    out.write(buf, 0, n);
                    written += n;
                    
                    update();
                }
                client.refresh();
                dispose();
            }
            catch(IOException e)
            {
                JOptionPane.showMessageDialog(
                        FileTransferWindow.this, "Error while sending file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                dispose();
            }
            catch(InvalidAuthException e)
            {
                JOptionPane.showMessageDialog(
                        FileTransferWindow.this,
                        "The password logged in with wasn't accepted for file transfer login.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                dispose();
            }
            catch(MalformedAnswerException e)
            {
                JOptionPane.showMessageDialog(
                        FileTransferWindow.this,
                        "The server made an unexpected reply and when trying to send the file.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                dispose();
            }
            catch(ProjiCastException e)
            {
                //should never happen
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    out.close();
                    in.close();
                }
                catch(IOException e) {}
            }
        }
        
        private void calcInfo()
        {
            //Speed
            if(System.currentTimeMillis() > nextCheck)
            {
                nextCheck = System.currentTimeMillis() + 3000;
                speed = (int)((written - lastCheck) / 3);
                lastCheck = written;
                
            }
            
            //Percentage
            percentage = (((float)written / (float)size) * 100f);
        }
        
        private void update()
        {
            calcInfo();
            bar.setValue((int)percentage);
            bar.setString((int)percentage + "%");
            
            if(size > 1024*1024)
            {
                statusLabel.setText(
                        mbForm.format((double)written / (1024d * 1024d)) + "Mb / " + 
                        mbForm.format((double)size / (1024d * 1024d)) + "Mb (" + 
                        kbForm.format((double)speed / 1024d) + "kb/s)");
            }
            else
            {
                statusLabel.setText(
                        kbForm.format((double)written / 1024d) + "kb / " + 
                        kbForm.format((double)size / 1024d) + "kb (" + 
                        kbForm.format((double)speed / 1024d) + "kb/s)");
            }
        }
    }
}
