package com.dpscalc;

import com.dpscalc.scenario.*;
import com.dpscalc.data.*;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScenarioPanelTest {
    @Test public void groupedEncounterControlsPersistAndDriveComparisonResults() throws Exception {
        MemoryStorage storage=new MemoryStorage();Scenario scenario=new Scenario();
        MonsterStats target=new MonsterStats();target.setId(-1);target.setName("Greater Nechryael");target.setSize(2);target.setSpeed(4);target.setHitpoints(205);target.setMagicLevel(1);target.setDefenceLevel(100);scenario.target=target;
        Scenario.Loadout staff=scenario.loadouts.get(0);staff.name="Powered staff";staff.player.getEquippedItemIds()[3]=11905;staff.player.setCombatStyle(com.dpscalc.state.CombatStyle.MAGIC_ACCURATE);
        Scenario.Loadout barrage=new Scenario.Loadout();barrage.name="Ice Barrage";barrage.player.getEquippedItemIds()[3]=4675;barrage.player.setCombatStyle(com.dpscalc.state.CombatStyle.MAGIC_AUTOCAST);barrage.player.setSpellName("Ice Barrage");barrage.player.setSpellbook("ancient");barrage.player.setSpellMaxHit(30);scenario.loadouts.add(barrage);
        storage.setConfiguration("wikiDpsScenarios","test-player","workspaceV1",Scenario.JSON.toJson(scenario));ScenarioPanel[] panel=new ScenarioPanel[1];
        SwingUtilities.invokeAndWait(()->{
            panel[0]=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            assertEquals("Single target",find(panel[0],JComboBox.class,c->"Encounter mode".equals(c.getAccessibleContext().getAccessibleName())).getSelectedItem());
            JButton hint=find(panel[0],JButton.class,b->"Use grouped encounter".equals(b.getText()));assertTrue(hint.isVisible());hint.doClick();
            find(panel[0],JSpinner.class,c->"Grouped target count".equals(c.getAccessibleContext().getAccessibleName())).setValue(6);
            assertTrue(saved(storage).encounter.grouped);assertEquals(6,saved(storage).encounter.targets);assertFalse(hint.isVisible());
        });
        try {
            boolean[] ready={false};for(int i=0;i<150&&!ready[0];i++){SwingUtilities.invokeAndWait(()->{JTable table=find(panel[0],JTable.class,t->"Loadout comparison".equals(t.getAccessibleContext().getAccessibleName()));ready[0]=table.getRowCount()==6&&table.getColumnCount()==2;});if(!ready[0])Thread.sleep(20);}
            assertTrue("Grouped comparison did not finish",ready[0]);
            SwingUtilities.invokeAndWait(()->{
                JTable table=find(panel[0],JTable.class,t->"Loadout comparison".equals(t.getAccessibleContext().getAccessibleName()));
                assertEquals(((Number)table.getValueAt(1,1)).doubleValue()*6,((Number)table.getValueAt(2,1)).doubleValue(),1e-12);assertEquals(table.getValueAt(1,0),table.getValueAt(2,0));
                EncounterPanel controls=find(panel[0],EncounterPanel.class,c->true);controls.setSize(213,220);layout(controls);
                BufferedImage image=new BufferedImage(213,220,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();controls.printAll(g);g.dispose();
                File file=new File("build/ui/group-encounter.png");file.getParentFile().mkdirs();try{javax.imageio.ImageIO.write(image,"png",file);}catch(java.io.IOException ex){throw new RuntimeException(ex);}
                find(panel[0],JComboBox.class,c->"Encounter mode".equals(c.getAccessibleContext().getAccessibleName())).setSelectedItem("Single target");assertFalse(saved(storage).encounter.grouped);assertEquals(6,saved(storage).encounter.targets);
            });
        }finally{SwingUtilities.invokeAndWait(panel[0]::dispose);}
    }
    private static class MemoryStorage implements ScenarioStorage {
        final Map<String,String> data=new HashMap<>();
        volatile String rsProfile="test-player";
        public long profileId(){return 1;}
        public String getRSProfileKey(){return rsProfile;}
        public String getConfiguration(String g,String p,String k){return data.get(g+"/"+p+"/"+k);}
        public void setConfiguration(String g,String p,String k,String v){data.put(g+"/"+p+"/"+k,v);}
    }
    @Test public void nativePanelRestoresSavedScenarioAndDisposesWorkers() throws Exception {
        MemoryStorage storage=new MemoryStorage();Scenario scenario=new Scenario();scenario.loadouts.get(0).name="Saved melee";
        storage.setConfiguration("wikiDpsScenarios","test-player","workspaceV1",Scenario.JSON.toJson(scenario));
        EquipmentPreparationFacade equipment=new EquipmentPreparationFacade();
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,equipment);
            try {
                assertTrue(hasButton(panel,"Equip selected item"));assertTrue(hasButton(panel,"Calculate all"));
                panel.setSize(225,1400);layout(panel);
                BufferedImage image=new BufferedImage(225,1400,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
                File output=new File("build/ui/scenario-panel.png");output.getParentFile().mkdirs();
                try{javax.imageio.ImageIO.write(image,"png",output);}catch(java.io.IOException ex){throw new RuntimeException(ex);}
            } finally {panel.dispose();}
            Scenario saved=Scenario.parse(storage.getConfiguration("wikiDpsScenarios","test-player","workspaceV1"));
            assertEquals("Saved melee",saved.loadouts.get(0).name);
        });
    }
    private static boolean hasButton(Container c,String text){for(Component child:c.getComponents()){if(child instanceof JButton&&((JButton)child).getText().equals(text))return true;if(child instanceof Container&&hasButton((Container)child,text))return true;}return false;}
    @Test public void equipmentSearchAndPrayerTogglesPersistCompatibleLoadouts() throws Exception {
        MemoryStorage storage=new MemoryStorage();
        ScenarioPanel[] holder=new ScenarioPanel[1];
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());holder[0]=panel;
            MonsterStats target=new MonsterStats();target.setId(-1);target.setName("Abyssal demon (test)");target.setSize(1);target.setSpeed(4);target.setHitpoints(150);target.setDefenceLevel(135);target.setMagicLevel(1);panel.acceptTarget(target);
            JTextField search=find(panel,JTextField.class,c->"Search equipment".equals(c.getAccessibleContext().getAccessibleName()));
            search.setText("Dragon defender");find(panel,JButton.class,b->"Equip selected item".equals(b.getText())).doClick();
            assertTrue(saved(storage).loadouts.get(0).player.getEquippedItemIds()[5]>0);
            search.setText("Twisted bow");find(panel,JButton.class,b->"Equip selected item".equals(b.getText())).doClick();
            assertEquals(-1,saved(storage).loadouts.get(0).player.getEquippedItemIds()[5]);
            assertTrue(saved(storage).loadouts.get(0).player.getEquippedItemIds()[3]>0);
            search.setText("Dragon defender");find(panel,JButton.class,b->"Equip selected item".equals(b.getText())).doClick();
            assertEquals(-1,saved(storage).loadouts.get(0).player.getEquippedItemIds()[3]);
            find(panel,JToggleButton.class,b->"Piety".equals(b.getAccessibleContext().getAccessibleName())).doClick();
            find(panel,JToggleButton.class,b->"Rigour".equals(b.getAccessibleContext().getAccessibleName())).doClick();
            assertEquals(Collections.singleton(com.dpscalc.state.Prayer.RIGOUR),saved(storage).loadouts.get(0).player.getActivePrayers());
            search.setText("Abyssal whip");find(panel,JButton.class,b->"Equip selected item".equals(b.getText())).doClick();
            find(panel,JToggleButton.class,b->"Piety".equals(b.getAccessibleContext().getAccessibleName())).doClick();
        });
        try {
            // Allow the analysis worker to publish its result before capturing populated tabs.
            for(int i=0;i<100;i++){
                boolean[] done={false};SwingUtilities.invokeAndWait(()->done[0]=find(holder[0],JTable.class,t->"Loadout comparison".equals(t.getAccessibleContext().getAccessibleName())).getColumnCount()>0);
                if(done[0])break;Thread.sleep(20);
            }
            SwingUtilities.invokeAndWait(()->{
                ScenarioPanel panel=holder[0];assertTrue(find(panel,JTable.class,t->"Loadout comparison".equals(t.getAccessibleContext().getAccessibleName())).getColumnCount()>0);
                JTabbedPane tabs=find(panel,JTabbedPane.class,t->"Loadout editor".equals(t.getAccessibleContext().getAccessibleName()));
                BufferedImage image=new BufferedImage(1125,2000,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                for(int i=0;i<5;i++){tabs.setSelectedIndex(i);panel.setSize(225,2000);layout(panel);Graphics2D cell=(Graphics2D)g.create(i*225,0,225,2000);panel.printAll(cell);cell.dispose();}
                g.dispose();File output=new File("build/ui/workspace-tabs.png");output.getParentFile().mkdirs();try{javax.imageio.ImageIO.write(image,"png",output);}catch(java.io.IOException ex){throw new RuntimeException(ex);}
            });
        } finally {SwingUtilities.invokeAndWait(()->holder[0].dispose());}
    }
    private static Scenario saved(MemoryStorage s){return Scenario.parse(s.getConfiguration("wikiDpsScenarios","test-player","workspaceV1"));}
    @Test public void iconTabsRespondAtTheirCentersAndEdges() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),new MemoryStorage(),new EquipmentPreparationFacade());
            try {
                JTabbedPane tabs=find(panel,JTabbedPane.class,t->"Loadout editor".equals(t.getAccessibleContext().getAccessibleName()));
                for(int i=0;i<5;i++)for(double fraction:new double[]{0.15,0.5,0.85}) {
                    tabs.setSelectedIndex((i+1)%5);panel.setSize(225,2600);layout(panel);
                    Rectangle bounds=tabs.getBoundsAt(i);
                    clickAt(tabs,bounds.x+(int)(bounds.width*fraction),bounds.y+bounds.height/2);
                    assertEquals("Click anywhere across tab "+i+" at "+fraction,i,tabs.getSelectedIndex());
                }
            } finally {panel.dispose();}
        });
    }
    private static void clickAt(Container root,int x,int y) {
        Component receiver=SwingUtilities.getDeepestComponentAt(root,x,y);
        // Match AWT's lightweight targeting: tooltip labels can receive mouse events
        // even when their containing tab also has a mouse handler.
        while(receiver!=root&&receiver.getMouseListeners().length==0)receiver=receiver.getParent();
        Point point=SwingUtilities.convertPoint(root,x,y,receiver);long time=System.currentTimeMillis();
        for(int id:new int[]{java.awt.event.MouseEvent.MOUSE_PRESSED,java.awt.event.MouseEvent.MOUSE_RELEASED,java.awt.event.MouseEvent.MOUSE_CLICKED})
            receiver.dispatchEvent(new java.awt.event.MouseEvent(receiver,id,time,0,point.x,point.y,1,false,java.awt.event.MouseEvent.BUTTON1));
    }
    @Test public void wikiEditorTabsAndTargetChangesStayIndependentAndFitSidebar() throws Exception {
        MemoryStorage storage=new MemoryStorage();
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            try {
                MonsterStats target=new MonsterStats();target.setId(-1);target.setName("Custom target");target.setSize(1);target.setSpeed(4);target.setHitpoints(150);panel.acceptTarget(target);
                JTabbedPane tabs=find(panel,JTabbedPane.class,t->"Loadout editor".equals(t.getAccessibleContext().getAccessibleName()));
                assertEquals(5,tabs.getTabCount());String[] names={"Combat","Skills","Equipment","Prayer","Settings"};
                for(int i=0;i<5;i++){assertEquals(names[i],tabs.getTitleAt(i));assertNotNull(tabs.getIconAt(i));}
                find(panel,JButton.class,b->"▸ Monster stats".equals(b.getText())).doClick();
                JSpinner defence=find(panel,JSpinner.class,c->"Target: Defence".equals(c.getAccessibleContext().getAccessibleName()));defence.setValue(200);
                JSpinner slash=find(panel,JSpinner.class,c->"Target: Slash defence".equals(c.getAccessibleContext().getAccessibleName()));slash.setValue(-20);
                find(panel,JButton.class,b->"▸ Defensive reductions".equals(b.getText())).doClick();
                find(panel,JSpinner.class,c->"Target: BGS damage".equals(c.getAccessibleContext().getAccessibleName())).setValue(30);
                assertEquals(200,saved(storage).target.getDefenceLevel());assertEquals(-20,saved(storage).target.getSlashDefence());
                assertEquals(30,saved(storage).target.getInputs().getDefenceReductions().getBgs());
                assertEquals(99,saved(storage).loadouts.get(0).player.getDefenceLevel());
                assertNotNull(find(panel,JButton.class,b->"▾ Defensive reductions".equals(b.getText())));
                find(panel,JButton.class,b->"▾ Defensive reductions".equals(b.getText())).doClick();
                for(int i=0;i<5;i++) {
                    tabs.setSelectedIndex(i);panel.setSize(225,2600);layout(panel);
                    assertTrue("All five tabs must stay on one row",tabs.getBoundsAt(4).x+tabs.getBoundsAt(4).width<=tabs.getWidth());
                    JButton targetHeading=find(panel,JButton.class,b->"▾ Target".equals(b.getText()));
                    assertTrue(SwingUtilities.convertPoint(tabs,0,tabs.getHeight(),panel).y<=SwingUtilities.convertPoint(targetHeading,0,0,panel).y);
                }
                tabs.setSelectedIndex(2);panel.setSize(225,2000);layout(panel);
                BufferedImage image=new BufferedImage(225,2000,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
                File output=new File("build/ui/wiki-target-editor.png");output.getParentFile().mkdirs();
                try{javax.imageio.ImageIO.write(image,"png",output);}catch(java.io.IOException ex){throw new RuntimeException(ex);}
            } finally {panel.dispose();}
        });
    }
    @Test public void equipmentBonusesAreAvailableBeforeChoosingTarget() {
        Scenario.Loadout loadout=new Scenario.Loadout();loadout.player.getEquippedItemIds()[3]=4151;
        ScenarioCalculator.Result result=new ScenarioCalculator(new EquipmentPreparationFacade()).calculate(loadout,null);
        assertEquals("Select or create a target",result.error);
        assertNotNull(result.equipmentStats);assertTrue(result.equipmentStats.getSlashAttack()>0);
        assertEquals(0,loadout.player.getEquipmentStats().getSlashAttack());
    }
    @Test public void prayerRefreshKeepsRuneLiteSidebarScrollPosition() throws Exception {
        MemoryStorage storage=new MemoryStorage();ScenarioPanel[] holder=new ScenarioPanel[1];JScrollPane[] scroll=new JScrollPane[1];int[] position={0};
        SwingUtilities.invokeAndWait(()->{
            holder[0]=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            JTabbedPane tabs=find(holder[0],JTabbedPane.class,t->"Loadout editor".equals(t.getAccessibleContext().getAccessibleName()));tabs.setSelectedIndex(3);
            JPanel wrapper=holder[0].getWrappedPanel();wrapper.setSize(242,550);layout(wrapper);
            scroll[0]=find(wrapper,JScrollPane.class,s->SwingUtilities.isDescendingFrom(holder[0],s));scroll[0].getVerticalScrollBar().setValue(400);position[0]=scroll[0].getVerticalScrollBar().getValue();assertTrue(position[0]>0);
            JToggleButton prayer=find(holder[0],JToggleButton.class,b->"Piety".equals(b.getAccessibleContext().getAccessibleName()));prayer.doClick();
            JTextArea note=find(holder[0],JTextArea.class,t->t.getText().contains("Select a target")||t.getText().contains("Choose a target"));
            assertEquals(javax.swing.text.DefaultCaret.NEVER_UPDATE,((javax.swing.text.DefaultCaret)note.getCaret()).getUpdatePolicy());
        });
        try{
            for(int i=0;i<100;i++){
                boolean[] ready={false};SwingUtilities.invokeAndWait(()->ready[0]=find(holder[0],JTable.class,t->"Loadout comparison".equals(t.getAccessibleContext().getAccessibleName())).getColumnCount()>0);
                if(ready[0])break;Thread.sleep(20);
            }
            SwingUtilities.invokeAndWait(()->{layout(holder[0].getWrappedPanel());assertEquals(position[0],scroll[0].getVerticalScrollBar().getValue());});
        }finally{SwingUtilities.invokeAndWait(()->holder[0].dispose());}
    }
    @Test public void directRenameUpdatesOnlySelectedDraftAndMonsterStatsStayCollapsed() throws Exception {
        MemoryStorage storage=new MemoryStorage();
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            try{
                find(panel,JButton.class,b->"Duplicate draft".equals(b.getText())).doClick();
                JTextField name=find(panel,JTextField.class,c->"Comparison loadout name".equals(c.getAccessibleContext().getAccessibleName()));name.setText("My boss setup");name.postActionEvent();
                assertEquals("My boss setup",saved(storage).loadouts.get(1).name);assertEquals("Loadout 1",saved(storage).loadouts.get(0).name);
                JTabbedPane tabs=find(panel,JTabbedPane.class,t->"Comparison loadouts".equals(t.getAccessibleContext().getAccessibleName()));assertEquals("My boss setup",tabs.getToolTipTextAt(1));
                tabs.setSelectedIndex(0);assertEquals("Loadout 1",name.getText());name.setText("   ");name.postActionEvent();assertEquals("Loadout 1",saved(storage).loadouts.get(0).name);
                JButton stats=find(panel,JButton.class,b->"▸ Monster stats".equals(b.getText()));stats.doClick();assertEquals("▾ Monster stats",stats.getText());
                find(panel,JButton.class,b->"Duplicate draft".equals(b.getText())).doClick();assertEquals("▾ Monster stats",stats.getText());stats.doClick();assertEquals("▸ Monster stats",stats.getText());
            }finally{panel.dispose();}
        });
    }
    @Test public void combatControlsUseIconsMultiSelectAndSeparateSections() throws Exception {
        MemoryStorage storage=new MemoryStorage();
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            try{
                JTabbedPane tabs=find(panel,JTabbedPane.class,t->"Loadout editor".equals(t.getAccessibleContext().getAccessibleName()));tabs.setSelectedIndex(1);
                JCheckBox attack=find(panel,JCheckBox.class,c->"Potion: Super attack".equals(c.getAccessibleContext().getAccessibleName()));
                JCheckBox strength=find(panel,JCheckBox.class,c->"Potion: Super strength".equals(c.getAccessibleContext().getAccessibleName()));attack.doClick();strength.doClick();
                assertEquals(2,saved(storage).loadouts.get(0).potions.size());assertTrue(attack.isSelected());assertTrue(strength.isSelected());
                JToggleButton piety=find(panel,JToggleButton.class,b->"Piety".equals(b.getAccessibleContext().getAccessibleName()));assertNotNull(piety.getIcon());assertEquals(4,((GridLayout)piety.getParent().getLayout()).getColumns());piety.doClick();assertTrue(piety.isSelected());
                assertTrue(find(panel,JCheckBox.class,b->"On a Slayer task".equals(b.getText())).isSelected());
                find(panel,JButton.class,b->"▾ Potions & boosts".equals(b.getText())).doClick();
                panel.setSize(225,3600);layout(panel);
                Component extras=tabs.getSelectedComponent();
                assertTrue("Combat controls must fit above Target",SwingUtilities.convertPoint(extras,0,extras.getHeight(),tabs).y<=tabs.getHeight());
                JButton target=find(panel,JButton.class,b->"▾ Target".equals(b.getText()));JButton results=find(panel,JButton.class,b->"▾ Results".equals(b.getText()));
                assertFalse(SwingUtilities.isDescendingFrom(target,tabs));assertFalse(SwingUtilities.isDescendingFrom(results,tabs));target.doClick();results.doClick();
                assertEquals("▸ Target",target.getText());assertEquals("▸ Results",results.getText());
                panel.setSize(225,2400);layout(panel);BufferedImage image=new BufferedImage(225,2400,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
                File output=new File("build/ui/combat-controls.png");output.getParentFile().mkdirs();try{javax.imageio.ImageIO.write(image,"png",output);}catch(java.io.IOException ex){throw new RuntimeException(ex);}
            }finally{panel.dispose();}
        });
    }
    @Test public void comparisonTabsSwitchIndependentLoadoutsAndSaveSelectedTab() throws Exception {
        MemoryStorage storage=new MemoryStorage();com.dpscalc.state.PlayerState player=Scenario.defaults();
        player.getEquippedItemIds()[3]=4151;player.getEquippedItemNames()[3]="Abyssal whip";
        DpsCalcPlugin plugin=new DpsCalcPlugin(){@Override public com.dpscalc.state.PlayerState getCachedPlayerState(){return player;}};
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(plugin,new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            try{
                JButton fromPlayer=find(panel,JButton.class,b->"New from current player".equals(b.getText()));fromPlayer.doClick();
                JTabbedPane tabs=find(panel,JTabbedPane.class,t->"Comparison loadouts".equals(t.getAccessibleContext().getAccessibleName()));
                assertEquals(2,tabs.getTabCount());assertEquals(1,tabs.getSelectedIndex());assertEquals(-1,saved(storage).loadouts.get(0).player.getWeaponId());
                tabs.setSelectedIndex(0);assertEquals(0,saved(storage).selected);
                find(panel,JButton.class,b->"Save as template".equals(b.getText())).doClick();
                JTextField name=find(panel,JTextField.class,c->"Template name".equals(c.getAccessibleContext().getAccessibleName()));assertTrue(name.getParent().isVisible());name.setText("Empty baseline");
                find(panel,JButton.class,b->"Save new template".equals(b.getText())).doClick();
                String library=storage.getConfiguration("wikiDpsScenarios","test-player",LoadoutLibrary.KEY);
                assertEquals(-1,LoadoutLibrary.parse(library).createDraft("Empty baseline").player.getWeaponId());
                tabs.setSelectedIndex(1);assertEquals(4151,saved(storage).loadouts.get(saved(storage).selected).player.getWeaponId());
                assertEquals(library,storage.getConfiguration("wikiDpsScenarios","test-player",LoadoutLibrary.KEY));
                for(int i=2;i<32;i++)fromPlayer.doClick();assertEquals(32,tabs.getTabCount());assertEquals(31,tabs.getSelectedIndex());
                tabs.setSelectedIndex(0);tabs.setSelectedIndex(31);assertEquals(31,saved(storage).selected);
                fromPlayer.doClick();assertEquals(32,tabs.getTabCount());
                panel.setSize(225,1500);layout(panel);BufferedImage image=new BufferedImage(225,1500,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
                File output=new File("build/ui/comparison-tabs.png");output.getParentFile().mkdirs();try{javax.imageio.ImageIO.write(image,"png",output);}catch(java.io.IOException ex){throw new RuntimeException(ex);}
                assertTrue(find(panel,JButton.class,b->"Templates ▸".equals(b.getText())).getY()<tabs.getParent().getY());
            }finally{panel.dispose();}
        });
    }
    @Test public void explicitLoadCurrentPlayerRestoresClearedWeapon() throws Exception {
        MemoryStorage storage=new MemoryStorage();
        com.dpscalc.state.PlayerState live=Scenario.defaults();
        live.getEquippedItemIds()[3]=4151;live.getEquippedItemNames()[3]="Abyssal whip";
        DpsCalcPlugin plugin=new DpsCalcPlugin(){@Override public com.dpscalc.state.PlayerState getCachedPlayerState(){return live;}};
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(plugin,new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            try{
                JButton load=find(panel,JButton.class,b->"Load current player".equals(b.getText()));load.doClick();
                assertEquals(4151,saved(storage).loadouts.get(0).player.getWeaponId());
                JButton weapon=find(panel,JButton.class,b->b.getToolTipText()!=null&&b.getToolTipText().startsWith("Weapon:"));
                weapon.dispatchEvent(new java.awt.event.MouseEvent(weapon,java.awt.event.MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,1,1,1,true,java.awt.event.MouseEvent.BUTTON3));
                Scenario cleared=saved(storage);assertEquals(-1,cleared.loadouts.get(0).player.getWeaponId());
                // Background synchronization continues to respect intentional edits.
                Scenario.mergeLive(cleared.loadouts.get(0),live);assertEquals(-1,cleared.loadouts.get(0).player.getWeaponId());
                find(panel,JButton.class,b->"Duplicate draft".equals(b.getText())).doClick();
                load.doClick();Scenario restored=saved(storage);
                assertEquals(4151,restored.loadouts.get(1).player.getWeaponId());
                assertEquals("Abyssal whip",restored.loadouts.get(1).player.getEquippedItemNames()[3]);
                assertFalse(restored.loadouts.get(1).overrides.contains("equippedItemIds.3"));
                assertEquals(-1,restored.loadouts.get(0).player.getWeaponId());
                assertEquals(4151,live.getWeaponId());
            }finally{panel.dispose();}
        });
    }
    @Test public void profileSwitchDoesNotWriteOldDraftIntoAnotherLibrary() throws Exception {
        MemoryStorage storage=new MemoryStorage();ScenarioPanel[] holder=new ScenarioPanel[1];
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());holder[0]=panel;
            find(panel,JTextField.class,c->"Template name".equals(c.getAccessibleContext().getAccessibleName())).setText("Private template");
            find(panel,JButton.class,b->"Save new template".equals(b.getText())).doClick();
            storage.rsProfile="other-player";
            find(panel,JButton.class,b->"Save new template".equals(b.getText())).doClick();
            assertNull(storage.getConfiguration("wikiDpsScenarios","other-player",LoadoutLibrary.KEY));
        });
        try{
            for(int i=0;i<100;i++){
                boolean[] reloaded={false};SwingUtilities.invokeAndWait(()->reloaded[0]=find(holder[0],JComboBox.class,c->"Loadout templates".equals(c.getAccessibleContext().getAccessibleName())).getItemCount()==3);
                if(reloaded[0])break;Thread.sleep(25);
            }
            SwingUtilities.invokeAndWait(()->{
                assertEquals(3,find(holder[0],JComboBox.class,c->"Loadout templates".equals(c.getAccessibleContext().getAccessibleName())).getItemCount());
                assertEquals(1,LoadoutLibrary.parse(storage.getConfiguration("wikiDpsScenarios","test-player",LoadoutLibrary.KEY)).names().size());
            });
        }finally{SwingUtilities.invokeAndWait(()->holder[0].dispose());}
    }
    @Test public void templateActionsKeepTargetAndDoNotSaveDraftEditsToLibrary() throws Exception {
        MemoryStorage storage=new MemoryStorage();
        SwingUtilities.invokeAndWait(()->{
            ScenarioPanel panel=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            try{
                find(panel,JButton.class,b->"Templates ▸".equals(b.getText())).doClick();
                MonsterStats target=new MonsterStats();target.setName("Comparison target");target.setId(-1);target.setSize(1);target.setHitpoints(100);target.setSpeed(4);panel.acceptTarget(target);
                find(panel,JButton.class,b->"Add to comparison".equals(b.getText())).doClick();
                assertEquals(2,saved(storage).loadouts.size());assertEquals(4151,saved(storage).loadouts.get(1).player.getWeaponId());
                find(panel,JTextField.class,c->"Template name".equals(c.getAccessibleContext().getAccessibleName())).setText("My melee");
                find(panel,JButton.class,b->"Save new template".equals(b.getText())).doClick();
                String library=storage.getConfiguration("wikiDpsScenarios","test-player",LoadoutLibrary.KEY);assertNotNull(library);
                find(panel,JButton.class,b->"Add to comparison".equals(b.getText())).doClick();
                assertEquals(3,saved(storage).loadouts.size());assertEquals("Comparison target",saved(storage).target.getName());
                JTextField search=find(panel,JTextField.class,c->"Search equipment".equals(c.getAccessibleContext().getAccessibleName()));search.setText("Twisted bow");
                find(panel,JButton.class,b->"Equip selected item".equals(b.getText())).doClick();
                assertEquals(library,storage.getConfiguration("wikiDpsScenarios","test-player",LoadoutLibrary.KEY));
                assertEquals(4151,saved(storage).loadouts.get(1).player.getWeaponId());
                assertNotEquals(4151,saved(storage).loadouts.get(2).player.getWeaponId());
                panel.setSize(225,1800);layout(panel);BufferedImage image=new BufferedImage(225,1800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
                File output=new File("build/ui/template-library.png");output.getParentFile().mkdirs();try{javax.imageio.ImageIO.write(image,"png",output);}catch(java.io.IOException ex){throw new RuntimeException(ex);}
            }finally{panel.dispose();}
            ScenarioPanel reopened=new ScenarioPanel(new DpsCalcPlugin(),new MonsterDataManager(),storage,new EquipmentPreparationFacade());
            try{assertEquals(3,saved(storage).loadouts.size());JComboBox<?> templates=find(reopened,JComboBox.class,c->"Loadout templates".equals(c.getAccessibleContext().getAccessibleName()));assertEquals(4,templates.getItemCount());}
            finally{reopened.dispose();}
        });
    }
    private static <T extends Component> T find(Container root,Class<T> type,java.util.function.Predicate<T> predicate){T result=findOptional(root,type,predicate);assertNotNull(result);return result;}
    private static <T extends Component> T findOptional(Container root,Class<T> type,java.util.function.Predicate<T> predicate){for(Component c:root.getComponents()){if(type.isInstance(c)&&predicate.test(type.cast(c)))return type.cast(c);if(c instanceof Container){T found=findOptional((Container)c,type,predicate);if(found!=null)return found;}}return null;}
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
}
