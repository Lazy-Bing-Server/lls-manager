package com.plusls.llsmanager.whitelist;

import com.plusls.llsmanager.LlsManager;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.UUID;

import com.plusls.llsmanager.LlsManager;

public class UUIDUtil {

    public static LlsManager llsManager;

    public static @Nullable UUID getUUID(LlsManager manager, String username_uuid) {
        llsManager = manager;
        manager.server.getConfiguration().isOnlineMode();
        try {
            return UUID.fromString(username_uuid);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            if (manager.server.getConfiguration().isOnlineMode()){
                return getOnlineUUIDFromUserName(username_uuid);
            } else {
                return getOfflineUUIDFromUserName(username_uuid);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static @Nullable UUID getOnlineUUIDFromUserName(String userName) {
        HttpURLConnection conn;
        try {
            URL url = new URL(String.format("https://api.mojang.com/users/profiles/minecraft/%s", userName));
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException ignored) {
            llsManager.logger.error("Error occurred: ", ignored);
            return null;
        }
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.connect();
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                if (null != is) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String inputLine;
                    StringBuilder buffer = new StringBuilder();
                    while ((inputLine = br.readLine()) != null) {
                        buffer.append(inputLine);
                    }
                    br.close();
                    String requestedString = buffer.toString();
                    MinecraftProfile profile = LlsManager.GSON.fromJson(requestedString, MinecraftProfile.class);
                    return profile.getUUID();
                }
            }
        } catch (Exception e) {
            llsManager.logger.error("Error occurred while querying online UUID: ", e);
            return null;
        } finally {
            conn.disconnect();
        }
        return null;
    }

    public @Nullable static UUID getOfflineUUIDFromUserName(String userName) {
        LlsManager llsManager = LlsManager.getInstance();
        try {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + userName).getBytes());
        } catch (IllegalArgumentException e) {
            llsManager.logger.error("Error while getting offline UUID", e);
        }
        return null;
    }

    public static String byteArrayToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(b);
        }
        return stringBuilder.toString();
    }

    public static class MinecraftProfile {
        public String name;
        public String id;

        public UUID getUUID() {
            return UUID.fromString(id.replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"));
        }
    }
}
