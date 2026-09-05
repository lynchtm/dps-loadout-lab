package com.dpscalc;

import com.dpscalc.scenario.*;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankOptimizerPanelTest {
    private static class Storage implements ScenarioStorage {
        final Map<String,String> data=new HashMap<>();
        public long profileId(){return 1;}public String getRSProfileKey(){return "player";}
        public String getConfiguration(String group,String profile,String key){return data.get(profile+key);}
        public void setConfiguration(String group,String profile,String key,String value){data.put(profile+key,value);}
    }
    private static Scenario scenario(){Scenario s=new Scenario();MonsterStats m=new MonsterStats();m.setId(-1);m.setName("Nechryael test");m.setSize(1);m.setSpeed(4);m.setHitpoints(100);m.setDefenceLevel(100);s.target=m;return s;}
    private static OwnedEquipment owned(long revision){return new OwnedEquipment("player",revision,System.currentTimeMillis(),Map.of(4151,1,1127,1),Map.of(),Map.of(),Map.of("attack",99,"defence",99));}
    @Test public void previewRequiresExplicitAddAndPreservesStartingDraft() throws Exception {
        Scenario scenario=scenario();String initial=Scenario.JSON.toJson(scenario);Storage storage=new Storage();java.util.List<Scenario.Loadout> added=new ArrayList<>();BankOptimizerPanel[] panel=new BankOptimizerPanel[1];
        SwingUtilities.invokeAndWait(()->{panel[0]=new BankOptimizerPanel(()->scenario,()->owned(1),()->"player",added::add,storage,new EquipmentPreparationFacade());button(panel[0],"Generate from bank ▸").doClick();button(panel[0],"Generate").doClick();});
        try {
            awaitResult(panel[0]);SwingUtilities.invokeAndWait(()->{
                assertEquals(initial,Scenario.JSON.toJson(scenario));assertTrue(added.isEmpty());assertFalse(button(panel[0],"Add to comparison").isEnabled());
                JCheckBox usable=find(panel[0],JCheckBox.class,c->c.getText().equals("I can use this setup"));usable.doClick();assertTrue(button(panel[0],"Add to comparison").isEnabled());
                LoadoutPreview preview=find(panel[0],LoadoutPreview.class,c->true);assertNotNull(preview);
                JLabel weapon=find(preview,JLabel.class,l->l.getAccessibleContext().getAccessibleName()!=null&&l.getAccessibleContext().getAccessibleName().startsWith("Suggested Weapon:"));
                assertEquals(4151,weapon.getClientProperty("itemId"));assertNotNull(weapon.getIcon());
                panel[0].setSize(213,1900);layout(panel[0]);BufferedImage image=new BufferedImage(213,1900,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel[0].printAll(g);g.dispose();File output=new File("build/ui/bank-optimizer.png");output.getParentFile().mkdirs();try{javax.imageio.ImageIO.write(image,"png",output);}catch(Exception ex){throw new RuntimeException(ex);}
                button(panel[0],"Add to comparison").doClick();assertEquals(1,added.size());assertTrue(added.get(0).name.startsWith("Bank melee"));assertEquals(4151,added.get(0).player.getWeaponId());assertEquals(initial,Scenario.JSON.toJson(scenario));
                assertNotNull(storage.data.get("playerbankSnapshotV1"));
            });
        }finally{SwingUtilities.invokeAndWait(panel[0]::dispose);}
    }
    @Test public void targetOwnershipAndProfileChangesInvalidatePreview() throws Exception {
        for(int change=0;change<6;change++){
            Scenario scenario=scenario();AtomicReference<OwnedEquipment> owned=new AtomicReference<>(owned(1));AtomicReference<String> profile=new AtomicReference<>("player");BankOptimizerPanel[] panel=new BankOptimizerPanel[1];
            SwingUtilities.invokeAndWait(()->{panel[0]=new BankOptimizerPanel(()->scenario,owned::get,profile::get,l->fail("Stale result inserted"),new Storage(),new EquipmentPreparationFacade());button(panel[0],"Generate").doClick();});
            try{awaitResult(panel[0]);final int kind=change;SwingUtilities.invokeAndWait(()->{
                if(kind==0)scenario.target.setDefenceLevel(300);if(kind==1)owned.set(owned(2));if(kind==2)profile.set("other");
                if(kind==3)find(panel[0],JComboBox.class,c->"Optimizer combat type".equals(c.getAccessibleContext().getAccessibleName())).setSelectedItem("Magic");
                if(kind==4)scenario.encounter.grouped=true;if(kind==5)scenario.encounter.targets=7;
                panel[0].refresh();assertFalse(button(panel[0],"Add to comparison").isEnabled());
                assertTrue(find(panel[0],JTextArea.class,t->t.getText().contains("Inputs changed")).getText().contains("Generate again"));
            });}finally{SwingUtilities.invokeAndWait(panel[0]::dispose);}
        }
    }
    @Test public void magicGeneratesAndAddsRecommendedSpellWithoutSpellInput() throws Exception {
        Scenario scenario=scenario();String initial=Scenario.JSON.toJson(scenario);BankOptimizerPanel[] panel=new BankOptimizerPanel[1];java.util.List<Scenario.Loadout> added=new ArrayList<>();
        OwnedEquipment bank=new OwnedEquipment("player",1,1,Map.of(4675,1),Map.of(),Map.of(),Map.of("attack",99,"magic",99));
        SwingUtilities.invokeAndWait(()->{
            panel[0]=new BankOptimizerPanel(()->scenario,()->bank,()->"player",added::add,new Storage(),new EquipmentPreparationFacade());
            button(panel[0],"Generate from bank ▸").doClick();
            find(panel[0],JComboBox.class,c->"Optimizer combat type".equals(c.getAccessibleContext().getAccessibleName())).setSelectedItem("Magic");
            assertNull(find(panel[0],JComboBox.class,c->"Optimizer spell".equals(c.getAccessibleContext().getAccessibleName())));
            button(panel[0],"Generate").doClick();
        });
        try{awaitResult(panel[0]);SwingUtilities.invokeAndWait(()->{
            assertNotNull(find(panel[0],JTextArea.class,c->c.getText().contains("Ice Barrage")&&c.getText().contains("ancient spellbook")));
            find(panel[0],JCheckBox.class,c->c.getText().equals("I can use this setup")).doClick();button(panel[0],"Add to comparison").doClick();
            assertEquals(1,added.size());assertEquals("Ice Barrage",added.get(0).player.getSpellName());assertEquals("ancient",added.get(0).player.getSpellbook());assertEquals(initial,Scenario.JSON.toJson(scenario));
        });}finally{SwingUtilities.invokeAndWait(panel[0]::dispose);}
    }
    @Test public void cancellationAndMissingScanDoNotCreateLoadouts() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            Scenario scenario=scenario();AtomicReference<OwnedEquipment> owned=new AtomicReference<>(OwnedEquipment.empty(0));BankOptimizerPanel panel=new BankOptimizerPanel(()->scenario,owned::get,()->"player",l->fail(),new Storage(),new EquipmentPreparationFacade());
            try{assertFalse(button(panel,"Generate").isEnabled());owned.set(owned(1));panel.refresh();button(panel,"Generate").doClick();button(panel,"Cancel").doClick();assertFalse(button(panel,"Add to comparison").isEnabled());assertFalse(button(panel,"Cancel").isEnabled());}finally{panel.dispose();}
        });
    }
    private static void awaitResult(BankOptimizerPanel panel) throws Exception {
        for(int i=0;i<150;i++){boolean[] ready={false};SwingUtilities.invokeAndWait(()->ready[0]=find(panel,JComboBox.class,c->"Generated alternatives".equals(c.getAccessibleContext().getAccessibleName())).getItemCount()>0);if(ready[0])return;Thread.sleep(20);}fail("Optimizer preview did not finish");
    }
    private static JButton button(Container parent,String text){return find(parent,JButton.class,b->text.equals(b.getText()));}
    private static <T extends Component>T find(Container parent,Class<T> type,Predicate<T> match){for(Component c:parent.getComponents()){if(type.isInstance(c)&&match.test(type.cast(c)))return type.cast(c);if(c instanceof Container){T found=find((Container)c,type,match);if(found!=null)return found;}}return null;}
    private static void layout(Container container){container.doLayout();for(Component c:container.getComponents())if(c instanceof Container)layout((Container)c);}
}
