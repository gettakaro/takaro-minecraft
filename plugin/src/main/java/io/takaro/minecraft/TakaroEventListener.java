package io.takaro.minecraft;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Logger;

public class TakaroEventListener implements Listener {
    
    private final TakaroPlugin plugin;
    private final Logger logger;
    
    public TakaroEventListener(TakaroPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        logger.info("Player join event triggered: " + player.getName());
        
        JsonObject eventData = new JsonObject();
        eventData.add("player", createPlayerDataWithDetails(player));
        
        sendGameEvent("player-connected", eventData);
        logger.info("Player connected event processed: " + player.getName());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        logger.info("Player quit event triggered: " + player.getName());
        
        JsonObject eventData = new JsonObject();
        eventData.add("player", createPlayerData(player));
        
        sendGameEvent("player-disconnected", eventData);
        logger.info("Player disconnected event processed: " + player.getName());
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        logger.info("Chat event triggered: " + player.getName() + ": " + message);
        
        // Since this is an async event, schedule the Takaro event sending on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            JsonObject eventData = new JsonObject();
            eventData.add("player", createPlayerData(player));
            eventData.addProperty("channel", "global");
            eventData.addProperty("msg", message);
            
            sendGameEvent("chat-message", eventData);
            logger.info("Chat message event processed: " + player.getName());
        });
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        JsonObject eventData = new JsonObject();
        eventData.add("player", createPlayerData(player));
        
        // Add attacker if it was a PvP death
        if (player.getKiller() != null) {
            eventData.add("attacker", createPlayerData(player.getKiller()));
        }
        
        // Add position
        JsonObject position = new JsonObject();
        position.addProperty("x", player.getLocation().getX());
        position.addProperty("y", player.getLocation().getY());
        position.addProperty("z", player.getLocation().getZ());
        position.addProperty("world", player.getLocation().getWorld().getName());
        eventData.add("position", position);
        
        sendGameEvent("player-death", eventData);
        logger.info("Player death event sent: " + player.getName());
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Only send if killed by a player
        if (event.getEntity().getKiller() instanceof Player) {
            Player killer = (Player) event.getEntity().getKiller();
            
            JsonObject entityData = new JsonObject();
            entityData.addProperty("type", event.getEntity().getType().name());
            entityData.addProperty("name", event.getEntity().getType().name().toLowerCase().replace("_", " "));
            
            JsonObject eventData = new JsonObject();
            eventData.add("player", createPlayerData(killer));
            eventData.add("entity", entityData);
            
            // Add weapon if player has item in hand
            if (killer.getInventory().getItemInMainHand() != null && 
                killer.getInventory().getItemInMainHand().getType().name() != "AIR") {
                JsonObject weapon = new JsonObject();
                weapon.addProperty("code", killer.getInventory().getItemInMainHand().getType().name());
                weapon.addProperty("name", killer.getInventory().getItemInMainHand().getType().name().toLowerCase().replace("_", " "));
                eventData.add("weapon", weapon);
            }
            
            sendGameEvent("entity-killed", eventData);
            
            if (plugin.getConfig().getBoolean("takaro.logging.debug", false)) {
                logger.info("Entity killed event sent: " + killer.getName() + " killed " + event.getEntity().getType().name());
            }
        }
    }
    
    private void sendGameEvent(String eventType, JsonObject data) {
        TakaroWebSocketClient client = plugin.getWebSocketClient();
        if (client == null) {
            logger.warning("Cannot send " + eventType + " event - WebSocket client is null");
            return;
        }
        
        if (!client.isAuthenticated()) {
            logger.warning("Cannot send " + eventType + " event - not authenticated");
            return;
        }
        
        logger.info("Sending " + eventType + " event to Takaro");
        client.sendGameEvent(eventType, data);
        logger.info("Successfully sent " + eventType + " event to Takaro");
    }
    
    private JsonObject createPlayerData(Player player) {
        TakaroWebSocketClient client = plugin.getWebSocketClient();
        if (client != null) {
            return client.createPlayerData(player);
        }
        // Fallback if client is not available
        JsonObject playerData = new JsonObject();
        playerData.addProperty("gameId", player.getUniqueId().toString());
        playerData.addProperty("name", player.getName());
        playerData.addProperty("platformId", "minecraft:" + player.getUniqueId().toString());
        return playerData;
    }
    
    private JsonObject createPlayerDataWithDetails(Player player) {
        TakaroWebSocketClient client = plugin.getWebSocketClient();
        if (client != null) {
            return client.createPlayerDataWithDetails(player);
        }
        // Fallback if client is not available
        return createPlayerData(player);
    }
}