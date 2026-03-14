package io.takaro.minecraft.fabric;

import io.takaro.minecraft.core.EventEmitter;
import io.takaro.minecraft.core.GameAdapter;
import io.takaro.minecraft.core.model.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class FabricGameAdapter implements GameAdapter {

    private final Logger logger;
    private final MinecraftServer server;
    private EventEmitter eventEmitter;

    public FabricGameAdapter(Logger logger, MinecraftServer server) {
        this.logger = logger;
        this.server = server;
    }

    public EventEmitter getEventEmitter() {
        return eventEmitter;
    }

    @Override
    public void logInfo(String msg) { logger.info(msg); }

    @Override
    public void logWarning(String msg) { logger.warn(msg); }

    @Override
    public void logDebug(String msg) { logger.info("[DEBUG] " + msg); }

    @Override
    public void runOnMainThread(Runnable task) { server.execute(task); }

    @Override
    public PlayerInfo getPlayer(String gameId) {
        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(gameId));
        return player != null ? toPlayerInfo(player) : null;
    }

    @Override
    public List<PlayerInfo> getPlayers() {
        return server.getPlayerList().getPlayers().stream()
                .map(this::toPlayerInfo)
                .collect(Collectors.toList());
    }

    @Override
    public PlayerLocation getPlayerLocation(String gameId) {
        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(gameId));
        if (player == null) return null;
        return new PlayerLocation(
                player.getX(), player.getY(), player.getZ(),
                mapDimension(player.level().dimension().location())
        );
    }

    @Override
    public List<InventoryItem> getPlayerInventory(String gameId) {
        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(gameId));
        if (player == null) return Collections.emptyList();
        List<InventoryItem> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                items.add(new InventoryItem(
                        key != null ? key.toString() : "unknown",
                        stack.getHoverName().getString(),
                        stack.getCount(),
                        ""
                ));
            }
        }
        return items;
    }

    @Override
    public List<GameItem> listItems() {
        List<GameItem> items = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item != Items.AIR) {
                var key = BuiltInRegistries.ITEM.getKey(item);
                items.add(new GameItem(
                        key.toString(),
                        item.getDescription().getString(),
                        ""
                ));
            }
        });
        return items;
    }

    @Override
    public List<GameEntity> listEntities() {
        List<GameEntity> entities = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            String category = type.getCategory().isFriendly() ? "friendly" : "hostile";
            entities.add(new GameEntity(
                    key.toString(),
                    type.getDescription().getString(),
                    "",
                    category
            ));
        });
        return entities;
    }

    @Override
    public List<GameLocation> listLocations() {
        return Collections.emptyList();
    }

    @Override
    public void giveItem(String gameId, String itemCode, int amount, String quality) {
        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(gameId));
        if (player == null) return;
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemCode));
        if (item == Items.AIR) return;
        ItemStack stack = new ItemStack(item, amount);
        player.getInventory().add(stack);
    }

    @Override
    public void sendMessage(String message, String recipientGameId) {
        var component = net.minecraft.network.chat.Component.literal(message);
        if (recipientGameId != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(recipientGameId));
            if (player != null) {
                player.sendSystemMessage(component);
            }
        } else {
            server.getPlayerList().broadcastSystemMessage(component, false);
        }
    }

    @Override
    public CommandResult executeConsoleCommand(String command) {
        try {
            var source = server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source, command);
            return new CommandResult(true, "", null);
        } catch (Exception e) {
            return new CommandResult(false, "", e.getMessage());
        }
    }

    @Override
    public void teleportPlayer(String gameId, double x, double y, double z, String dimension) {
        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(gameId));
        if (player == null) return;
        ServerLevel level = dimension != null ? getLevelForDimension(dimension) : (ServerLevel) player.level();
        if (level == null) level = (ServerLevel) player.level();
        player.teleportTo(level, x, y, z, Set.of(), player.getYRot(), player.getXRot());
    }

    @Override
    public void kickPlayer(String gameId, String reason) {
        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(gameId));
        if (player != null) {
            player.connection.disconnect(net.minecraft.network.chat.Component.literal(reason));
        }
    }

    @Override
    public void banPlayer(String gameId, String reason, String expiresAt) {
        UUID uuid = UUID.fromString(gameId);
        var profile = server.getProfileCache().get(uuid).orElse(null);
        if (profile == null) {
            profile = new com.mojang.authlib.GameProfile(uuid, "");
        }
        Date expiry = null;
        if (expiresAt != null) {
            expiry = Date.from(java.time.Instant.parse(expiresAt));
        }
        var entry = new UserBanListEntry(profile, null, "Takaro", expiry, reason);
        server.getPlayerList().getBans().add(entry);
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            player.connection.disconnect(net.minecraft.network.chat.Component.literal("Banned: " + reason));
        }
    }

    @Override
    public void unbanPlayer(String gameId) {
        UUID uuid = UUID.fromString(gameId);
        var profile = server.getProfileCache().get(uuid).orElse(null);
        if (profile == null) {
            profile = new com.mojang.authlib.GameProfile(uuid, "");
        }
        server.getPlayerList().getBans().remove(profile);
    }

    @Override
    public List<BanEntry> listBans() {
        List<BanEntry> bans = new ArrayList<>();
        var banList = server.getPlayerList().getBans();
        for (String key : banList.getUserList()) {
            // Iterate via getUserList keys, parse UUID from the key
            try {
                UUID uuid = UUID.fromString(key);
                var profile = server.getProfileCache().get(uuid).orElse(
                        new com.mojang.authlib.GameProfile(uuid, "")
                );
                var entry = banList.get(profile);
                if (entry != null) {
                    String expiresAt = entry.getExpires() != null ? entry.getExpires().toInstant().toString() : null;
                    bans.add(new BanEntry(
                            profile.getId() != null ? profile.getId().toString() : "",
                            profile.getName() != null ? profile.getName() : "",
                            entry.getReason(),
                            expiresAt
                    ));
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Skipping ban entry with non-UUID key '{}': {}", key, e.getMessage());
            }
        }
        return bans;
    }

    @Override
    public void shutdownServer() {
        server.halt(false);
    }

    @Override
    public void setEventEmitter(EventEmitter emitter) {
        this.eventEmitter = emitter;
    }

    // --- Helpers ---

    PlayerInfo toPlayerInfo(ServerPlayer player) {
        String ip = player.getIpAddress();
        return new PlayerInfo(
                player.getUUID().toString(),
                player.getGameProfile().getName(),
                null, null, null,
                "minecraft",
                ip != null ? ip : "",
                player.connection.latency()
        );
    }

    String mapDimension(ResourceLocation dimensionId) {
        String path = dimensionId.toString();
        return switch (path) {
            case "minecraft:overworld" -> "overworld";
            case "minecraft:the_nether" -> "nether";
            case "minecraft:the_end" -> "the_end";
            default -> path;
        };
    }

    private ServerLevel getLevelForDimension(String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (mapDimension(level.dimension().location()).equals(dimension)) {
                return level;
            }
        }
        return null;
    }
}
