package com.dpscalc.scenario;

import java.awt.*;

import javax.swing.*;

/** A major sidebar section. The entire header is one native button hit target. */
final class WorkspaceSection extends JPanel {
    private final JPanel body;
    private final Heading heading;
    private final String title;

    WorkspaceSection(String title, String subtitle, JPanel body, boolean expanded) {
        this.title = title;
        this.body = body;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createLineBorder(CalculatorWorkspace.EDGE));
        putClientProperty("wikiDpsSurface", CalculatorWorkspace.CARD);
        getAccessibleContext().setAccessibleName(title + " section");
        heading = new Heading(title, subtitle);
        heading.addActionListener(e -> setExpanded(!body.isVisible()));
        body.setBorder(
                BorderFactory.createCompoundBorder(
                        body.getBorder(), BorderFactory.createEmptyBorder(10, 8, 10, 8)));
        body.putClientProperty("wikiDpsSurface", CalculatorWorkspace.CARD);
        add(heading);
        add(body);
        setExpanded(expanded);
    }

    void setExpanded(boolean expanded) {
        boolean previous = body.isVisible();
        body.setVisible(expanded);
        heading.setText((expanded ? "▾ " : "▸ ") + title);
        heading.getAccessibleContext()
                .setAccessibleDescription(
                        (expanded ? "Expanded. " : "Collapsed. ") + heading.subtitle);
        for (Container parent = this; parent != null; parent = parent.getParent())
            parent.invalidate();
        revalidate();
        repaint();
        firePropertyChange("expanded", previous, expanded);
    }

    boolean isExpanded() {
        return body.isVisible();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    static final class Heading extends JButton {
        private final String title, subtitle;

        Heading(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
            getAccessibleContext().setAccessibleName(title);
            setToolTipText(subtitle);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(170, 52);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, 52);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(
                    getModel().isRollover()
                            ? CalculatorWorkspace.FIELD
                            : CalculatorWorkspace.HEADER);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(CalculatorWorkspace.TEXT);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.drawString(title, 10, 21);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.setColor(CalculatorWorkspace.MUTED);
            Shape clip = g.getClip();
            g.clipRect(10, 25, Math.max(0, getWidth() - 30), 20);
            g.drawString(subtitle, 10, 38);
            g.setClip(clip);
            g.drawString(getText().startsWith("▾") ? "▾" : "▸", getWidth() - 17, 23);
            g.setColor(CalculatorWorkspace.EDGE);
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            if (hasFocus()) {
                g.setColor(CalculatorWorkspace.GOLD);
                g.drawRect(2, 2, getWidth() - 5, getHeight() - 5);
            }
            g.dispose();
        }

        @Override
        protected void paintBorder(Graphics g) {}
    }
}
