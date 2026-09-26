package com.arenamalang.futsal.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Representasi entitas Jadwal Sewa Lapangan Olahraga
 * Futsal & Mini Soccer Arena Malang.
 * 
 * Atribut utama sesuai rancangan OOP:
 * - idBooking
 * - namaTim
 * - noHp
 * - tanggalMain
 * - jamMulai
 * - durasiJam
 * - statusDP
 * - totalTarif
 */
public class JadwalSewa {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String idBooking;
    private String namaTim;
    private String noHp;
    private LocalDate tanggalMain;
    private int jamMulai;       // 8 s.d. 23 (jam buka 08.00 s.d. 23.00)
    private int durasiJam;      // Durasi sewa dalam jam (min 1 jam)
    private String statusDP;     // Status pembayaran uang muka / lunas
    private double totalTarif;   // Total biaya sewa (perhitungan siang vs malam)
    private double nominalDP;    // Nominal uang muka yang telah dibayarkan

    public JadwalSewa() {
    }

    public JadwalSewa(String idBooking, String namaTim, String noHp, LocalDate tanggalMain,
                      int jamMulai, int durasiJam, String statusDP, double totalTarif, double nominalDP) {
        this.idBooking = idBooking;
        this.namaTim = namaTim;
        this.noHp = noHp;
        this.tanggalMain = tanggalMain;
        this.jamMulai = jamMulai;
        this.durasiJam = durasiJam;
        this.statusDP = statusDP;
        this.totalTarif = totalTarif;
        this.nominalDP = nominalDP;
    }

    // Constructor dengan 8 parameter utama sesuai spesifikasi spesifik tugas
    public JadwalSewa(String idBooking, String namaTim, String noHp, LocalDate tanggalMain,
                      int jamMulai, int durasiJam, String statusDP, double totalTarif) {
        this(idBooking, namaTim, noHp, tanggalMain, jamMulai, durasiJam, statusDP, totalTarif, totalTarif * 0.5);
    }

    public String getIdBooking() {
        return idBooking;
    }

    public void setIdBooking(String idBooking) {
        this.idBooking = idBooking;
    }

    public String getNamaTim() {
        return namaTim;
    }

    public void setNamaTim(String namaTim) {
        this.namaTim = namaTim;
    }

    public String getNoHp() {
        return noHp;
    }

    public void setNoHp(String noHp) {
        this.noHp = noHp;
    }

    public LocalDate getTanggalMain() {
        return tanggalMain;
    }

    public void setTanggalMain(LocalDate tanggalMain) {
        this.tanggalMain = tanggalMain;
    }

    public int getJamMulai() {
        return jamMulai;
    }

    public void setJamMulai(int jamMulai) {
        this.jamMulai = jamMulai;
    }

    public int getDurasiJam() {
        return durasiJam;
    }

    public void setDurasiJam(int durasiJam) {
        this.durasiJam = durasiJam;
    }

    public int getJamSelesai() {
        return jamMulai + durasiJam;
    }

    public String getStatusDP() {
        return statusDP;
    }

    public void setStatusDP(String statusDP) {
        this.statusDP = statusDP;
    }

    public double getTotalTarif() {
        return totalTarif;
    }

    public void setTotalTarif(double totalTarif) {
        this.totalTarif = totalTarif;
    }

    public double getNominalDP() {
        return nominalDP;
    }

    public void setNominalDP(double nominalDP) {
        this.nominalDP = nominalDP;
    }

    public double getSisaPembayaran() {
        double sisa = totalTarif - nominalDP;
        return sisa > 0 ? sisa : 0.0;
    }

    /**
     * Memeriksa apakah jadwal sewa ini bentrok (overlap) dengan waktu sewa yang dituju.
     * Menggunakan logika interval waktu [startA, endA) dan [startB, endB):
     * Dua rentang bentrok jika startA < endB dan startB < endA pada tanggal yang sama.
     */
    public boolean isOverlap(LocalDate targetTanggal, int targetJamMulai, int targetDurasi) {
        if (!Objects.equals(this.tanggalMain, targetTanggal)) {
            return false;
        }
        int targetJamSelesai = targetJamMulai + targetDurasi;
        int thisJamSelesai = this.getJamSelesai();

        return (this.jamMulai < targetJamSelesai && targetJamMulai < thisJamSelesai);
    }

    /**
     * Format rentang jam, contoh: "08:00 - 10:00"
     */
    public String getFormattedRentangJam() {
        return String.format("%02d:00 - %02d:00", jamMulai, getJamSelesai());
    }

    /**
     * Mengonversi objek JadwalSewa ke baris CSV yang aman.
     */
    public String toCsvLine() {
        return String.format("%s,%s,%s,%s,%d,%d,%s,%.0f,%.0f",
                escapeCsv(idBooking),
                escapeCsv(namaTim),
                escapeCsv(noHp),
                tanggalMain.format(DATE_FORMATTER),
                jamMulai,
                durasiJam,
                escapeCsv(statusDP),
                totalTarif,
                nominalDP
        );
    }

    /**
     * Parsing baris CSV menjadi objek JadwalSewa.
     */
    public static JadwalSewa fromCsvLine(String line) {
        String[] parts = parseCsvLine(line);
        if (parts.length < 8) {
            throw new IllegalArgumentException("Baris data CSV tidak valid: " + line);
        }

        String id = parts[0].trim();
        String nama = parts[1].trim();
        String hp = parts[2].trim();
        LocalDate tgl = LocalDate.parse(parts[3].trim(), DATE_FORMATTER);
        int jam = Integer.parseInt(parts[4].trim());
        int durasi = Integer.parseInt(parts[5].trim());
        String status = parts[6].trim();
        double tarif = Double.parseDouble(parts[7].trim());
        double dp = tarif * 0.5; // fallback default 50%
        if (parts.length >= 9 && !parts[8].trim().isEmpty()) {
            try {
                dp = Double.parseDouble(parts[8].trim());
            } catch (NumberFormatException ignored) {}
        }

        return new JadwalSewa(id, nama, hp, tgl, jam, durasi, status, tarif, dp);
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private static String[] parseCsvLine(String line) {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return "JadwalSewa{" +
                "idBooking='" + idBooking + '\'' +
                ", namaTim='" + namaTim + '\'' +
                ", noHp='" + noHp + '\'' +
                ", tanggalMain=" + tanggalMain +
                ", jamMulai=" + jamMulai +
                ", durasiJam=" + durasiJam +
                ", statusDP='" + statusDP + '\'' +
                ", totalTarif=" + totalTarif +
                ", nominalDP=" + nominalDP +
                '}';
    }
}
