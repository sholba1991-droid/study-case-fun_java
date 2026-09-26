package com.arenamalang.futsal.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Dialog Kalender Interaktif untuk memilih tanggal reservasi lapangan.
 */
public class DatePickerDialog extends JDialog {

    private LocalDate selectedDate;
    private YearMonth currentYearMonth;
    private final JPanel daysGridPanel;
    private final JLabel monthYearLabel;
    private final Locale localeId = Locale.of("id", "ID");

    public DatePickerDialog(Frame owner, LocalDate initialDate) {
        super(owner, "Pilih Tanggal Reservasi — Futsal & Mini Soccer Arena", true);
        this.selectedDate = (initialDate != null) ? initialDate : LocalDate.now();
        this.currentYearMonth = YearMonth.from(selectedDate);

        setSize(420, 380);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Header Panel (Navigasi Bulan/Tahun)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 76, 50)); // Dark Emerald Sports Theme
        headerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JButton prevBtn = new JButton("◀");
        styleNavButton(prevBtn);
        prevBtn.addActionListener(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            updateCalendar();
        });

        JButton nextBtn = new JButton("▶");
        styleNavButton(nextBtn);
        nextBtn.addActionListener(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            updateCalendar();
        });

        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        monthYearLabel.setForeground(Color.WHITE);

        headerPanel.add(prevBtn, BorderLayout.WEST);
        headerPanel.add(monthYearLabel, BorderLayout.CENTER);
        headerPanel.add(nextBtn, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main Center Panel (Nama Hari + Grid Tanggal)
        JPanel calendarContainer = new JPanel(new BorderLayout());
        calendarContainer.setBorder(new EmptyBorder(10, 15, 10, 15));

        // Hari dalam seminggu
        JPanel dayNamesPanel = new JPanel(new GridLayout(1, 7, 4, 4));
        String[] days = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
        for (String day : days) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if ("Min".equals(day)) {
                lbl.setForeground(new Color(220, 53, 69)); // Merah untuk hari Minggu
            } else {
                lbl.setForeground(new Color(80, 80, 80));
            }
            dayNamesPanel.add(lbl);
        }
        calendarContainer.add(dayNamesPanel, BorderLayout.NORTH);

        // Grid tombol tanggal
        daysGridPanel = new JPanel(new GridLayout(6, 7, 4, 4));
        calendarContainer.add(daysGridPanel, BorderLayout.CENTER);
        add(calendarContainer, BorderLayout.CENTER);

        // Bottom Footer Panel (Tombol Hari Ini & Batal)
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footerPanel.setBackground(new Color(245, 247, 248));

        JButton todayBtn = new JButton("📅 Hari Ini");
        todayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        todayBtn.addActionListener(e -> {
            selectedDate = LocalDate.now();
            currentYearMonth = YearMonth.from(selectedDate);
            updateCalendar();
        });

        JButton selectBtn = new JButton("Pilih Tanggal Ini");
        selectBtn.setBackground(new Color(33, 136, 56));
        selectBtn.setForeground(Color.WHITE);
        selectBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        selectBtn.setFocusPainted(false);
        selectBtn.addActionListener(e -> dispose());

        JButton cancelBtn = new JButton("Batal");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.addActionListener(e -> {
            selectedDate = null;
            dispose();
        });

        footerPanel.add(todayBtn);
        footerPanel.add(cancelBtn);
        footerPanel.add(selectBtn);
        add(footerPanel, BorderLayout.SOUTH);

        updateCalendar();
    }

    private void styleNavButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(15, 50, 30));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void updateCalendar() {
        monthYearLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, localeId).toUpperCase(localeId)
                + " " + currentYearMonth.getYear());

        daysGridPanel.removeAll();

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue(); // 1 = Senin, 7 = Minggu
        int leadingEmptyCells = (dayOfWeekValue == 7) ? 0 : dayOfWeekValue; // 0 = Minggu

        int daysInMonth = currentYearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        // Sel kosong sebelum tanggal 1
        for (int i = 0; i < leadingEmptyCells; i++) {
            JLabel emptyLabel = new JLabel("");
            daysGridPanel.add(emptyLabel);
        }

        // Tanggal dalam bulan
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            dayButton.setFocusPainted(false);
            dayButton.setMargin(new Insets(2, 2, 2, 2));
            dayButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            boolean isToday = date.equals(today);
            boolean isSelected = (selectedDate != null && date.equals(selectedDate));

            if (isSelected) {
                dayButton.setBackground(new Color(25, 135, 84)); // Green theme
                dayButton.setForeground(Color.WHITE);
                dayButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            } else if (isToday) {
                dayButton.setBackground(new Color(225, 245, 230));
                dayButton.setForeground(new Color(15, 80, 40));
                dayButton.setBorder(BorderFactory.createLineBorder(new Color(40, 167, 69), 1));
            } else {
                dayButton.setBackground(Color.WHITE);
                if (date.getDayOfWeek().getValue() == 7) {
                    dayButton.setForeground(new Color(210, 45, 45)); // Sunday
                } else {
                    dayButton.setForeground(Color.BLACK);
                }
            }

            dayButton.addActionListener(e -> {
                selectedDate = date;
                dispose();
            });

            daysGridPanel.add(dayButton);
        }

        // Sisa sel kosong untuk melengkapi grid 6x7 = 42 sel
        int totalCells = leadingEmptyCells + daysInMonth;
        for (int i = totalCells; i < 42; i++) {
            daysGridPanel.add(new JLabel(""));
        }

        daysGridPanel.revalidate();
        daysGridPanel.repaint();
    }

    public static LocalDate showDialog(Frame owner, LocalDate initialDate) {
        DatePickerDialog dialog = new DatePickerDialog(owner, initialDate);
        dialog.setVisible(true);
        return dialog.selectedDate;
    }
}
