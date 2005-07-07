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
import java.io.IOException;
import java.net.ConnectException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;


public class ConnectDialog extends JDialog
{
    private JTextField hostField;
    private JSpinner portSpinner;
    private JPasswordField passwordField;
    private Client client;
    
    /**
     * Constructs a new ConnectWindow.
     */
    public ConnectDialog()
    {
        setTitle("ProjiCast - Connect");
        Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        
        setResizable(false);
        setModal(true);
        setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(2,2,2,2);
        
        gc.gridx = 0;
        gc.gridy = 0;
        pane.add(new JLabel("Host:"), gc);
        
        gc.gridx = 0;
        gc.gridy = 1;
        hostField = new JTextField();
        hostField.setPreferredSize(new Dimension(200, 19));
        pane.add(hostField, gc);
        
        gc.gridx = 0;
        gc.gridy = 2;
        pane.add(new JLabel("Port:"), gc);
        
        gc.gridx = 0;
        gc.gridy = 3;
        portSpinner = new JSpinner(new SpinnerNumberModel(
                30010, 1, 66000, 1));
        portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));
        pane.add(portSpinner, gc);
        
        gc.gridx = 0;
        gc.gridy = 4;
        pane.add(new JLabel("Password: "), gc);
        
        gc.gridx = 0;
        gc.gridy = 5;
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(200, 19));
        pane.add(passwordField, gc);
        
        gc.gridx = 0;
        gc.gridy = 6;
        gc.anchor = GridBagConstraints.CENTER;
        JButton button = new JButton("Connect");
        button.addActionListener(new ConnectListener());
        pane.add(button, gc);
        
        pack();
        setSize(getWidth() + 10, getHeight() + 10);
    }
    
    /**
     * Returns the Client object.
     */
    public Client getClient()
    {
        return client;
    }
    
    class ConnectListener implements ActionListener
    {
        public void actionPerformed(ActionEvent arg0)
        {
            try
            {
                String host = hostField.getText();
                int port = (Integer)portSpinner.getValue();
                String password = new String(passwordField.getPassword());
                client = new Client(host, port, password);
                client.connect();
                setVisible(false);
            }
            catch(ConnectException e)
            {
                JOptionPane.showMessageDialog(
                        ConnectDialog.this, "Connection refused.", "Connection refused", JOptionPane.ERROR_MESSAGE);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            catch(InvalidAuthException e)
            {
                JOptionPane.showMessageDialog(
                        ConnectDialog.this, "Wrong password.", "Wrong password", JOptionPane.ERROR_MESSAGE);
            }
            catch(ProjiCastException e)
            {
                e.printStackTrace();
            }
        }
    }
}
