package com.possystem.util;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A self-painted "New York City at dusk" backdrop: a sunset sky gradient behind a
 * procedurally generated skyline silhouette (varying building heights, setback towers,
 * antenna spires, and twinkling window lights). Nothing here traces or reproduces any real
 * building — it's a generic, original skyline shape, safe to use as decorative branding.
 *
 * Two modes:
 *  - full (compact=false): tall panel with a glowing sunset sun and a richer skyline, meant
 *    for the login screen background.
 *  - compact (compact=true): short strip with a muted navy-on-navy skyline, meant to sit
 *    behind the POS header bar without competing with the text on top of it.
 *
 * The skyline layout is deterministic (fixed random seed) so it looks identical on every
 * repaint/resize rather than jittering.
 */
public class NYCSkylinePanel extends JPanel {

    private final boolean compact;
    private final Color skyTop;
    private final Color skyBottom;
    private final Color buildingColor;
    private final Color windowColor;

    public NYCSkylinePanel(boolean compact) {
        this.compact = compact;
        setOpaque(true);
        if (compact) {
            skyTop = new Color(24, 33, 48);
            skyBottom = new Color(38, 50, 68);
            buildingColor = new Color(14, 19, 30);
            windowColor = new Color(255, 210, 120, 80);
        } else {
            skyTop = new Color(30, 24, 64);
            skyBottom = new Color(255, 150, 92);
            buildingColor = new Color(19, 17, 32);
            windowColor = new Color(255, 214, 120, 220);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) { g2.dispose(); return; }

        g2.setPaint(new GradientPaint(0, 0, skyTop, 0, h, skyBottom));
        g2.fillRect(0, 0, w, h);

        if (!compact) {
            int sunR = Math.max(36, w / 11);
            int sunX = (int) (w * 0.74);
            int sunY = (int) (h * 0.40);
            for (int i = 3; i >= 0; i--) {
                int r = sunR + i * 16;
                g2.setColor(new Color(255, 196, 130, 26 - i * 5));
                g2.fillOval(sunX - r, sunY - r, r * 2, r * 2);
            }
            g2.setColor(new Color(255, 224, 172, 235));
            g2.fillOval(sunX - sunR, sunY - sunR, sunR * 2, sunR * 2);
        }

        Random rnd = new Random(42);
        int skylineTop = compact ? (int) (h * 0.30) : (int) (h * 0.40);
        int maxBh = Math.max(4, h - skylineTop);
        List<Rectangle> buildings = new ArrayList<>();
        g2.setColor(buildingColor);
        int x = -20;
        while (x < w + 20) {
            int bw = compact ? (16 + rnd.nextInt(20)) : (34 + rnd.nextInt(46));
            int bh = (int) (maxBh * (compact ? (0.35 + rnd.nextDouble() * 0.65) : (0.30 + rnd.nextDouble() * 0.85)));
            int by = h - bh;
            g2.fillRect(x, by, bw, bh);
            buildings.add(new Rectangle(x, by, bw, bh));

            if (!compact && rnd.nextDouble() < 0.35) {
                int tw = (int) (bw * 0.55);
                int th = (int) (bh * (0.15 + rnd.nextDouble() * 0.25));
                int tx = x + (bw - tw) / 2;
                int ty = by - th;
                g2.fillRect(tx, ty, tw, th);
                buildings.add(new Rectangle(tx, ty, tw, th));
                if (rnd.nextDouble() < 0.4) {
                    int spikeH = (int) (bh * 0.18);
                    g2.fillRect(tx + tw / 2 - 1, ty - spikeH, 2, spikeH);
                }
            }
            x += bw + (compact ? 2 : 4);
        }

        g2.setColor(windowColor);
        for (Rectangle b : buildings) {
            int stepX = compact ? 8 : 7;
            int stepY = compact ? 10 : 9;
            int cols = Math.max(1, b.width / stepX);
            int rows = Math.max(1, b.height / stepY);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (rnd.nextDouble() < (compact ? 0.16 : 0.28)) {
                        int wx = b.x + 3 + c * stepX;
                        int wy = b.y + 3 + r * stepY;
                        if (wx < b.x + b.width - 2 && wy < b.y + b.height - 2) {
                            g2.fillRect(wx, wy, compact ? 2 : 3, compact ? 3 : 4);
                        }
                    }
                }
            }
        }

        g2.dispose();
    }
}
