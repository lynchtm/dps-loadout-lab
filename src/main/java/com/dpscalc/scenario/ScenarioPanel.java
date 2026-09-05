package com.dpscalc.scenario;

import com.dpscalc.*;
import com.dpscalc.data.*;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.state.*;
import com.google.gson.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public final class ScenarioPanel extends PluginPanel {
    private static final String GROUP="wikiDpsScenarios", KEY="workspaceV1";
    private final DpsCalcPlugin plugin;
    private final MonsterDataManager monsters;
    private final ScenarioStorage config;
    private final ScenarioCalculator calculator;
    private final EquipmentPreparationFacade equipment;
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>(),r->{Thread t=new Thread(r,"wiki-dps-analysis");t.setDaemon(true);return t;});
    private Future<?> pending;
    private long revision;
    private boolean binding, disposed;
    private String rsProfile;
    private long configProfile;
    private Scenario scenario = new Scenario();
    private LoadoutLibrary library = new LoadoutLibrary();
    private boolean libraryReady = true;
    private final JComboBox<String> templates = new JComboBox<>();
    private final JTextField templateName = new JTextField();
    private final JPanel libraryPanel = CalculatorWorkspace.column();
    private final LoadoutTabs loadouts = new LoadoutTabs();
    private final JTextField draftTitle = new JTextField("Loadout 1");
    private Scenario.Loadout nameOwner;
    private final JPanel saveTemplatePanel = CalculatorWorkspace.column();
    private final JButton libraryToggle = new JButton("Templates ▸");
    private final JLabel status = new JLabel("Select a target to calculate");
    private final JLabel targetName = new JLabel("No target selected");
    private final JCheckBox live = new JCheckBox("Auto-refresh player",false);
    private final FieldEditor playerFields=new FieldEditor(),targetFields=new FieldEditor(),conditionFields=new FieldEditor();
    private final JTextArea explanation=new JTextArea();
    private final DefaultTableModel resultModel=new DefaultTableModel(new String[]{"Loadout","DPS","Accuracy %","Max","E[damage]","Ticks","TTK s","Variance s²","Spec damage","Rotation DPS","Taken/s","Taken/kill","Prayer s","Status","Group DPS (estimate)","Grouped targets","Targets hit"},0){
        public boolean isCellEditable(int r,int c){return false;}
        public Class<?> getColumnClass(int c){return c==15||c==16?Integer.class:c==14||c>0&&c<13?Double.class:String.class;}
    };
    private final CalculatorWorkspace workspace;
    private final BankOptimizerPanel optimizer;
    private final PlotPanel plot=new PlotPanel();
    private final JComboBox<String> graph=new JComboBox<>(new String[]{"Hit distribution","DPS comparison","DPS vs defence (0–300)","DPS vs level (1–99)","DPS vs boost seconds (0–600)","Kill-time distribution","Damage taken"});
    private List<ScenarioCalculator.Result> results=Collections.emptyList();
    private List<double[]> graphData=Collections.emptyList();
    private final javax.swing.Timer timer;

    @Inject public ScenarioPanel(DpsCalcPlugin plugin,MonsterDataManager monsters,ConfigManager config,EquipmentPreparationFacade equipment) {
        this(plugin,monsters,ScenarioStorage.runeLite(config),equipment);
    }
    public ScenarioPanel(DpsCalcPlugin plugin,MonsterDataManager monsters,ScenarioStorage config,EquipmentPreparationFacade equipment) {
        this.plugin=plugin;this.monsters=monsters;this.config=config;this.calculator=new ScenarioCalculator(equipment);
        this.equipment=equipment;
        setLayout(new BorderLayout());setBackground(CalculatorWorkspace.BG);SidebarScrolling.configure(getScrollPane());
        JPanel content=CalculatorWorkspace.column();
        JLabel title=CalculatorWorkspace.label("DPS Loadout Lab",CalculatorWorkspace.GOLD);title.setFont(title.getFont().deriveFont(Font.BOLD,19));
        JButton menu=button("•••",()->{});JPopupMenu options=new JPopupMenu();
        menuItem(options,"Rename loadout…",()->{String n=JOptionPane.showInputDialog(this,"Loadout name",selected().name);if(n!=null&&!n.trim().isEmpty()){selected().name=n.trim();changed();}});
        menuItem(options,"Duplicate loadout",()->addLoadout(true));
        menuItem(options,"Remove loadout",()->{if(scenario.loadouts.size()==1)throw new IllegalArgumentException("Keep at least one loadout");scenario.loadouts.remove(scenario.selected);scenario.selected=Math.min(scenario.selected,scenario.loadouts.size()-1);changed();});
        menuItem(options,"Move loadout left",()->move(-1));menuItem(options,"Move loadout right",()->move(1));options.addSeparator();
        menuItem(options,"Reset from live player",this::loadLive);menuItem(options,"Advanced settings…",this::advanced);
        menuItem(options,"Save target preset",()->{if(scenario.target!=null){scenario.targets.put(scenario.target.getName()+" / "+scenario.target.getVersion(),scenario.target.copy());save();}});
        menuItem(options,"Restore target preset…",this::restoreTarget);
        menuItem(options,"Reset target to Wiki defaults",()->{if(scenario.target!=null)acceptTarget(monsters.getMonster(scenario.target.getId(),scenario.target.getVersion()));});options.addSeparator();
        menuItem(options,"Save scenario…",this::saveNamed);menuItem(options,"Open scenario…",this::restoreNamed);
        menuItem(options,"Copy scenario JSON",this::copyScenario);menuItem(options,"Import scenario…",this::importScenario);
        menuItem(options,"Copy results CSV",this::exportResults);menuItem(options,"Copy graph CSV",this::exportGraph);
        menu.addActionListener(e->options.show(menu,0,menu.getHeight()));
        JPanel heading=new JPanel(new BorderLayout());heading.setBackground(CalculatorWorkspace.BG);heading.add(title,BorderLayout.CENTER);heading.add(menu,BorderLayout.EAST);content.add(heading);content.add(Box.createVerticalStrut(8));
        libraryToggle.addActionListener(e->{libraryPanel.setVisible(!libraryPanel.isVisible());libraryToggle.setText(libraryPanel.isVisible()?"Templates ▾":"Templates ▸");revalidate();});
        content.add(libraryToggle);buildLibraryPanel();content.add(libraryPanel);
        content.add(Box.createVerticalStrut(10));
        JLabel calculatorTitle=CalculatorWorkspace.label("Loadout",CalculatorWorkspace.GOLD);content.add(calculatorTitle);
        JPanel loadoutRow=new JPanel(new BorderLayout(4,0));loadoutRow.setBackground(CalculatorWorkspace.BG);loadoutRow.add(loadouts,BorderLayout.CENTER);
        JButton add=button("+",()->{});add.setToolTipText("Start a new comparison loadout");
        JPopupMenu starts=new JPopupMenu();menuItem(starts,"From current player",this::addPlayerLoadout);
        menuItem(starts,"From template",()->{libraryPanel.setVisible(true);libraryToggle.setText("Templates ▾");templates.requestFocusInWindow();revalidate();});
        menuItem(starts,"Blank loadout",()->addLoadout(false));add.addActionListener(e->starts.show(add,0,add.getHeight()));loadoutRow.add(add,BorderLayout.EAST);content.add(loadoutRow);
        loadouts.addChangeListener(e->{if(!binding&&loadouts.getSelectedIndex()>=0){int next=loadouts.getSelectedIndex();commitLoadoutName();scenario.selected=next;saveTemplatePanel.setVisible(false);changed();}});
        draftTitle.setFont(draftTitle.getFont().deriveFont(Font.BOLD,13));draftTitle.getAccessibleContext().setAccessibleName("Comparison loadout name");
        draftTitle.setToolTipText("Rename this loadout — press Enter or click away to save");
        draftTitle.addActionListener(e->commitLoadoutName());
        draftTitle.addFocusListener(new java.awt.event.FocusAdapter(){public void focusLost(java.awt.event.FocusEvent e){commitLoadoutName();}});content.add(draftTitle);
        content.add(CalculatorWorkspace.pair(button("Duplicate draft",()->addLoadout(true)),button("Save as template",()->{
            saveTemplatePanel.setVisible(!saveTemplatePanel.isVisible());templateName.setText(selected().name);templateName.selectAll();templateName.requestFocusInWindow();revalidate();
        })));
        buildSaveTemplatePanel();content.add(saveTemplatePanel);
        JPanel moreLoadoutActions=CalculatorWorkspace.column();moreLoadoutActions.add(button("New from current player",this::addPlayerLoadout));
        JButton reload=button("Load current player",this::loadLive);reload.setToolTipText("Replace this tab with your current player; other tabs and saved templates stay unchanged");content.add(reload);live.setText("Keep live stats in sync");content.add(Box.createVerticalStrut(7));
        explanation.setEditable(false);explanation.setLineWrap(true);explanation.setWrapStyleWord(true);SidebarScrolling.passiveText(explanation);
        workspace=new CalculatorWorkspace(()->scenario,this::changed,this::message,this::acceptTarget,plugin,equipment,monsters,plot,graph,explanation,()->acceptTarget(plugin.getCurrentMonsterStats()),this::customTarget,this::advanced);
        JPanel loadoutSettings=(JPanel)workspace.tabs.getComponentAt(4);loadoutSettings.add(live);loadoutSettings.add(moreLoadoutActions);content.add(workspace);
        optimizer=new BankOptimizerPanel(()->scenario,plugin::getOwnedEquipment,()->{requireCurrentProfile();return rsProfile;},this::addGenerated,config,equipment,plugin::getItemManager);workspace.installOptimizer(optimizer);
        graph.addActionListener(e->{if(!binding)calculate();});content.add(button("Calculate all",this::calculate));
        status.setFont(status.getFont().deriveFont(10f));status.setForeground(CalculatorWorkspace.MUTED);content.add(status);
        JLabel coverage=CalculatorWorkspace.label("Reference data · coverage in Results",CalculatorWorkspace.MUTED);coverage.setToolTipText("Wiki engine b6bc098d. Current Wiki parity is incomplete; see calculation details and COVERAGE.md.");content.add(coverage);
        add(content,BorderLayout.NORTH);CalculatorWorkspace.theme(content);
        rsProfile=config.getRSProfileKey(); configProfile=config.profileId();
        String saved=config.getConfiguration(GROUP,rsProfile,KEY);
        if(saved!=null)try{scenario=Scenario.parse(saved);}catch(RuntimeException ex){message("Saved scenario could not be loaded: "+ex.getMessage());}
        loadLibrary();
        bind();
        timer=new javax.swing.Timer(1200,e->{if(!Objects.equals(rsProfile,config.getRSProfileKey()) || configProfile!=config.profileId()){rsProfile=config.getRSProfileKey();configProfile=config.profileId();String stored=config.getConfiguration(GROUP,rsProfile,KEY);try{scenario=stored==null?new Scenario():Scenario.parse(stored);}catch(RuntimeException ex){scenario=new Scenario();message("Profile scenario could not be loaded");}loadLibrary();live.setSelected(false);bind();calculate();}if(live.isSelected() && plugin.getCachedPlayerState()!=null){for(Scenario.Loadout l:scenario.loadouts)Scenario.mergeLive(l,plugin.getCachedPlayerState());bind();calculate();}});timer.start();
        calculate();
    }
    private void buildLibraryPanel() {
        libraryPanel.setBorder(BorderFactory.createEmptyBorder(7,0,7,0));
        libraryPanel.add(CalculatorWorkspace.label("LOADOUT TEMPLATES",CalculatorWorkspace.GOLD));
        libraryPanel.add(CalculatorWorkspace.note("Start with a copy. Your saved template stays unchanged."));
        templates.getAccessibleContext().setAccessibleName("Loadout templates");libraryPanel.add(templates);
        libraryPanel.add(button("Add to comparison",this::useTemplate));
        libraryPanel.add(CalculatorWorkspace.note("Starters use this draft's base levels. Saved templates include their saved stats and settings."));
        libraryPanel.add(CalculatorWorkspace.pair(button("Replace saved",()->{
            String name=selectedSavedTemplate();updateLibrary(next->next.replace(name,selected()));templates.setSelectedItem("Saved · "+name);message("Replaced saved template: "+name);
        }),button("Delete saved",()->{
            String name=selectedSavedTemplate();updateLibrary(next->next.remove(name));message("Deleted template; comparison drafts kept");
        })));
        libraryPanel.setVisible(false);
    }
    private void buildSaveTemplatePanel() {
        saveTemplatePanel.add(CalculatorWorkspace.label("SAVE AS TEMPLATE",CalculatorWorkspace.MUTED));
        templateName.getAccessibleContext().setAccessibleName("Template name");templateName.setToolTipText("Name for a new saved template");saveTemplatePanel.add(templateName);
        saveTemplatePanel.add(button("Save new template",()->{
            String name=templateName.getText().trim();
            updateLibrary(next->next.saveNew(name,selected()));templates.setSelectedItem("Saved · "+name);
            saveTemplatePanel.setVisible(false);revalidate();message("Saved template: "+name);
        }));
        saveTemplatePanel.setVisible(false);
    }
    private void addGenerated(Scenario.Loadout draft){
        requireCurrentProfile();if(scenario.loadouts.size()>=32)throw new IllegalArgumentException("Maximum 32 comparison loadouts");
        String base=draft.name;Set<String> names=new HashSet<>();for(Scenario.Loadout existing:scenario.loadouts)names.add(existing.name);int suffix=2;while(names.contains(draft.name))draft.name=base+" "+suffix++;
        scenario.loadouts.add(draft);scenario.selected=scenario.loadouts.size()-1;changed();workspace.tabs.setSelectedIndex(2);
    }
    private void addPlayerLoadout() {
        requireCurrentProfile();PlayerState player=plugin.getCachedPlayerState();
        if(player==null)throw new IllegalArgumentException("Log in to start a loadout from your player");
        if(scenario.loadouts.size()>=32)throw new IllegalArgumentException("Maximum 32 comparison loadouts");
        Scenario.Loadout draft=new Scenario.Loadout();draft.name="Current player";Scenario.mergeLive(draft,player);draft.inventory=plugin.getInventoryIds();
        scenario.loadouts.add(draft);scenario.selected=scenario.loadouts.size()-1;saveTemplatePanel.setVisible(false);changed();
    }
    private void requireCurrentProfile() {
        if(disposed || configProfile!=config.profileId() || !Objects.equals(rsProfile,config.getRSProfileKey()))
            throw new IllegalArgumentException("Profile changed. Wait for the workspace to reload.");
    }
    private void loadLibrary() {
        libraryReady=false;
        try { library=LoadoutLibrary.parse(config.getConfiguration(GROUP,rsProfile,LoadoutLibrary.KEY));libraryReady=true; }
        catch(RuntimeException ex){library=new LoadoutLibrary();message("Saved templates could not be loaded. Existing data has been kept.");}
        templateName.setText("");saveTemplatePanel.setVisible(false);refreshTemplates();
    }
    private void refreshTemplates() {
        Object previous=templates.getSelectedItem();templates.removeAllItems();
        for(String name:new String[]{"Starter · Whip melee","Starter · Crossbow ranged","Starter · Trident magic"})templates.addItem(name);
        for(String name:library.names())templates.addItem("Saved · "+name);
        if(previous!=null)for(int i=0;i<templates.getItemCount();i++)if(previous.equals(templates.getItemAt(i)))templates.setSelectedIndex(i);
    }
    private String selectedSavedTemplate() {
        requireCurrentProfile();int index=templates.getSelectedIndex()-3;
        if(index<0 || index>=library.names().size())throw new IllegalArgumentException("Choose a Saved template first. Starters cannot be changed.");
        return library.names().get(index);
    }
    private void updateLibrary(java.util.function.Consumer<LoadoutLibrary> update) {
        requireCurrentProfile();if(!libraryReady)throw new IllegalArgumentException("Library unavailable; existing saved data is protected.");
        LoadoutLibrary next=LoadoutLibrary.parse(library.toJson());update.accept(next);
        config.setConfiguration(GROUP,rsProfile,LoadoutLibrary.KEY,next.toJson());library=next;refreshTemplates();
    }
    private void useTemplate() {
        requireCurrentProfile();if(scenario.loadouts.size()>=32)throw new IllegalArgumentException("Maximum 32 comparison drafts");
        int index=templates.getSelectedIndex();
        Scenario.Loadout draft=index<3?LoadoutLibrary.starter(index,selected().player,equipment):library.createDraft(selectedSavedTemplate());
        String base=draft.name;Set<String> names=new HashSet<>();for(Scenario.Loadout l:scenario.loadouts)names.add(l.name);
        int suffix=2;while(names.contains(draft.name))draft.name=base+" "+suffix++;
        scenario.loadouts.add(draft);scenario.selected=scenario.loadouts.size()-1;changed();workspace.tabs.setSelectedIndex(2);
        message("Added independent comparison draft");
    }
    private void commitLoadoutName(){
        if(binding||disposed||nameOwner==null||!scenario.loadouts.contains(nameOwner))return;
        String name=draftTitle.getText().trim();
        if(name.equals(nameOwner.name))return;
        if(name.isEmpty()||name.length()>200){message("Use a loadout name between 1 and 200 characters");draftTitle.setText(nameOwner.name);return;}
        try{requireCurrentProfile();nameOwner.name=name;changed();}catch(IllegalArgumentException ex){message(ex.getMessage());}
    }
    private void menuItem(JPopupMenu menu,String text,Runnable action){JMenuItem item=new JMenuItem(text);item.addActionListener(e->{try{action.run();}catch(RuntimeException ex){message(ex.getMessage());}});menu.add(item);}
    private void addLoadout(boolean copy){if(scenario.loadouts.size()>=32)throw new IllegalArgumentException("Maximum 32 loadouts");Scenario.Loadout l=copy?selected().copy():new Scenario.Loadout();l.name=copy?l.name+" copy":"Loadout "+(scenario.loadouts.size()+1);scenario.loadouts.add(l);scenario.selected=scenario.loadouts.size()-1;changed();}
    private void advanced(){JTabbedPane tabs=new JTabbedPane();tabs.addTab("Player overrides",playerFields);tabs.addTab("Monster settings",targetFields);tabs.addTab("Extra conditions",conditionFields);tabs.setPreferredSize(new Dimension(600,500));JOptionPane.showMessageDialog(this,tabs,"Advanced scenario settings",JOptionPane.PLAIN_MESSAGE);}
    private Scenario.Loadout selected(){return scenario.loadouts.get(scenario.selected);}
    private void move(int delta){int n=scenario.selected+delta;if(n<0||n>=scenario.loadouts.size())return;Collections.swap(scenario.loadouts,n,scenario.selected);scenario.selected=n;changed();}
    private JPanel row(JComponent...components){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,2,2));for(JComponent c:components)p.add(c);return p;}
    private JButton button(String text,Runnable action){JButton b=new JButton(text);b.setMargin(new Insets(3,4,3,4));b.addActionListener(e->{try{action.run();}catch(RuntimeException ex){message(ex.getMessage());}});return b;}
    private void message(String text){status.setText(text);status.setToolTipText(text);}
    private void changed(){bind();save();calculate();}
    private void bind(){
        binding=true;try{loadouts.bind(scenario.loadouts,scenario.selected);}finally{binding=false;}
        if(nameOwner!=selected()||!draftTitle.isFocusOwner())draftTitle.setText(selected().name);nameOwner=selected();
        workspace.refresh();
        Scenario.Loadout l=selected();
        playerFields.bind(Scenario.JSON.toJsonTree(l.player).getAsJsonObject(),path->l.overrides.contains(path)?"Manual override":l.sources.getOrDefault(path,path.startsWith("equipmentStats")?"Inferred on calculation":"Unavailable / default"),(path,obj)->{
            Scenario.Loadout candidate=l.copy();candidate.player=Scenario.JSON.fromJson(obj,PlayerState.class);Scenario.validate(candidate);
            l.player=candidate.player;l.overrides.add(path);l.sources.put(path,"Manual override");
            if(path.startsWith("equipmentStats")||path.equals("weaponSpeed"))l.manualEquipmentStats=true;changed();
        },this::message,path->{l.overrides.remove(path);l.sources.remove(path);if(plugin.getCachedPlayerState()!=null)Scenario.mergeLive(l,plugin.getCachedPlayerState());changed();});
        JsonObject conditions=Scenario.JSON.toJsonTree(l).getAsJsonObject();for(String k:new String[]{"player","overrides","sources","name","inventory","potions"})conditions.remove(k);
        conditionFields.bind(conditions,path->"Manual scenario",(path,obj)->{JsonObject full=Scenario.JSON.toJsonTree(l).getAsJsonObject();for(Map.Entry<String,JsonElement> field:obj.entrySet())full.add(field.getKey(),field.getValue());Scenario.Loadout next=Scenario.JSON.fromJson(full,Scenario.Loadout.class);Scenario.validate(next);scenario.loadouts.set(scenario.selected,next);changed();},this::message,null);
        if(scenario.target!=null){targetName.setText(scenario.target.getName()+" / "+Objects.toString(scenario.target.getVersion(),"default"));targetFields.bind(Scenario.JSON.toJsonTree(scenario.target).getAsJsonObject(),path->scenario.targetSources.getOrDefault(path,"Wiki data / default"),(path,obj)->{MonsterStats next=Scenario.JSON.fromJson(obj,MonsterStats.class);Scenario.validate(next);scenario.target=next;scenario.targetSources.put(path,"Manual override");changed();},this::message,path->{MonsterStats base=monsters.getMonster(scenario.target.getId(),scenario.target.getVersion());if(base==null){message("No Wiki default for custom target");return;}JsonObject next=Scenario.JSON.toJsonTree(scenario.target).getAsJsonObject();JsonElement value=Scenario.flatten(Scenario.JSON.toJsonTree(base).getAsJsonObject()).get(path);if(value!=null){Scenario.put(next,path,value);scenario.target=Scenario.JSON.fromJson(next,MonsterStats.class);scenario.targetSources.remove(path);changed();}});}
    }
    private void loadLive(){requireCurrentProfile();PlayerState p=plugin.getCachedPlayerState();if(p==null)throw new IllegalArgumentException("Log in to load player state");selected().overrides.clear();selected().sources.clear();selected().manualEquipmentStats=false;Scenario.mergeLive(selected(),p);selected().inventory=plugin.getInventoryIds();changed();}
    public void acceptTarget(MonsterStats target){if(target==null){message("Target data unavailable. Find an NPC or create a custom definition.");return;}scenario.target=target.copy();scenario.targetSources.clear();changed();}
    private void customTarget(){MonsterStats m=new MonsterStats();m.setId(-1);m.setName("Custom target");m.setSize(1);m.setSpeed(4);m.setHitpoints(100);m.getInputs().setMonsterCurrentHp(100);m.setDefenceLevel(1);m.setMagicLevel(1);acceptTarget(m);scenario.targetSources.replaceAll((k,v)->"Manual");}
    private void restoreTarget(){String[] names=scenario.targets.keySet().toArray(new String[0]);String n=(String)JOptionPane.showInputDialog(this,"Saved target","Targets",JOptionPane.PLAIN_MESSAGE,null,names,names.length==0?null:names[0]);if(n!=null)acceptTarget(scenario.targets.get(n));}
    private void save(){if(!disposed && configProfile==config.profileId() && Objects.equals(rsProfile,config.getRSProfileKey()))config.setConfiguration(GROUP,rsProfile,KEY,Scenario.JSON.toJson(scenario));}
    private void saveNamed(){String n=JOptionPane.showInputDialog(this,"Scenario name",scenario.name);if(n==null||n.trim().isEmpty())return;scenario.name=n.trim();Map<String,String> saved=presets();saved.put(scenario.name,Scenario.JSON.toJson(scenario));config.setConfiguration(GROUP,rsProfile,"presetsV1",Scenario.JSON.toJson(saved));save();message("Saved "+n);}
    private Map<String,String> presets(){String raw=config.getConfiguration(GROUP,rsProfile,"presetsV1");Map<String,String> saved=new LinkedHashMap<>();if(raw!=null){JsonObject obj=new JsonParser().parse(raw).getAsJsonObject();for(Map.Entry<String,JsonElement> e:obj.entrySet())saved.put(e.getKey(),e.getValue().getAsString());}return saved;}
    private void restoreNamed(){Map<String,String> saved=presets();String[] names=saved.keySet().toArray(new String[0]);String name=(String)JOptionPane.showInputDialog(this,"Scenario","Restore",JOptionPane.PLAIN_MESSAGE,null,names,names.length==0?null:names[0]);if(name!=null){scenario=Scenario.parse(saved.get(name));changed();}}
    private void clipboard(String value){Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value),null);message("Copied to clipboard");}
    private void copyScenario(){clipboard(Scenario.JSON.toJson(scenario));}
    private void importScenario(){JTextArea input=new JTextArea(20,40);if(JOptionPane.showConfirmDialog(this,new JScrollPane(input),"Paste scenario JSON",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){Scenario imported=Scenario.parse(input.getText());scenario=imported;changed();}}
    private void exportResults(){StringBuilder csv=new StringBuilder();for(int c=0;c<resultModel.getColumnCount();c++)csv.append(c==0?"":",").append(resultModel.getColumnName(c));csv.append('\n');for(int r=0;r<resultModel.getRowCount();r++){for(int c=0;c<resultModel.getColumnCount();c++){if(c>0)csv.append(',');Object value=resultModel.getValueAt(r,c);csv.append('"').append(value==null?"":value.toString().replace("\"","\"\"")).append('"');}csv.append('\n');}clipboard(csv.toString());}
    private void exportGraph(){StringBuilder csv=new StringBuilder("series,index,value\n");for(int s=0;s<graphData.size();s++)for(int i=0;i<graphData.get(s).length;i++)csv.append(s+1).append(',').append(i).append(',').append(graphData.get(s)[i]).append('\n');clipboard(csv.toString());}
    private void calculate(){
        if(disposed)return;long request=++revision;if(pending!=null)pending.cancel(true);worker.getQueue().clear();
        Scenario snapshot=Scenario.JSON.fromJson(Scenario.JSON.toJson(scenario),Scenario.class);int graphIndex=graph.getSelectedIndex();message("Calculating…");
        pending=worker.submit(()->{List<ScenarioCalculator.Result> calculated=new ArrayList<>();for(Scenario.Loadout l:snapshot.loadouts){if(Thread.currentThread().isInterrupted())return;calculated.add(calculator.calculate(l,snapshot.target,snapshot.encounter));}
            List<double[]> plots=new ArrayList<>();String graphError=null;
            try{for(int n=0;n<calculated.size();n++){ScenarioCalculator.Result r=calculated.get(n);if(r.error!=null){plots.add(new double[0]);continue;}double[] data;
                if(graphIndex==0)data=r.histogram;
                else if(graphIndex==1)data=new double[]{r.normal.getDps(),r.normal.getDps()};
                else if(graphIndex==5)data=DistributionAnalysis.killDistribution(r.histogram,r.normal.getMonsterHp(),100);
                else if(graphIndex==6)data=r.damageTaken==null?new double[0]:new double[]{r.damageTaken,r.damageTaken};
                else {data=new double[31];for(int i=0;i<data.length;i++){if(Thread.currentThread().isInterrupted())return;Scenario.Loadout l=snapshot.loadouts.get(n).copy();MonsterStats m=snapshot.target.copy();if(graphIndex==2)m.setDefenceLevel(i*10);if(graphIndex==3){int level=1+i*98/30;l.player.setAttackLevel(level);l.player.setStrengthLevel(level);l.player.setRangedLevel(level);l.player.setMagicLevel(level);}if(graphIndex==4)l.elapsedSeconds=i*20;ScenarioCalculator.Result point=calculator.calculate(l,m);if(point.error!=null)throw new IllegalArgumentException(point.error);data[i]=point.normal.getDps();}}plots.add(data);}
            }catch(RuntimeException ex){graphError=ex.getMessage();plots.clear();}
            final String finalGraphError=graphError;SwingUtilities.invokeLater(()->{if(disposed||request!=revision)return;results=calculated;graphData=plots;render();plot.setSeries((String)graph.getSelectedItem()+(snapshot.encounter.grouped&&graphIndex!=6?" (per monster)":""),plots,snapshot.loadouts.stream().map(l->l.name).collect(java.util.stream.Collectors.toList()));message(finalGraphError==null?"Calculated · inspect result limits":finalGraphError);});
        });
    }
    private void render(){workspace.showResults(results);resultModel.setRowCount(0);for(ScenarioCalculator.Result r:results){if(r.error!=null){Object[] row=new Object[17];row[0]=r.name;row[13]=r.error;resultModel.addRow(row);continue;}resultModel.addRow(new Object[]{r.name,r.normal.getDps(),r.normal.getAccuracy()*100,(double)r.normal.getMaxHit(),r.normal.getExpectedDamage(),r.normal.getExpectedAttackSpeed(),r.ttk,r.ttkVariance,r.special==null?null:r.special.getExpectedDamage(),r.rotationDps,r.damageTaken,r.damageTaken==null||r.ttk==null?null:r.damageTaken*r.ttk,r.prayerSeconds,String.join(" ",r.warnings),r.groupDps,r.encounterTargets,r.targetsHit});}if(scenario.selected<results.size()){ScenarioCalculator.Result r=results.get(scenario.selected);explanation.setText(r.error==null?r.breakdown:r.error);explanation.setCaretPosition(0);}}
    public void dispose(){save();disposed=true;optimizer.dispose();revision++;timer.stop();if(pending!=null)pending.cancel(true);worker.shutdownNow();}
}
