package com.possystem.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Original, procedurally painted "coffee shop corner" illustration: a warm glow behind a
 * steaming cup and saucer, a scatter of coffee-bean silhouettes, the brand name/tagline, and a
 * dusk NYC skyline strip along the bottom (reusing {@link NYCSkylinePanel} in compact mode).
 *
 * Meant to fill the empty space below the sidebar nav buttons in {@code MainDashboard} so it
 * feels like part of the shop rather than dead space. Nothing here is a photo/raster asset -
 * it's all vector shapes drawn with Graphics2D, same convention as {@link NYCSkylinePanel} and
 * {@link CoffeeVibeHeaderPanel} - so there's nothing to license and it scales cleanly to any
 * sidebar width/height.
 */
public class CoffeeShopScenePanel extends JPanel {

    public CoffeeShopScenePanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(35, 45, 60));
        setPreferredSize(new Dimension(180, 320));
        setMaximumSize(new Dimension(180, Short.MAX_VALUE));
        setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel art = new ScenePaint();
        art.setOpaque(false);
        add(art, BorderLayout.CENTER);

        NYCSkylinePanel skyline = new NYCSkylinePanel(true);
        skyline.setPreferredSize(new Dimension(180, 64));
        add(skyline, BorderLayout.SOUTH);
    }

    private static class ScenePaint extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) { g2.dispose(); return; }

            int gx = w / 2;
            int gy = (int) (h * 0.36);
            int glowR = Math.min(w, h) / 2 + 24;
            for (int i = 4; i >= 0; i--) {
                int r = glowR - i * 10;
                if (r <= 0) continue;
                g2.setColor(new Color(214, 130, 60, 10 + i * 4));
                g2.fillOval(gx - r, gy - r, r * 2, r * 2);
            }

            Random rnd = new Random(11);
            int beanBandH = Math.max(1, (int) (h * 0.14));
            for (int i = 0; i < 7; i++) {
                int bx = 14 + rnd.nextInt(Math.max(1, w - 28));
                int by = 10 + rnd.nextInt(beanBandH);
                double angle = rnd.nextDouble() * Math.PI;
                drawBean(g2, bx, by, 12 + rnd.nextInt(4), 7 + rnd.nextInt(3), angle);
            }

            int cupW = Math.min(84, Math.max(40, w - 40));
            int cupH = (int) (cupW * 0.62);
            int cupX = (w - cupW) / 2;
            int cupY = gy - cupH / 2;
            drawCup(g2, cupX, cupY, cupW, cupH);
            drawSteam(g2, cupX + cupW / 2 - 12, cupY - 6);
            drawSteam(g2, cupX + cupW / 2 + 10, cupY - 10);

            g2.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 20));
            g2.setColor(new Color(250, 235, 214));
            String line1 = "NY Coffee Co.";
            FontMetrics fm1 = g2.getFontMetrics();
            int t1x = (w - fm1.stringWidth(line1)) / 2;
            int t1y = cupY + cupH + 46;
            g2.drawString(line1, t1x, t1y);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(new Color(190, 170, 150));
            drawCentered(g2, "Brewed in the", w, t1y + 20);
            drawCentered(g2, "Heart of NYC", w, t1y + 36);

            g2.dispose();
        }

        private void drawCentered(Graphics2D g2, String s, int w, int y) {
            FontMetrics fm = g2.getFontMetrics();
            int x = (w - fm.stringWidth(s)) / 2;
            g2.drawString(s, x, y);
        }

        private void drawBean(Graphics2D base, int cx, int cy, int w, int h, double angle) {
            Graphics2D g2 = (Graphics2D) base.create();
            g2.translate(cx, cy);
            g2.rotate(angle);
            g2.setColor(new Color(120, 78, 45, 140));
            g2.fillOval(-w / 2, -h / 2, w, h);
            g2.setColor(new Color(250, 235, 214, 130));
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawLine(0, -h / 2 + 1, 0, h / 2 - 1);
            g2.dispose();
        }

        private void drawCup(Graphics2D base, int x, int y, int cupW, int cupH) {
            Graphics2D g2 = (Graphics2D) base.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int saucerW = (int) (cupW * 1.5), saucerH = Math.max(6, (int) (cupH * 0.22));
            int saucerX = x + (cupW - saucerW) / 2, saucerY = y + cupH;
            g2.setColor(new Color(255, 255, 255, 235));
            g2.fillRoundRect(saucerX, saucerY, saucerW, saucerH, saucerH, saucerH);
            g2.setColor(new Color(0, 0, 0, 40));
            g2.drawRoundRect(saucerX, saucerY, saucerW, saucerH, saucerH, saucerH);

            g2.setColor(new Color(255, 255, 255, 245));
            g2.fillRoundRect(x, y, cupW, cupH, 10, 10);
            g2.setColor(new Color(0, 0, 0, 35));
            g2.drawRoundRect(x, y, cupW, cupH, 10, 10);

            g2.setColor(new Color(93, 64, 55));
            g2.fillRoundRect(x + 5, y + 4, cupW - 10, Math.max(6, cupH / 5), 6, 6);

            g2.setStroke(new BasicStroke(4f));
            g2.setColor(new Color(255, 255, 255, 245));
            g2.drawArc(x + cupW - 10, y + cupH / 5, cupW / 3, (int) (cupH * 0.6), -90, 180);

            g2.dispose();
        }

        private void drawSteam(Graphics2D base, int sx, int sy) {
            Graphics2D g2 = (Graphics2D) base.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 130));
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Double path = new Path2D.Double();
            path.moveTo(sx, sy);
            path.curveTo(sx - 8, sy - 16, sx + 8, sy - 26, sx, sy - 42);
            g2.draw(path);
            g2.dispose();
        }
    }
}
