package net.garb.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class SkinLoader {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void loadForUsername(String username, Consumer<SkinResult> onResult) {
        Thread.ofVirtual().start(() -> {
            try {
                String profileUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
                String profileJson = get(profileUrl);
                if (profileJson == null) {
                    onResult.accept(SkinResult.failure("Player not found"));
                    return;
                }

                JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                String uuidStr = profile.get("id").getAsString();
                UUID uuid = UUID.fromString(
                        uuidStr.replaceFirst(
                                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                                "$1-$2-$3-$4-$5"
                        )
                );

                String sessionUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuidStr;
                String sessionJson = get(sessionUrl);
                if (sessionJson == null) {
                    onResult.accept(SkinResult.failure("Could not fetch session"));
                    return;
                }

                JsonObject session = JsonParser.parseString(sessionJson).getAsJsonObject();
                String encodedTextures = session.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();

                String decoded = new String(Base64.getDecoder().decode(encodedTextures));
                JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");

                JsonObject skinObj = textures.getAsJsonObject("SKIN");
                String skinUrl = skinObj.get("url").getAsString();

                boolean slim;
                if (skinObj.has("metadata")) {
                    String model = skinObj.getAsJsonObject("metadata").get("model").getAsString();
                    slim = "slim".equals(model);
                } else {
                    slim = false;
                }

                byte[] imageBytes = getBytes(skinUrl);
                if (imageBytes == null) {
                    onResult.accept(SkinResult.failure("Could not download skin"));
                    return;
                }

                MinecraftClient.getInstance().execute(() -> {
                    try {
                        NativeImage image = NativeImage.read(new ByteArrayInputStream(imageBytes));
                        Identifier textureId = Identifier.of("garb", "skin/" + uuid);
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                        MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);

                        GameProfile gameProfile = new GameProfile(uuid, username);
                        gameProfile.getProperties().put("textures", new Property("textures", encodedTextures));

                        onResult.accept(SkinResult.success(gameProfile, textureId, slim));
                    } catch (Exception e) {
                        onResult.accept(SkinResult.failure("Texture error: " + e.getMessage()));
                    }
                });

            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() ->
                        onResult.accept(SkinResult.failure(e.getMessage()))
                );
            }
        });
    }

    private static String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            return res.body();
        }
        return null;
    }

    private static byte[] getBytes(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() == 200) {
            return res.body();
        }
        return null;
    }

    public record SkinResult(boolean success, GameProfile gameProfile, Identifier textureId, boolean slim, String errorMessage) {

        public static SkinResult success(GameProfile profile, Identifier id, boolean slim) {
            return new SkinResult(true, profile, id, slim, null);
        }

        public static SkinResult failure(String msg) {
            return new SkinResult(false, null, null, false, msg);
        }
    }
}
