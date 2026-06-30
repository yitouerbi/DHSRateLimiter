package io.Sriptirc_wp_1484.dhsratelimiter.limiter;

import io.Sriptirc_wp_1484.dhsratelimiter.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 核心限速器
 * 维护一个滑动窗口统计每秒加载的区块数，
 * 超限的区块进入延迟队列，等窗口刷新后再放行。
 */
public class ChunkRateLimiter {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    // 滑动窗口：当前这一秒内已经加载的区块数
    private final AtomicInteger currentSecondCount = new AtomicInteger(0);
    private long lastSecondTimestamp = System.currentTimeMillis() / 1000;

    // 延迟加载队列
    private final Queue<QueuedChunk> pendingQueue = new LinkedList<>();

    // 当前实际生效的每秒限制（取 fixed 和 tps 动态的较小值）
    private volatile int currentLimit = 20;

    // 是否启用
    private volatile boolean enabled = true;

    public ChunkRateLimiter(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * 尝试加载一个区块
     * @return true = 允许加载, false = 被限速（已加入队列）
     */
    public boolean tryLoadChunk(World world, int x, int z) {
        if (!enabled) return true;

        // 刷新每秒计数器
        refreshSecondCounter();

        int fixedLimit = configManager.getFixedChunksPerSecond();
        int dynamicLimit = configManager.getDynamicLimit(Bukkit.getTPS()[0]);

        // 取两者较小值作为实际限制
        int effectiveLimit;
        if (fixedLimit <= 0) {
            effectiveLimit = dynamicLimit;
        } else {
            effectiveLimit = Math.min(fixedLimit, dynamicLimit);
        }
        this.currentLimit = effectiveLimit;

        // 检查是否超限
        if (currentSecondCount.get() < effectiveLimit) {
            currentSecondCount.incrementAndGet();
            if (configManager.isDebug()) {
                plugin.getLogger().info("[DHSRateLimiter] 允许加载区块 [" + world.getName() + "] (" + x + ", " + z + ") 当前秒计数: " + currentSecondCount.get());
            }
            return true;
        }

        // 超限，加入延迟队列
        if (pendingQueue.size() < configManager.getMaxQueueSize()) {
            pendingQueue.offer(new QueuedChunk(world, x, z));
            if (configManager.isDebug()) {
                plugin.getLogger().info("[DHSRateLimiter] 区块 [" + world.getName() + "] (" + x + ", " + z + ") 被限速，加入队列，队列大小: " + pendingQueue.size());
            }
        } else {
            // 队列满了，直接丢弃
            if (configManager.isDebug()) {
                plugin.getLogger().warning("[DHSRateLimiter] 队列已满，丢弃区块 [" + world.getName() + "] (" + x + ", " + z + ")");
            }
        }
        return false;
    }

    /**
     * 从队列中放行区块（由定时任务调用）
     */
    public void processQueue() {
        if (!enabled || pendingQueue.isEmpty()) return;

        refreshSecondCounter();

        int fixedLimit = configManager.getFixedChunksPerSecond();
        int dynamicLimit = configManager.getDynamicLimit(Bukkit.getTPS()[0]);
        int effectiveLimit;
        if (fixedLimit <= 0) {
            effectiveLimit = dynamicLimit;
        } else {
            effectiveLimit = Math.min(fixedLimit, dynamicLimit);
        }

        // 每秒还能放行多少
        int slotsAvailable = effectiveLimit - currentSecondCount.get();
        if (slotsAvailable <= 0) return;

        int released = 0;
        while (!pendingQueue.isEmpty() && released < slotsAvailable) {
            QueuedChunk qc = pendingQueue.poll();
            if (qc == null) break;

            // 检查区块是否已被加载（可能被其他插件加载了）
            Chunk chunk = qc.world.getChunkAt(qc.x, qc.z);
            if (!chunk.isLoaded()) {
                // 强制加载
                qc.world.getChunkAt(qc.x, qc.z);
                currentSecondCount.incrementAndGet();
                released++;
                if (configManager.isDebug()) {
                    plugin.getLogger().info("[DHSRateLimiter] 从队列放行区块 [" + qc.world.getName() + "] (" + qc.x + ", " + qc.z + ")");
                }
            }
            // 如果已经加载了，就不计数，直接跳过
        }
    }

    /**
     * 获取当前队列大小
     */
    public int getQueueSize() {
        return pendingQueue.size();
    }

    /**
     * 获取当前生效的限速值
     */
    public int getCurrentLimit() {
        return currentLimit;
    }

    /**
     * 获取当前秒已加载数
     */
    public int getCurrentSecondCount() {
        return currentSecondCount.get();
    }

    /**
     * 启用/禁用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void refreshSecondCounter() {
        long now = System.currentTimeMillis() / 1000;
        if (now != lastSecondTimestamp) {
            currentSecondCount.set(0);
            lastSecondTimestamp = now;
        }
    }

    /**
     * 队列中的区块信息
     */
    private static class QueuedChunk {
        final World world;
        final int x;
        final int z;

        QueuedChunk(World world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }
    }
}
