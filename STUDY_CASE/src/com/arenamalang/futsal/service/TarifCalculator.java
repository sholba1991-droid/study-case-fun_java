package com.arenamalang.futsal.service;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Kalkulator Tarif Sewa Lapangan Futsal & Mini Soccer Arena Malang.
 * 
 * Aturan Bisnis Tarif:
 * - Jam Siang (08:00 - 18:00): Rp 120.000 / jam (Pencahayaan Alami)
 * - Jam Malam (18:00 - 23:00): Rp 180.000 / jam (Include Lampu Penerangan Stadion LED)
 * - Jam Operasional: 08:00 s.d. 23:00
 * - Uang Muka (DP): Minimal 50% dari Total Tarif
 */
public class TarifCalculator {

    public static final int JAM_BUKA = 8;
    public static final int JAM_TUTUP = 23;
    public static final int BATAS_JAM_SIANG = 18; // Mulai jam 18:00 dihitung malam (perlu lampu)

    public static final double TARIF_SIANG_PER_JAM = 120_000.0;
    public static final double TARIF_MALAM_PER_JAM = 180_000.0;
    public static final double PERSENTASE_MINIMAL_DP = 0.50; // 50%

    private static final Locale LOCALE_ID = Locale.of("id", "ID");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_ID);

    static {
        CURRENCY_FORMAT.setMaximumFractionDigits(0);
    }

    /**
     * Data Transfer Object untuk rincian perhitungan tarif.
     */
    public static class TarifResult {
        private final int jamMulai;
        private final int durasiJam;
        private final int jamSiang;
        private final int jamMalam;
        private final double subtotalSiang;
        private final double subtotalMalam;
        private final double totalTarif;
        private final double minimalDP;

        public TarifResult(int jamMulai, int durasiJam, int jamSiang, int jamMalam,
                           double subtotalSiang, double subtotalMalam, double totalTarif, double minimalDP) {
            this.jamMulai = jamMulai;
            this.durasiJam = durasiJam;
            this.jamSiang = jamSiang;
            this.jamMalam = jamMalam;
            this.subtotalSiang = subtotalSiang;
            this.subtotalMalam = subtotalMalam;
            this.totalTarif = totalTarif;
            this.minimalDP = minimalDP;
        }

        public int getJamMulai() { return jamMulai; }
        public int getDurasiJam() { return durasiJam; }
        public int getJamSelesai() { return jamMulai + durasiJam; }
        public int getJamSiang() { return jamSiang; }
        public int getJamMalam() { return jamMalam; }
        public double getSubtotalSiang() { return subtotalSiang; }
        public double getSubtotalMalam() { return subtotalMalam; }
        public double getTotalTarif() { return totalTarif; }
        public double getMinimalDP() { return minimalDP; }

        public String getRincianHtml() {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><div style='font-family:sans-serif;'>");
            sb.append("<b>Rincian Perhitungan Tarif:</b><br>");
            if (jamSiang > 0) {
                sb.append(String.format("• Siang (08.00-18.00): %d Jam × %s = <b>%s</b><br>",
                        jamSiang, formatRupiah(TARIF_SIANG_PER_JAM), formatRupiah(subtotalSiang)));
            }
            if (jamMalam > 0) {
                sb.append(String.format("• Malam + Lampu (18.00-23.00): %d Jam × %s = <b>%s</b><br>",
                        jamMalam, formatRupiah(TARIF_MALAM_PER_JAM), formatRupiah(subtotalMalam)));
            }
            sb.append(String.format("<hr><b>Total Tarif: <span style='color:#0d6efd;'>%s</span></b><br>", formatRupiah(totalTarif)));
            sb.append(String.format("<b>Syarat DP Minimal (50%%): <span style='color:#198754;'>%s</span></b>", formatRupiah(minimalDP)));
            sb.append("</div></html>");
            return sb.toString();
        }

        public String getRincianPlainText() {
            StringBuilder sb = new StringBuilder();
            if (jamSiang > 0) {
                sb.append(String.format("%d Jam Siang (@ %s) = %s", jamSiang, formatRupiah(TARIF_SIANG_PER_JAM), formatRupiah(subtotalSiang)));
            }
            if (jamMalam > 0) {
                if (sb.length() > 0) sb.append(" + ");
                sb.append(String.format("%d Jam Malam (@ %s) = %s", jamMalam, formatRupiah(TARIF_MALAM_PER_JAM), formatRupiah(subtotalMalam)));
            }
            return sb.toString();
        }
    }

    /**
     * Menghitung total tarif berdasarkan jam sewa dan durasi.
     * Mengkalkulasi jam siang vs jam malam secara akurat per jam.
     */
    public static TarifResult hitung(int jamMulai, int durasiJam) {
        if (jamMulai < JAM_BUKA || jamMulai >= JAM_TUTUP) {
            throw new IllegalArgumentException(String.format("Jam mulai harus di antara %02d:00 s.d. %02d:00", JAM_BUKA, JAM_TUTUP - 1));
        }
        if (durasiJam <= 0) {
            throw new IllegalArgumentException("Durasi sewa minimal 1 jam.");
        }
        if (jamMulai + durasiJam > JAM_TUTUP) {
            throw new IllegalArgumentException(String.format("Jam selesai (%02d:00) melebihi jam operasional arena (%02d:00).",
                    jamMulai + durasiJam, JAM_TUTUP));
        }

        int countSiang = 0;
        int countMalam = 0;

        for (int h = jamMulai; h < jamMulai + durasiJam; h++) {
            if (h < BATAS_JAM_SIANG) {
                countSiang++;
            } else {
                countMalam++;
            }
        }

        double subSiang = countSiang * TARIF_SIANG_PER_JAM;
        double subMalam = countMalam * TARIF_MALAM_PER_JAM;
        double total = subSiang + subMalam;
        double minDP = total * PERSENTASE_MINIMAL_DP;

        return new TarifResult(jamMulai, durasiJam, countSiang, countMalam, subSiang, subMalam, total, minDP);
    }

    /**
     * Memeriksa apakah nominal DP memenuhi syarat minimal 50%.
     */
    public static boolean isDPCukup(double nominalDP, double totalTarif) {
        return nominalDP >= (totalTarif * PERSENTASE_MINIMAL_DP);
    }

    /**
     * Menghasilkan status DP string deskriptif berdasarkan nominal yang diserahkan.
     */
    public static String tentukanStatusDP(double nominalDP, double totalTarif) {
        if (nominalDP >= totalTarif) {
            return "Lunas Penuh (100%)";
        } else if (nominalDP >= (totalTarif * PERSENTASE_MINIMAL_DP)) {
            double sisa = totalTarif - nominalDP;
            return String.format("DP Masuk (%s) - Sisa %s", formatRupiah(nominalDP), formatRupiah(sisa));
        } else {
            return "Belum Memenuhi Minimal DP (50%)";
        }
    }

    /**
     * Format angka ke format Rupiah standar Indonesia.
     */
    public static String formatRupiah(double amount) {
        // Menggunakan format standar IDR tanpa desimal
        long value = Math.round(amount);
        return String.format(Locale.of("id", "ID"), "Rp %,d", value).replace(',', '.');
    }
}
