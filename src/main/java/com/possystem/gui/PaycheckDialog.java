package com.possystem.gui;

import com.possystem.model.Employee;
import com.possystem.model.PayrollRun;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Prints/displays a single payroll run as a bank-check-style paycheck: payor info, check
 * number, date, "Pay to the order of" line, the amount spelled out in words (the standard
 * fraud-resistant check-writing convention), a memo line, a signature line, and a MICR-style
 * routing/account line along the bottom.
 *
 * The bank name and routing/account numbers here are entirely fictional placeholders generated
 * from the app's own data (not tied to any real financial institution or account) - this is a
 * printable internal record for the shop, not a negotiable financial instrument, which is
 * called out in the footer disclaimer.
 */
public class PaycheckDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final Color INK = new Color(25, 40, 70);
    private static final Color CHECK_BG = new Color(244, 247, 238);
    private static final Color BORDER = new Color(150, 165, 140);

    private final JPanel check;

    public PaycheckDialog(Window owner, PayrollRun run, Employee employee) {
        super(owner, "Paycheck", ModalityType.APPLICATION_MODAL);
        check = buildCheck(run, employee);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        wrap.add(check, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(new JScrollPane(wrap), BorderLayout.CENTER);

        JButton printBtn = new JButton("Print");
        printBtn.addActionListener(e -> printCheck());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(printBtn);
        buttons.add(closeBtn);
        add(buttons, BorderLayout.SOUTH);

        setSize(700, 480);
        setLocationRelativeTo(owner);
    }

    private void printCheck() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                double scale = Math.min(pf.getImageableWidth() / check.getWidth(), 1.0);
                g2.scale(scale, scale);
                check.printAll(g2);
                return PAGE_EXISTS;
            }
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildCheck(PayrollRun run, Employee employee) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CHECK_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 2),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        panel.setPreferredSize(new Dimension(640, 300));
        panel.setMaximumSize(new Dimension(640, 300));

        // ---- Payor header + check number/date ----
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel payor = new JPanel();
        payor.setLayout(new BoxLayout(payor, BoxLayout.Y_AXIS));
        payor.setOpaque(false);
        JLabel company = new JLabel("NY Coffee Co.");
        company.setFont(new Font("Serif", Font.BOLD, 18));
        company.setForeground(INK);
        JLabel address = new JLabel("Brewed in the Heart of NYC");
        address.setFont(new Font("SansSerif", Font.PLAIN, 11));
        address.setForeground(INK);
        JLabel bank = new JLabel("First Harbor Bank & Trust — NYC Branch — Payroll Checking Account");
        bank.setFont(new Font("SansSerif", Font.PLAIN, 10));
        bank.setForeground(new Color(90, 100, 80));
        payor.add(company);
        payor.add(address);
        payor.add(Box.createVerticalStrut(4));
        payor.add(bank);
        topRow.add(payor, BorderLayout.WEST);

        JPanel checkMeta = new JPanel();
        checkMeta.setLayout(new BoxLayout(checkMeta, BoxLayout.Y_AXIS));
        checkMeta.setOpaque(false);
        JLabel checkNoLabel = new JLabel("Check No. " + nvl(run.getCheckNumber()));
        checkNoLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        checkNoLabel.setForeground(INK);
        checkNoLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel dateLabel = new JLabel("Date: " + fmt(run.getPayDate()));
        dateLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        dateLabel.setForeground(INK);
        dateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        checkMeta.add(checkNoLabel);
        checkMeta.add(dateLabel);
        topRow.add(checkMeta, BorderLayout.EAST);

        panel.add(topRow);
        panel.add(Box.createVerticalStrut(20));

        // ---- Pay to the order of / amount box ----
        JPanel payLine = new JPanel(new BorderLayout(10, 0));
        payLine.setOpaque(false);
        JLabel payTo = new JLabel("PAY TO THE ORDER OF:   " + underline(employee.getFullName(), 34));
        payTo.setFont(new Font("Monospaced", Font.PLAIN, 14));
        payTo.setForeground(INK);
        payLine.add(payTo, BorderLayout.WEST);

        JLabel amountBox = new JLabel("$ " + money(run.getNetPay()));
        amountBox.setOpaque(true);
        amountBox.setBackground(Color.WHITE);
        amountBox.setForeground(INK);
        amountBox.setFont(new Font("Monospaced", Font.BOLD, 16));
        amountBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        payLine.add(amountBox, BorderLayout.EAST);

        panel.add(payLine);
        panel.add(Box.createVerticalStrut(10));

        JTextArea words = new JTextArea(capitalize(amountInWords(run.getNetPay())) + " dollars " + underline("", 10));
        words.setLineWrap(true);
        words.setWrapStyleWord(true);
        words.setEditable(false);
        words.setFocusable(false);
        words.setOpaque(false);
        words.setFont(new Font("Monospaced", Font.PLAIN, 13));
        words.setForeground(INK);
        words.setAlignmentX(Component.LEFT_ALIGNMENT);
        words.setMaximumSize(new Dimension(600, 40));
        panel.add(words);
        panel.add(Box.createVerticalStrut(24));

        // ---- Memo + signature ----
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);

        JLabel memo = new JLabel("Memo: Pay period " + fmt(run.getPeriodStart()) + " - " + fmt(run.getPeriodEnd()));
        memo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        memo.setForeground(new Color(90, 100, 80));
        bottomRow.add(memo, BorderLayout.WEST);

        JPanel sigCol = new JPanel();
        sigCol.setLayout(new BoxLayout(sigCol, BoxLayout.Y_AXIS));
        sigCol.setOpaque(false);
        JLabel signature = new JLabel("Rezaul Karim", SwingConstants.RIGHT);
        signature.setFont(new Font("Serif", Font.ITALIC, 20));
        signature.setForeground(INK);
        signature.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JSeparator sigLine = new JSeparator();
        sigLine.setForeground(BORDER);
        sigLine.setMaximumSize(new Dimension(220, 1));
        JLabel sigCaption = new JLabel("Authorized Signature — Owner and Managing Director", SwingConstants.RIGHT);
        sigCaption.setFont(new Font("SansSerif", Font.PLAIN, 9));
        sigCaption.setForeground(new Color(90, 100, 80));
        sigCaption.setAlignmentX(Component.RIGHT_ALIGNMENT);
        sigCol.add(signature);
        sigCol.add(sigLine);
        sigCol.add(sigCaption);
        bottomRow.add(sigCol, BorderLayout.EAST);

        panel.add(bottomRow);
        panel.add(Box.createVerticalStrut(16));

        // ---- MICR-style routing/account line (fictional) ----
        String accountNo = String.format("%010d", 4400000000L + employee.getId());
        String routingNo = "021999999"; // fictional - not a real routing number
        JLabel micr = new JLabel(":" + routingNo + ":   " + accountNo + "\"   " + nvl(run.getCheckNumber()));
        micr.setFont(new Font("Monospaced", Font.BOLD, 14));
        micr.setForeground(INK);
        micr.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(micr);
        panel.add(Box.createVerticalStrut(8));

        JLabel disclaimer = new JLabel(
            "For internal payroll record-keeping — generated by NY Coffee Co. POS, not a negotiable instrument.",
            SwingConstants.CENTER);
        disclaimer.setFont(new Font("SansSerif", Font.ITALIC, 9));
        disclaimer.setForeground(Color.GRAY);
        disclaimer.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(disclaimer);

        return panel;
    }

    private static String underline(String text, int minDots) {
        int dots = Math.max(0, minDots - text.length());
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < dots; i++) sb.append('_');
        return sb.toString();
    }

    private static String fmt(java.sql.Date d) {
        return d == null ? "N/A" : d.toLocalDate().format(DATE_FMT);
    }

    private static String money(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }

    private static String nvl(String s) {
        return (s == null || s.isEmpty()) ? "N/A" : s;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---- Number-to-words (standard check-writing convention: "one hundred twenty-three and 45/100") ----

    private static final String[] ONES = {"zero", "one", "two", "three", "four", "five", "six", "seven",
        "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen"};
    private static final String[] TENS = {"", "", "twenty", "thirty", "forty", "fifty", "sixty",
        "seventy", "eighty", "ninety"};

    static String amountInWords(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        amount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        long dollars = amount.longValue();
        int cents = amount.subtract(BigDecimal.valueOf(dollars)).movePointRight(2)
            .setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        String dollarWords = dollars == 0 ? "zero" : numberToWords(dollars);
        return dollarWords + " and " + String.format("%02d", cents) + "/100";
    }

    private static String numberToWords(long n) {
        if (n == 0) return "zero";
        StringBuilder sb = new StringBuilder();
        long[] scales = {1_000_000_000L, 1_000_000L, 1_000L, 1L};
        String[] scaleNames = {"billion", "million", "thousand", ""};
        for (int i = 0; i < scales.length; i++) {
            long chunk = n / scales[i];
            n %= scales[i];
            if (chunk > 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(threeDigitWords(chunk));
                if (!scaleNames[i].isEmpty()) sb.append(" ").append(scaleNames[i]);
            }
        }
        return sb.toString();
    }

    private static String threeDigitWords(long n) {
        StringBuilder sb = new StringBuilder();
        if (n >= 100) {
            sb.append(ONES[(int) (n / 100)]).append(" hundred");
            n %= 100;
            if (n > 0) sb.append(" ");
        }
        if (n >= 20) {
            sb.append(TENS[(int) (n / 10)]);
            if (n % 10 > 0) sb.append("-").append(ONES[(int) (n % 10)]);
        } else if (n > 0) {
            sb.append(ONES[(int) n]);
        }
        return sb.toString();
    }
}
