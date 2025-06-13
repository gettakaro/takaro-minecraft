package io.takaro.minecraft;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TakaroPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Takaro Minecraft Plugin has been enabled!");
        getLogger().info("Hello World from Takaro!");
        
        getCommand("takaro").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Takaro Minecraft Plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("takaro")) {
            if (args.length == 0) {
                sender.sendMessage("§a[Takaro] §fHello World! Plugin is running.");
                sender.sendMessage("§a[Takaro] §fVersion: " + getDescription().getVersion());
                
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    sender.sendMessage("§a[Takaro] §fWelcome, " + player.getName() + "!");
                }
                
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
                sender.sendMessage("§a[Takaro] §fPlugin Status: §aActive");
                sender.sendMessage("§a[Takaro] §fPlayers Online: §e" + Bukkit.getOnlinePlayers().size());
                return true;
            }
        }
        return false;
    }
}