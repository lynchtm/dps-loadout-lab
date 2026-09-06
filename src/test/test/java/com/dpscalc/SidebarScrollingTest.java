package com.dpscalc;

import com.dpscalc.scenario.SidebarScrolling;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SidebarScrollingTest {
    @Test public void wheelPassesThroughEmptyNestedPaneAndContinuesAtBothListEdges() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            JPanel page=new JPanel(null);page.setPreferredSize(new Dimension(220,2000));
            JScrollPane inner=new JScrollPane(new JList<>(new String[]{"One"}));inner.setBounds(0,200,200,140);page.add(inner);
            JScrollPane outer=new JScrollPane(page);SidebarScrolling.configure(outer);SidebarScrolling.nested(inner);
            outer.setSize(240,400);layout(outer);outer.getVerticalScrollBar().setValue(500);
            wheel(inner,1);assertTrue("Empty list must not trap downward wheel",outer.getVerticalScrollBar().getValue()>500);
            wheel(inner,-1);assertEquals(500,outer.getVerticalScrollBar().getValue());
            String[] rows=new String[100];for(int i=0;i<rows.length;i++)rows[i]="Row "+i;
            inner.setViewportView(new JList<>(rows));layout(outer);outer.getVerticalScrollBar().setValue(500);
            wheel(inner,1);assertTrue(inner.getVerticalScrollBar().getValue()>0);assertEquals(500,outer.getVerticalScrollBar().getValue());
            inner.getVerticalScrollBar().setValue(inner.getVerticalScrollBar().getMaximum());wheel(inner,1);assertTrue(outer.getVerticalScrollBar().getValue()>500);
            outer.getVerticalScrollBar().setValue(500);inner.getVerticalScrollBar().setValue(0);wheel(inner,-1);assertTrue(outer.getVerticalScrollBar().getValue()<500);
        });
    }
    @Test public void verticalWheelOverHorizontalComparisonScrollsSidebar() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            JPanel page=new JPanel(null);page.setPreferredSize(new Dimension(220,2000));
            JTable table=new JTable(4,12);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            JScrollPane inner=new JScrollPane(table);inner.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);inner.setBounds(0,200,200,140);page.add(inner);
            JScrollPane outer=new JScrollPane(page);SidebarScrolling.configure(outer);SidebarScrolling.nested(inner);
            outer.setSize(240,400);layout(outer);outer.getVerticalScrollBar().setValue(500);
            wheel(inner,-1);assertTrue(outer.getVerticalScrollBar().getValue()<500);assertEquals(0,inner.getHorizontalScrollBar().getValue());
        });
    }
    private static void wheel(Component component,int direction){component.dispatchEvent(new MouseWheelEvent(component,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,30,30,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,3,direction));}
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
}
