package com.dpscalc.scenario;

import java.awt.*;
import java.util.function.*;
import javax.swing.*;
import net.runelite.client.game.ItemManager;

/** Keeps the preview and error together, and permits retrying a failed create. */
final class BankLayoutPanel extends JPanel {
    private final JTextField name;
    private final JButton create;
    private final JTextArea status=CalculatorWorkspace.note("Choose a new name. Existing bank tabs will be kept.");
    private boolean busy,complete;
    BankLayoutPanel(Scenario.Loadout draft,BankLayoutPlan plan,Supplier<ItemManager> items,
                    BiConsumer<String,Consumer<BankLayoutService.Result>> writer){
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setBackground(CalculatorWorkspace.BG);
        add(CalculatorWorkspace.label("New bank tab name",CalculatorWorkspace.GOLD));
        name=new JTextField(BankLayoutPlan.suggestedName(draft.name),30);name.getAccessibleContext().setAccessibleName("New bank tab name");add(name);
        JPanel cells=new JPanel(new GridLayout(0,8,3,3));cells.setBackground(CalculatorWorkspace.BG);
        int last=plan.positions.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        for(int pos=0;pos<((last/8)+1)*8;pos++){
            Integer id=plan.positions.get(pos);JLabel cell=new JLabel("",SwingConstants.CENTER);cell.setForeground(CalculatorWorkspace.TEXT);cell.setPreferredSize(new Dimension(38,35));cell.setBorder(BorderFactory.createLineBorder(CalculatorWorkspace.EDGE));
            if(id!=null){cell.setToolTipText("Item "+id);ItemManager manager=items.get();if(manager!=null){net.runelite.client.util.AsyncBufferedImage image=manager.getImage(id);cell.setIcon(new ImageIcon(image));image.onLoaded(cell::repaint);}else cell.setText(id.toString());}cells.add(cell);
        }
        add(cells);JTextArea note=CalculatorWorkspace.note("Snapshot of "+draft.name+". Equipment left · inventory right · pouch below equipment · optional switches at the bottom. Quantities and withdrawals remain manual.");note.setColumns(38);note.setRows(4);add(note);
        create=CalculatorWorkspace.action("Create bank tab",()->{
            if(busy||complete)return;
            try{String tag=BankLayoutPlan.tagName(name.getText());busy=true;name.setEnabled(false);createState();status.setText("Creating bank tab…");
                writer.accept(tag,result->{busy=false;complete=result.success;name.setEnabled(!complete);status.setText(result.message);createState();});
            }catch(RuntimeException error){busy=false;name.setEnabled(true);status.setText(error.getMessage());createState();}
        });
        add(create);status.setRows(4);status.getAccessibleContext().setAccessibleName("Bank layout creation status");add(status);CalculatorWorkspace.theme(this);
    }
    private void createState(){create.setEnabled(!busy&&!complete);create.setText(complete?"Bank tab created":busy?"Creating…":"Create bank tab");revalidate();repaint();}
}
