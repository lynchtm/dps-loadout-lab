package com.dpscalc.scenario;

import com.dpscalc.DpsCalcPlugin;
import com.dpscalc.data.MonsterDataManager;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.equipment.EquipmentCatalogItem;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.Prayer;
import com.google.gson.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;

/** Narrow, native calculator workspace. All edits affect the scenario, never the game. */
final class CalculatorWorkspace extends JPanel {
    static final Color BG = new Color(43, 36, 30);
    static final Color CARD = new Color(64, 55, 46);
    static final Color TEXT = new Color(244, 236, 222), MUTED = new Color(193, 180, 162);
    static final Color GOLD = new Color(226, 196, 143), EDGE = new Color(112, 97, 79);
    static final Color ORANGE = new Color(193, 65, 3), BEST = new Color(181, 220, 112);
    private final Map<String, JLabel> bonusValues = new LinkedHashMap<>();
    private JPanel itemResults, monsterResults;
    private final JPanel optimizerHost=column();
    void installOptimizer(JComponent optimizer){optimizerHost.add(optimizer);}
    private final JLabel editorTitle = label("Equipment", TEXT);
    private static final String[] SLOTS = {"head", "cape", "neck", "weapon", "body", "shield", "legs", "hands", "feet", "ring", "ammo"};
    private static final int[] SLOT_IDS = {0, 1, 2, 3, 4, 5, 7, 9, 10, 12, 13};
    private final Supplier<Scenario> model;
    private final Runnable changed;
    private final Consumer<String> message;
    private final Consumer<MonsterStats> targetChanged;
    private final DpsCalcPlugin plugin;
    private final EquipmentPreparationFacade catalog;
    private final MonsterDataManager monsters;
    private final List<Runnable> bindings = new ArrayList<>();
    private final Map<String, JButton> slots = new LinkedHashMap<>();
    private final JComboBox<String> slotFilter = new JComboBox<>();
    private final JComboBox<String> source = new JComboBox<>(new String[]{"All equipment", "Inventory", "Equipped", "Bank", "Drafts"});
    private final JTextField itemSearch = new HintField("Search for equipment…"), npcSearch = new HintField("Search for monster…");
    private final DefaultListModel<ItemChoice> itemModel = new DefaultListModel<>();
    private final JList<ItemChoice> itemList = new JList<>(itemModel);
    private final DefaultListModel<MonsterChoice> npcModel = new DefaultListModel<>();
    private final JList<MonsterChoice> npcList = new JList<>(npcModel);
    private final JLabel itemHint = label("Type a name to find equipment", MUTED);
    private final JLabel targetTitle = label("Choose a monster", GOLD);
    private final JLabel targetSubtitle = label("Search in the Target section", MUTED);
    private final JPanel targetStats = column();
    private final ComparisonTable comparisons;
    private final JTextArea resultNote = note("Choose a target to see results.");
    private final JTextArea encounterResultNote=note("");
    final JTabbedPane tabs = new JTabbedPane() {
        @Override public Dimension getPreferredSize(){
            Dimension size=super.getPreferredSize();
            if(getSelectedIndex()>=0)size.height=getSelectedComponent().getPreferredSize().height+48;
            return size;
        }
    };
    private boolean binding;

    CalculatorWorkspace(Supplier<Scenario> model, Runnable changed, Consumer<String> message,
                        Consumer<MonsterStats> targetChanged, DpsCalcPlugin plugin,
                        EquipmentPreparationFacade catalog, MonsterDataManager monsters,
                        JComponent plot, JComboBox<String> graph, JTextArea explanation,
                        Runnable liveTarget, Runnable customTarget, Runnable advanced) {
        this.model = model; this.changed = changed; this.message = message;
        this.targetChanged = targetChanged; this.plugin = plugin; this.catalog = catalog; this.monsters = monsters;
        comparisons=new ComparisonTable(index->{model.get().selected=index;changed.run();tabs.setSelectedIndex(2);});
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setBackground(BG);
        JPanel targetCard = column(); targetCard.setBorder(new EmptyBorder(8, 8, 8, 8)); targetCard.setBackground(CARD);
        targetTitle.setFont(targetTitle.getFont().deriveFont(Font.BOLD,14));targetCard.add(targetTitle); targetCard.add(targetSubtitle);
        targetCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        targetCard.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { npcSearch.requestFocusInWindow(); }});

        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            protected int calculateTabWidth(int placement, int index, FontMetrics metrics) { return 39; }
            protected int calculateTabHeight(int placement, int index, int height) { return 40; }
            protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h, boolean selected) { g.setColor(selected ? new Color(115, 96, 73) : index==getRolloverTab() ? new Color(86, 73, 59) : CARD); g.fillRect(x,y,w,h); if(selected){g.setColor(ORANGE);g.fillRect(x,y+h-3,w,3);} }
            protected void layoutLabel(int placement, FontMetrics metrics, int index, String title, Icon icon, Rectangle tab, Rectangle iconBounds, Rectangle textBounds, boolean selected) {
                iconBounds.setBounds(tab.x+(tab.width-icon.getIconWidth())/2,tab.y+(tab.height-icon.getIconHeight())/2,icon.getIconWidth(),icon.getIconHeight());
                textBounds.setBounds(0,0,0,0);
            }
            protected void paintText(Graphics g,int placement,Font font,FontMetrics metrics,int index,String title,Rectangle bounds,boolean selected) { }
            protected void paintTabBorder(Graphics g,int p,int i,int x,int y,int w,int h,boolean selected) { }
            protected void paintFocusIndicator(Graphics g,int p,Rectangle[] r,int i,Rectangle icon,Rectangle text,boolean selected) { if(tabs.hasFocus()&&selected){g.setColor(GOLD);g.drawRect(r[i].x+2,r[i].y+2,r[i].width-5,r[i].height-6);} }
            protected void paintContentBorder(Graphics g,int placement,int selected) { }
        });
        tabs.getAccessibleContext().setAccessibleName("Loadout editor");
        JPanel[] pages = {combat(), skills(), gear(), prayers(), settings(advanced)};
        String[] names = {"Combat", "Skills", "Equipment", "Prayer", "Settings"};
        String[] icons = {"combat", "skills", "equipment", "prayer", "options"};
        for (int i=0;i<pages.length;i++) {
            tabs.addTab(names[i], icon("tabs/"+icons[i], 27), pages[i], names[i]);
            // Paint icons directly through the tab UI. Child tooltip labels intercept
            // center clicks instead of letting the native tab handler select the page.
        }
        tabs.setSelectedIndex(2);
        tabs.addChangeListener(e->{
            // The active editor determines height; invalidate ancestor BoxLayout caches
            // immediately so a taller prayer book never gets clipped to Gear's height.
            for(Container parent=tabs;parent!=null;parent=parent.getParent())parent.invalidate();
            editorTitle.setText(tabs.getTitleAt(tabs.getSelectedIndex()));
            revalidate();
        });
        add(editorTitle);add(tabs);add(Box.createVerticalStrut(8));
        JPanel targetSection=column();targetSection.add(targetCard);targetSection.add(target(liveTarget,customTarget));
        add(fold("Target",targetSection,true));
        JPanel resultPage = column();
        resultPage.add(encounterResultNote);encounterResultNote.setVisible(false);
        resultPage.add(comparisons);
        resultPage.add(action("Expand comparison",()->{
            ComparisonTable wide=new ComparisonTable(null);wide.setColumnWidth(Math.max(100,650/Math.max(1,lastResults.size())));wide.setResults(lastResults,model.get().selected);wide.setPreferredSize(new Dimension(760,430));
            JOptionPane.showMessageDialog(this,wide,"Loadout comparison",JOptionPane.PLAIN_MESSAGE);
        }));
        JButton details = action("Calculation details ▸", () -> {
            JScrollPane scroll = new JScrollPane(explanation); scroll.setPreferredSize(new Dimension(540, 480));
            JOptionPane.showMessageDialog(this, scroll, "Calculation details & coverage", JOptionPane.PLAIN_MESSAGE);
        });
        JPanel detailsPage=column();detailsPage.add(resultNote);detailsPage.add(details);
        resultPage.add(fold("Calculation notes", detailsPage, false));
        add(Box.createVerticalStrut(8));add(fold("Results",resultPage,true));
        JPanel distribution=column();distribution.add(graph);distribution.add(plot);
        add(Box.createVerticalStrut(8));add(fold("Hit distribution",distribution,true));
        theme(this);
    }

    private JPanel gear() {
        JPanel page = column();
        JPanel grid = new JPanel(new GridLayout(5, 3, 5, 5)); grid.setBackground(BG);
        String[] layout = {"", "head", "", "cape", "neck", "ammo", "weapon", "body", "shield", "", "legs", "", "hands", "feet", "ring"};
        for (String slot : layout) {
            if (slot.isEmpty()) { JPanel blank = new JPanel(); blank.setOpaque(false); grid.add(blank); continue; }
            JButton b = action(pretty(slot), () -> { slotFilter.setSelectedItem(pretty(slot)); itemSearch.requestFocusInWindow(); });
            b.setPreferredSize(new Dimension(48, 44)); b.setHorizontalTextPosition(SwingConstants.CENTER);
            b.setVerticalTextPosition(SwingConstants.BOTTOM); b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            b.addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { if (SwingUtilities.isRightMouseButton(e)) { equip(slot, -1, null); changed.run(); } }});
            slots.put(slot, b); grid.add(b);
        }
        grid.setPreferredSize(new Dimension(154,240));
        JPanel equipmentFigure=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));equipmentFigure.setBackground(BG);equipmentFigure.add(grid);
        page.add(equipmentFigure); page.add(Box.createVerticalStrut(8));

        itemSearch.setToolTipText("Search equipment by name or item ID"); itemSearch.getAccessibleContext().setAccessibleName("Search equipment");
        page.add(itemSearch); slotFilter.addItem("All slots"); for (String slot : SLOTS) slotFilter.addItem(pretty(slot));
        itemResults=column();itemResults.add(pair(slotFilter, source)); itemResults.add(itemHint);
        itemList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                ItemChoice item = (ItemChoice) value;
                l.setText("<html>" + escape(item.facts.getName()) + "<br><font color='#9fa6b1'>" + escape(pretty(item.facts.getSlot()) + " · " + Objects.toString(item.facts.getVersion(), "") + " #" + item.id) + "</font></html>");
                l.setBorder(new EmptyBorder(6, 5, 6, 5)); return l;
            }
        });
        itemResults.add(scroll(itemList, 155));
        itemResults.add(action("Equip selected item", this::equipSelection));
        page.add(itemResults);itemResults.setVisible(false);
        page.add(optimizerHost);page.add(bonuses());
        activate(itemList, this::equipSelection);
        onText(itemSearch, this::filterItems); slotFilter.addActionListener(e -> filterItems()); source.addActionListener(e -> filterItems());
        return page;
    }

    private JPanel combat() {
        JPanel page = column(); page.add(label("ATTACK STYLE", MUTED));
        JComboBox<CombatStyle> styles = new JComboBox<>();styles.getAccessibleContext().setAccessibleName("Weapon attack style");
        styles.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof CombatStyle) { CombatStyle s = (CombatStyle) value; l.setText(s.getName() + " · " + s.getStance()); l.setToolTipText(pretty(s.getAttackType().toString())); } return l;
            }
        });
        styles.addActionListener(e -> { if (!binding&&styles.getSelectedItem()!=null) { WeaponStyles.select(current(),(CombatStyle)styles.getSelectedItem());changed.run(); }});
        bindings.add(() -> {
            WeaponStyles.normalize(current(),catalog);styles.removeAllItems();
            for(CombatStyle style:WeaponStyles.available(current().player,catalog))styles.addItem(style);
            styles.setEnabled(styles.getItemCount()>0);styles.setToolTipText(styles.getItemCount()==0?"Weapon styles unavailable in the reference catalog":"Styles available for this loadout's weapon");
            styles.setSelectedItem(current().player.getCombatStyle());
        });
        page.add(styles);
        JLabel spellLabel=label("SPELL",MUTED);page.add(spellLabel);
        JComboBox<String> spells = new JComboBox<>(); spells.addItem("Powered weapon / no spell");
        List<JsonObject> spellData = new ArrayList<>();
        try (InputStream stream = getClass().getResourceAsStream("/com/dpscalc/spells.json"); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            for (JsonElement element : new JsonParser().parse(reader).getAsJsonArray()) { JsonObject spell = element.getAsJsonObject(); spellData.add(spell); spells.addItem(spell.get("name").getAsString()); }
        } catch (IOException ex) { message.accept("Spell catalog unavailable"); }
        spells.addActionListener(e -> { if (!binding) { int index = spells.getSelectedIndex() - 1; PlayerState p = current().player;
            if (index < 0) { p.setSpellName(null); p.setSpellElement(null); p.setSpellMaxHit(0); }
            else { JsonObject spell = spellData.get(index); p.setSpellName(spell.get("name").getAsString()); p.setSpellbook(spell.get("spellbook").getAsString()); p.setSpellElement(spell.get("element").isJsonNull() ? null : spell.get("element").getAsString()); p.setSpellMaxHit(spell.get("max_hit").getAsInt()); for(CombatStyle option:WeaponStyles.available(p,catalog))if(option.getStance().equals("Autocast")){WeaponStyles.select(current(),option);break;} for (String path : Scenario.flatten(Scenario.JSON.toJsonTree(p).getAsJsonObject()).keySet()) if (path.startsWith("combatStyle.")) current().overrides.add(path); }
            current().overrides.addAll(Arrays.asList("spellName", "spellbook", "spellElement", "spellMaxHit")); changed.run(); }});
        bindings.add(() -> {spells.setSelectedItem(current().player.getSpellName() == null ? "Powered weapon / no spell" : current().player.getSpellName());boolean casting=WeaponStyles.available(current().player,catalog).stream().anyMatch(style->style.getStance().equals("Autocast"));spells.setVisible(casting);spellLabel.setVisible(casting);}); page.add(spells);
        return page;
    }

    private JPanel skills() {
        JPanel page=column();
        JPanel skills = column(); skills.add(pair(label("Skill", MUTED), label("Base / boost", MUTED)));
        for (String skill : new String[]{"attack", "strength", "defence", "ranged", "magic", "prayer", "hitpoints"}) {
            String base = skill + "Level", boosted = skill + "Boost";
            Map<String, JsonElement> fields = Scenario.flatten(Scenario.JSON.toJsonTree(current().player).getAsJsonObject());
            JComponent input = fields.containsKey(boosted) ? pair(number(base, false, 1, 126), number(boosted, false, -126, 126)) : number(base, false, 1, 126);
            skills.add(pair(statLabel(pretty(skill), skill.equals("prayer") ? "tabs/prayer" : "bonuses/"+skill), input));
        }
        page.add(skills);
        page.add(potions());
        return page;
    }

    private JPanel prayers() {
        JPanel page=column();
        JPanel prayers = new JPanel(new GridLayout(0, 4, 5, 5)); prayers.setBackground(BG);
        for (Prayer prayer : prayerOrder()) {
            JToggleButton toggle = new JToggleButton();toggle.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI(){protected void paintButtonPressed(Graphics g,AbstractButton b){g.setColor(new Color(107,79,35));g.fillRect(0,0,b.getWidth(),b.getHeight());}});toggle.getAccessibleContext().setAccessibleName(pretty(prayer.name()));toggle.setToolTipText(pretty(prayer.name()));
            toggle.setIcon(new ImageIcon(getClass().getResource("/com/dpscalc/prayers/"+prayer.name()+".png")));toggle.setPreferredSize(new Dimension(42,42));toggle.setMargin(new Insets(4,4,4,4));
            toggle.addActionListener(e -> { if (binding) return; Set<Prayer> active = current().player.getActivePrayers();
                if (!toggle.isSelected()) active.remove(prayer); else { active.removeIf(p -> conflicts(p, prayer)); active.add(prayer); }
                current().overrides.add("activePrayers"); changed.run(); });
            bindings.add(() -> { toggle.setSelected(current().player.getActivePrayers().contains(prayer)); toggle.setBackground(toggle.isSelected() ? new Color(107, 79, 35) : CARD); }); prayers.add(toggle);
        }
        page.add(prayers);
        page.add(note("Click to toggle prayers. Incompatible prayers are replaced automatically."));
        return page;
    }

    private JPanel potions() {
        JPanel page=column();
        JPanel boosts = column();boosts.add(note("Select any combination. The strongest boost per stat applies."));
        for(String potion:PotionSelection.OPTIONS){
            JCheckBox box=new JCheckBox(potion);box.getAccessibleContext().setAccessibleName("Potion: "+potion);
            box.addActionListener(e->{if(!binding){PotionSelection.toggle(current(),potion);changed.run();}});
            bindings.add(()->box.setSelected(PotionSelection.selected(current()).contains(potion)));boosts.add(box);
        }
        boosts.add(pair(label("Elapsed seconds", TEXT), number("elapsedSeconds", true, 0, 36000)));
        boosts.add(check("Divine boost", "divine", true)); page.add(fold("Potions & boosts", boosts, true));
        return page;
    }

    private JPanel settings(Runnable advanced) {
        JPanel page=column();
        JPanel extras = column(); extras.add(check("On a Slayer task", "onSlayerTask", false)); extras.add(check("In the Wilderness", "inWilderness", false)); extras.add(check("Use special attack", "specialAttack", true));
        extras.add(pair(label("Blowpipe dart ID", TEXT), number("blowpipeDartId", true, 0, 100000)));
        page.add(extras);page.add(action("Advanced settings…", advanced)); return page;
    }

    private static List<Prayer> prayerOrder(){
        try(Reader reader=new InputStreamReader(CalculatorWorkspace.class.getResourceAsStream("/com/dpscalc/prayer-order.json"),StandardCharsets.UTF_8)){
            List<Prayer> order=new ArrayList<>();for(JsonElement value:new JsonParser().parse(reader).getAsJsonArray())order.add(Prayer.valueOf(value.getAsString()));return order;
        }catch(IOException ex){throw new IllegalStateException("Prayer book unavailable",ex);}
    }

    private JPanel target(Runnable liveTarget, Runnable customTarget) {
        JPanel page = column();
        npcSearch.getAccessibleContext().setAccessibleName("Search monsters"); npcSearch.setToolTipText("Search monsters by name or NPC ID"); page.add(npcSearch);
        npcList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus); MonsterStats m = ((MonsterChoice) value).monster;
                l.setText("<html>" + escape(m.getName()) + "<br><font color='#9fa6b1'>" + escape(Objects.toString(m.getVersion(), "Standard") + " · #" + m.getId()) + "</font></html>"); l.setBorder(new EmptyBorder(6, 5, 6, 5)); return l;
            }
        });
        monsterResults=column();monsterResults.add(scroll(npcList, 160));page.add(monsterResults);monsterResults.setVisible(false); Runnable select = () -> { MonsterChoice c = npcList.getSelectedValue(); if (c != null) { targetChanged.accept(c.monster);npcSearch.setText(""); } };
        monsterResults.add(action("Use selected monster", select)); activate(npcList, select); onText(npcSearch, this::filterMonsters);
        page.add(pair(action("Current target", liveTarget), action("Custom…", customTarget)));
        EncounterPanel encounter=new EncounterPanel(model,changed);page.add(Box.createVerticalStrut(8));page.add(encounter);bindings.add(encounter::refresh);
        page.add(Box.createVerticalStrut(8));buildTargetStats(); page.add(fold("Monster stats",targetStats,false)); return page;
    }

    void refresh() {
        binding = true;
        try {
            for (Runnable bind : bindings) bind.run();
            for (Map.Entry<String, JButton> entry : slots.entrySet()) {
                String slot = entry.getKey(); JButton b = entry.getValue(); int id = current().player.getEquippedItemIds()[slotIndex(slot)];
                String name = current().player.getEquippedItemNames()[slotIndex(slot)];
                EquipmentCatalogItem facts = itemFacts(id); if (facts != null) name = facts.getName();
                b.setIcon(id < 0 ? icon("slots/"+slot, 30) : null); b.setText(id < 0 ? "" : "<html><center>" + escape(shortName(Objects.toString(name, "Item " + id))) + "</center></html>");
                b.setToolTipText(pretty(slot) + ": " + (id < 0 ? "Empty — click to choose" : name + " — right-click to clear"));
                b.setBorder(BorderFactory.createLineBorder(id < 0 ? EDGE : new Color(127, 105,  60)));
                if (id >= 0 && plugin.getItemManager() != null) { net.runelite.client.util.AsyncBufferedImage icon = plugin.getItemManager().getImage(id); b.setIcon(new ImageIcon(icon)); icon.onLoaded(b::repaint); b.setText(""); }
            }
            MonsterStats target = model.get().target;
            targetTitle.setText(target == null ? "Choose a monster" : target.getName());
            targetTitle.setToolTipText(targetTitle.getText());
            targetSubtitle.setText(target == null ? "Search in the Target section" : Objects.toString(target.getVersion(), "Standard") + " · " + target.getHitpoints() + " HP");
            theme(targetStats);
        } finally { binding = false; }
        revalidate(); repaint();
    }

    private List<ScenarioCalculator.Result> lastResults=Collections.emptyList();
    void showResults(List<ScenarioCalculator.Result> results) {
        lastResults=new ArrayList<>(results);comparisons.setResults(results,model.get().selected);
        Encounter encounter=model.get().encounter;encounterResultNote.setVisible(encounter.grouped);
        if(encounter.grouped){encounterResultNote.setText("Group DPS estimate · "+encounter.targets+" identical targets. Other metrics and graphs are per monster. Burst/barrage hits the group; other attacks count once.");encounterResultNote.setRows(6);}
        if (model.get().selected < results.size()) {
            ScenarioCalculator.Result r = results.get(model.get().selected);
            String notice = r.error == null ? String.join("\n\n", r.warnings) : r.error; resultNote.setText(notice); resultNote.setRows(Math.min(8, Math.max(1,(notice.length()+29)/30)));
        }
        if (model.get().selected < results.size()) {
            com.dpscalc.state.EquipmentStats stats=results.get(model.get().selected).equipmentStats;
            JsonObject values=stats==null?new JsonObject():Scenario.JSON.toJsonTree(stats).getAsJsonObject();
            bonusValues.forEach((key,label)->label.setText(values.has(key)?values.get(key).getAsString():"—"));
        }
        theme(comparisons); revalidate(); repaint();
    }

    private void filterItems() {
        itemModel.clear(); String query = itemSearch.getText().trim().toLowerCase(Locale.ROOT);
        itemResults.setVisible(!query.isEmpty());resizeEditors();
        if (query.isEmpty()) { itemHint.setText("Type a name to find equipment"); return; }
        String slot = slotFilter.getSelectedIndex() <= 0 ? null : SLOTS[slotFilter.getSelectedIndex() - 1];
        Set<Integer> owned = new HashSet<>(); int src = source.getSelectedIndex();
        if (src == 1) for (int id : plugin.getInventoryIds()) owned.add(id);
        if (src == 2 && plugin.getCachedPlayerState() != null) for (int id : plugin.getCachedPlayerState().getEquippedItemIds()) owned.add(id);
        if (src == 3) for (int id : plugin.getBankIds()) owned.add(id);
        if (src == 4) for (Scenario.Loadout loadout : model.get().loadouts) for (int id : loadout.player.getEquippedItemIds()) owned.add(id);
        catalog.getItems().entrySet().stream().filter(e -> src == 0 || owned.contains(e.getKey())).filter(e -> slot == null || slot.equals(e.getValue().getSlot()))
            .filter(e -> (e.getValue().getName() + " " + e.getKey()).toLowerCase(Locale.ROOT).contains(query))
            .sorted(Comparator.comparing(e -> e.getValue().getName())).limit(100).forEach(e -> itemModel.addElement(new ItemChoice(e.getKey(), e.getValue())));
        itemHint.setText(itemModel.isEmpty() ? "No matches — try another name" : itemModel.size() + (itemModel.size() == 100 ? "+ matches · refine search" : " matches · double-click to equip"));
        if (!itemModel.isEmpty()) itemList.setSelectedIndex(0);
    }
    private void filterMonsters() {
        npcModel.clear(); String q = npcSearch.getText().trim().toLowerCase(Locale.ROOT); monsterResults.setVisible(!q.isEmpty());resizeEditors();if (q.isEmpty()) return;
        monsters.getAllMonsters().stream().filter(m -> (m.getName() + " " + m.getId()).toLowerCase(Locale.ROOT).contains(q))
            .sorted(Comparator.comparing(MonsterStats::getName).thenComparing(m -> Objects.toString(m.getVersion(), ""))).limit(100).forEach(m -> npcModel.addElement(new MonsterChoice(m)));
        if (!npcModel.isEmpty()) npcList.setSelectedIndex(0);
    }
    private EquipmentCatalogItem itemFacts(int id) { try { return catalog.getItemFacts(id); } catch (IllegalArgumentException ex) { return null; } }
    private void equipSelection() {
        ItemChoice item = itemList.getSelectedValue(); if (item == null) return;
        if (item.facts.isTwoHanded()) equip("shield", -1, null);
        EquipmentCatalogItem weapon = itemFacts(current().player.getEquippedItemIds()[3]);
        if ("shield".equals(item.facts.getSlot()) && weapon != null && weapon.isTwoHanded()) equip("weapon", -1, null);
        equip(item.facts.getSlot(), item.id, item.facts.getName()); changed.run(); itemSearch.setText(""); message.accept("Equipped " + item.facts.getName() + " in this loadout");
    }
    private void equip(String slot, int id, String name) {
        int index = slotIndex(slot); if (index < 0) return;
        current().player.getEquippedItemIds()[index] = id; current().player.getEquippedItemNames()[index] = name;
        current().overrides.add("equippedItemIds." + index); current().overrides.add("equippedItemNames." + index);
        current().player.setRawEquipmentLoadout(null); current().manualEquipmentStats = false;
    }
    private JSpinner number(String path, boolean condition, int min, int max) {
        JSpinner spin = spinner(min, min, max);
        bindings.add(() -> { JsonElement value = Scenario.flatten(Scenario.JSON.toJsonTree(condition ? current() : current().player).getAsJsonObject()).get(path); if (value != null) spin.setValue(value.getAsInt()); spin.setToolTipText(current().overrides.contains(path) ? "Manual override" : "Loaded / default value"); });
        spin.addChangeListener(e -> { if (!binding) edit(path, new JsonPrimitive((Number) spin.getValue()), condition); }); return spin;
    }
    private JCheckBox check(String text, String path, boolean condition) {
        JCheckBox box = new JCheckBox(text); bindings.add(() -> { JsonElement value = Scenario.flatten(Scenario.JSON.toJsonTree(condition ? current() : current().player).getAsJsonObject()).get(path); box.setSelected(value != null && value.getAsBoolean()); });
        box.addActionListener(e -> { if (!binding) edit(path, new JsonPrimitive(box.isSelected()), condition); }); return box;
    }
    private void edit(String path, JsonElement value, boolean condition) {
        Scenario.Loadout candidate = current().copy(); JsonObject obj = Scenario.JSON.toJsonTree(condition ? candidate : candidate.player).getAsJsonObject(); Scenario.put(obj, path, value);
        if (condition) candidate = Scenario.JSON.fromJson(obj, Scenario.Loadout.class); else { candidate.player = Scenario.JSON.fromJson(obj, PlayerState.class); candidate.overrides.add(path); }
        try { Scenario.validate(candidate); model.get().loadouts.set(model.get().selected, candidate); changed.run(); } catch (RuntimeException ex) { message.accept(ex.getMessage()); refresh(); }
    }
    private Scenario.Loadout current() { return model.get().loadouts.get(model.get().selected); }
    private static boolean conflicts(Prayer a, Prayer b) { return a.getAttackBonus() > 0 && b.getAttackBonus() > 0 || a.getStrengthBonus() > 0 && b.getStrengthBonus() > 0 || a.getDefenceBonus() > 0 && b.getDefenceBonus() > 0 || a.getRangedBonus() > 0 && b.getRangedBonus() > 0 || a.getMagicBonus() > 0 && b.getMagicBonus() > 0; }
    private static int slotIndex(String slot) { for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i].equals(slot)) return SLOT_IDS[i]; return -1; }
    private static String shortName(String text) { return text.length() >  20 ? text.substring(0, 18) + "…" : text; }
    private static String pretty(String text) { String lower = text.replace('_', ' ').toLowerCase(Locale.ROOT); return Character.toUpperCase(lower.charAt(0)) + lower.substring(1); }
    private static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    static JPanel column() { JPanel p = new JPanel() { public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); } }; p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(BG); return p; }
    static JLabel label(String text, Color color) { JLabel l = new JLabel(text); l.setForeground(color); l.setToolTipText(text); l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11)); return l; }
    static JTextArea note(String text) { JTextArea a = new JTextArea(text); a.setColumns(17); a.setRows(Math.max(1, (text.length()+29)/30)); a.setEditable(false); SidebarScrolling.passiveText(a); a.setLineWrap(true); a.setWrapStyleWord(true); a.setOpaque(false); a.setForeground(MUTED); a.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11)); a.setBorder(new EmptyBorder(5, 0, 7, 0)); return a; }
    static JButton action(String text, Runnable action) { JButton b = new JButton(text); b.setMargin(new Insets(7, 5, 7, 5)); b.addActionListener(e -> action.run()); return b; }
    static JPanel pair(JComponent a, JComponent b) { JPanel p = new JPanel(new GridLayout(1, 2, 4, 0)) { public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);} }; p.setBackground(BG); p.add(a); p.add(b); p.setBorder(new EmptyBorder(3, 0, 3, 0)); return p; }
    private static JScrollPane scroll(JComponent view, int height) { JScrollPane s = new JScrollPane(view); s.setPreferredSize(new Dimension(190, height)); s.setMinimumSize(new Dimension(0, height)); s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); SidebarScrolling.nested(s); return s; }
    private static JSpinner spinner(int value, int min, int max) { JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, 1)); s.setPreferredSize(new Dimension(48, 26)); ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(2); return s; }
    private static JPanel fold(String title, JPanel content, boolean expanded) { JPanel p = column(); JButton heading = action((expanded ? "▾ " : "▸ ") + title, () -> {}); heading.putClientProperty("wikiDpsSection",Boolean.TRUE);heading.setHorizontalAlignment(SwingConstants.LEFT); content.setVisible(expanded); heading.addActionListener(e -> { content.setVisible(!content.isVisible()); heading.setText((content.isVisible() ? "▾ " : "▸ ") + title); for(Container parent=p;parent!=null;parent=parent.getParent())parent.invalidate();p.revalidate(); }); p.add(heading); p.add(content); return p; }
    private static void onText(JTextField field, Runnable action) { field.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) { action.run(); } public void removeUpdate(DocumentEvent e) { action.run(); } public void changedUpdate(DocumentEvent e) { action.run(); } }); }
    private static void activate(JList<?> list, Runnable action) { list.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) action.run(); }}); list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "choose"); list.getActionMap().put("choose", new AbstractAction() { public void actionPerformed(ActionEvent e) { action.run(); }}); }
    static void theme(Component component) {
        if (component instanceof JComponent) { JComponent c = (JComponent) component; c.setAlignmentX(LEFT_ALIGNMENT);
            if(c instanceof javax.swing.text.JTextComponent)SidebarScrolling.quietCaret((javax.swing.text.JTextComponent)c);
            if (!(c instanceof JLabel) && !(c instanceof JTextArea)) { c.setForeground(TEXT); if (!(c instanceof JPanel)) c.setBackground(CARD); }
            if (c instanceof AbstractButton) { ((AbstractButton) c).setFocusPainted(false); c.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11)); if(c instanceof JButton || c instanceof JToggleButton && !(c instanceof JCheckBox)) c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(EDGE),new EmptyBorder(6,3,6,3))); }
            if (c instanceof JTextField) { c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(EDGE), new EmptyBorder(6, 5, 6, 5))); ((JTextField) c).setCaretColor(TEXT);c.setBackground(new Color(99,86,71)); }
            if (c instanceof JList) { ((JList<?>) c).setSelectionBackground(new Color(94, 75, 43)); ((JList<?>) c).setSelectionForeground(TEXT); }
            if (c instanceof JTextField || c instanceof JComboBox || c instanceof AbstractButton) c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        }
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) theme(child);
        if (component instanceof JButton && Boolean.TRUE.equals(((JButton)component).getClientProperty("wikiDpsSection"))) {component.setBackground(new Color(48,40,33));component.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));}
        if (component instanceof JSpinner) {
            JFormattedTextField field=((JSpinner.DefaultEditor)((JSpinner)component).getEditor()).getTextField();
            field.setBorder(new EmptyBorder(2,1,2,1));field.setHorizontalAlignment(JTextField.CENTER);
        }
    }

    private void resizeEditors() {
        for(Container parent=this;parent!=null;parent=parent.getParent())parent.invalidate();
        revalidate();
    }

    static ImageIcon icon(String path, int size) {
        java.net.URL resource=CalculatorWorkspace.class.getResource("/com/dpscalc/ui/"+path+".png");
        if(resource==null)throw new IllegalStateException("Missing UI icon: "+path);
        ImageIcon source=new ImageIcon(resource);
        double scale=Math.min(1.0,Math.min((double)size/source.getIconWidth(),(double)size/source.getIconHeight()));
        return new ImageIcon(source.getImage().getScaledInstance(Math.max(1,(int)(source.getIconWidth()*scale)),Math.max(1,(int)(source.getIconHeight()*scale)),Image.SCALE_SMOOTH));
    }
    private static JLabel statLabel(String name,String image) {
        JLabel label=label(name,TEXT);label.setIcon(icon(image,18));label.setToolTipText(name);return label;
    }
    private JPanel bonuses() {
        JPanel page=column();page.add(label("Bonuses",GOLD));
        JPanel columns=new JPanel(new GridLayout(1,3,4,0));columns.setBackground(BG);
        String[][] fields={{"stabAttack","slashAttack","crushAttack","magicAttack","rangedAttack"},{"stabDefence","slashDefence","crushDefence","magicDefence","rangedDefence"},{"meleeStrength","rangedStrength","magicDamage","prayerBonus"}};
        String[][] images={{"bonuses/dagger","bonuses/scimitar","bonuses/warhammer","bonuses/magic","bonuses/ranged"},{"bonuses/dagger","bonuses/scimitar","bonuses/warhammer","bonuses/magic","bonuses/ranged"},{"bonuses/strength","bonuses/ranged_strength","bonuses/magic_strength","tabs/prayer"}};
        String[] headings={"Offensive","Defensive","Other"};
        for(int i=0;i<3;i++) {
            JPanel col=column();JLabel heading=label(headings[i],MUTED);heading.setFont(heading.getFont().deriveFont(10f));col.add(heading);
            for(int j=0;j<fields[i].length;j++) {
                String name=fields[i][j].replaceAll("([a-z])([A-Z])","$1 $2");
                JLabel value=label("—",TEXT);value.setHorizontalAlignment(SwingConstants.CENTER);value.setOpaque(true);value.setBackground(CARD);
                value.setToolTipText(name+" · calculated from equipment");value.getAccessibleContext().setAccessibleName("Equipment bonus: "+fields[i][j]);bonusValues.put(fields[i][j],value);
                JPanel row=new JPanel(new BorderLayout(2,0));row.setBackground(BG);row.setPreferredSize(new Dimension(60,29));
                row.add(statLabel("",images[i][j]),BorderLayout.WEST);row.add(value,BorderLayout.CENTER);col.add(row);
            }
            columns.add(col);
        }
        page.add(columns);return page;
    }
    private JSpinner targetNumber(String name,String path,int min,int max) {
        JSpinner spin=spinner(min,min,max);spin.getAccessibleContext().setAccessibleName("Target: "+name);spin.setToolTipText(name);
        bindings.add(()->{
            MonsterStats target=model.get().target;spin.setEnabled(target!=null);
            JsonElement value=target==null?null:Scenario.flatten(Scenario.JSON.toJsonTree(target).getAsJsonObject()).get(path);
            spin.setValue(value==null?min:value.getAsInt());
        });
        spin.addChangeListener(e->{if(binding||model.get().target==null)return;editTarget(path,new JsonPrimitive((Number)spin.getValue()));});
        return spin;
    }
    private void editTarget(String path,JsonElement value) {
        JsonObject next=Scenario.JSON.toJsonTree(model.get().target).getAsJsonObject();Scenario.put(next,path,value);
        MonsterStats candidate=Scenario.JSON.fromJson(next,MonsterStats.class);
        try {Scenario.validate(candidate);model.get().target=candidate;model.get().targetSources.put(path,"Manual override");changed.run();}
        catch(RuntimeException ex){message.accept(ex.getMessage());refresh();}
    }
    private void buildTargetStats() {
        JPanel levels=column(), defence=column();levels.add(label("Skills",MUTED));defence.add(label("Defensive",MUTED));
        String[][] skills={{"Hitpoints","hitpoints","hitpoints"},{"Attack","attackLevel","attack"},{"Strength","strengthLevel","strength"},{"Defence","defenceLevel","defence"},{"Magic","magicLevel","magic"},{"Ranged","rangedLevel","ranged"}};
        for(String[] field:skills)levels.add(targetStat(field[0],field[1],"bonuses/"+field[2],field[1].equals("hitpoints")?1:0,100000));
        String[][] defs={{"Stab defence","stabDefence","dagger"},{"Slash defence","slashDefence","scimitar"},{"Crush defence","crushDefence","warhammer"},{"Magic defence","magicDefence","magic"},{"Light ranged defence","lightRangedDefence","ranged_light"},{"Standard ranged defence","standardRangedDefence","ranged_standard"},{"Heavy ranged defence","heavyRangedDefence","ranged_heavy"}};
        for(String[] field:defs)defence.add(targetStat(field[0],field[1],"bonuses/"+field[2],-1000,100000));
        targetStats.add(pair(levels,defence));
        JPanel offensive=column();offensive.add(pair(label("Magic accuracy",TEXT),targetNumber("Magic accuracy","offensiveMagic",-1000,100000)));
        targetStats.add(fold("Offensive bonuses",offensive,false));
        JPanel attributes=column();
        for(com.dpscalc.data.MonsterAttribute attribute:com.dpscalc.data.MonsterAttribute.values()) {
            JCheckBox box=new JCheckBox(pretty(attribute.name()));
            bindings.add(()->{box.setEnabled(model.get().target!=null);box.setSelected(model.get().target!=null&&model.get().target.getAttributes().contains(attribute));});
            box.addActionListener(e->{if(binding||model.get().target==null)return;Set<com.dpscalc.data.MonsterAttribute> attrs=new HashSet<>(model.get().target.getAttributes());if(box.isSelected())attrs.add(attribute);else attrs.remove(attribute);editTarget("attributes",Scenario.JSON.toJsonTree(attrs));});attributes.add(box);
        }
        targetStats.add(fold("Attributes",attributes,false));
        JPanel reductions=column();
        for(String[] field:new String[][]{{"DWH hits","dwh"},{"Elder maul hits","elderMaul"},{"BGS damage","bgs"},{"Arclight","arclight"},{"Emberlight","emberlight"},{"Tonalztics","tonalztic"},{"Vulnerability","vulnerability"},{"Accursed sceptre","accursedSceptre"},{"Seercull damage","seercull"},{"Ayak reduction","ayak"}})
            reductions.add(pair(label(field[0],TEXT),targetNumber(field[0],"inputs.defenceReductions."+field[1],0,10000)));
        targetStats.add(fold("Defensive reductions",reductions,false));
        JPanel settings=column();
        for(String[] field:new String[][]{{"HP (0 = full)","inputs.monsterCurrentHp","0","100000"},{"Size","size","1","10"},{"Speed (ticks)","speed","1","100"},{"Flat armour","flatArmour","0","100000"},{"ToA invocation","inputs.toaInvocationLevel","0","600"},{"ToA path level","inputs.toaPathLevel","0","6"},{"Party size","inputs.partySize","1","100"}})
            settings.add(pair(label(field[0],TEXT),targetNumber(field[0],field[1],Integer.parseInt(field[2]),Integer.parseInt(field[3]))));
        targetStats.add(fold("Monster settings",settings,false));
    }
    private JPanel targetStat(String name,String path,String image,int min,int max) {
        JPanel row=new JPanel(new BorderLayout(4,0));row.setBackground(BG);row.setBorder(new EmptyBorder(2,0,2,0));
        JLabel label=statLabel("",image);label.setToolTipText(name);row.add(label,BorderLayout.WEST);row.add(targetNumber(name,path,min,max),BorderLayout.CENTER);return row;
    }
    private static final class HintField extends JTextField {
        private final String hint;
        HintField(String hint){this.hint=hint;}
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);if(getText().isEmpty()){g.setColor(MUTED);g.setFont(getFont());g.drawString(hint,getInsets().left,(getHeight()+g.getFontMetrics().getAscent()-g.getFontMetrics().getDescent())/2);}}
    }

    private static final class ItemChoice { final int id; final EquipmentCatalogItem facts; ItemChoice(int id, EquipmentCatalogItem facts) { this.id = id; this.facts = facts; } public String toString() { return facts.getName(); } }
    private static final class MonsterChoice { final MonsterStats monster; MonsterChoice(MonsterStats monster) { this.monster = monster; } public String toString() { return monster.getName(); } }
}
