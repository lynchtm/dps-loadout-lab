package com.dpscalc.scenario;

import java.awt.*;
import java.util.*;
import java.util.function.Supplier;
import javax.swing.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/** Read-only equipment figure with the same slot arrangement as the loadout editor. */
public final class LoadoutPreview extends JPanel {
    private static final int[] LAYOUT={-1,0,-1,1,2,13,3,4,5,-1,7,-1,9,10,12};
    private static final String[] ICONS={"head","cape","neck","weapon","body","shield","legs","hands","feet","ring","ammo"};
    private final Map<Integer,JLabel> slots=new LinkedHashMap<>();
    private final Supplier<ItemManager> items;
    public LoadoutPreview(Supplier<ItemManager> items){
        this.items=items;setLayout(new FlowLayout(FlowLayout.CENTER,0,4));setBackground(CalculatorWorkspace.BG);
        getAccessibleContext().setAccessibleName("Generated equipment preview");
        JPanel grid=new JPanel(new GridLayout(5,3,5,5));grid.setBackground(CalculatorWorkspace.BG);grid.setPreferredSize(new Dimension(154,240));
        for(int slot:LAYOUT){if(slot<0){JPanel blank=new JPanel();blank.setOpaque(false);grid.add(blank);continue;}
            JLabel label=new JLabel("",SwingConstants.CENTER);label.setOpaque(true);label.setBackground(CalculatorWorkspace.CARD);label.setForeground(CalculatorWorkspace.TEXT);
            label.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,9));label.setHorizontalTextPosition(SwingConstants.CENTER);label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setBorder(BorderFactory.createLineBorder(CalculatorWorkspace.EDGE));slots.put(slot,label);grid.add(label);
        }
        add(grid);
    }
    public void showLoadout(Scenario.Loadout loadout){
        for(int i=0;i<BankOptimizer.SLOTS.length;i++){
            int slot=BankOptimizer.SLOTS[i],id=loadout.player.getEquippedItemIds()[slot];JLabel label=slots.get(slot);
            String name=id>0?Objects.toString(loadout.player.getEquippedItemNames()[slot],"Item "+id):"Empty";
            String description=BankOptimizer.SLOT_NAMES[i]+": "+name;label.setToolTipText(description);label.getAccessibleContext().setAccessibleName("Suggested "+description);label.putClientProperty("itemId",id);
            label.setText("");label.setIcon(CalculatorWorkspace.icon("slots/"+ICONS[i],30));
            if(id>0){
                ItemManager manager=items.get();
                if(manager!=null){AsyncBufferedImage sprite=manager.getImage(id);label.setIcon(new ImageIcon(sprite));sprite.onLoaded(label::repaint);}
                else {label.setText(name.length()>9?name.substring(0,8)+"…":name);}
            }
        }
        repaint();
    }
    @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
}
