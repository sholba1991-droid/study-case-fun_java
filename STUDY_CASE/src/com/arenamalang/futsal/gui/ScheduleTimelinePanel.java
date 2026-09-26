package com.arenamalang.futsal.gui;

import com.arenamalang.futsal.model.JadwalSewa;
import com.arenamalang.futsal.service.JadwalManager;
import com.arenamalang.futsal.service.TarifCalculator;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Panel visualisasi slot jadwal jam operasional (08.00 s.d. 23.00) untuk tanggal tertentu.
 * Menghilangkan risiko bentrok jadwal dengan visualisasi status Terisi vs Tersedia.
 */
public class ScheduleTimelinePanel extends JPanel {

    private final JadwalManager jadwalManager;
    private LocalDate currentTanggal;
    private Consumer<Integer> onSlotClickedListener;
    private final JPanel slotsContainer;
    private final JLabel headerTitleLabel;

    public ScheduleTimelinePanel(JadwalManager jadwalManager) {
        this.jadwalManager = jadwalManager;
        this.currentTanggal = LocalDate.now();

        setLayout(new BorderLayout(8, 8));
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        // Header Panel Timeline
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setOpaque(false);

        headerTitleLabel = new JLabel("Status Ketersediaan Slot Lapangan: " + currentTanggal.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", java.util.Locale.of("id", "ID"))));
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerTitleLabel.setForeground(new Color(33, 37, 41));

        // Legend panel
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        legendPanel.setOpaque(false);
        legendPanel.add(createLegendBadge("🟢 Tersedia", new Color(230, 247, 238), new Color(25, 135, 84)));
        legendPanel.add(createLegendBadge("🔴 Terisi", new Color(253, 237, 237), new Color(220, 53, 69)));
        legendPanel.add(createLegendBadge("☀️ Siang (Rp 120rb)", new Color(255, 250, 230), new Color(180, 120, 0)));
        legendPanel.add(createLegendBadge("🌙 Malam+Lampu (Rp 180rb)", new Color(235, 240, 255), new Color(30, 70, 180)));

        topPanel.add(headerTitleLabel, BorderLayout.WEST);
        topPanel.add(legendPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Slots grid container: Jam 08.00 s.d. 23.00 (15 slot jam)
        slotsContainer = new JPanel(new GridLayout(3, 5, 8, 8));
        slotsContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(slotsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        refreshTimeline(currentTanggal);
    }

    public void setOnSlotClickedListener(Consumer<Integer> listener) {
        this.onSlotClickedListener = listener;
    }

    private JLabel createLegendBadge(String text, Color bg, Color fg) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(fg);
        label.setBackground(bg);
        label.setOpaque(true);
        label.setBorder(new CompoundBorder(
                new LineBorder(fg, 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        return label;
    }

    public void refreshTimeline(LocalDate tanggal) {
        this.currentTanggal = tanggal;
        headerTitleLabel.setText("Status Ketersediaan Slot Lapangan: " +
                currentTanggal.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", java.util.Locale.of("id", "ID"))));

        slotsContainer.removeAll();

        // Operasional: Jam 08:00 sampai 23:00 (15 jam)
        for (int jam = TarifCalculator.JAM_BUKA; jam < TarifCalculator.JAM_TUTUP; jam++) {
            JadwalSewa booked = jadwalManager.getJadwalPadaJam(currentTanggal, jam);
            boolean isMalam = (jam >= TarifCalculator.BATAS_JAM_SIANG);
            String slotLabel = String.format("%02d:00 - %02d:00", jam, jam + 1);

            JPanel slotCard = new JPanel(new BorderLayout(4, 2));
            slotCard.setBorder(new EmptyBorder(6, 8, 6, 8));
            slotCard.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel timeHeader = new JLabel((isMalam ? "🌙 " : "☀️ ") + slotLabel);
            timeHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));

            JLabel statusLabel = new JLabel();
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            final int clickedJam = jam;

            if (booked != null) {
                // Terisi
                slotCard.setBackground(new Color(254, 242, 242));
                slotCard.setBorder(new LineBorder(new Color(239, 68, 68), 1, true));
                timeHeader.setForeground(new Color(153, 27, 27));

                String teamName = booked.getNamaTim();
                if (teamName.length() > 16) {
                    teamName = teamName.substring(0, 14) + "..";
                }
                statusLabel.setText("⛔ Terisi: " + teamName);
                statusLabel.setForeground(new Color(185, 28, 28));

                slotCard.setToolTipText(String.format(
                        "<html><b>TERISI (JADWAL SEWA)</b><br>Tim: <b>%s</b><br>No HP: %s<br>Rentang Sewa: %s<br>Status: %s</html>",
                        booked.getNamaTim(), booked.getNoHp(), booked.getFormattedRentangJam(), booked.getStatusDP()
                ));

                slotCard.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        JOptionPane.showMessageDialog(slotCard,
                                String.format("Slot jam %s sudah disewa oleh:\n\nTim: %s\nKontak HP: %s\nRentang Sewa: %s (%d Jam)\nStatus: %s",
                                        slotLabel, booked.getNamaTim(), booked.getNoHp(), booked.getFormattedRentangJam(),
                                        booked.getDurasiJam(), booked.getStatusDP()),
                                "Slot Jam Terisi",
                                JOptionPane.WARNING_MESSAGE);
                    }
                });
            } else {
                // Tersedia
                slotCard.setBackground(isMalam ? new Color(240, 249, 255) : new Color(240, 253, 244));
                slotCard.setBorder(new LineBorder(isMalam ? new Color(147, 197, 253) : new Color(134, 239, 172), 1, true));
                timeHeader.setForeground(isMalam ? new Color(30, 64, 175) : new Color(22, 101, 52));

                double tarifSlot = isMalam ? TarifCalculator.TARIF_MALAM_PER_JAM : TarifCalculator.TARIF_SIANG_PER_JAM;
                statusLabel.setText("✓ Tersedia (" + TarifCalculator.formatRupiah(tarifSlot) + ")");
                statusLabel.setForeground(isMalam ? new Color(29, 78, 216) : new Color(21, 128, 61));

                slotCard.setToolTipText(String.format("Slot %s Tersedia! Klik untuk memilih jam mulai ini di formulir.", slotLabel));

                slotCard.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (onSlotClickedListener != null) {
                            onSlotClickedListener.accept(clickedJam);
                        }
                    }
                });
            }

            slotCard.add(timeHeader, BorderLayout.NORTH);
            slotCard.add(statusLabel, BorderLayout.CENTER);
            slotsContainer.add(slotCard);
        }

        slotsContainer.revalidate();
        slotsContainer.repaint();
    }
}
