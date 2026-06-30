# DHSRateLimiter

限制 Distant Horizons Support (DHS) 插件 LOD 区块加载速率的 Bukkit 插件。

## 问题

DHS 插件会以最高优先级疯狂加载 LOD 区块，导致服务器 TPS 暴跌甚至卡死。

## 解决方案

本插件通过监听区块加载事件，对 DHS 的区块加载请求进行限速：

- **固定限速**：限制每秒最多加载的区块数
- **TPS 动态调节**：当 TPS 低于阈值时自动收紧限速
- **延迟队列**：超限的区块进入队列，等 TPS 恢复后放行
- **防抖机制**：避免同一区块被反复卸载/加载

## 命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/dhsrate status` | dhsratelimiter.admin | 查看当前限速状态、TPS、队列大小 |
| `/dhsrate reload` | dhsratelimiter.admin | 重载 config.yml 配置 |
| `/dhsrate toggle` | dhsratelimiter.admin | 启用/禁用限速器 |

## 配置

见 `config.yml`，主要参数：

- `fixed-rate.chunks-per-second` - 固定限速（-1 为不限制）
- `tps-throttle.thresholds` - TPS 分档及对应限速值
- `queue.max-queue-size` - 最大排队区块数
- `queue.check-interval-ticks` - 队列检查间隔（tick）
