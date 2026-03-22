# MultiServerMOTD - 多服务器人数显示插件

一个Bukkit/Spigot插件，可以显示多个服务器的总在线人数在MOTD第二行上，第一行支持自定义文本。

## 功能特性

- 显示多个服务器的总在线人数在MOTD上
- MOTD第一行支持自定义文本，支持颜色代码
- 支持变量替换：`%total%`（总人数）和`%online%`（本服人数）
- 定期自动刷新服务器状态
- 支持配置多个远程服务器
- 提供状态查看和配置重载命令
- 灵活的离线服务器处理方式

## 安装

1. 下载编译好的插件jar文件
2. 放入服务器的`plugins/`目录
3. 重启服务器
4. 修改`plugins/MultiServerMOTD/config.yml`配置文件

## 配置

配置文件位于 `plugins/MultiServerMOTD/config.yml`：

```yaml
# MultiServerMOTD 配置文件
# ScriptIrc-config-version: 1

# MOTD 第一行文本（支持颜色代码 &）
first-line: "&a欢迎来到我们的服务器网络！"

# MOTD 第二行文本（支持颜色代码 &）
# 可用变量：
# %total% - 所有服务器的总在线人数
# %online% - 本服务器的在线人数
second-line: "&e总在线人数：&6%total%"

# 服务器列表（格式：地址:端口）
# 支持添加多个服务器
servers:
  - "server2.example.com:25565"
  - "server3.example.com:25566"

# 人数刷新频率（秒）
# 建议不要设置得太低，避免对其他服务器造成压力
refresh-interval: 30

# 离线服务器处理方式
# ignore - 忽略离线服务器，不计入总数
# zero - 视为0人在线，计入总数
# error - 在MOTD中显示错误信息
offline-handling: "ignore"

# 是否启用调试模式
debug: false
```

### 变量说明

- `%total%`: 所有服务器的总在线人数（包括本服）
- `%online%`: 本服务器的在线人数

### 颜色代码

使用 `&` 符号作为颜色代码前缀，例如：
- `&a` - 绿色
- `&e` - 黄色
- `&6` - 金色
- `&c` - 红色
- `&l` - 粗体

## 命令

- `/multiservers` (`/ms`, `/msstatus`) - 显示所有服务器状态信息
- `/multiservers update` - 手动更新服务器状态
- `/multiserversreload` (`/msreload`) - 重载插件配置

## 权限

- `multiservers.use` - 使用 `/multiservers` 命令的权限
- `multiservers.reload` - 使用 `/multiserversreload` 命令的权限

## 使用方法

1. 安装插件后，编辑 `config.yml` 文件
2. 在 `servers` 部分添加要监控的服务器地址和端口
3. 自定义 `first-line` 和 `second-line` 的文本
4. 执行 `/multiserversreload` 重载配置
5. 使用 `/multiservers` 查看服务器状态

## 注意事项

- 插件会定期ping配置的服务器，请合理设置刷新间隔
- 如果某个服务器无法连接，根据 `offline-handling` 设置处理
- MOTD更新是实时的，当有玩家查看服务器列表时生效
- 确保目标服务器开启了服务器列表查询功能

## 版本信息

- 当前版本: 1.0.0
- 支持Minecraft版本: 1.21.x
- 作者:dian-ZD

## 问题反馈

如果遇到任何问题，请检查服务器日志中的错误信息，并确保配置正确。

### 常见问题

#### MOTD完全不显示

1. **检查插件是否正常启用**：查看服务器启动日志，确认插件加载时没有报错。正常情况应看到：
   ```
   [MultiServerMOTD] 正在启用多服务器MOTD插件...
   [MultiServerMOTD] 默认配置文件已保存
   [MultiServerMOTD] 配置管理器已初始化
   [MultiServerMOTD] 服务器状态管理器已初始化
   [MultiServerMOTD] MOTD监听器已初始化
   [MultiServerMOTD] 事件监听器已注册
   [MultiServerMOTD] 命令已注册
   [MultiServerMOTD] 服务器状态更新任务已启动
   [MultiServerMOTD] 多服务器MOTD插件已启用！
   ```

2. **启用调试模式**：在 `config.yml` 中设置 `debug: true`，然后重载插件或重启服务器。当有玩家查看服务器列表时，查看控制台是否显示：
   ```
   [MultiServerMOTD] ServerListPingEvent 被触发，开始处理MOTD
   [MultiServerMOTD] MOTD已更新: 总人数=...
   ```

3. **检查其他插件冲突**：可能有其他插件也修改了MOTD。本插件使用 `EventPriority.HIGHEST` 确保最后执行，但某些插件可能使用相同优先级。

4. **检查命令是否可用**：在游戏中执行 `/multiservers`，查看是否能显示服务器状态信息。

#### 服务器总是显示离线

1. **启用调试模式**：在 `config.yml` 中设置 `debug: true`，然后重载插件或重启服务器。查看控制台日志中的详细错误信息。

2. **检查服务器地址和端口**：确保服务器地址正确，且服务器开启了服务器列表查询功能。

3. **防火墙/网络问题**：确保服务器之间网络连通，防火墙没有阻止查询端口。

4. **协议兼容性**：本插件使用标准Minecraft服务器列表协议（1.7+）。如果目标服务器使用旧版本或自定义配置，可能无法正常响应。

#### 调试信息示例

启用调试模式后，你可能会看到类似以下信息：

**连接成功**：
```
[MultiServerMOTD] 服务器 mc.example.com:25565 在线，人数: 5
```

**连接失败**：
```
[MultiServerMOTD] Ping服务器 example.com:25565 时出错: 无法ping服务器 example.com:25565: Connection timed out
[MultiServerMOTD] 详细错误: java.net.SocketTimeoutException: Connection timed out
[MultiServerMOTD]     at io.Sriptirc_wp_989.multiservermotd.util.ServerPingUtil.getOnlinePlayers(ServerPingUtil.java:94)
```

**事件触发**：
```
[MultiServerMOTD] ServerListPingEvent 被触发，开始处理MOTD
[MultiServerMOTD] MOTD已更新: 总人数=15, 本服人数=10
```

根据错误信息可以判断是网络问题、服务器离线还是协议问题。
