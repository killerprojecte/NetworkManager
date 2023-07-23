package dev.rgbmc.network.command;

import dev.rgbmc.network.NetworkManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NetworkManagerCommand implements CommandExecutor {
    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        NetworkManager.instance.reloadConfig();
        sender.sendMessage(color("&a配置已重载"));
        NetworkManager.instance.clearCache();
        sender.sendMessage(color("&eURL地址缓存已清除..."));
        return false;
    }
}
