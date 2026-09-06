package com.dpscalc.scenario;

import static org.junit.Assert.*;

import okhttp3.*;

import org.junit.Test;

import java.io.*;
import java.util.concurrent.atomic.*;

public class WikiSetupServiceTest {
    @Test
    public void allCapturedComponentNpcSearchesFindTheirEncounterGuide() throws Exception {
        com.google.gson.JsonArray components =
                BossWikiCorpusTest.json("component-searches.json").getAsJsonArray("components");
        assertEquals(33, components.size());
        for (com.google.gson.JsonElement element : components) {
            com.google.gson.JsonObject component = element.getAsJsonObject();
            AtomicInteger requests = new AtomicInteger();
            OkHttpClient http =
                    new OkHttpClient.Builder()
                            .addInterceptor(
                                    chain -> {
                                        com.google.gson.JsonObject captured =
                                                component
                                                        .getAsJsonArray("queries")
                                                        .get(requests.getAndIncrement())
                                                        .getAsJsonObject();
                                        assertEquals(
                                                captured.get("query").getAsString(),
                                                chain.request().url().queryParameter("srsearch"));
                                        com.google.gson.JsonArray hits =
                                                new com.google.gson.JsonArray();
                                        for (String title :
                                                BossWikiCorpusTest.strings(
                                                        captured.getAsJsonArray("searchTitles"))) {
                                            com.google.gson.JsonObject hit =
                                                    new com.google.gson.JsonObject();
                                            hit.addProperty("title", title);
                                            hits.add(hit);
                                        }
                                        com.google.gson.JsonObject query =
                                                new com.google.gson.JsonObject();
                                        query.add("search", hits);
                                        com.google.gson.JsonObject root =
                                                new com.google.gson.JsonObject();
                                        root.add("query", query);
                                        return response(chain.request(), root.toString());
                                    })
                            .build();
            java.util.List<String> results =
                    new WikiSetupService(http)
                            .search(
                                    component.get("component").getAsString(),
                                    new WikiSetupService.RequestScope());
            assertEquals(2, requests.get());
            for (String title :
                    BossWikiCorpusTest.strings(component.getAsJsonArray("expectedTitles")))
                assertTrue(component.get("component") + " -> " + title, results.contains(title));
            assertEquals(results.size(), new java.util.HashSet<>(results).size());
        }
    }

    @Test
    public void exactBossStrategiesOutrankUnrelatedSearchHits() {
        assertEquals(
                java.util.List.of(
                        "The Nightmare/Strategies", "The Nightmare", "Nightmare Zone/Strategies"),
                WikiSetupService.rankSearch(
                        "Nightmare",
                        java.util.List.of(
                                "Nightmare Zone/Strategies",
                                "The Nightmare",
                                "The Nightmare/Strategies")));
        assertEquals(
                "The Gauntlet/Strategies",
                WikiSetupService.rankSearch(
                                "The Gauntlet",
                                java.util.List.of("Sorceress's Garden", "The Gauntlet/Strategies"))
                        .get(0));
    }

    @Test
    public void requestIsWikiOnlyAndCacheFallsBackWithExplicitStaleFlag() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicLong clock = new AtomicLong(1000);
        AtomicBoolean offline = new AtomicBoolean();
        OkHttpClient http =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                chain -> {
                                    calls.incrementAndGet();
                                    Request request = chain.request();
                                    assertEquals("oldschool.runescape.wiki", request.url().host());
                                    assertEquals("https", request.url().scheme());
                                    assertEquals("parse", request.url().queryParameter("action"));
                                    assertEquals(5, request.url().querySize());
                                    if (offline.get()) throw new IOException("offline");
                                    return response(
                                            request,
                                            "{\"parse\":{\"title\":\"Test\",\"revid\":123,\"wikitext\":{\"*\":\"{{Inventory|Shark}}\"}}}");
                                })
                        .build();
        WikiSetupService service = new WikiSetupService(http, clock::get);
        WikiSetupService.Page first = service.page("Test", new WikiSetupService.RequestScope());
        assertFalse(first.stale);
        assertEquals("123", first.revision);
        service.page("Test", new WikiSetupService.RequestScope());
        assertEquals(1, calls.get());
        clock.addAndGet(16 * 60_000);
        offline.set(true);
        WikiSetupService.Page cached = service.page("Test", new WikiSetupService.RequestScope());
        assertTrue(cached.stale);
        assertEquals(first.fetchedAt, cached.fetchedAt);
        WikiSetupService.RequestScope cancelled = new WikiSetupService.RequestScope();
        cancelled.cancel();
        try {
            service.page("Test", cancelled);
            fail();
        } catch (IOException expected) {
            assertEquals("Cancelled", expected.getMessage());
        }
    }

    @Test
    public void searchEncodesInputAndNeverAddsPlayerOrBankData() throws Exception {
        OkHttpClient http =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                chain -> {
                                    Request r = chain.request();
                                    assertTrue(
                                            r.url()
                                                    .queryParameter("srsearch")
                                                    .startsWith("Tombs & raids"));
                                    assertNull(r.body());
                                    assertNull(r.url().queryParameter("player"));
                                    return response(
                                            r,
                                            "{\"query\":{\"search\":[{\"title\":\"Tombs of"
                                                + " Amascut/Strategies\"}]}}");
                                })
                        .build();
        assertEquals(
                "Tombs of Amascut/Strategies",
                new WikiSetupService(http)
                        .search("Tombs & raids", new WikiSetupService.RequestScope())
                        .get(0));
    }

    @Test
    public void responseSizeIsCappedWhileReadingUnknownLengthStreams() throws Exception {
        byte[] oversized = new byte[WikiSetupService.MAX_BYTES + 1];
        try {
            WikiSetupService.bounded(new ByteArrayInputStream(oversized));
            fail();
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("too large"));
        }
        assertEquals(
                "ok", WikiSetupService.bounded(new ByteArrayInputStream(new byte[] {'o', 'k'})));
    }

    @Test
    public void pageCacheIsBoundedAndMalformedPagesFailClearly() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        OkHttpClient http =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                chain -> {
                                    calls.incrementAndGet();
                                    return response(
                                            chain.request(),
                                            "{\"parse\":{\"title\":\"Test\",\"revid\":123,\"wikitext\":{\"*\":\"text\"}}}");
                                })
                        .build();
        WikiSetupService service = new WikiSetupService(http);
        for (int i = 0; i <= WikiSetupService.MAX_CACHE; i++)
            service.page("Page " + i, new WikiSetupService.RequestScope());
        service.page("Page 0", new WikiSetupService.RequestScope());
        assertEquals(WikiSetupService.MAX_CACHE + 2, calls.get());
        http =
                new OkHttpClient.Builder()
                        .addInterceptor(chain -> response(chain.request(), "{\"parse\":{}}"))
                        .build();
        try {
            new WikiSetupService(http).page("Missing", new WikiSetupService.RequestScope());
            fail();
        } catch (IOException expected) {
            assertTrue(expected.getMessage().startsWith("Wiki page unavailable"));
        }
    }

    private static Response response(Request request, String json) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.parse("application/json"), json))
                .build();
    }
}
