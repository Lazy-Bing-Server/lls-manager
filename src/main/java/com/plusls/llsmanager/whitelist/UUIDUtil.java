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
import java.util.UUID;

public class UUIDUtil {
    public static @Nullable UUID getOnlineUUIDFromUserName(String userName) throws IOException {
        URL url = new URL(String.format("https://api.mojang.com/users/profiles/minecraft/%s", userName));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
        } finally {
            conn.disconnect();
        }
        return null;
    }

    public @Nullable static UUID getOfflineUUIDFromUserName(String userName) {
        LlsManager llsManager = LlsManager.getInstance();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(String.format("OfflinePlayer:%s", userName).getBytes());
            bytes[6] &= 0x0F;
            bytes[6] |= 0x30;
            bytes[8] &= 0x3F;
            bytes[8] |= 0x80;
            return UUID.fromString(byteArrayToHex(bytes));
        } catch (NoSuchAlgorithmException e) {
            llsManager.logger.error("No such algo in Message digest", e);
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
            return UUID.fromString(id);
        }
    }
}
