package com.dpscalc.scenario;

import com.dpscalc.wikisetups.items.*;

import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;

import java.util.*;

import javax.inject.*;

/** Client definitions are copied in small batches. Readers only see a complete immutable index. */
@Singleton
public final class SetupItemIndex implements ItemNameIndex {
    private final Client client;
    private final ClientThread thread;
    private volatile Data data;
    private volatile long generation;

    @Inject
    public SetupItemIndex(Client client, ClientThread thread) {
        this.client = client;
        this.thread = thread;
    }

    SetupItemIndex(Map<Integer, String> names, Set<Integer> stacks) {
        client = null;
        thread = null;
        Map<String, List<Integer>> ids = new HashMap<>();
        names.forEach(
                (id, name) ->
                        ids.computeIfAbsent(ItemNames.normalize(name), key -> new ArrayList<>())
                                .add(id));
        ids.replaceAll((key, value) -> List.copyOf(value));
        data = new Data(Map.copyOf(names), Map.copyOf(ids), Set.copyOf(stacks));
    }

    public void start() {
        long token = ++generation;
        Map<Integer, String> names = new HashMap<>();
        Map<String, List<Integer>> ids = new HashMap<>();
        Set<Integer> stacks = new HashSet<>();
        int[] next = {0};
        thread.invokeLater(
                () -> {
                    if (token != generation) return true;
                    if (client.getGameState() == GameState.STARTING
                            || client.getGameState() == GameState.UNKNOWN) return false;
                    int count = client.getItemCount();
                    if (count <= 0) return false;
                    int end = Math.min(count, next[0] + 400);
                    for (; next[0] < end; next[0]++) {
                        int id = next[0];
                        ItemComposition item = client.getItemDefinition(id);
                        String name = item.getName();
                        if (item.getNote() != -1
                                || item.getPlaceholderTemplateId() != -1
                                || name == null
                                || name.equals("null")) continue;
                        names.put(id, name);
                        ids.computeIfAbsent(ItemNames.normalize(name), k -> new ArrayList<>())
                                .add(id);
                        if (item.isStackable()) stacks.add(id);
                    }
                    if (end < count) return false;
                    ids.replaceAll((k, v) -> Collections.unmodifiableList(v));
                    data =
                            new Data(
                                    Collections.unmodifiableMap(names),
                                    Collections.unmodifiableMap(ids),
                                    Collections.unmodifiableSet(stacks));
                    return true;
                });
    }

    public void stop() {
        generation++;
    }

    public boolean isReady() {
        return data != null;
    }

    public String nameById(int id) {
        Data d = data;
        return d == null ? null : d.names.get(id);
    }

    public List<Integer> idsByName(String name) {
        Data d = data;
        return d == null
                ? Collections.emptyList()
                : d.ids.getOrDefault(name, Collections.emptyList());
    }

    public boolean stackable(int id) {
        Data d = data;
        return d != null && d.stacks.contains(id);
    }

    public List<Integer> search(String query, int limit) {
        Data d = data;
        if (d == null || query.isBlank()) return Collections.emptyList();
        String q = query.toLowerCase(Locale.ROOT);
        List<Integer> found = new ArrayList<>();
        d.names.forEach(
                (id, name) -> {
                    if (name.toLowerCase(Locale.ROOT).contains(q) || Integer.toString(id).equals(q))
                        found.add(id);
                });
        found.sort(
                Comparator.comparing((Integer id) -> !d.names.get(id).equalsIgnoreCase(query))
                        .thenComparing(id -> d.names.get(id))
                        .thenComparingInt(id -> id));
        return new ArrayList<>(found.subList(0, Math.min(limit, found.size())));
    }

    private static final class Data {
        final Map<Integer, String> names;
        final Map<String, List<Integer>> ids;
        final Set<Integer> stacks;

        Data(Map<Integer, String> n, Map<String, List<Integer>> i, Set<Integer> s) {
            names = n;
            ids = i;
            stacks = s;
        }
    }
}
