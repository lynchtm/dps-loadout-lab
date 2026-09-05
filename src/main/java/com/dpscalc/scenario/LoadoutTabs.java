package com.dpscalc.scenario;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/** Compact numbered tabs, with native keyboard navigation and overflow scrolling. */
final class LoadoutTabs extends JTabbedPane {
    LoadoutTabs() {
        super(TOP, SCROLL_TAB_LAYOUT);
        getAccessibleContext().setAccessibleName("Comparison loadouts");
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        setBackground(CalculatorWorkspace.BG);setForeground(CalculatorWorkspace.TEXT);
        setUI(new BasicTabbedPaneUI() {
            protected int calculateTabWidth(int placement,int index,FontMetrics metrics){return 36;}
            protected int calculateTabHeight(int placement,int index,int height){return 30;}
            protected void paintTabBackground(Graphics g,int placement,int index,int x,int y,int w,int h,boolean selected){
                g.setColor(selected?CalculatorWorkspace.ORANGE:CalculatorWorkspace.CARD);g.fillRect(x,y,w,h);
                if(selected){g.setColor(CalculatorWorkspace.ORANGE);g.fillRect(x,y+h-3,w,3);}
            }
            protected void paintTabBorder(Graphics g,int p,int i,int x,int y,int w,int h,boolean selected){}
            protected void paintContentBorder(Graphics g,int placement,int selected){}
            protected void paintFocusIndicator(Graphics g,int p,Rectangle[] r,int i,Rectangle icon,Rectangle text,boolean selected){
                if(hasFocus()&&selected){g.setColor(CalculatorWorkspace.GOLD);g.drawRect(r[i].x+2,r[i].y+2,r[i].width-5,r[i].height-6);}
            }
        });
    }
    void bind(List<Scenario.Loadout> loadouts,int selected) {
        // Retain tab components on ordinary edits so focus and scrolling remain stable.
        while(getTabCount()>loadouts.size())removeTabAt(getTabCount()-1);
        while(getTabCount()<loadouts.size())addTab(String.valueOf(getTabCount()+1),new JPanel());
        for(int i=0;i<loadouts.size();i++){
            setToolTipTextAt(i,loadouts.get(i).name);
            getComponentAt(i).getAccessibleContext().setAccessibleName(loadouts.get(i).name);
        }
        setSelectedIndex(selected);
    }
    @Override public Dimension getPreferredSize(){return new Dimension(170,36);}
    @Override public Dimension getMinimumSize(){return new Dimension(50,36);}
    @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,36);}
}
