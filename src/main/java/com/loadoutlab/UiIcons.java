// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab;

import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/** Original vector badges; item and prayer art is supplied by the running client. */
public final class UiIcons {
    private UiIcons() {}

    private static volatile SpriteManager sprites;

    public static void useClientSprites(SpriteManager manager) {
        sprites = manager;
    }

    public static ImageIcon icon(String key, int size) {
        ImageIcon icon = new ImageIcon(image(key, size));
        SpriteManager manager = sprites;
        int id = sprite(key);
        if (manager != null && id >= 0)
            manager.getSpriteAsync(
                    id,
                    0,
                    source ->
                            SwingUtilities.invokeLater(
                                    () -> {
                                        double scale =
                                                Math.min(
                                                        (double) size / source.getWidth(),
                                                        (double) size / source.getHeight());
                                        icon.setImage(
                                                source.getScaledInstance(
                                                        Math.max(
                                                                1,
                                                                (int) (source.getWidth() * scale)),
                                                        Math.max(
                                                                1,
                                                                (int) (source.getHeight() * scale)),
                                                        Image.SCALE_SMOOTH));
                                        for (Window window : Window.getWindows())
                                            if (window.isShowing()) window.repaint();
                                    }));
        return icon;
    }

    private static int sprite(String key) {
        switch (key) {
            case "tabs/combat":
                return SpriteID.TAB_COMBAT;
            case "tabs/skills":
                return SpriteID.RS2_TAB_STATS;
            case "tabs/equipment":
                return SpriteID.RS2_TAB_EQUIPMENT;
            case "tabs/prayer":
                return SpriteID.TAB_PRAYER;
            case "tabs/options":
                return SpriteID.TAB_OPTIONS;
            case "slots/head":
                return SpriteID.EQUIPMENT_SLOT_HEAD;
            case "slots/cape":
                return SpriteID.EQUIPMENT_SLOT_CAPE;
            case "slots/neck":
                return SpriteID.EQUIPMENT_SLOT_NECK;
            case "slots/weapon":
                return SpriteID.EQUIPMENT_SLOT_WEAPON;
            case "slots/body":
                return SpriteID.EQUIPMENT_SLOT_TORSO;
            case "slots/shield":
                return SpriteID.EQUIPMENT_SLOT_SHIELD;
            case "slots/legs":
                return SpriteID.EQUIPMENT_SLOT_LEGS;
            case "slots/hands":
                return SpriteID.EQUIPMENT_SLOT_HANDS;
            case "slots/feet":
                return SpriteID.EQUIPMENT_SLOT_FEET;
            case "slots/ring":
                return SpriteID.EQUIPMENT_SLOT_RING;
            case "slots/ammo":
                return SpriteID.EQUIPMENT_SLOT_AMMUNITION;
            case "bonuses/attack":
            case "bonuses/dagger":
                return SpriteID.SKILL_ATTACK;
            case "bonuses/defence":
                return SpriteID.SKILL_DEFENCE;
            case "bonuses/strength":
            case "bonuses/warhammer":
                return SpriteID.SKILL_STRENGTH;
            case "bonuses/scimitar":
                return SpriteID.COMBAT_STYLE_SWORD_SLASH;
            case "bonuses/ranged":
            case "bonuses/ranged_strength":
                return SpriteID.SKILL_RANGED;
            case "bonuses/magic":
            case "bonuses/magic_strength":
                return SpriteID.SKILL_MAGIC;
            case "bonuses/hitpoints":
                return SpriteID.SKILL_HITPOINTS;
            default:
                return -1;
        }
    }

    public static BufferedImage image(String key, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(196, 175, 125));
        g.setStroke(new BasicStroke(1.5f));
        if (key.equals("logo")) {
            for (int i = 0; i < 3; i++)
                g.fillRoundRect(
                        2 + i * size / 3,
                        size - (i + 1) * size / 4 - 2,
                        Math.max(2, size / 5),
                        (i + 1) * size / 4,
                        2,
                        2);
        } else {
            String label = key.substring(key.lastIndexOf('/') + 1).replace('_', ' ');
            String mark = label.length() > 2 ? label.substring(0, 2) : label;
            g.drawRoundRect(1, 1, size - 3, size - 3, 5, 5);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, size / 3)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(
                    mark.toUpperCase(java.util.Locale.ROOT),
                    (size - fm.stringWidth(mark.toUpperCase(java.util.Locale.ROOT))) / 2,
                    (size - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
        return image;
    }
}
