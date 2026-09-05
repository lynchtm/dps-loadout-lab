package com.dpscalc.scenario;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public final class PlotPanel extends JPanel {
    private String title="No graph data";
    private List<double[]> series=Collections.emptyList();
    private List<String> labels=Collections.emptyList();
    public PlotPanel(){setPreferredSize(new Dimension(220,260)); setBackground(CalculatorWorkspace.BG);}
    public void setSeries(String title,List<double[]> series,List<String> labels){this.title=title;this.series=series;this.labels=labels;repaint();}
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,10));
            g.setColor(CalculatorWorkspace.MUTED);
            boolean probability=title.equals("Hit distribution")||title.equals("Kill-time distribution");
            int left=40,top=24,width=Math.max(1,getWidth()-52),height=Math.max(1,getHeight()-110);
            double max=series.stream().flatMapToDouble(Arrays::stream).filter(Double::isFinite).max().orElse(1); if(max<=0)max=1;
            int samples=series.stream().mapToInt(data->data.length).max().orElse(1);
            for(int tick=1;tick<=4;tick++){
                int y=top+height-tick*height/4;g.setColor(CalculatorWorkspace.EDGE);g.drawLine(left,y,left+width,y);
            }
            g.setColor(CalculatorWorkspace.MUTED);
            g.drawLine(left,top,left,top+height);g.drawLine(left,top+height,left+width,top+height);
            g.drawString(String.format(java.util.Locale.ROOT,probability?"%.0f%%":"%.2f",probability?max*100:max),2,top+8);
            g.drawString("0",24,top+height);
            if(probability){
                g.drawString("0",left,top+height+14);
                String end=String.valueOf(Math.max(0,samples-1));g.drawString(end,left+width-g.getFontMetrics().stringWidth(end),top+height+14);
                g.drawString(title.equals("Hit distribution")?"Damage":"Attacks to kill",left+width/2-20,top+height+28);
            }
            for(int s=0;s<series.size();s++) {
                g.setColor(s==0?new Color(237,129,52):s==1?CalculatorWorkspace.BEST:Color.getHSBColor((s*.21f)%1,.6f,.95f)); double[] data=series.get(s);
                for(int i=1;i<data.length;i++) {
                    if(!Double.isFinite(data[i-1])||!Double.isFinite(data[i]))continue;
                    g.drawLine(left+(i-1)*width/Math.max(1,samples-1),top+height-(int)(data[i-1]/max*height),left+i*width/Math.max(1,samples-1),top+height-(int)(data[i]/max*height));
                }
                if(s<4){String label=labels.get(s);while(label.length()>1&&g.getFontMetrics().stringWidth(label)>width)label=label.substring(0,label.length()-2)+"…";g.drawString(label,left,top+height+43+s*12);}
            }
        } finally {g.dispose();}
    }
}
