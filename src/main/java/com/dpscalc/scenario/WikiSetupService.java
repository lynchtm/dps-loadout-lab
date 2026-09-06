package com.dpscalc.scenario;

import com.google.gson.*;

import okhttp3.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.inject.*;

/** Explicit, read-only Wiki requests. No character or container data enters a request. */
@Singleton
public final class WikiSetupService {
    static final int MAX_BYTES = 2_000_000, MAX_CACHE = 16;
    private final OkHttpClient http;
    private final java.util.function.LongSupplier clock;
    private final Map<String, Page> cache =
            new LinkedHashMap<String, Page>(16, .75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, Page> e) {
                    return size() > MAX_CACHE;
                }
            };

    @Inject
    public WikiSetupService(OkHttpClient http) {
        this(http, System::currentTimeMillis);
    }

    WikiSetupService(OkHttpClient http, java.util.function.LongSupplier clock) {
        this.http = http.newBuilder().callTimeout(15, TimeUnit.SECONDS).build();
        this.clock = clock;
    }

    public static final class Page {
        public final String title, revision, text;
        public final long fetchedAt;
        public final boolean stale;

        Page(String title, String revision, String text, long fetchedAt, boolean stale) {
            this.title = title;
            this.revision = revision;
            this.text = text;
            this.fetchedAt = fetchedAt;
            this.stale = stale;
        }

        public String url() {
            return new HttpUrl.Builder()
                    .scheme("https")
                    .host("oldschool.runescape.wiki")
                    .addPathSegment("w")
                    .addPathSegment(title.replace(" ", "_"))
                    .addQueryParameter("oldid", revision)
                    .build()
                    .toString();
        }
    }

    public static final class RequestScope {
        private volatile boolean cancelled;
        private Call call;

        public synchronized void cancel() {
            cancelled = true;
            if (call != null) call.cancel();
        }

        synchronized void attach(Call next) throws IOException {
            if (cancelled) throw new IOException("Cancelled");
            call = next;
        }

        public boolean cancelled() {
            return cancelled;
        }
    }

    public List<String> search(String query, RequestScope scope) throws IOException {
        if (query.trim().isEmpty() || query.length() > 200)
            throw new IOException("Enter an activity or monster name (up to 200 characters)");
        Set<String> titles = new LinkedHashSet<>();
        for (String template : List.of("Recommended equipment", "Equipment")) {
            JsonObject root =
                    get(
                            new HttpUrl.Builder()
                                    .scheme("https")
                                    .host("oldschool.runescape.wiki")
                                    .addPathSegment("api.php")
                                    .addQueryParameter("action", "query")
                                    .addQueryParameter("format", "json")
                                    .addQueryParameter("list", "search")
                                    .addQueryParameter(
                                            "srsearch", query + " hastemplate:\"" + template + "\"")
                                    .addQueryParameter("srnamespace", "0")
                                    .addQueryParameter("srlimit", "15")
                                    .build(),
                            scope);
            for (JsonElement e : root.getAsJsonObject("query").getAsJsonArray("search"))
                titles.add(e.getAsJsonObject().get("title").getAsString());
        }
        return rankSearch(query, new ArrayList<>(titles));
    }

    static List<String> rankSearch(String query, List<String> titles) {
        String target = searchName(query);
        List<String> result = new ArrayList<>(titles);
        result.sort(
                Comparator.comparingInt(
                        title -> {
                            String name = searchName(title);
                            return name.equals(target + "/strategies")
                                    ? 0
                                    : name.equals(target)
                                            ? 1
                                            : name.startsWith(target + "/") ? 2 : 3;
                        }));
        return result;
    }

    private static String searchName(String title) {
        return title.trim().replace('_', ' ').toLowerCase(Locale.ROOT).replaceFirst("^the ", "");
    }

    public Page page(String title, RequestScope scope) throws IOException {
        if (scope.cancelled()) throw new IOException("Cancelled");
        if (title == null || title.isBlank() || title.length() > 300)
            throw new IOException("Invalid Wiki page title");
        Page cached;
        synchronized (cache) {
            cached = cache.get(title);
        }
        if (cached != null && clock.getAsLong() - cached.fetchedAt < 15 * 60_000L) return cached;
        try {
            JsonObject root =
                    get(
                            new HttpUrl.Builder()
                                    .scheme("https")
                                    .host("oldschool.runescape.wiki")
                                    .addPathSegment("api.php")
                                    .addQueryParameter("action", "parse")
                                    .addQueryParameter("format", "json")
                                    .addQueryParameter("page", title)
                                    .addQueryParameter("prop", "wikitext|revid")
                                    .addQueryParameter("redirects", "1")
                                    .build(),
                            scope);
            JsonObject p = root.getAsJsonObject("parse");
            Page page =
                    new Page(
                            p.get("title").getAsString(),
                            p.get("revid").getAsString(),
                            p.getAsJsonObject("wikitext").get("*").getAsString(),
                            clock.getAsLong(),
                            false);
            synchronized (cache) {
                cache.put(title, page);
            }
            return page;
        } catch (IOException | RuntimeException e) {
            if (cached != null && !scope.cancelled())
                return new Page(cached.title, cached.revision, cached.text, cached.fetchedAt, true);
            throw new IOException("Wiki page unavailable: " + e.getMessage(), e);
        }
    }

    private JsonObject get(HttpUrl url, RequestScope scope) throws IOException {
        Call call =
                http.newCall(
                        new Request.Builder()
                                .url(url)
                                .header(
                                        "User-Agent",
                                        "DPS-Loadout-Lab/1.0 (RuneLite;"
                                            + " https://github.com/lynchtm/dps-loadout-lab)")
                                .build());
        scope.attach(call);
        try (Response response = call.execute()) {
            if (!response.isSuccessful() || response.body() == null)
                throw new IOException("Wiki returned HTTP " + response.code());
            String text = bounded(response.body().byteStream());
            JsonObject root = new JsonParser().parse(text).getAsJsonObject();
            if (root.has("error")) throw new IOException("Wiki could not find or parse that page");
            return root;
        }
    }

    static String bounded(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) != -1) {
            if (out.size() + n > MAX_BYTES) throw new IOException("Wiki response too large");
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
