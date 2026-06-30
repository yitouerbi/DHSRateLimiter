package io.Sriptirc_wp_1484.dhsratelimiter.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    // 固定限速
    private int fixedChunksPerSecond;

    // TPS 阈值映射：TPS下限 -> 允许的区块数/秒
    private final NavigableMap<Double, Integer> tpsThresholds = new TreeMap<>();

    // 队列设置
    private int maxQueueSize;
    private int checkIntervalTicks;

    private boolean debug;

    private static final int CONFIG_VERSION = 1;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // 检查配置版本
        int version = config.getInt("ScriptIrc-config-version", 0);
        if (version < CONFIG_VERSION) {
            plugin.getLogger().warning("配置文件版本过旧 (" + version + ")，请删除 config.yml 后重启以生成新配置");
        }

        // 固定限速
        fixedChunksPerSecond = config.getInt("fixed-rate.chunks-per-second", 20);

        // TPS 阈值
        tpsThresholds.clear();
        if (config.contains("tps-throttle.thresholds")) {
            for (String key : config.getConfigurationSection("tps-throttle.thresholds").getKeys(false)) {
                try {
                    double tps = Double.parseDouble(key);
                    int limit = config.getInt("tps-throttle.thresholds." + key);
                    tpsThresholds.put(tps, limit);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("无效的 TPS 阈值键: " + key);
                }
            }
        }

        // 队列设置
        maxQueueSize = config.getInt("queue.max-queue-size", 200);
        checkIntervalTicks = config.getInt("queue.check-interval-ticks", 2);
        debug = config.getBoolean("debug", false);
    }

    /**
     * 根据当前 TPS 获取动态限速值
     * 返回当前档位允许的 chunks-per-second
     */
    public int getDynamicLimit(double currentTps) {
        // 从高到低遍历，找到第一个 TPS 阈值 >= 当前 TPS 的档位
        Map.Entry<Double, Integer> entry = tpsThresholds.floorEntry(currentTps);
        if (entry == null) {
            // 没有匹配的档位，不额外限制（返回一个很大的值）
            return Integer.MAX_VALUE;
        }
        return entry.getValue();
    }

    public int getFixedChunksPerSecond() {
        return fixedChunksPerSecond;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public int getCheckIntervalTicks() {
        return checkIntervalTicks;
    }

    public boolean isDebug() {
        return debug;
    }
}
