package com.arenamalang.futsal.service;

import com.arenamalang.futsal.model.JadwalSewa;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Manager untuk mengelola penyimpanan (Data Persistence) ke berkas `jadwal_futsal.csv`,
 * validasi pencegahan jadwal bentrok antar tim, serta operasi CRUD reservasi.
 */
public class JadwalManager {

    public static final String DEFAULT_CSV_FILE = "jadwal_futsal.csv";
    private static final String CSV_HEADER = "idBooking,namaTim,noHp,tanggalMain,jamMulai,durasiJam,statusDP,totalTarif,nominalDP";

    private final Path csvPath;
    private final List<JadwalSewa> daftarJadwal = new ArrayList<>();

    public JadwalManager() {
        this(DEFAULT_CSV_FILE);
    }

    public JadwalManager(String csvFileName) {
        this.csvPath = Paths.get(csvFileName);
        muatData();
    }

    /**
     * Membaca seluruh data dari berkas CSV saat aplikasi dijalankan.
     * Jika berkas belum ada, maka dibuat berkas baru dan diisi sampel jadwal untuk demonstrasi.
     */
    public synchronized void muatData() {
        daftarJadwal.clear();
        if (!Files.exists(csvPath)) {
            inisialisasiDataAwal();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (firstLine) {
                    firstLine = false;
                    // Abaikan baris header CSV
                    if (line.toLowerCase().startsWith("idbooking")) {
                        continue;
                    }
                }
                try {
                    JadwalSewa jadwal = JadwalSewa.fromCsvLine(line);
                    daftarJadwal.add(jadwal);
                } catch (Exception e) {
                    System.err.println("Gagal membaca baris CSV: " + line + " | Alasan: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Gagal membaca berkas CSV " + csvPath + ": " + e.getMessage());
        }
    }

    /**
     * Menyimpan seluruh daftar jadwal ke berkas CSV.
     */
    public synchronized void simpanData() throws IOException {
        // Tulis ke file temporer lalu atomic move untuk keamanan data
        Path tempFile = Files.createTempFile("jadwal_futsal_", ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            writer.write(CSV_HEADER);
            writer.newLine();
            for (JadwalSewa j : daftarJadwal) {
                writer.write(j.toCsvLine());
                writer.newLine();
            }
        }

        Files.move(tempFile, csvPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Mengisi data sampel awal jika berkas baru pertama kali dibuat.
     */
    private void inisialisasiDataAwal() {
        LocalDate today = LocalDate.now();

        // Contoh: Tim Fakultas Teknik sewa jam 09.00 - 11.00 (2 jam siang)
        TarifCalculator.TarifResult tarif1 = TarifCalculator.hitung(9, 2);
        JadwalSewa j1 = new JadwalSewa(
                "BKG-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-001",
                "Teknik Informatika 22",
                "081234567891",
                today,
                9,
                2,
                TarifCalculator.tentukanStatusDP(tarif1.getTotalTarif(), tarif1.getTotalTarif()),
                tarif1.getTotalTarif(),
                tarif1.getTotalTarif()
        );

        // Contoh: BEM FEB sewa jam 15.00 - 17.00 (2 jam siang)
        TarifCalculator.TarifResult tarif2 = TarifCalculator.hitung(15, 2);
        JadwalSewa j2 = new JadwalSewa(
                "BKG-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-002",
                "BEM FEB Universitas Brawijaya",
                "085678901234",
                today,
                15,
                2,
                TarifCalculator.tentukanStatusDP(tarif2.getMinimalDP(), tarif2.getTotalTarif()),
                tarif2.getTotalTarif(),
                tarif2.getMinimalDP()
        );

        // Contoh: Arema FC Fans sewa malam 19.00 - 21.00 (2 jam malam dengan lampu)
        TarifCalculator.TarifResult tarif3 = TarifCalculator.hitung(19, 2);
        JadwalSewa j3 = new JadwalSewa(
                "BKG-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-003",
                "Arema Muda FC Malang",
                "082198765432",
                today,
                19,
                2,
                TarifCalculator.tentukanStatusDP(tarif3.getMinimalDP(), tarif3.getTotalTarif()),
                tarif3.getTotalTarif(),
                tarif3.getMinimalDP()
        );

        daftarJadwal.add(j1);
        daftarJadwal.add(j2);
        daftarJadwal.add(j3);

        try {
            simpanData();
        } catch (IOException e) {
            System.err.println("Gagal menulis sampel awal ke " + csvPath + ": " + e.getMessage());
        }
    }

    /**
     * Memeriksa apakah terjadi bentrok jadwal dengan reservasi yang ada.
     * Mengembalikan jadwal yang bentrok (jika ada), atau null jika slot kosong/aman.
     *
     * @param tanggal Tanggal sewa yang diinginkan
     * @param jamMulai Jam mulai (08 - 22)
     * @param durasi Durasi jam sewa
     * @param excludeIdBooking ID booking yang dikecualikan (misal saat edit data), atau null
     * @return JadwalSewa yang bentrok, atau null jika tidak ada bentrok
     */
    public synchronized JadwalSewa cekBentrok(LocalDate tanggal, int jamMulai, int durasi, String excludeIdBooking) {
        for (JadwalSewa existing : daftarJadwal) {
            if (excludeIdBooking != null && existing.getIdBooking().equalsIgnoreCase(excludeIdBooking)) {
                continue;
            }
            if (existing.isOverlap(tanggal, jamMulai, durasi)) {
                return existing;
            }
        }
        return null;
    }

    /**
     * Menambahkan reservasi jadwal baru setelah melalui serangkaian validasi ketat:
     * 1. Validasi jam operasional (08:00 - 23:00)
     * 2. Validasi bentrok jadwal antar tim (No two teams can rent at overlapping hours)
     * 3. Validasi minimal DP 50%
     */
    public synchronized void tambahJadwal(JadwalSewa jadwal) throws BentrokJadwalException, IOException {
        // 1. Validasi jam operasional
        int jamSelesai = jadwal.getJamMulai() + jadwal.getDurasiJam();
        if (jadwal.getJamMulai() < TarifCalculator.JAM_BUKA || jamSelesai > TarifCalculator.JAM_TUTUP) {
            throw new IllegalArgumentException(String.format("Waktu sewa (%s) berada di luar jam operasional arena (%02d:00 - %02d:00).",
                    jadwal.getFormattedRentangJam(), TarifCalculator.JAM_BUKA, TarifCalculator.JAM_TUTUP));
        }

        // 2. Validasi bentrok jadwal
        JadwalSewa bentrok = cekBentrok(jadwal.getTanggalMain(), jadwal.getJamMulai(), jadwal.getDurasiJam(), jadwal.getIdBooking());
        if (bentrok != null) {
            throw new BentrokJadwalException(bentrok);
        }

        // 3. Validasi DP minimal 50%
        double minimalDP = jadwal.getTotalTarif() * TarifCalculator.PERSENTASE_MINIMAL_DP;
        if (jadwal.getNominalDP() < minimalDP) {
            throw new IllegalArgumentException(String.format(
                    "Uang muka (DP) minimal 50%% dari total tarif!\nTotal Tarif: %s\nMinimal DP: %s\nNominal yang diinput: %s",
                    TarifCalculator.formatRupiah(jadwal.getTotalTarif()),
                    TarifCalculator.formatRupiah(minimalDP),
                    TarifCalculator.formatRupiah(jadwal.getNominalDP())
            ));
        }

        // 4. Update status DP deskriptif
        jadwal.setStatusDP(TarifCalculator.tentukanStatusDP(jadwal.getNominalDP(), jadwal.getTotalTarif()));

        // 5. Tambahkan dan simpan ke file CSV
        daftarJadwal.add(jadwal);
        simpanData();
    }

    /**
     * Menghapus reservasi berdasarkan ID Booking.
     */
    public synchronized boolean hapusJadwal(String idBooking) throws IOException {
        boolean removed = daftarJadwal.removeIf(j -> j.getIdBooking().equalsIgnoreCase(idBooking));
        if (removed) {
            simpanData();
        }
        return removed;
    }

    /**
     * Mengambil seluruh daftar jadwal yang terurut berdasarkan tanggal dan jam sewa.
     */
    public synchronized List<JadwalSewa> getAllJadwal() {
        return daftarJadwal.stream()
                .sorted(Comparator.comparing(JadwalSewa::getTanggalMain)
                        .thenComparing(JadwalSewa::getJamMulai))
                .collect(Collectors.toList());
    }

    /**
     * Mengambil daftar jadwal pada tanggal tertentu, diurutkan jam mulai.
     */
    public synchronized List<JadwalSewa> getJadwalByTanggal(LocalDate tanggal) {
        return daftarJadwal.stream()
                .filter(j -> j.getTanggalMain().equals(tanggal))
                .sorted(Comparator.comparing(JadwalSewa::getJamMulai))
                .collect(Collectors.toList());
    }

    /**
     * Memeriksa apakah satu slot jam tertentu (misal jam 10:00 s.d 11:00) sedang disewa pada tanggal tertentu.
     */
    public synchronized boolean isSlotTersedia(LocalDate tanggal, int jam) {
        return getJadwalPadaJam(tanggal, jam) == null;
    }

    /**
     * Mengambil objek JadwalSewa yang menempati slot jam tertentu, atau null jika kosong.
     */
    public synchronized JadwalSewa getJadwalPadaJam(LocalDate tanggal, int jam) {
        for (JadwalSewa j : daftarJadwal) {
            if (j.getTanggalMain().equals(tanggal) && jam >= j.getJamMulai() && jam < j.getJamSelesai()) {
                return j;
            }
        }
        return null;
    }

    /**
     * Generate ID Booking otomatis berformat BKG-YYYYMMDD-XXX
     */
    public synchronized String generateNextId(LocalDate tanggal) {
        String tglStr = tanggal.format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "BKG-" + tglStr + "-";
        int maxSeq = 0;
        for (JadwalSewa j : daftarJadwal) {
            if (j.getIdBooking() != null && j.getIdBooking().startsWith(prefix)) {
                try {
                    String seqStr = j.getIdBooking().substring(prefix.length());
                    int seq = Integer.parseInt(seqStr);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%03d", prefix, maxSeq + 1);
    }

    public Path getCsvPath() {
        return csvPath;
    }

    public static void main(String[] args) {
        JadwalManager manager = new JadwalManager();
        System.out.println("JadwalManager siap. Lokasi file: " + manager.getCsvPath().toAbsolutePath());
        System.out.println("Jumlah reservasi termuat: " + manager.getAllJadwal().size());
        for (JadwalSewa j : manager.getAllJadwal()) {
            System.out.println(" - [" + j.getIdBooking() + "] " + j.getNamaTim() + " | " + j.getTanggalMain() + " " + j.getFormattedRentangJam() + " | " + j.getStatusDP());
        }
    }
}
