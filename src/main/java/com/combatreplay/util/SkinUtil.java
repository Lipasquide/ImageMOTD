package com.combatreplay.util;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class SkinUtil {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static CompletableFuture<TextureProperty> fetchSkin(String uuid) {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", "") + "?unsigned=false";

        return CLIENT.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            if (response.statusCode() != 200) return null;

            try {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray properties = json.getAsJsonArray("properties");
                for (int i = 0; i < properties.size(); i++) {
                    JsonObject prop = properties.get(i).getAsJsonObject();
                    if (prop.get("name").getAsString().equals("textures")) {
                        return new TextureProperty(
                                "textures",
                                prop.get("value").getAsString(),
                                prop.get("signature").getAsString()
                        );
                    }
                }
            } catch (Exception e) {
                return null;
            }
            return null;
        }).exceptionally(ex -> (TextureProperty) null);
    }
}
