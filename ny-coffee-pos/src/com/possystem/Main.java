package com.possystem;

import com.possystem.gui.LoginFrame;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.Enumeration;

public class Main {
    public static void main(String[] args) {
        installUnicodeFont();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /** Swap every default Swing font for one that can render Bangla and Hindi (Devanagari)
     *  glyphs in addition to Latin script — needed because the app's 5-language support
     *  (English/Bangla/Hindi/Spanish/French, see com.possystem.util.I18n) would otherwise show
     *  "tofu" boxes for Bangla/Hindi text under the platform look-and-feel's default font.
     *  "Nirmala UI" ships with Windows 8+ and covers Latin, Bengali, and Devanagari. */
    private static void installUnicodeFont() {
        String family = "Nirmala UI";
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                FontUIResource old = (FontUIResource) value;
                UIManager.put(key, new FontUIResource(family, old.getStyle(), old.getSize()));
            }
        }
    }
}
