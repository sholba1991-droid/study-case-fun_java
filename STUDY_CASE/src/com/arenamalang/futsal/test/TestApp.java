package com.arenamalang.futsal.test;

import com.arenamalang.futsal.model.JadwalSewa;
import com.arenamalang.futsal.service.BentrokJadwalException;
import com.arenamalang.futsal.service.JadwalManager;
import com.arenamalang.futsal.service.TarifCalculator;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Unit Test Otomatis untuk memverifikasi logika bisnis, validasi bentrok jadwal,
 * perhitungan tarif siang/malam, dan persistensi berkas CSV.
 */
public class TestApp {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  PENGUJIAN SISTEM RESERVASI ARENA FUTSAL MALANG  ");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Logika Perhitungan Tarif Siang
        try {
            TarifCalculator.TarifResult resSiang = TarifCalculator.hitung(9, 2); // 09.00 - 11.00 (2 jam siang)
            assert resSiang.getJamSiang() == 2 : "Jam siang harus 2";
            assert resSiang.getJamMalam() == 0 : "Jam malam harus 0";
            assert resSiang.getTotalTarif() == 240_000.0 : "Tarif harus 240.000";
            assert resSiang.getMinimalDP() == 120_000.0 : "Minimal DP (50%) harus 120.000";
            System.out.println("✓ Test 1: Perhitungan Tarif Siang (09.00-11.00) PASSED");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ Test 1 FAILED: " + t.getMessage());
            failed++;
        }

        // Test 2: Logika Perhitungan Tarif Malam + Lampu
        try {
            TarifCalculator.TarifResult resMalam = TarifCalculator.hitung(19, 2); // 19.00 - 21.00 (2 jam malam)
            assert resMalam.getJamSiang() == 0 : "Jam siang harus 0";
            assert resMalam.getJamMalam() == 2 : "Jam malam harus 2";
            assert resMalam.getTotalTarif() == 360_000.0 : "Tarif harus 360.000";
            assert resMalam.getMinimalDP() == 180_000.0 : "Minimal DP (50%) harus 180.000";
            System.out.println("✓ Test 2: Perhitungan Tarif Malam + Lampu (19.00-21.00) PASSED");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ Test 2 FAILED: " + t.getMessage());
            failed++;
        }

        // Test 3: Perhitungan Lintas Siang & Malam (Transisi Jam 18.00)
        try {
            TarifCalculator.TarifResult resMix = TarifCalculator.hitung(17, 2); // 17.00 - 19.00 (1 jam siang + 1 jam malam)
            assert resMix.getJamSiang() == 1 : "Jam siang harus 1";
            assert resMix.getJamMalam() == 1 : "Jam malam harus 1";
            assert resMix.getTotalTarif() == 300_000.0 : "Total tarif harus 120.000 + 180.000 = 300.000";
            assert resMix.getMinimalDP() == 150_000.0 : "Minimal DP harus 150.000";
            System.out.println("✓ Test 3: Perhitungan Tarif Lintas Siang-Malam PASSED");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ Test 3 FAILED: " + t.getMessage());
            failed++;
        }

        // Test 4: Validasi Deteksi Bentrok Jadwal (Overlap Interval)
        try {
            LocalDate tgl = LocalDate.of(2026, 10, 15);
            JadwalSewa existing = new JadwalSewa("BKG-01", "Tim A", "081", tgl, 10, 2, "DP", 240_000); // 10.00 - 12.00

            // 1. Bentrok: Pas overlap di tengah (11.00 - 13.00)
            assert existing.isOverlap(tgl, 11, 2) : "Harus terdeteksi overlap";

            // 2. Bentrok: Didalam rentang (10.00 - 11.00)
            assert existing.isOverlap(tgl, 10, 1) : "Harus terdeteksi overlap";

            // 3. Tidak bentrok: Sebelum (08.00 - 10.00)
            assert !existing.isOverlap(tgl, 8, 2) : "Tidak boleh bentrok jika selesai pas jam mulai";

            // 4. Tidak bentrok: Sesudah (12.00 - 14.00)
            assert !existing.isOverlap(tgl, 12, 2) : "Tidak boleh bentrok jika mulai pas jam selesai";

            // 5. Tidak bentrok: Tanggal berbeda
            assert !existing.isOverlap(tgl.plusDays(1), 10, 2) : "Tidak boleh bentrok jika tanggal beda";

            System.out.println("✓ Test 4: Deteksi Matematika Overlap Jam Sewa PASSED");
            passed++;
        } catch (Throwable t) {
            System.err.println("✗ Test 4 FAILED: " + t.getMessage());
            failed++;
        }

        // Test 5: Persistensi CSV & Validasi Bentrok Jadwal Real pada JadwalManager
        String testCsv = "test_jadwal_temp.csv";
        File f = new File(testCsv);
        if (f.exists()) f.delete();

        try {
            JadwalManager mgr = new JadwalManager(testCsv);
            LocalDate tglSewa = LocalDate.of(2026, 11, 20);

            // Tambah Booking 1: 14.00 - 16.00 (2 Jam Siang = 240.000, DP = 120.000)
            TarifCalculator.TarifResult t1 = TarifCalculator.hitung(14, 2);
            JadwalSewa j1 = new JadwalSewa("TEST-001", "FTI Angkatan 21", "0811111111", tglSewa, 14, 2,
                    "DP 50%", t1.getTotalTarif(), 120_000);
            mgr.tambahJadwal(j1);
            System.out.println("✓ Test 5a: Berhasil tambah booking perdana");

            // Tambah Booking 2 (Harus GAGAL karena DP < 50%)
            boolean dpCheckPassed = false;
            try {
                TarifCalculator.TarifResult t2 = TarifCalculator.hitung(16, 2);
                JadwalSewa j2 = new JadwalSewa("TEST-002", "FEB Angkatan 22", "0822222222", tglSewa, 16, 2,
                        "DP Kurang", t2.getTotalTarif(), 50_000); // 50.000 < 120.000
                mgr.tambahJadwal(j2);
            } catch (IllegalArgumentException e) {
                dpCheckPassed = true;
                System.out.println("✓ Test 5b: Penolakan DP < 50% berhasil terverifikasi (" + e.getMessage().substring(0, 30) + "...)");
            }
            assert dpCheckPassed : "Sistem harus menolak reservasi jika DP < 50%";

            // Tambah Booking 3 (Harus GAGAL BENTROK karena menabrak 14.00 - 16.00)
            boolean bentrokCheckPassed = false;
            try {
                TarifCalculator.TarifResult t3 = TarifCalculator.hitung(15, 2); // 15.00 - 17.00
                JadwalSewa j3 = new JadwalSewa("TEST-003", "Hukum 20", "0833333333", tglSewa, 15, 2,
                        "Lunas", t3.getTotalTarif(), t3.getTotalTarif());
                mgr.tambahJadwal(j3);
            } catch (BentrokJadwalException e) {
                bentrokCheckPassed = true;
                System.out.println("✓ Test 5c: Pencegahan Jadwal Bentrok berhasil terverifikasi (" + e.getConflictingJadwal().getNamaTim() + ")");
            }
            assert bentrokCheckPassed : "Sistem harus menolak reservasi yang bentrok jam sewa!";

            // Verifikasi Reload Data dari CSV
            JadwalManager mgrReload = new JadwalManager(testCsv);
            List<JadwalSewa> reloadedList = mgrReload.getAllJadwal();
            // File baru diinisialisasi 3 sampel awal + 1 booking baru = 4 data
            assert reloadedList.size() >= 4 : "Harus terdapat minimal 4 jadwal tersimpan di CSV";
            boolean found = reloadedList.stream().anyMatch(j -> "TEST-001".equals(j.getIdBooking()) && "FTI Angkatan 21".equals(j.getNamaTim()));
            assert found : "Booking TEST-001 harus ditemukan di CSV";

            System.out.println("✓ Test 5d: Data Persistence & reload dari berkas CSV PASSED");
            passed += 4;
        } catch (Throwable t) {
            System.err.println("✗ Test 5 FAILED: " + t.getMessage());
            t.printStackTrace();
            failed++;
        } finally {
            if (f.exists()) f.delete();
        }

        System.out.println("==================================================");
        System.out.printf("HASIL PENGUJIAN: %d PASSED, %d FAILED\n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
