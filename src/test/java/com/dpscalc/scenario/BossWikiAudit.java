package com.dpscalc.scenario;

import com.loadoutlab.equipment.EquipmentCatalogItem;
import com.loadoutlab.equipment.EquipmentPreparationFacade;
import com.dpscalc.wikisetups.items.ItemNameIndex;
import com.dpscalc.wikisetups.wiki.EquipSlot;
import com.dpscalc.wikisetups.wiki.ExampleSetup;
import com.dpscalc.wikisetups.wiki.ExampleSetupParser;
import com.dpscalc.wikisetups.wiki.GearSetup;
import com.dpscalc.wikisetups.wiki.GearSetupParser;
import com.dpscalc.wikisetups.wiki.GearSetups;
import com.dpscalc.wikisetups.wiki.InventoryParser;
import com.dpscalc.wikisetups.wiki.InventorySetup;
import com.dpscalc.wikisetups.wiki.RankedItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministically audits a local manifest of Wiki pages with the production WikiLoadouts parser.
 * This is an offline tool: it never fetches a page and treats an unpaired inventory as an audit
 * finding rather than a parser error.
 */
public final class BossWikiAudit {
    private static final Gson JSON =
            new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private static final String MANIFEST = "manifest.json";

    private BossWikiAudit() {}

    public static void main(String[] args) throws IOException {
        Arguments options = Arguments.parse(args);
        JsonObject manifest = readObject(options.input.resolve(MANIFEST));
        JsonArray pages = requiredArray(manifest, "pages");
        DraftContext draftContext = new DraftContext(options.itemNames);
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1);
        report.add("manifest", manifest);
        JsonObject draftInfo = new JsonObject();
        draftInfo.addProperty("itemIndex", draftContext.itemIndexSource);
        if (options.itemNames != null && Files.isRegularFile(options.itemNames))
            draftInfo.addProperty("itemNamesSha256", sha256(Files.readAllBytes(options.itemNames)));
        draftInfo.addProperty(
                "stackability",
                "unknown_for_supply_items; zero-quantity supplies are reported as uncertain");
        report.add("draftAudit", draftInfo);
        JsonArray pageReports = new JsonArray();
        JsonArray errors = new JsonArray();
        JsonObject totals = new JsonObject();
        int failed = 0;
        for (int i = 0; i < pages.size(); i++) {
            JsonElement entryElement = pages.get(i);
            if (!entryElement.isJsonObject()) {
                failed++;
                JsonObject error =
                        error("pages[" + i + "]", "manifest page entry is not an object", null);
                errors.add(error);
                continue;
            }
            JsonObject entry = entryElement.getAsJsonObject();
            JsonObject pageReport = auditPage(options.input, entry, i, draftContext);
            pageReports.add(pageReport);
            if (pageReport.getAsJsonArray("errors").size() != 0) {
                failed++;
                for (JsonElement error : pageReport.getAsJsonArray("errors")) errors.add(error);
            }
            addTotals(totals, pageReport.getAsJsonObject("counts"));
        }
        report.add("pages", pageReports);
        report.add("errors", errors);
        report.add("counts", totals);
        Path parent = options.output.toAbsolutePath().normalize().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(options.output, JSON.toJson(report).getBytes(StandardCharsets.UTF_8));
        if (failed != 0)
            throw new IllegalStateException(
                    "Wiki audit failed for "
                            + failed
                            + " page entr"
                            + (failed == 1 ? "y" : "ies")
                            + "; see "
                            + options.output);
    }

    private static JsonObject auditPage(
            Path corpus, JsonObject entry, int index, DraftContext draftContext) {
        JsonObject report = new JsonObject();
        // Keep the manifest entry both in a dedicated field and at the page root so
        // reports remain convenient to inspect without losing unknown metadata.
        report.add("metadata", entry);
        for (Map.Entry<String, JsonElement> field : entry.entrySet())
            if (!report.has(field.getKey())) report.add(field.getKey(), field.getValue());
        JsonArray errors = new JsonArray();
        report.add("errors", errors);
        JsonObject counts = emptyCounts();
        report.add("counts", counts);
        try {
            if (entry.has("error")) {
                errors.add(
                        error(
                                "page[" + index + "]",
                                "manifest fetch failed: " + entry.get("error").getAsString(),
                                null));
                return report;
            }
            String file = requiredString(entry, "file");
            Path root = corpus.toAbsolutePath().normalize();
            Path sourcePath = root.resolve(file).normalize();
            if (!sourcePath.startsWith(root))
                throw new IOException("source file escapes corpus directory: " + file);
            byte[] sourceBytes = Files.readAllBytes(sourcePath);
            String source = new String(sourceBytes, StandardCharsets.UTF_8);
            String actualSha256 = sha256(sourceBytes);
            String expectedSha256 = requiredString(entry, "sha256");
            if (!actualSha256.equalsIgnoreCase(expectedSha256))
                throw new IOException(
                        "source sha256 mismatch: manifest "
                                + expectedSha256
                                + ", actual "
                                + actualSha256);
            report.addProperty("sourceSha256", actualSha256);
            report.addProperty("sourceBytes", sourceBytes.length);
            if (entry.has("searchTitles") && entry.get("searchTitles").isJsonArray()) {
                String query =
                        entry.has("query")
                                ? entry.get("query").getAsString()
                                : entry.has("title")
                                        ? entry.get("title").getAsString()
                                        : entry.get("key").getAsString();
                List<String> searchTitles = new ArrayList<>();
                for (JsonElement title : entry.getAsJsonArray("searchTitles"))
                    searchTitles.add(title.getAsString());
                JsonArray ranked = new JsonArray();
                for (String title : WikiSetupService.rankSearch(query, searchTitles))
                    ranked.add(title);
                report.add("rankedSearchTitles", ranked);
            }

            // WikiLoadouts.parse is the authority for the choices emitted here.
            List<WikiLoadouts.Choice> choices = WikiLoadouts.parse(source);
            List<GearSetup> rawGear = new GearSetupParser().parse(source);
            GearSetups.Partition partition = GearSetups.partition(rawGear);
            List<InventorySetup> inventories = new InventoryParser().parse(source);
            List<ExampleSetup> examples = new ExampleSetupParser().parse(source);
            counts.addProperty("gearRaw", rawGear.size());
            counts.addProperty("loadouts", partition.getLoadouts().size());
            counts.addProperty("switches", partition.getSwitches().size());
            counts.addProperty("inventories", inventories.size());
            counts.addProperty("examples", examples.size());
            counts.addProperty("choices", choices.size());

            JsonArray gear = new JsonArray();
            for (GearSetup setup : rawGear) gear.add(gearJson(setup));
            report.add("gear", gear);
            JsonArray switches = new JsonArray();
            for (GearSetup setup : partition.getSwitches()) switches.add(gearJson(setup));
            report.add("switches", switches);
            JsonArray inventoryReport = new JsonArray();
            for (int inventoryIndex = 0; inventoryIndex < inventories.size(); inventoryIndex++) {
                JsonObject inventory = inventoryJson(inventories.get(inventoryIndex));
                inventory.addProperty("index", inventoryIndex);
                inventoryReport.add(inventory);
            }
            report.add("inventories", inventoryReport);

            JsonArray choiceReport = new JsonArray();
            JsonArray unmatched = new JsonArray();
            JsonArray conversionErrors = new JsonArray();
            report.add("conversionErrors", conversionErrors);
            for (int choiceIndex = 0; choiceIndex < choices.size(); choiceIndex++) {
                WikiLoadouts.Choice choice = choices.get(choiceIndex);
                JsonObject out = new JsonObject();
                out.addProperty("index", choiceIndex);
                out.addProperty(
                        "source",
                        choiceIndex < partition.getLoadouts().size() ? "loadout" : "example");
                out.addProperty("label", choice.label);
                out.add("gear", gearJson(choice.gear));
                if (choice.inventory == null) {
                    out.addProperty("inventoryStatus", "unmatched");
                    JsonObject finding = new JsonObject();
                    finding.addProperty("choiceIndex", choiceIndex);
                    finding.addProperty("label", choice.label);
                    finding.addProperty(
                            "reason",
                            inventories.isEmpty()
                                    ? "no_inventory_templates"
                                    : "no_unambiguous_inventory_pairing");
                    unmatched.add(finding);
                } else {
                    out.addProperty("inventoryStatus", "matched");
                    out.add("inventory", inventoryJson(choice.inventory));
                }
                try {
                    out.add("draft", draftJson(choice, entry, draftContext));
                } catch (Exception conversionError) {
                    JsonObject failure =
                            error(
                                    "page[" + index + "].choices[" + choiceIndex + "]",
                                    conversionError.getMessage(),
                                    conversionError);
                    conversionErrors.add(failure);
                    errors.add(failure);
                }
                choiceReport.add(out);
            }
            counts.addProperty("unmatchedChoices", unmatched.size());
            report.add("choices", choiceReport);
            report.add("unmatchedChoices", unmatched);
            JsonArray unmatchedInventories = new JsonArray();
            for (int inventoryIndex = 0; inventoryIndex < inventories.size(); inventoryIndex++) {
                InventorySetup inventory = inventories.get(inventoryIndex);
                boolean matched = false;
                for (WikiLoadouts.Choice choice : choices)
                    if (choice.inventory != null && sameInventory(choice.inventory, inventory)) {
                        matched = true;
                        break;
                    }
                if (!matched) {
                    JsonObject finding = new JsonObject();
                    finding.addProperty("index", inventoryIndex);
                    finding.addProperty("label", inventory.getLabel());
                    finding.addProperty("reason", "no_choice_uses_inventory_contents");
                    unmatchedInventories.add(finding);
                }
            }
            report.add("unmatchedInventories", unmatchedInventories);
            counts.addProperty("unmatchedInventories", unmatchedInventories.size());
        } catch (Exception error) {
            errors.add(error("page[" + index + "]", error.getMessage(), error));
        }
        return report;
    }

    private static JsonObject gearJson(GearSetup setup) {
        JsonObject out = new JsonObject();
        out.addProperty("styleLabel", setup.getStyleLabel());
        out.addProperty("paneLabel", setup.getPaneLabel());
        out.addProperty("slotCount", setup.getSlots().size());
        JsonObject slots = new JsonObject();
        for (Map.Entry<EquipSlot, List<RankedItem>> slot : setup.getSlots().entrySet()) {
            JsonArray candidates = new JsonArray();
            for (RankedItem item : slot.getValue()) {
                JsonObject candidate = new JsonObject();
                candidate.addProperty("name", item.getName());
                candidate.addProperty("rank", item.getRank());
                candidate.addProperty("note", item.getNote());
                JsonArray fallbacks = new JsonArray();
                for (String fallback : item.getFallbackNames()) fallbacks.add(fallback);
                candidate.add("fallbackNames", fallbacks);
                candidates.add(candidate);
            }
            slots.add(slot.getKey().name().toLowerCase(Locale.ROOT), candidates);
        }
        out.add("slotCandidates", slots);
        return out;
    }

    private static JsonObject inventoryJson(InventorySetup setup) {
        JsonObject out = new JsonObject();
        out.addProperty("label", setup.getLabel());
        out.addProperty("slotCount", setup.getSlots().size());
        JsonArray slots = new JsonArray();
        JsonArray quantities = new JsonArray();
        JsonArray noted = new JsonArray();
        JsonArray quantityStatus = new JsonArray();
        for (int i = 0; i < 28; i++) {
            String item = i < setup.getSlots().size() ? setup.getSlots().get(i) : "";
            slots.add(item == null ? "" : item);
            quantities.add(setup.quantity(i, false));
            noted.add(setup.noted(i));
            quantityStatus.add(
                    item == null || item.isEmpty()
                            ? "empty"
                            : setup.quantity(i, false) > 0
                                    ? "specified"
                                    : "unknown_or_unspecified");
        }
        out.add("slots", slots);
        out.add("quantities", quantities);
        out.add("noted", noted);
        out.add("quantityStatus", quantityStatus);
        JsonArray pouch = new JsonArray();
        JsonArray pouchQuantities = new JsonArray();
        JsonArray pouchQuantityStatus = new JsonArray();
        for (int i = 0; i < setup.getRunes().size(); i++) {
            pouch.add(setup.getRunes().get(i));
            pouchQuantities.add(setup.quantity(i, true));
            pouchQuantityStatus.add(
                    setup.quantity(i, true) > 0 ? "specified" : "unknown_or_unspecified");
        }
        out.add("runePouch", pouch);
        out.add("runePouchQuantities", pouchQuantities);
        out.add("runePouchQuantityStatus", pouchQuantityStatus);
        return out;
    }

    private static boolean sameInventory(InventorySetup left, InventorySetup right) {
        if (!sameList(left.getSlots(), right.getSlots(), 28)) return false;
        if (left.getRunes().size() != right.getRunes().size()
                || !sameList(left.getRunes(), right.getRunes(), left.getRunes().size()))
            return false;
        for (int i = 0; i < 28; i++)
            if (left.quantity(i, false) != right.quantity(i, false)
                    || left.noted(i) != right.noted(i)) return false;
        for (int i = 0; i < Math.max(left.getRunes().size(), right.getRunes().size()); i++)
            if (left.quantity(i, true) != right.quantity(i, true)) return false;
        return true;
    }

    private static boolean sameList(List<String> left, List<String> right, int width) {
        for (int i = 0; i < width; i++) {
            String a = i < left.size() && left.get(i) != null ? left.get(i) : "";
            String b = i < right.size() && right.get(i) != null ? right.get(i) : "";
            if (!a.equals(b)) return false;
        }
        return true;
    }

    private static JsonObject draftJson(
            WikiLoadouts.Choice choice, JsonObject entry, DraftContext context) {
        String title =
                entry.has("title")
                        ? entry.get("title").getAsString()
                        : entry.get("key").getAsString();
        String revision = entry.has("revision") ? entry.get("revision").getAsString() : "audit";
        WikiSetupService.Page page = new WikiSetupService.Page(title, revision, "", 0L, false);
        Scenario.Loadout draft =
                WikiLoadouts.draft(
                        choice,
                        page,
                        new Scenario.Loadout(),
                        OwnedEquipment.empty(1),
                        "audit",
                        false,
                        context.itemIndex,
                        context.stackable,
                        context.equipment);
        Scenario scenario = new Scenario();
        scenario.loadouts.clear();
        scenario.loadouts.add(draft);
        scenario.selected = 0;
        String before = Scenario.JSON.toJson(draft.carry);
        Scenario restored = Scenario.parse(Scenario.JSON.toJson(scenario));
        String after = Scenario.JSON.toJson(restored.loadouts.get(0).carry);
        if (!before.equals(after))
            throw new IllegalStateException("Carry plan changed during scenario persistence");
        JsonObject out = new JsonObject();
        out.addProperty("status", before.equals(after) ? "ok" : "roundtrip_mismatch");
        out.addProperty("carryPlanRoundTrip", before.equals(after));
        JsonArray gearFindings = new JsonArray();
        for (String note : draft.wikiNotes)
            if (note.startsWith("Unrecognized ") || note.startsWith("No compatible DPS data"))
                gearFindings.add(note);
        out.add("missingEquipment", gearFindings);
        JsonArray unresolvedInventory = new JsonArray();
        for (CarryPlan.Entry item : draft.carry.slots)
            if (item != null && item.id <= 0 && item.name != null && !item.name.isEmpty())
                unresolvedInventory.add(item.name);
        for (CarryPlan.Entry item : draft.carry.runes)
            if (item != null && item.id <= 0 && item.name != null && !item.name.isEmpty())
                unresolvedInventory.add(item.name);
        out.add("unresolvedInventory", unresolvedInventory);
        out.add("equippedItemIds", ints(draft.player.getEquippedItemIds()));
        out.add("equippedItemNames", strings(draft.player.getEquippedItemNames()));
        out.add("carry", carryJson(draft.carry));
        out.add("wikiNotes", strings(draft.wikiNotes));
        return out;
    }

    private static JsonObject carryJson(CarryPlan carry) {
        JsonObject out = new JsonObject();
        JsonArray slots = new JsonArray();
        for (CarryPlan.Entry entry : carry.slots) slots.add(entryJson(entry));
        JsonArray runes = new JsonArray();
        for (CarryPlan.Entry entry : carry.runes) runes.add(entryJson(entry));
        JsonArray switches = new JsonArray();
        for (CarryPlan.Entry entry : carry.switches) switches.add(entryJson(entry));
        out.add("slots", slots);
        out.add("runes", runes);
        out.add("switches", switches);
        return out;
    }

    private static JsonElement entryJson(CarryPlan.Entry entry) {
        if (entry == null) return com.google.gson.JsonNull.INSTANCE;
        JsonObject out = new JsonObject();
        out.addProperty("id", entry.id);
        out.addProperty("name", entry.name);
        out.addProperty("quantity", entry.quantity);
        out.addProperty("noted", entry.noted);
        out.addProperty("note", entry.note);
        return out;
    }

    private static JsonArray ints(int[] values) {
        JsonArray out = new JsonArray();
        for (int value : values) out.add(value);
        return out;
    }

    private static JsonArray strings(String[] values) {
        JsonArray out = new JsonArray();
        for (String value : values) out.add(value);
        return out;
    }

    private static JsonArray strings(List<String> values) {
        JsonArray out = new JsonArray();
        for (String value : values) out.add(value);
        return out;
    }

    private static final class DraftContext {
        final EquipmentPreparationFacade equipment = new EquipmentPreparationFacade();
        final ItemNameIndex itemIndex;
        final String itemIndexSource;
        final java.util.function.IntPredicate stackable = id -> false;

        DraftContext(Path namesPath) {
            Map<Integer, String> names = new LinkedHashMap<>();
            List<Map.Entry<Integer, EquipmentCatalogItem>> equipmentItems =
                    new ArrayList<>(equipment.getItems().entrySet());
            equipmentItems.sort(Comparator.comparingInt(Map.Entry::getKey));
            for (Map.Entry<Integer, EquipmentCatalogItem> item : equipmentItems)
                names.put(item.getKey(), item.getValue().getName());
            String source = "EquipmentPreparationFacade.getItems()";
            if (namesPath != null && Files.isRegularFile(namesPath)) {
                try {
                    JsonObject itemNames = readObject(namesPath);
                    List<Integer> ids = new ArrayList<>();
                    for (Map.Entry<String, JsonElement> item : itemNames.entrySet())
                        ids.add(Integer.parseInt(item.getKey()));
                    ids.sort(Integer::compareTo);
                    for (Integer id : ids) {
                        JsonElement name = itemNames.get(Integer.toString(id));
                        // Keep catalog IDs at the front of each normalized-name bucket, but
                        // use the public cache's display spelling (e.g. "(ri)" variants)
                        // so ItemResolver sees the same names as the offline item index.
                        if (name.isJsonPrimitive()
                                && name.getAsString() != null
                                && !name.getAsString().isEmpty()) names.put(id, name.getAsString());
                    }
                    source += "; " + namesPath;
                } catch (IOException | RuntimeException error) {
                    throw new IllegalArgumentException(
                            "Invalid item names JSON: " + namesPath, error);
                }
            }
            itemIndex = new SetupItemIndex(names, new HashSet<>());
            itemIndexSource = source;
        }
    }

    private static JsonObject emptyCounts() {
        JsonObject counts = new JsonObject();
        for (String name :
                new String[] {
                    "gearRaw",
                    "loadouts",
                    "switches",
                    "inventories",
                    "examples",
                    "choices",
                    "unmatchedChoices",
                    "unmatchedInventories"
                }) counts.addProperty(name, 0);
        return counts;
    }

    private static void addTotals(JsonObject totals, JsonObject page) {
        for (Map.Entry<String, JsonElement> field : page.entrySet())
            if (field.getValue().isJsonPrimitive()
                    && field.getValue().getAsJsonPrimitive().isNumber())
                totals.addProperty(
                        field.getKey(),
                        totals.has(field.getKey())
                                ? totals.get(field.getKey()).getAsInt()
                                        + field.getValue().getAsInt()
                                : field.getValue().getAsInt());
    }

    private static JsonObject readObject(Path path) throws IOException {
        JsonElement parsed =
                new JsonParser()
                        .parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        if (!parsed.isJsonObject()) throw new IOException(path + " must contain a JSON object");
        return parsed.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject object, String key) throws IOException {
        if (!object.has(key) || !object.get(key).isJsonArray())
            throw new IOException("manifest requires array: " + key);
        return object.getAsJsonArray(key);
    }

    private static String requiredString(JsonObject object, String key) throws IOException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive())
            throw new IOException("manifest page requires string: " + key);
        return object.get(key).getAsString();
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest)
                result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static JsonObject error(String location, String message, Exception cause) {
        JsonObject error = new JsonObject();
        error.addProperty("location", location);
        error.addProperty("message", message == null ? cause.getClass().getName() : message);
        if (cause != null) error.addProperty("type", cause.getClass().getName());
        return error;
    }

    private static final class Arguments {
        final Path input;
        final Path output;

        static Arguments parse(String[] args) {
            String input = null;
            String output = null;
            List<String> positional = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("--input=")) input = arg.substring("--input=".length());
                else if (arg.startsWith("--output=")) output = arg.substring("--output=".length());
                else if (arg.equals("--input") && i + 1 < args.length) input = args[++i];
                else if (arg.equals("--output") && i + 1 < args.length) output = args[++i];
                else positional.add(arg);
            }
            if (input == null && !positional.isEmpty()) input = positional.get(0);
            if (output == null && positional.size() > 1) output = positional.get(1);
            Path namesPath = null;
            String names = null;
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("--item-names="))
                    names = args[i].substring("--item-names=".length());
                else if (args[i].equals("--item-names") && i + 1 < args.length) names = args[i + 1];
            }
            if (names == null && input != null) {
                namesPath = Paths.get(input).resolve("item-names.json");
                if (!Files.isRegularFile(namesPath)
                        && namesPath.toAbsolutePath().normalize().getParent() != null)
                    namesPath =
                            namesPath
                                    .toAbsolutePath()
                                    .normalize()
                                    .getParent()
                                    .resolve("item-names.json");
            } else if (names != null) namesPath = Paths.get(names);
            if (input == null || output == null)
                throw new IllegalArgumentException(
                        "Usage: BossWikiAudit <corpus-directory> <output.json> (or"
                            + " --input/--output)");
            return new Arguments(Paths.get(input), Paths.get(output), namesPath);
        }

        final Path itemNames;

        private Arguments(Path input, Path output, Path itemNames) {
            this.input = input;
            this.output = output;
            this.itemNames = itemNames;
        }
    }
}
