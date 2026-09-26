package com.arenamalang.futsal;

import com.arenamalang.futsal.gui.MainFrame;

import javax.swing.*;

/**
 * Entry Point Aplikasi: "Futsal & Mini Soccer Arena Malang" — Reservasi Lapangan Olahraga.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Gunakan tampilan Look and Feel yang konsisten dan rapi
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
