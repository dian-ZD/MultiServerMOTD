package io.Sriptirc_wp_989.multiservermotd.listener;

import io.Sriptirc_wp_989.multiservermotd.config.ConfigManager;
import io.Sriptirc_wp_989.multiservermotd.manager.ServerStatusManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class MOTDListener implements Listener {
    
    private final ConfigManager configManager;
    private final ServerStatusManager statusManager;
    
    public MOTDListener(ConfigManager configManager, ServerStatusManager statusManager) {
        this.configManager = configManager;
        this.statusManager = statusManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(ServerListPingEvent event) {
        try {
            // 调试日志：记录事件被触发
            if (configManager.isDebug()) {
                configManager.getLogger().info("ServerListPingEvent 被触发，开始处理MOTD");
            }
            
            // 安全检查
            if (statusManager == null) {
                configManager.getLogger().warning("statusManager 为 null，无法更新MOTD");
                return;
            }
            
            // 获取总人数和本服人数
            int totalPlayers = statusManager.getTotalPlayers();
            int localPlayers = statusManager.getLocalPlayers();
            
            // 调试日志：显示获取到的人数
            if (configManager.isDebug()) {
                configManager.getLogger().info("获取到的人数 - 总人数: " + totalPlayers + ", 本服人数: " + localPlayers);
            }
            
            // 获取配置的MOTD文本
            String firstLine = configManager.getFirstLine();
            String secondLine = configManager.getSecondLine();
            
            // 替换变量
            firstLine = firstLine.replace("%total%", String.valueOf(totalPlayers))
                                .replace("%online%", String.valueOf(localPlayers));
            secondLine = secondLine.replace("%total%", String.valueOf(totalPlayers))
                                  .replace("%online%", String.valueOf(localPlayers));
            
            // 转换颜色代码
            firstLine = ChatColor.translateAlternateColorCodes('&', firstLine);
            secondLine = ChatColor.translateAlternateColorCodes('&', secondLine);
            
            // 组合成完整的MOTD（两行）
            String motd = firstLine + "\n" + secondLine;
            
            // 调试日志：显示MOTD内容
            if (configManager.isDebug()) {
                configManager.getLogger().info("原始MOTD文本 - 第一行: '" + configManager.getFirstLine() + "', 第二行: '" + configManager.getSecondLine() + "'");
                configManager.getLogger().info("替换后MOTD文本 - 第一行: '" + firstLine + "', 第二行: '" + secondLine + "'");
                configManager.getLogger().info("最终MOTD: '" + motd + "'");
            }
            
            // 设置MOTD
            event.setMotd(motd);
            
            // 调试日志
            if (configManager.isDebug()) {
                configManager.getLogger().info(
                    "MOTD已更新: 总人数=" + totalPlayers + ", 本服人数=" + localPlayers
                );
            }
            
        } catch (Exception e) {
            // 出错时使用默认MOTD，避免影响服务器
            configManager.getLogger().warning(
                "更新MOTD时出错: " + e.getMessage()
            );
        }
    }
}