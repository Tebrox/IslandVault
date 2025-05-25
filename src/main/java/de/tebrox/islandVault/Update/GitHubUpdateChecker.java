package de.tebrox.islandVault.Update;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class GitHubUpdateChecker {
    private final String user;
    private final String repo;

    public GitHubUpdateChecker(String user, String repo) {
        this.user = user;
        this.repo = repo;
    }

    public void getLatestRelease(Consumer<String> callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + user + "/" + repo + "/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                String body = response.toString();

                String tag = "\"tag_name\":\"";
                int start = body.indexOf(tag);
                if (start == -1) return;
                int end = body.indexOf("\"", start + tag.length());
                if (end == -1) return;
                String latestVersion = body.substring(start + tag.length(), end);

                if (latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                }

                callback.accept(latestVersion);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
