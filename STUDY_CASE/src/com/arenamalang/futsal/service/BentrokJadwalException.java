package com.arenamalang.futsal.service;

import com.arenamalang.futsal.model.JadwalSewa;

/**
 * Exception khusus ketika terjadi bentrok jadwal sewa lapangan.
 * Menyimpan informasi jadwal yang sudah ada dan berbenturan.
 */
public class BentrokJadwalException extends Exception {
    private final JadwalSewa conflictingJadwal;

    public BentrokJadwalException(JadwalSewa conflictingJadwal) {
        super(String.format("JADWAL BENTROK! Slot waktu sudah disewa oleh Tim '%s' (%s) pada jam %s. " +
                        "Silakan pilih jam atau tanggal lain.",
                conflictingJadwal.getNamaTim(),
                conflictingJadwal.getNoHp(),
                conflictingJadwal.getFormattedRentangJam()));
        this.conflictingJadwal = conflictingJadwal;
    }

    public JadwalSewa getConflictingJadwal() {
        return conflictingJadwal;
    }
}
