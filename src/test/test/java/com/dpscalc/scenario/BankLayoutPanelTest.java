package com.dpscalc.scenario;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankLayoutPanelTest {
    @Test public void failedCreationStaysVisibleAndCanBeRetriedWithoutDuplicateSubmission()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            Scenario.Loadout draft=draft();AtomicInteger writes=new AtomicInteger();AtomicReference<Consumer<BankLayoutService.Result>> callback=new AtomicReference<>();
            BankLayoutPanel panel=new BankLayoutPanel(draft,BankLayoutPlan.from(draft),()->null,(name,done)->{writes.incrementAndGet();callback.set(done);});
            JButton create=find(panel,JButton.class);JTextField name=find(panel,JTextField.class);JTextArea status=status(panel);
            create.doClick();assertFalse(create.isEnabled());assertFalse(name.isEnabled());create.doClick();assertEquals(1,writes.get());
            callback.get().accept(new BankLayoutService.Result(false,"That bank tag already exists. Choose a new name."));
            assertTrue(create.isEnabled());assertTrue(name.isEnabled());assertTrue(status.getText().contains("already exists"));assertTrue(status.isVisible());
            name.setText("DPS retry");create.doClick();assertEquals(2,writes.get());callback.get().accept(new BankLayoutService.Result(true,"Created and opened bank tag: dps retry"));
            assertFalse(create.isEnabled());assertEquals("Bank tab created",create.getText());assertTrue(status.getText().startsWith("Created and opened"));
        });
    }
    @Test public void longWikiNamesHaveValidDefaultsAndValidationDoesNotDismissPreview()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            Scenario.Loadout draft=draft();draft.name="Tombs of Amascut / Strategies · Entry mode · Minimum melee equipment for a long encounter";AtomicInteger writes=new AtomicInteger();
            BankLayoutPanel panel=new BankLayoutPanel(draft,BankLayoutPlan.from(draft),()->null,(name,done)->writes.incrementAndGet());
            JTextField name=find(panel,JTextField.class);assertTrue(name.getText().length()<=60);BankLayoutPlan.tagName(name.getText());
            name.setText("invalid, name");find(panel,JButton.class).doClick();assertEquals(0,writes.get());assertTrue(status(panel).getText().contains("1–60"));assertTrue(find(panel,JButton.class).isEnabled());
            panel.setSize(380,650);layout(panel);BufferedImage image=new BufferedImage(380,650,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();File out=new File("build/ui/bank-layout-retry.png");out.getParentFile().mkdirs();try{javax.imageio.ImageIO.write(image,"png",out);}catch(java.io.IOException error){throw new IllegalStateException(error);}
        });
    }
    @Test public void synchronousProfileFailureIsShownAndAllowsRetry()throws Exception{
        SwingUtilities.invokeAndWait(()->{BankLayoutPanel panel=new BankLayoutPanel(draft(),BankLayoutPlan.from(draft()),()->null,(name,done)->{throw new IllegalArgumentException("Profile changed. Reopen preview.");});JButton create=find(panel,JButton.class);create.doClick();assertTrue(create.isEnabled());assertEquals("Profile changed. Reopen preview.",status(panel).getText());});
    }
    @Test public void uninitializedBankTabsAreRejectedBeforeAnySave(){
        BankLayoutWriter.requireLoaded(true,true,List.of("farm","slayer"),List.of("farm","slayer"));
        for(int state=0;state<4;state++)try{BankLayoutWriter.requireLoaded(state!=0,state!=1,List.of("farm","slayer"),state==2?List.of():state==3?List.of("farm"):List.of("farm","slayer"));fail("Unsafe state "+state);}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}
    }
    private static Scenario.Loadout draft(){Scenario.Loadout draft=new Scenario.Loadout();draft.player.getEquippedItemIds()[3]=4151;draft.carry.slots[0]=new CarryPlan.Entry(385,"Shark",1);return draft;}
    private static JTextArea status(Container root){for(Component child:root.getComponents())if(child instanceof JTextArea&&"Bank layout creation status".equals(child.getAccessibleContext().getAccessibleName()))return (JTextArea)child;throw new AssertionError("Missing status");}
    private static <T extends Component>T find(Container root,Class<T> type){for(Component child:root.getComponents()){if(type.isInstance(child))return type.cast(child);if(child instanceof Container){T result=find((Container)child,type);if(result!=null)return result;}}return null;}
    private static void layout(Container root){root.doLayout();for(Component child:root.getComponents())if(child instanceof Container)layout((Container)child);}
}
