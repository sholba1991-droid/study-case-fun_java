package com.arenamalang.futsal.gui;

import com.arenamalang.futsal.model.JadwalSewa;
import com.arenamalang.futsal.service.TarifCalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Dialog Bukti Pembayaran / Struk Reservasi Lapangan Olahraga
 * Futsal & Mini Soccer Arena Malang.
 */
public class ReceiptDialog extends JDialog {

    private final JadwalSewa jadwal;
    private final JEditorPane receiptEditorPane;
    private final Locale localeId = Locale.of("id", "ID");

    public ReceiptDialog(Frame owner, JadwalSewa jadwal) {
        super(owner, "Bukti Reservasi — " + jadwal.getIdBooking(), true);
        this.jadwal = jadwal;

        setSize(540, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        receiptEditorPane = new JEditorPane();
        receiptEditorPane.setContentType("text/html");
        receiptEditorPane.setEditable(false);
        receiptEditorPane.setText(generateHtmlReceipt());
        receiptEditorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(receiptEditorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Tombol Aksi Bawah
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(new Color(245, 247, 248));

        JButton copyWaBtn = new JButton("📋 Salin Teks WhatsApp");
        copyWaBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        copyWaBtn.addActionListener(e -> copyToClipboard(generateWhatsAppText()));

        JButton printBtn = new JButton("🖨️ Cetak Struk");
        printBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        printBtn.setBackground(new Color(25, 135, 84));
        printBtn.setForeground(Color.WHITE);
        printBtn.setFocusPainted(false);
        printBtn.addActionListener(e -> {
            try {
                receiptEditorPane.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal mencetak: " + ex.getMessage(), "Error Cetak", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton closeBtn = new JButton("Tutup");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(copyWaBtn);
        buttonPanel.add(printBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private String generateHtmlReceipt() {
        TarifCalculator.TarifResult calc = TarifCalculator.hitung(jadwal.getJamMulai(), jadwal.getDurasiJam());
        String tanggalStr = jadwal.getTanggalMain().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", localeId));

        return "<html><body style='font-family:sans-serif; color:#212529; padding:15px;'>" +
                "<div style='text-align:center; border-bottom:2px dashed #0f5132; padding-bottom:12px; margin-bottom:15px;'>" +
                "<h2 style='margin:0; color:#0f5132; letter-spacing:1px;'>FUTSAL & MINI SOCCER ARENA MALANG</h2>" +
                "<p style='margin:3px 0; font-size:12px; color:#555;'>Jl. Soekarno-Hatta / Tlogomas, Kota Malang, Jawa Timur</p>" +
                "<p style='margin:2px 0; font-size:11px; color:#777;'>Hotline / WA: 0812-3456-7890 | Jam Operasional: 08.00 - 23.00 WIB</p>" +
                "<h3 style='margin:10px 0 0 0; color:#198754;'>BUKTI RESERVASI SEWA LAPANGAN</h3>" +
                "</div>" +

                "<table style='width:100%; font-size:12px; border-collapse:collapse; margin-bottom:15px;'>" +
                "<tr><td style='padding:4px 0; width:140px;'><b>No. Booking:</b></td><td><b>" + jadwal.getIdBooking() + "</b></td></tr>" +
                "<tr><td style='padding:4px 0;'><b>Nama Tim / Pemesan:</b></td><td>" + jadwal.getNamaTim() + "</td></tr>" +
                "<tr><td style='padding:4px 0;'><b>No. Kontak (WhatsApp):</b></td><td>" + jadwal.getNoHp() + "</td></tr>" +
                "<tr><td style='padding:4px 0;'><b>Hari & Tanggal:</b></td><td>" + tanggalStr + "</td></tr>" +
                "<tr><td style='padding:4px 0;'><b>Slot Waktu:</b></td><td><b>" + jadwal.getFormattedRentangJam() + " WIB</b> (" + jadwal.getDurasiJam() + " Jam)</td></tr>" +
                "</table>" +

                "<div style='background-color:#f8f9fa; border:1px solid #dee2e6; border-radius:5px; padding:10px; margin-bottom:15px;'>" +
                "<b style='font-size:12px; color:#0f5132;'>Rincian Biaya Sewa:</b>" +
                "<table style='width:100%; font-size:12px; margin-top:6px; border-collapse:collapse;'>" +
                (calc.getJamSiang() > 0 ? "<tr><td>• Siang (08.00-18.00) [" + calc.getJamSiang() + " Jam × " + TarifCalculator.formatRupiah(TarifCalculator.TARIF_SIANG_PER_JAM) + "]</td><td style='text-align:right;'>" + TarifCalculator.formatRupiah(calc.getSubtotalSiang()) + "</td></tr>" : "") +
                (calc.getJamMalam() > 0 ? "<tr><td>• Malam + Lampu LED (" + calc.getJamMalam() + " Jam × " + TarifCalculator.formatRupiah(TarifCalculator.TARIF_MALAM_PER_JAM) + ")</td><td style='text-align:right;'>" + TarifCalculator.formatRupiah(calc.getSubtotalMalam()) + "</td></tr>" : "") +
                "<tr style='border-top:1px solid #ccc; font-weight:bold;'><td style='padding-top:6px;'>TOTAL TARIF:</td><td style='text-align:right; padding-top:6px; color:#0d6efd;'>" + TarifCalculator.formatRupiah(jadwal.getTotalTarif()) + "</td></tr>" +
                "<tr><td style='color:#198754; font-weight:bold;'>Uang Muka (DP Masuk):</td><td style='text-align:right; color:#198754; font-weight:bold;'>-" + TarifCalculator.formatRupiah(jadwal.getNominalDP()) + "</td></tr>" +
                "<tr style='border-top:1px solid #000; font-weight:bold;'><td style='padding-top:5px; color:#dc3545;'>SISA PELUNASAN:</td><td style='text-align:right; padding-top:5px; color:#dc3545;'>" + TarifCalculator.formatRupiah(jadwal.getSisaPembayaran()) + "</td></tr>" +
                "</table>" +
                "</div>" +

                "<div style='border:1px dashed #198754; background:#e8f5e9; padding:8px; border-radius:4px; font-size:11px; margin-bottom:15px;'>" +
                "<b>Status DP:</b> <span style='color:#0f5132; font-weight:bold;'>" + jadwal.getStatusDP() + "</span><br>" +
                "<i>*Uang muka (DP) minimal 50% telah tervalidasi oleh sistem Arena Malang.</i>" +
                "</div>" +

                "<div style='font-size:10px; color:#6c757d; border-top:1px solid #e9ecef; padding-top:8px;'>" +
                "<b>Tata Tertib & Ketentuan Sewa:</b><br>" +
                "1. Wajib hadir 15 menit sebelum jadwal kick-off dimulai.<br>" +
                "2. Wajib menggunakan sepatu futsal/olahraga yang sesuai standar.<br>" +
                "3. Pelunasan sisa biaya sewa dilakukan di kasir sebelum memasuki lapangan.<br>" +
                "4. Pembatalan H-1 uang muka tidak dapat dihanguskan (dapat dialihkan ke jadwal lain).<br>" +
                "</div>" +
                "</body></html>";
    }

    private String generateWhatsAppText() {
        TarifCalculator.TarifResult calc = TarifCalculator.hitung(jadwal.getJamMulai(), jadwal.getDurasiJam());
        String tanggalStr = jadwal.getTanggalMain().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", localeId));

        return "⚽ *BUKTI RESERVASI — FUTSAL & MINI SOCCER ARENA MALANG* ⚽\n" +
                "----------------------------------------------\n" +
                "📋 *No. Booking:* " + jadwal.getIdBooking() + "\n" +
                "👕 *Nama Tim:* " + jadwal.getNamaTim() + "\n" +
                "📱 *No. HP:* " + jadwal.getNoHp() + "\n" +
                "📅 *Tanggal:* " + tanggalStr + "\n" +
                "⏰ *Waktu Main:* " + jadwal.getFormattedRentangJam() + " WIB (" + jadwal.getDurasiJam() + " Jam)\n" +
                "----------------------------------------------\n" +
                "💰 *Rincian Biaya:*\n" +
                calc.getRincianPlainText() + "\n" +
                "💵 *Total Biaya:* " + TarifCalculator.formatRupiah(jadwal.getTotalTarif()) + "\n" +
                "✅ *DP Masuk:* " + TarifCalculator.formatRupiah(jadwal.getNominalDP()) + "\n" +
                "❗ *Sisa Pelunasan:* " + TarifCalculator.formatRupiah(jadwal.getSisaPembayaran()) + "\n" +
                "📌 *Status:* " + jadwal.getStatusDP() + "\n" +
                "----------------------------------------------\n" +
                "Silakan tunjukkan pesan/bukti ini ke kasir sebelum kick-off. Terima kasih! 🏟️";
    }

    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        JOptionPane.showMessageDialog(this, "Format teks reservasi berhasil disalin ke Clipboard! Siap dikirim ke WhatsApp pemesan.", "Salin Berhasil", JOptionPane.INFORMATION_MESSAGE);
    }
}
