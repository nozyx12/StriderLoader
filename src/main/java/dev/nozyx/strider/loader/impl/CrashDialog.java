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

import dev.nozyx.strider.loader.api.StriderLoaderInternal;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

@StriderLoaderInternal
final class CrashDialog {

    static void showCrashDialog(Frame parent, String lastStep, String msg) {
        showCrashDialog(parent, msg, lastStep, null);
    }

    static void showCrashDialog(Frame parent, String lastStep, Throwable th) {
        showCrashDialog(parent, null, lastStep, th);
    }

    static void showCrashDialog(Frame parent, String msg, String lastStep, Throwable th) {
        JDialog dialog = new JDialog(parent, "StriderLoader Crash", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setResizable(false);

        URL iconURL = CrashDialog.class.getResource("/icon.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            dialog.setIconImage(icon.getImage());
        }

        JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane.errorIcon"));

        JLabel titleLabel = new JLabel("<html><h2>StriderLoader crashed!</h2></html>");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));
        headerPanel.add(iconLabel);
        headerPanel.add(titleLabel);

        dialog.add(headerPanel, BorderLayout.PAGE_START);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        dialog.add(mainPanel, BorderLayout.CENTER);

        StringBuilder labelHtml = new StringBuilder("<html>");
        if (msg != null && !msg.isEmpty()) {
            labelHtml.append("<u>Message:</u><br><i>")
                    .append(msg.replace("\n", "<br>"))
                    .append("</i><br><br>");
        }
        if (lastStep != null && !lastStep.isEmpty()) {
            labelHtml.append("<u>Last step:</u><br><i>")
                    .append(lastStep.replace("\n", "<br>"))
                    .append("</i><br><br>");
        }
        if (th != null && th.getMessage() != null && !th.getMessage().isEmpty()) {
            labelHtml.append("<u>Error message:</u><br><i>")
                    .append(th.getMessage())
                    .append("</i><br><br>");
        }
        labelHtml.append("<u>Error stacktrace:</u></html>");

        JLabel label = new JLabel(labelHtml.toString());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(label);

        if (th != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            String stackTrace = sw.toString();

            mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

            JTextArea textArea = new JTextArea(stackTrace);
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setMargin(new Insets(5, 5, 5, 5));
            textArea.setLineWrap(false);

            FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
            String[] lines = stackTrace.split("\n");
            int maxLineWidth = 0;
            for (String line : lines) {
                int width = fm.stringWidth(line);
                if (width > maxLineWidth) maxLineWidth = width;
            }

            int scrollbarWidth = (Integer) UIManager.get("ScrollBar.width");
            int padding = 20;
            int scrollPaneWidth = Math.min(maxLineWidth + scrollbarWidth + padding, 600);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(scrollPaneWidth, 180));
            scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(scrollPane);
        }

        dialog.pack();

        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
