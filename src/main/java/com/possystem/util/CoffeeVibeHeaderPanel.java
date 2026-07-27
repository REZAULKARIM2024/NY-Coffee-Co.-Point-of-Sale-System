package com.possystem.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * A compact, self-painted "coffee shop vibes" banner: a warm latte-cream-to-mocha gradient,
 * a scatter of coffee-bean silhouettes, and a simple steaming-cup silhouette, with the page
 * title (and a small brand caption) laid over it.
 *
 * Meant to sit at the top of the non-POS management screens (Inventory, Menu Management,
 * Reports, etc.) so every page in the app feels branded and warm, not just the POS screen and
 * login — without touching those screens' own internal layouts. See
 * {@code MainDashboard.showPanel(String, JPanel)}, which wraps whatever panel is being shown
 * with one of these headers.
 *
 * Everything is procedurally drawn (no photos/assets), so there's nothing to license and it
 * scales cleanly to any panel width.
 */
public class CoffeeVibeHeaderPanel extends JPanel {

    private final String title;

    public CoffeeVibeHeaderPanel(String title) {
        this.title = title;
        setPreferredSize(new Dimension(0, 64));
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) { g2.dispose(); return; }

        g2.setPaint(new GradientPaint(0, 0, new Color(250, 235, 214), w, 0, new Color(120, 84, 55)));
        g2.fillRect(0, 0, w, h);

        Random rnd = new Random(7);
        int beanCount = Math.max(6, w / 90);
        for (int i = 0; i < beanCount; i++) {
            int bx = rnd.nextInt(Math.max(1, w));
            int by = 8 + rnd.nextInt(Math.max(1, h - 16));
            int bw = 13 + rnd.nextInt(6);
            int bh = 8 + rnd.nextInt(4);
            double angle = rnd.nextDouble() * Math.PI;
            drawBean(g2, bx, by, bw, bh, angle);
        }

        drawCupAndSteam(g2, Math.max(20, w - 66), h);

        g2.setColor(new Color(48, 30, 18));
        Font titleFont = getFont().deriveFont(Font.BOLD, 20f);
        g2.setFont(titleFont);
        FontMetrics fm = g2.getFontMetrics();
        int textY = h / 2 - 4;
        g2.drawString(title, 20, textY);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        g2.setColor(new Color(96, 64, 38));
        g2.drawString("NY Coffee Co.", 20, textY + fm.getDescent() + 14);

        g2.dispose();
    }

    private void drawBean(Graphics2D base, int cx, int cy, int w, int h, double angle) {
        Graphics2D g2 = (Graphics2D) base.create();
        g2.translate(cx, cy);
        g2.rotate(angle);
        g2.setColor(new Color(70, 45, 25, 60));
        g2.fillOval(-w / 2, -h / 2, w, h);
        g2.setColor(new Color(250, 235, 214, 110));
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawLine(0, -h / 2 + 1, 0, h / 2 - 1);
        g2.dispose();
    }

    private void drawCupAndSteam(Graphics2D base, int cupX, int panelH) {
        Graphics2D g2 = (Graphics2D) base.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cupW = 34, cupH = 22;
        int cupY = panelH - cupH - 8;

        g2.setColor(new Color(255, 255, 255, 205));
        g2.fillRoundRect(cupX, cupY, cupW, cupH, 6, 6);
        g2.setColor(new Color(255, 255, 255, 150));
        g2.fillRoundRect(cupX - 4, cupY + cupH, cupW + 8, 4, 4, 4);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(255, 255, 255, 205));
        g2.drawArc(cupX + cupW - 6, cupY + 4, 14, cupH - 8, -90, 180);

        g2.setColor(new Color(255, 255, 255, 150));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 2; i++) {
            int sx = cupX + 8 + i * 14;
            Path2D.Double path = new Path2D.Double();
            path.moveTo(sx, cupY - 2);
            path.curveTo(sx - 6, cupY - 14, sx + 6, cupY - 22, sx, cupY - 34);
            g2.draw(path);
        }
        g2.dispose();
    }
}
