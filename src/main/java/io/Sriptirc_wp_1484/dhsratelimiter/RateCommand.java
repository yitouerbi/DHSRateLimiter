package io.Sriptirc_wp_1484.dhsratelimiter;

import io.Sriptirc_wp_1484.dhsratelimiter.config.ConfigManager;
import io.Sriptirc_wp_1484.dhsratelimiter.limiter.ChunkRateLimiter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RateCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ChunkRateLimiter rateLimiter;

    private static final List<String> SUB_COMMANDS = Arrays.asList("status", "reload", "toggle");

    public RateCommand(JavaPlugin plugin, ConfigManager configManager, ChunkRateLimiter rateLimiter) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendStatus(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                sendStatus(sender);
                break;
            case "reload":
                reloadConfig(sender);
                break;
            case "toggle":
                toggleLimiter(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "用法: /dhsrate <status|reload|toggle>");
                break;
        }
        return true;
    }

    private void sendStatus(CommandSender sender) {
        double tps = Bukkit.getTPS()[0];
        int limit = rateLimiter.getCurrentLimit();
        int currentCount = rateLimiter.getCurrentSecondCount();
        int queueSize = rateLimiter.getQueueSize();
        boolean enabled = rateLimiter.isEnabled();

        sender.sendMessage(ChatColor.GOLD + "=== DHSRateLimiter 状态 ===");
        sender.sendMessage(ChatColor.WHITE + "状态: " + (enabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.WHITE + "当前 TPS: " + ChatColor.YELLOW + String.format("%.1f", tps));
        sender.sendMessage(ChatColor.WHITE + "当前限速: " + ChatColor.AQUA + (limit == Integer.MAX_VALUE ? "无限制" : limit + " 区块/秒"));
        sender.sendMessage(ChatColor.WHITE + "当前加载: " + ChatColor.AQUA + currentCount + " 区块/秒");
        sender.sendMessage(ChatColor.WHITE + "排队中: " + ChatColor.AQUA + queueSize + " 个区块");
    }

    private void reloadConfig(CommandSender sender) {
        try {
            configManager.load();
            sender.sendMessage(ChatColor.GREEN + "配置已重载");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "重载配置失败: " + e.getMessage());
            plugin.getLogger().warning("重载配置失败: " + e.getMessage());
        }
    }

    private void toggleLimiter(CommandSender sender) {
        boolean newState = !rateLimiter.isEnabled();
        rateLimiter.setEnabled(newState);
        sender.sendMessage(ChatColor.GREEN + "限速器已" + (newState ? "启用" : "禁用"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
