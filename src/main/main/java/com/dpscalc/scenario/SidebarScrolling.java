package com.dpscalc.scenario;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

/** Let nested lists scroll locally, then pass the wheel back to the sidebar at an edge. */
public final class SidebarScrolling {
    private SidebarScrolling() {}

    public static void configure(JScrollPane pane) {
        pane.setWheelScrollingEnabled(true);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        pane.getVerticalScrollBar().setBlockIncrement(180);
    }

    public static void nested(JScrollPane pane) {
        configure(pane);
        MouseWheelListener[] delegates=pane.getMouseWheelListeners();
        for(MouseWheelListener listener:delegates)pane.removeMouseWheelListener(listener);
        pane.addMouseWheelListener(event->{
            JScrollBar bar=event.isShiftDown()?pane.getHorizontalScrollBar():pane.getVerticalScrollBar();
            boolean enabled=event.isShiftDown()?pane.getHorizontalScrollBarPolicy()!=ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER:pane.getVerticalScrollBarPolicy()!=ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;
            boolean canScroll=enabled&&bar.isVisible()&&(event.getPreciseWheelRotation()<0?bar.getValue()>bar.getMinimum():bar.getValue()<bar.getMaximum()-bar.getVisibleAmount());
            if(canScroll){for(MouseWheelListener listener:delegates)listener.mouseWheelMoved(event);return;}
            JScrollPane parent=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,pane.getParent());
            if(parent==null){for(MouseWheelListener listener:delegates)listener.mouseWheelMoved(event);return;}
            Point point=SwingUtilities.convertPoint(pane,event.getPoint(),parent);
            parent.dispatchEvent(new MouseWheelEvent(parent,event.getID(),event.getWhen(),event.getModifiersEx(),point.x,point.y,event.getXOnScreen(),event.getYOnScreen(),event.getClickCount(),event.isPopupTrigger(),event.getScrollType(),event.getScrollAmount(),event.getWheelRotation(),event.getPreciseWheelRotation()));
            event.consume();
        });
    }

    public static void passiveText(JTextArea text) {
        // DefaultCaret otherwise schedules scrollRectToVisible on document updates,
        // pulling the whole sidebar down to refreshed result text after every edit.
        quietCaret(text);
        ((DefaultCaret)text.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }

    public static void quietCaret(JTextComponent text) {
        if(Boolean.TRUE.equals(text.getClientProperty("wikiDpsQuietCaret")))return;
        DefaultCaret caret=new DefaultCaret(){
            @Override protected void adjustVisibility(Rectangle rectangle){
                // JTextComponent.setText also moves the caret explicitly, even with
                // NEVER_UPDATE. Only real, focused text editing may reveal the caret.
                if(text.isEditable()&&text.isFocusOwner())super.adjustVisibility(rectangle);
            }
        };
        caret.setBlinkRate(text.getCaret().getBlinkRate());text.setCaret(caret);
        text.putClientProperty("wikiDpsQuietCaret",Boolean.TRUE);
    }
}
