package io.Sriptirc_wp_1484.dhsratelimiter.listener;

import io.Sriptirc_wp_1484.dhsratelimiter.limiter.ChunkRateLimiter;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

/**
 * 监听区块加载事件
 * 
 * 核心思路：
 * DHS 加载区块 -> 触发 ChunkLoadEvent -> 限速器判断是否超限
 * -> 超限则卸载区块，DHS 下次 tick 会重试 -> 再次被限速
 * -> 自然形成反压，降低 DHS 的加载速率
 * 
 * 防抖机制：短时间内同一个区块被反复卸载/加载，只计一次
 */
public class ChunkLoadListener implements Listener {

    private final JavaPlugin plugin;
    private final ChunkRateLimiter rateLimiter;

    // 防抖集合：记录最近被限速卸载的区块，避免同一区块反复卸载
    private final Set<String> recentThrottled = new HashSet<>();
    private long lastCleanup = System.currentTimeMillis();

    // 防抖时间窗口（毫秒）
    private static final long DEBOUNCE_MS = 2000;

    public ChunkLoadListener(JavaPlugin plugin, ChunkRateLimiter rateLimiter) {
        this.plugin = plugin;
        this.rateLimiter = rateLimiter;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        // 只处理 DHS 加载的区块
        if (!isLikelyDhs()) {
            return;
        }

        // 清理过期的防抖记录
        cleanupDebounce();

        Chunk chunk = event.getChunk();
        String chunkKey = event.getWorld().getName() + ":" + chunk.getX() + "," + chunk.getZ();

        // 如果这个区块刚被限速过，跳过（防抖）
        if (recentThrottled.contains(chunkKey)) {
            return;
        }

        // 尝试限速
        boolean allowed = rateLimiter.tryLoadChunk(
                event.getWorld(),
                chunk.getX(),
                chunk.getZ()
        );

        if (!allowed) {
            // 记录防抖
            recentThrottled.add(chunkKey);

            // 卸载区块，迫使 DHS 重试
            chunk.unload(true);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[DHSRateLimiter] 限速卸载区块: " + chunkKey);
            }
        }
    }

    /**
     * 通过调用栈判断是否由 DHS 触发
     * 性能优化：只检查前 15 层栈帧
     */
    private boolean isLikelyDhs() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int maxDepth = Math.min(stack.length, 15);
        for (int i = 0; i < maxDepth; i++) {
            String className = stack[i].getClassName().toLowerCase();
            if (className.contains("distanthorizons") ||
                    className.contains("dhs") ||
                    className.contains("horizonssupport")) {
                return true;
            }
        }
        return false;
    }

    private void cleanupDebounce() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > DEBOUNCE_MS) {
            recentThrottled.clear();
            lastCleanup = now;
        }
    }
}
