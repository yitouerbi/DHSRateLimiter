package io.Sriptirc_wp_1484.dhsratelimiter;

import io.Sriptirc_wp_1484.dhsratelimiter.config.ConfigManager;
import io.Sriptirc_wp_1484.dhsratelimiter.limiter.ChunkRateLimiter;
import io.Sriptirc_wp_1484.dhsratelimiter.listener.ChunkLoadListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Dhsratelimiter extends JavaPlugin {

    private ConfigManager configManager;
    private ChunkRateLimiter rateLimiter;

    @Override
    public void onEnable() {
        // 配置管理
        this.configManager = new ConfigManager(this);
        configManager.load();

        // 核心限速器
        this.rateLimiter = new ChunkRateLimiter(this, configManager);

        // 注册事件监听
        Bukkit.getPluginManager().registerEvents(
                new ChunkLoadListener(this, rateLimiter), this
        );

        // 注册命令
        RateCommand command = new RateCommand(this, configManager, rateLimiter);
        getCommand("dhsrate").setExecutor(command);
        getCommand("dhsrate").setTabCompleter(command);

        // 定时任务：从队列中放行区块
        int checkInterval = configManager.getCheckIntervalTicks();
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            rateLimiter.processQueue();
        }, checkInterval, checkInterval);

        getLogger().info("DHSRateLimiter 已启用 - 固定限速: " +
                (configManager.getFixedChunksPerSecond() <= 0 ? "无" : configManager.getFixedChunksPerSecond() + "/秒") +
                " | 队列上限: " + configManager.getMaxQueueSize());
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("DHSRateLimiter 已禁用");
    }
}
