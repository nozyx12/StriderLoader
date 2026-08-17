/*
 * Copyright (C) 2026 Nozyx
 *
 * This file is part of StriderLoader.
 *
 * StriderLoader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * StriderLoader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with StriderLoader. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nozyx.strider.loader.impl;

import com.formdev.flatlaf.FlatLightLaf;
import dev.nozyx.strider.loader.api.StriderLoaderInternal;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

@StriderLoaderInternal
final class StriderUI {

    private JFrame frame;

    private JProgressBar progressBar;

    private boolean started = false;

    StriderUI() {}

    void start() {
        FlatLightLaf.setup();

        try {
            UIManager.setLookAndFeel(
                    new FlatLightLaf()
            );
        } catch (UnsupportedLookAndFeelException ignored) {
        }

        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
        UIManager.put("ProgressBar.selectionBackground", Color.DARK_GRAY);

        frame = new JFrame("StriderLoader v" + StriderLoader.LOADER_VERSION);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 20, 20));
        frame.setLocationRelativeTo(null);

        URL iconURL = CrashDialog.class.getResource("/icon.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            frame.setIconImage(icon.getImage());
        }

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel logoLabel = new JLabel();
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            BufferedImage logoImg = ImageIO.read(StriderUI.class.getResource("/logo.png"));
            Image scaledLogo = logoImg.getScaledInstance(250, 110, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(scaledLogo));
        } catch (IOException e) {
            logoLabel.setText("Logo not found");
        }

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(logoLabel);

        JLabel versionLabel = new JLabel("v" + StriderLoader.LOADER_VERSION);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        versionLabel.setForeground(Color.DARK_GRAY);
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(versionLabel);

        progressBar = new JProgressBar();
        progressBar.setBorderPainted(false);
        progressBar.setOpaque(true);
        progressBar.setForeground(new Color(123, 0, 21));
        progressBar.setBackground(new Color(230, 230, 230));
        progressBar.setIndeterminate(true);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        progressBar.setStringPainted(true);
        progressBar.setString("Starting");
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(progressBar);

        frame.setLayout(new BorderLayout());
        frame.add(mainPanel, BorderLayout.NORTH);

        frame.setVisible(true);

        started = true;
    }

    void setStatus(String status) {
        if (started) progressBar.setString(status);
    }

    String getStatus() {
        if (started) return progressBar.getString();
        else return null;
    }

    void close() {
        frame.dispose();
    }

    JFrame getFrame() {
        return frame;
    }
}
