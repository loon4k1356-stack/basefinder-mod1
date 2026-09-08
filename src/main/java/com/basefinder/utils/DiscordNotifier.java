package com.basefinder.utils;

import com.basefinder.BaseFinderClient;
import net.minecraft.client.MinecraftClient;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordNotifier {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static String webhookUrl = ""; // URL вебхука (нужно задать в конфиге или GUI)

    public static void setWebhook(String url) {
        webhookUrl = url;
    }

    public static void sendNotification(String title, String message, int color) {
        if (webhookUrl.isEmpty()) return;

        executor.submit(() -> {
            try {
                String json = "{\n" +
                        "  \"embeds\": [{\n" +
                        "    \"title\": \"" + title + "\",\n" +
                        "    \"description\": \"" + message + "\",\n" +
                        "    \"color\": " + color + ",\n" +
                        "    \"footer\": {\"text\": \"freezdlc Client\"},\n" +
                        "    \"timestamp\": \"" + java.time.Instant.now().toString() + "\"\n" +
                        "  }]\n" +
                        "}";

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 204 && responseCode != 200) {
                    System.err.println("[freezdlc] Discord Webhook Error: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendBaseFound(String type, double x, double y, double z) {
        String msg = String.format("Найдено: %s\nКоординаты: %.0f, %.0f, %.0f", type, x, y, z);
        sendNotification("🎯 База найдена!", msg, 0x00FF00);
    }
}
