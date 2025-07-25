package blacklist.plugin.errin.dev;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import jakarta.inject.Inject;
import java.io.*;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "blacklist", name = "BlacklistPlugin", version = "1.0.1", authors = {"Zippy"})
public class Main {

    private final Path dataDirectory;
    private final Logger logger;
    private JsonObject blacklistData;

    @Inject
    public Main(Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        logger.info("Made with <3 (and pain) by Errin (dev@errin.dev)");
        loadBlacklistData();
    }

    protected void loadBlacklistData() {
        Path jsonFile = dataDirectory.resolve("blacklist.json");
        File file = jsonFile.toFile();
        
        // Ensure parent directories exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            logger.info("Creating parent directory: " + parentDir.getAbsolutePath());
            boolean created = parentDir.mkdirs();
            if (!created) {
                logger.warning("Failed to create parent directory: " + parentDir.getAbsolutePath());
            }
        }
        
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                blacklistData = JsonParser.parseReader(reader).getAsJsonObject();
                logger.info("Successfully loaded blacklist.json");
            } catch (IOException e) {
                logger.warning("Failed to load blacklist.json: " + e.getMessage());
                createDefaultBlacklist();
            }
        } else {
            logger.info("blacklist.json does not exist, creating default file");
            createDefaultBlacklist();
            saveBlacklistData(); // Save the default file
        }
    }
    
    private void createDefaultBlacklist() {
        blacklistData = new JsonObject();
        blacklistData.add("ips", new JsonArray());
        blacklistData.add("usernames", new JsonArray());
    }

    private void saveBlacklistData() {
        Path jsonFile = dataDirectory.resolve("blacklist.json");

        try (FileWriter writer = new FileWriter(jsonFile.toFile())) {
            writer.write(blacklistData.toString());
            logger.info("Blacklist data saved successfully.");
        } catch (IOException e) {
            logger.warning("Failed to save blacklist.json: " + e.getMessage());
        }
    }


    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        loadBlacklistData();
        JsonArray ips = blacklistData.getAsJsonArray("ips");
        JsonArray usernames = blacklistData.getAsJsonArray("usernames");

        // Check if the player is already blacklisted
        if (ips.contains(new JsonPrimitive(ip)) || usernames.contains(new JsonPrimitive(username))) {
            logger.info("Player is blacklisted: " + username + " (" + ip + ")");
            player.disconnect(Component.text("You have been blacklisted from this network. If you believe this is in error please create A Secure Ticket on our discord (.gg/afterbloom)"));

            //add missing details
            if (!ips.contains(new JsonPrimitive(ip))) {
                ips.add(ip);
            }
            if (!usernames.contains(new JsonPrimitive(username))) {
                usernames.add(username);
            }

            // Update the JSON object with the new data
            blacklistData.add("ips", ips);
            blacklistData.add("usernames", usernames);

            logger.info("Updated blacklist data: " + blacklistData);

            // Save changes to the file
            saveBlacklistData();
        }




    }
}