package com.arenamalang.futsal.gui;

import com.arenamalang.futsal.model.JadwalSewa;
import com.arenamalang.futsal.service.BentrokJadwalException;
import com.arenamalang.futsal.service.JadwalManager;
import com.arenamalang.futsal.service.TarifCalculator;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Antarmuka Utama GUI: Futsal & Mini Soccer Arena Malang.
 * Mengintegrasikan form pemesanan dengan kalender interaktif,
 * pemilih slot jam 08.00-23.00, perhitungan tarif otomatis siang vs malam,
 * validasi uang muka (DP minimal 50%), serta data persistence jadwal_futsal.csv.
 */
public class MainFrame extends JFrame {

    private final JadwalManager jadwalManager;
    private LocalDate selectedDate;

    // Komponen Form Input
    private JTextField txtIdBooking;
    private JTextField txtNamaTim;
    private JTextField txtNoHp;
    private JTextField txtTanggalMain;
    private JComboBox<String> cmbJamMulai;
    private JComboBox<Integer> cmbDurasi;
    private JLabel lblJamSelesai;
    private JLabel lblRincianTarif;
    private JLabel lblTotalTarif;
    private JLabel lblMinDP;
    private JTextField txtNominalDP;
    private JLabel lblStatusValidasiDP;
    private JLabel lblSlotStatusLive;
    private JButton btnSimpan;

    // Visual Timeline Slot Jam
    private ScheduleTimelinePanel timelinePanel;

    // Komponen Tabel Jadwal
    private JTable tableJadwal;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField txtCari;
    private JComboBox<String> cmbFilterTanggal;
    private JLabel lblStatistik;

    private final Locale localeId = Locale.of("id", "ID");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public MainFrame() {
        this.jadwalManager = new JadwalManager();
        this.selectedDate = LocalDate.now();

        initWindow();
        initUI();
        refreshAllData();
    }

    private void initWindow() {
        setTitle("Futsal & Mini Soccer Arena Malang — Sistem Reservasi Lapangan Olahraga");
        setSize(1240, 800);
        setMinimumSize(new Dimension(1020, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            // Coba Nimbus atau System Look and Feel
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));

        // 1. Header Banner
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Content Split (Kiri: Form Input & Visual Timeline; Kanan: Tabel Data & Aksi)
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
        mainContentPanel.setBorder(new EmptyBorder(10, 14, 10, 14));
        mainContentPanel.setBackground(new Color(245, 247, 250));

        // Bagian Atas Content: Visual Timeline Slot Hari Terpilih
        timelinePanel = new ScheduleTimelinePanel(jadwalManager);
        timelinePanel.setOnSlotClickedListener(this::onTimelineSlotClicked);
        mainContentPanel.add(timelinePanel, BorderLayout.NORTH);

        // Split Panel Bawah (Form di Kiri, Tabel di Kanan)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.38);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        // Panel Kiri: Form Reservasi
        JPanel leftPanel = createFormPanel();
        splitPane.setLeftComponent(leftPanel);

        // Panel Kanan: Tabel Daftar Reservasi
        JPanel rightPanel = createTablePanel();
        splitPane.setRightComponent(rightPanel);

        mainContentPanel.add(splitPane, BorderLayout.CENTER);
        add(mainContentPanel, BorderLayout.CENTER);

        // 3. Status Bar Bawah
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 5));
        header.setBackground(new Color(15, 65, 38)); // Deep Emerald Green
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Sisi Kiri: Logo teks & Nama Arena
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("⚽ FUTSAL & MINI SOCCER ARENA MALANG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Sistem Manajemen Reservasi Lapangan Digital • Mencegah Bentrok Jadwal Sewa Antar Tim");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(200, 235, 215));

        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);
        header.add(titlePanel, BorderLayout.WEST);

        // Sisi Kanan: Statistik Ringkas
        lblStatistik = new JLabel("Memuat statistik...", SwingConstants.RIGHT);
        lblStatistik.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatistik.setForeground(new Color(230, 250, 238));
        header.add(lblStatistik, BorderLayout.EAST);

        return header;
    }

    private JPanel createFormPanel() {
        JPanel formContainer = new JPanel(new BorderLayout(5, 5));
        formContainer.setOpaque(false);

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Judul Bagian Form
        JLabel formHeader = new JLabel("📝 Formulir Reservasi Baru");
        formHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formHeader.setForeground(new Color(15, 65, 38));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formCard.add(formHeader, gbc);
        gbc.gridwidth = 1;

        // Separator
        JSeparator sep = new JSeparator();
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formCard.add(sep, gbc);
        gbc.gridwidth = 1;

        // 1. ID Booking (Auto)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formCard.add(new JLabel("ID Booking:"), gbc);

        JPanel pnlId = new JPanel(new BorderLayout(4, 0));
        pnlId.setOpaque(false);
        txtIdBooking = new JTextField();
        txtIdBooking.setEditable(false);
        txtIdBooking.setFont(new Font("Consolas", Font.BOLD, 12));
        txtIdBooking.setBackground(new Color(245, 245, 245));
        JButton btnRefreshId = new JButton("🔄");
        btnRefreshId.setToolTipText("Generate Ulang ID");
        btnRefreshId.addActionListener(e -> generateNewIdBooking());
        pnlId.add(txtIdBooking, BorderLayout.CENTER);
        pnlId.add(btnRefreshId, BorderLayout.EAST);

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formCard.add(pnlId, gbc);

        // 2. Nama Tim
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formCard.add(new JLabel("Nama Tim / Pemesan:*"), gbc);

        txtNamaTim = new JTextField();
        txtNamaTim.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formCard.add(txtNamaTim, gbc);

        // 3. No. HP / WhatsApp
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formCard.add(new JLabel("No. HP (WhatsApp):*"), gbc);

        txtNoHp = new JTextField();
        txtNoHp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formCard.add(txtNoHp, gbc);

        // 4. Tanggal Main (dengan Kalender Popup)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formCard.add(new JLabel("Tanggal Main:*"), gbc);

        JPanel pnlTanggal = new JPanel(new BorderLayout(4, 0));
        pnlTanggal.setOpaque(false);
        txtTanggalMain = new JTextField(selectedDate.format(dateFormatter));
        txtTanggalMain.setEditable(false);
        txtTanggalMain.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtTanggalMain.setBackground(Color.WHITE);

        JButton btnKalender = new JButton("📅 Kalender");
        btnKalender.setBackground(new Color(235, 245, 238));
        btnKalender.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnKalender.addActionListener(e -> bukaDialogKalender());

        pnlTanggal.add(txtTanggalMain, BorderLayout.CENTER);
        pnlTanggal.add(btnKalender, BorderLayout.EAST);

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formCard.add(pnlTanggal, gbc);

        // 5. Slot Jam Mulai (08.00 s.d. 22.00)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formCard.add(new JLabel("Jam Mulai (08-22):*"), gbc);

        DefaultComboBoxModel<String> jamMulaiModel = new DefaultComboBoxModel<>();
        for (int h = TarifCalculator.JAM_BUKA; h < TarifCalculator.JAM_TUTUP; h++) {
            jamMulaiModel.addElement(String.format("%02d:00", h));
        }
        cmbJamMulai = new JComboBox<>(jamMulaiModel);
        cmbJamMulai.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cmbJamMulai.addActionListener(e -> updateTarifAndConflictCheck());

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formCard.add(cmbJamMulai, gbc);

        // 6. Durasi Jam Sewa
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formCard.add(new JLabel("Durasi Sewa:*"), gbc);

        Integer[] durasiOptions = {1, 2, 3, 4, 5};
        cmbDurasi = new JComboBox<>(durasiOptions);
        cmbDurasi.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cmbDurasi.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) setText(value + " Jam");
                return this;
            }
        });
        cmbDurasi.addActionListener(e -> updateTarifAndConflictCheck());

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formCard.add(cmbDurasi, gbc);

        // Jam Selesai Display
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formCard.add(new JLabel("Jam Selesai:"), gbc);

        lblJamSelesai = new JLabel("10:00 WIB");
        lblJamSelesai.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblJamSelesai.setForeground(new Color(50, 50, 50));
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formCard.add(lblJamSelesai, gbc);

        // Status Ketersediaan Real-Time (Live Conflict Detector)
        lblSlotStatusLive = new JLabel("✓ Slot Tersedia");
        lblSlotStatusLive.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSlotStatusLive.setForeground(new Color(25, 135, 84));
        lblSlotStatusLive.setBorder(new EmptyBorder(2, 0, 4, 0));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formCard.add(lblSlotStatusLive, gbc);
        gbc.gridwidth = 1;

        // 7. Kotak Rincian Perhitungan Tarif Otomatis Siang vs Malam
        JPanel tarifBox = new JPanel(new GridLayout(4, 1, 2, 3));
        tarifBox.setBackground(new Color(248, 250, 252));
        tarifBox.setBorder(new CompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        lblRincianTarif = new JLabel("Rincian Jam: -");
        lblRincianTarif.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        lblTotalTarif = new JLabel("Total Biaya: Rp 0");
        lblTotalTarif.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalTarif.setForeground(new Color(13, 110, 253));

        lblMinDP = new JLabel("Minimal DP (50%): Rp 0");
        lblMinDP.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMinDP.setForeground(new Color(25, 135, 84));

        JLabel infoTarifKecil = new JLabel("<html><i>Siang (08-18): Rp120rb/jam | Malam+Lampu (18-23): Rp180rb/jam</i></html>");
        infoTarifKecil.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        infoTarifKecil.setForeground(Color.GRAY);

        tarifBox.add(infoTarifKecil);
        tarifBox.add(lblRincianTarif);
        tarifBox.add(lblTotalTarif);
        tarifBox.add(lblMinDP);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formCard.add(tarifBox, gbc);
        gbc.gridwidth = 1;

        // 8. Input Uang Muka (DP Minimal 50%)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formCard.add(new JLabel("Uang Muka / DP (Rp):*"), gbc);

        JPanel pnlDp = new JPanel(new BorderLayout(4, 0));
        pnlDp.setOpaque(false);
        txtNominalDP = new JTextField();
        txtNominalDP.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtNominalDP.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validasiInputDPRealtime(); }
            @Override public void removeUpdate(DocumentEvent e) { validasiInputDPRealtime(); }
            @Override public void changedUpdate(DocumentEvent e) { validasiInputDPRealtime(); }
        });

        // Tombol Shortcut DP
        JPanel pnlPresetDP = new JPanel(new GridLayout(1, 2, 3, 0));
        pnlPresetDP.setOpaque(false);
        JButton btnDp50 = new JButton("DP 50%");
        btnDp50.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnDp50.setToolTipText("Set DP ke batas minimal 50%");
        btnDp50.addActionListener(e -> setShortcutDP(0.50));

        JButton btnDp100 = new JButton("Lunas");
        btnDp100.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnDp100.setToolTipText("Set pembayaran lunas penuh 100%");
        btnDp100.addActionListener(e -> setShortcutDP(1.00));

        pnlPresetDP.add(btnDp50);
        pnlPresetDP.add(btnDp100);

        pnlDp.add(txtNominalDP, BorderLayout.CENTER);
        pnlDp.add(pnlPresetDP, BorderLayout.EAST);

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formCard.add(pnlDp, gbc);

        // Status Validasi DP Realtime
        lblStatusValidasiDP = new JLabel("Input DP minimal 50%");
        lblStatusValidasiDP.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formCard.add(lblStatusValidasiDP, gbc);
        gbc.gridwidth = 1;

        // 9. Tombol Aksi Simpan & Reset
        JPanel pnlAksiForm = new JPanel(new GridLayout(1, 2, 8, 0));
        pnlAksiForm.setOpaque(false);

        JButton btnReset = new JButton("🔄 Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReset.addActionListener(e -> resetFormInput());

        btnSimpan = new JButton("💾 Simpan Reservasi");
        btnSimpan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSimpan.setBackground(new Color(25, 135, 84));
        btnSimpan.setForeground(Color.WHITE);
        btnSimpan.setFocusPainted(false);
        btnSimpan.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSimpan.addActionListener(e -> simpanReservasi());

        pnlAksiForm.add(btnReset);
        pnlAksiForm.add(btnSimpan);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 4, 4, 4);
        formCard.add(pnlAksiForm, gbc);

        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(12);
        formContainer.add(formScroll, BorderLayout.CENTER);

        return formContainer;
    }

    private JPanel createTablePanel() {
        JPanel tableContainer = new JPanel(new BorderLayout(8, 8));
        tableContainer.setOpaque(false);

        // Header Toolbar Tabel (Pencarian & Filter Tanggal)
        JPanel tableToolbar = new JPanel(new BorderLayout(8, 8));
        tableToolbar.setOpaque(false);

        // Filter Sisi Kiri
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filterPanel.setOpaque(false);

        filterPanel.add(new JLabel("Tampilkan:"));
        cmbFilterTanggal = new JComboBox<>(new String[]{"Semua Jadwal", "Tanggal Terpilih (" + selectedDate + ")", "Hari Ini Saja"});
        cmbFilterTanggal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbFilterTanggal.addActionListener(e -> applyFilterTabel());
        filterPanel.add(cmbFilterTanggal);

        tableToolbar.add(filterPanel, BorderLayout.WEST);

        // Pencarian Sisi Kanan
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("🔍 Cari:"));
        txtCari = new JTextField(14);
        txtCari.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtCari.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { filterSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { filterSearch(); }
        });
        searchPanel.add(txtCari);

        tableToolbar.add(searchPanel, BorderLayout.EAST);
        tableContainer.add(tableToolbar, BorderLayout.NORTH);

        // Tabel Data Reservasi
        String[] columnNames = {"ID Booking", "Nama Tim", "No. HP", "Tanggal", "Jam Sewa", "Durasi", "Total Biaya", "Status DP"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only pada tabel
            }
        };

        tableJadwal = new JTable(tableModel);
        tableJadwal.setRowHeight(28);
        tableJadwal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableJadwal.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableJadwal.setShowGrid(true);
        tableJadwal.setGridColor(new Color(230, 235, 240));

        JTableHeader th = tableJadwal.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(new Color(241, 245, 249));
        th.setForeground(new Color(30, 41, 59));
        th.setPreferredSize(new Dimension(th.getWidth(), 32));

        // Format kolom
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        tableJadwal.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
        tableJadwal.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Tanggal
        tableJadwal.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Jam Sewa
        tableJadwal.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Durasi

        // Renderer khusus Kolom Status DP
        tableJadwal.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (value != null) {
                    String str = value.toString();
                    if (str.startsWith("Lunas")) {
                        lbl.setForeground(new Color(22, 101, 52));
                    } else if (str.startsWith("DP")) {
                        lbl.setForeground(new Color(133, 77, 14));
                    } else {
                        lbl.setForeground(new Color(153, 27, 27));
                    }
                }
                return lbl;
            }
        });

        // Sorter
        rowSorter = new TableRowSorter<>(tableModel);
        tableJadwal.setRowSorter(rowSorter);

        JScrollPane tableScroll = new JScrollPane(tableJadwal);
        tableScroll.setBorder(new LineBorder(new Color(220, 224, 230), 1));
        tableContainer.add(tableScroll, BorderLayout.CENTER);

        // Tombol Aksi Bawah Tabel
        JPanel bottomActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bottomActionPanel.setOpaque(false);

        JButton btnLihatStruk = new JButton("🧾 Cetak Bukti / Struk");
        btnLihatStruk.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLihatStruk.setBackground(new Color(13, 110, 253));
        btnLihatStruk.setForeground(Color.WHITE);
        btnLihatStruk.setFocusPainted(false);
        btnLihatStruk.addActionListener(e -> bukaStrukPilihan());

        JButton btnHapus = new JButton("❌ Batalkan Reservasi");
        btnHapus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnHapus.setForeground(new Color(220, 53, 69));
        btnHapus.addActionListener(e -> hapusReservasiTerpilih());

        JButton btnBukaCsv = new JButton("📂 Buka CSV");
        btnBukaCsv.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnBukaCsv.setToolTipText("Buka berkas data jadwal_futsal.csv");
        btnBukaCsv.addActionListener(e -> bukaFileCsv());

        JButton btnRefreshData = new JButton("🔄 Refresh");
        btnRefreshData.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshData.addActionListener(e -> refreshAllData());

        bottomActionPanel.add(btnBukaCsv);
        bottomActionPanel.add(btnRefreshData);
        bottomActionPanel.add(btnHapus);
        bottomActionPanel.add(btnLihatStruk);

        tableContainer.add(bottomActionPanel, BorderLayout.SOUTH);

        return tableContainer;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout(10, 0));
        status.setBackground(new Color(238, 242, 246));
        status.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 215, 220), 1),
                new EmptyBorder(6, 15, 6, 15)
        ));

        JLabel lblFile = new JLabel("📁 Berkas Persistensi: " + jadwalManager.getCsvPath().toAbsolutePath());
        lblFile.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblFile.setForeground(new Color(80, 80, 80));

        JLabel lblEngine = new JLabel("🛡️ Algoritma Validasi Jadwal Bentrok & Kalkulasi Siang/Malam Aktif");
        lblEngine.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEngine.setForeground(new Color(25, 135, 84));

        status.add(lblFile, BorderLayout.WEST);
        status.add(lblEngine, BorderLayout.EAST);

        return status;
    }

    // ==========================================
    // LOGIKA BISNIS & EVENT HANDLING
    // ==========================================

    private void bukaDialogKalender() {
        LocalDate picked = DatePickerDialog.showDialog(this, selectedDate);
        if (picked != null) {
            this.selectedDate = picked;
            txtTanggalMain.setText(selectedDate.format(dateFormatter));
            generateNewIdBooking();
            timelinePanel.refreshTimeline(selectedDate);
            updateTarifAndConflictCheck();
            updateFilterComboBoxLabel();
            applyFilterTabel();
        }
    }

    private void updateFilterComboBoxLabel() {
        if (cmbFilterTanggal != null && cmbFilterTanggal.getItemCount() >= 2) {
            int selectedIndex = cmbFilterTanggal.getSelectedIndex();
            cmbFilterTanggal.removeItemAt(1);
            cmbFilterTanggal.insertItemAt("Tanggal Terpilih (" + selectedDate + ")", 1);
            cmbFilterTanggal.setSelectedIndex(selectedIndex);
        }
    }

    private void onTimelineSlotClicked(int jam) {
        // Otomatis arahkan jam mulai formulir ke jam yang diklik pada timeline
        cmbJamMulai.setSelectedItem(String.format("%02d:00", jam));
        updateTarifAndConflictCheck();
    }

    private int getSelectedJamMulai() {
        String item = (String) cmbJamMulai.getSelectedItem();
        if (item == null) return 8;
        return Integer.parseInt(item.split(":")[0]);
    }

    private int getSelectedDurasi() {
        Integer durasi = (Integer) cmbDurasi.getSelectedItem();
        return (durasi != null) ? durasi : 1;
    }

    private void updateTarifAndConflictCheck() {
        int jamMulai = getSelectedJamMulai();
        int durasi = getSelectedDurasi();
        int jamSelesai = jamMulai + durasi;

        lblJamSelesai.setText(String.format("%02d:00 WIB", jamSelesai));

        // Cek apakah melebihi jam tutup
        if (jamSelesai > TarifCalculator.JAM_TUTUP) {
            lblSlotStatusLive.setText(String.format("⛔ Melebihi jam tutup arena (%02d:00 WIB)!", TarifCalculator.JAM_TUTUP));
            lblSlotStatusLive.setForeground(new Color(220, 53, 69));
            btnSimpan.setEnabled(false);
            return;
        }

        // 1. Cek bentrok jadwal
        JadwalSewa bentrok = jadwalManager.cekBentrok(selectedDate, jamMulai, durasi, txtIdBooking.getText());
        if (bentrok != null) {
            lblSlotStatusLive.setText(String.format("⛔ BENTROK dengan Tim '%s' (%s)!", bentrok.getNamaTim(), bentrok.getFormattedRentangJam()));
            lblSlotStatusLive.setForeground(new Color(220, 53, 69));
            btnSimpan.setEnabled(false);
        } else {
            lblSlotStatusLive.setText("✓ Slot Jam Tersedia & Belum Ada Yang Sewa");
            lblSlotStatusLive.setForeground(new Color(25, 135, 84));
            btnSimpan.setEnabled(true);
        }

        // 2. Hitung tarif siang vs malam
        TarifCalculator.TarifResult res = TarifCalculator.hitung(jamMulai, durasi);
        lblRincianTarif.setText(res.getRincianPlainText());
        lblTotalTarif.setText("Total Biaya: " + TarifCalculator.formatRupiah(res.getTotalTarif()));
        lblMinDP.setText("Minimal DP (50%): " + TarifCalculator.formatRupiah(res.getMinimalDP()));

        validasiInputDPRealtime();
    }

    private void setShortcutDP(double percentage) {
        int jamMulai = getSelectedJamMulai();
        int durasi = getSelectedDurasi();
        if (jamMulai + durasi > TarifCalculator.JAM_TUTUP) return;

        TarifCalculator.TarifResult res = TarifCalculator.hitung(jamMulai, durasi);
        double amount = res.getTotalTarif() * percentage;
        txtNominalDP.setText(String.format(localeId, "%.0f", amount));
    }

    private void validasiInputDPRealtime() {
        int jamMulai = getSelectedJamMulai();
        int durasi = getSelectedDurasi();
        if (jamMulai + durasi > TarifCalculator.JAM_TUTUP) return;

        TarifCalculator.TarifResult res = TarifCalculator.hitung(jamMulai, durasi);
        String dpStr = txtNominalDP.getText().trim().replaceAll("[^0-9]", "");

        if (dpStr.isEmpty()) {
            lblStatusValidasiDP.setText("⚠️ Masukkan nominal uang muka (DP minimal 50%)");
            lblStatusValidasiDP.setForeground(new Color(180, 100, 0));
            return;
        }

        try {
            double dpNominal = Double.parseDouble(dpStr);
            if (dpNominal < res.getMinimalDP()) {
                double kekurangan = res.getMinimalDP() - dpNominal;
                lblStatusValidasiDP.setText(String.format("✗ DP Kurang! Min DP: %s (Kurang %s)",
                        TarifCalculator.formatRupiah(res.getMinimalDP()), TarifCalculator.formatRupiah(kekurangan)));
                lblStatusValidasiDP.setForeground(new Color(220, 53, 69));
            } else if (dpNominal >= res.getTotalTarif()) {
                lblStatusValidasiDP.setText("✓ Pembayaran Lunas Penuh (100%)");
                lblStatusValidasiDP.setForeground(new Color(25, 135, 84));
            } else {
                double sisa = res.getTotalTarif() - dpNominal;
                lblStatusValidasiDP.setText(String.format("✓ DP Memenuhi Syarat (Sisa pelunasan: %s)", TarifCalculator.formatRupiah(sisa)));
                lblStatusValidasiDP.setForeground(new Color(25, 135, 84));
            }
        } catch (NumberFormatException e) {
            lblStatusValidasiDP.setText("Nominal DP tidak valid!");
            lblStatusValidasiDP.setForeground(Color.RED);
        }
    }

    private void generateNewIdBooking() {
        String nextId = jadwalManager.generateNextId(selectedDate);
        txtIdBooking.setText(nextId);
    }

    private void simpanReservasi() {
        String namaTim = txtNamaTim.getText().trim();
        String noHp = txtNoHp.getText().trim();
        int jamMulai = getSelectedJamMulai();
        int durasi = getSelectedDurasi();
        int jamSelesai = jamMulai + durasi;

        // Validasi kelengkapan form
        if (namaTim.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama Tim / Pemesan wajib diisi!", "Validasi Form", JOptionPane.WARNING_MESSAGE);
            txtNamaTim.requestFocus();
            return;
        }
        if (noHp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nomor HP / WhatsApp wajib diisi!", "Validasi Form", JOptionPane.WARNING_MESSAGE);
            txtNoHp.requestFocus();
            return;
        }
        if (jamSelesai > TarifCalculator.JAM_TUTUP) {
            JOptionPane.showMessageDialog(this, String.format("Jam selesai (%02d:00) melewati jam operasional arena (23:00)!", jamSelesai),
                    "Validasi Jam Operasional", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validasi nominal DP
        String dpStr = txtNominalDP.getText().trim().replaceAll("[^0-9]", "");
        if (dpStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nominal uang muka (DP) wajib diisi!", "Validasi DP", JOptionPane.WARNING_MESSAGE);
            txtNominalDP.requestFocus();
            return;
        }

        double nominalDP;
        try {
            nominalDP = Double.parseDouble(dpStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Nominal DP harus berupa angka!", "Validasi DP", JOptionPane.ERROR_MESSAGE);
            return;
        }

        TarifCalculator.TarifResult calc = TarifCalculator.hitung(jamMulai, durasi);
        if (nominalDP < calc.getMinimalDP()) {
            JOptionPane.showMessageDialog(this,
                    String.format("Uang Muka (DP) minimal 50%% dari total tarif!\n\n" +
                                    "Total Tarif: %s\n" +
                                    "Minimal DP Wajib: %s\n" +
                                    "Nominal Diinput: %s\n\n" +
                                    "Silakan lengkapi pembayaran uang muka terlebih dahulu.",
                            TarifCalculator.formatRupiah(calc.getTotalTarif()),
                            TarifCalculator.formatRupiah(calc.getMinimalDP()),
                            TarifCalculator.formatRupiah(nominalDP)),
                    "Validasi DP Tidak Memenuhi Syarat", JOptionPane.WARNING_MESSAGE);
            txtNominalDP.requestFocus();
            return;
        }

        String statusDP = TarifCalculator.tentukanStatusDP(nominalDP, calc.getTotalTarif());
        String idBooking = txtIdBooking.getText();

        JadwalSewa baru = new JadwalSewa(
                idBooking,
                namaTim,
                noHp,
                selectedDate,
                jamMulai,
                durasi,
                statusDP,
                calc.getTotalTarif(),
                nominalDP
        );

        try {
            jadwalManager.tambahJadwal(baru);

            JOptionPane.showMessageDialog(this,
                    String.format("Reservasi berhasil disimpan ke jadwal_futsal.csv!\n\n" +
                                    "ID Booking: %s\n" +
                                    "Tim: %s\n" +
                                    "Jadwal: %s (%s)\n" +
                                    "Total Biaya: %s\n" +
                                    "Status: %s",
                            baru.getIdBooking(), baru.getNamaTim(), baru.getFormattedRentangJam(),
                            baru.getTanggalMain().format(dateFormatter),
                            TarifCalculator.formatRupiah(baru.getTotalTarif()), baru.getStatusDP()),
                    "Reservasi Sukses", JOptionPane.INFORMATION_MESSAGE);

            // Buka struk bukti pembayaran
            ReceiptDialog receiptDialog = new ReceiptDialog(this, baru);
            receiptDialog.setVisible(true);

            resetFormInput();
            refreshAllData();

        } catch (BentrokJadwalException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "JADWAL BENTROK!", JOptionPane.ERROR_MESSAGE);
            updateTarifAndConflictCheck();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + ex.getMessage(), "Error Penyimpanan", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetFormInput() {
        txtNamaTim.setText("");
        txtNoHp.setText("");
        cmbJamMulai.setSelectedIndex(0);
        cmbDurasi.setSelectedIndex(0);
        txtNominalDP.setText("");
        generateNewIdBooking();
        updateTarifAndConflictCheck();
    }

    private void refreshAllData() {
        jadwalManager.muatData();
        generateNewIdBooking();
        timelinePanel.refreshTimeline(selectedDate);
        updateTarifAndConflictCheck();
        applyFilterTabel();
        updateStatistikRingkas();
    }

    private void applyFilterTabel() {
        tableModel.setRowCount(0);
        int filterIndex = cmbFilterTanggal.getSelectedIndex();
        List<JadwalSewa> list;

        if (filterIndex == 1) { // Tanggal Terpilih
            list = jadwalManager.getJadwalByTanggal(selectedDate);
        } else if (filterIndex == 2) { // Hari Ini Saja
            list = jadwalManager.getJadwalByTanggal(LocalDate.now());
        } else { // Semua
            list = jadwalManager.getAllJadwal();
        }

        for (JadwalSewa j : list) {
            tableModel.addRow(new Object[]{
                    j.getIdBooking(),
                    j.getNamaTim(),
                    j.getNoHp(),
                    j.getTanggalMain().format(dateFormatter),
                    j.getFormattedRentangJam(),
                    j.getDurasiJam() + " Jam",
                    TarifCalculator.formatRupiah(j.getTotalTarif()),
                    j.getStatusDP()
            });
        }
    }

    private void filterSearch() {
        String query = txtCari.getText().trim();
        if (query.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
    }

    private void updateStatistikRingkas() {
        LocalDate today = LocalDate.now();
        List<JadwalSewa> todayList = jadwalManager.getJadwalByTanggal(today);
        int totalJadwal = jadwalManager.getAllJadwal().size();

        double totalPendapatanDP = jadwalManager.getAllJadwal().stream()
                .mapToDouble(JadwalSewa::getNominalDP).sum();

        lblStatistik.setText(String.format("<html><div style='text-align:right;'>" +
                        "📅 Hari Ini (%s): <b>%d Booking</b> | Total Keseluruhan: <b>%d</b><br>" +
                        "💰 Total Uang Masuk (DP/Lunas): <b>%s</b>" +
                        "</div></html>",
                today.format(dateFormatter), todayList.size(), totalJadwal, TarifCalculator.formatRupiah(totalPendapatanDP)));
    }

    private void bukaStrukPilihan() {
        int selectedRow = tableJadwal.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Silakan pilih salah satu reservasi pada tabel terlebih dahulu!", "Pilih Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tableJadwal.convertRowIndexToModel(selectedRow);
        String idBooking = (String) tableModel.getValueAt(modelRow, 0);

        JadwalSewa found = jadwalManager.getAllJadwal().stream()
                .filter(j -> j.getIdBooking().equalsIgnoreCase(idBooking))
                .findFirst().orElse(null);

        if (found != null) {
            ReceiptDialog dlg = new ReceiptDialog(this, found);
            dlg.setVisible(true);
        }
    }

    private void hapusReservasiTerpilih() {
        int selectedRow = tableJadwal.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Silakan pilih reservasi yang ingin dibatalkan!", "Pilih Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tableJadwal.convertRowIndexToModel(selectedRow);
        String idBooking = (String) tableModel.getValueAt(modelRow, 0);
        String namaTim = (String) tableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Apakah Anda yakin ingin membatalkan/menghapus reservasi:\nID: %s\nTim: %s ?", idBooking, namaTim),
                "Konfirmasi Pembatalan", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean ok = jadwalManager.hapusJadwal(idBooking);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Reservasi " + idBooking + " berhasil dibatalkan dan dihapus dari CSV!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    refreshAllData();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Gagal memperbarui berkas CSV: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void bukaFileCsv() {
        try {
            File f = jadwalManager.getCsvPath().toFile();
            if (f.exists()) {
                Desktop.getDesktop().open(f);
            } else {
                JOptionPane.showMessageDialog(this, "Berkas CSV belum ada di: " + f.getAbsolutePath(), "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Tidak dapat membuka berkas CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
