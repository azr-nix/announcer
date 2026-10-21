# RizzlersAnnouncements

A high-performance staff announcement plugin designed for the **RIZZLERS** Minecraft network running **Purpur 26.3** on **Java 25**.

Every command broadcasts announcements **locally** to players on the server instance where the command was executed (e.g. Lobby or Main Survival). Announcements do not cross Velocity proxies or affect other backend servers.

---

## Features

- **Local Backend Scoping:** Executing an announcement command on Lobby broadcasts strictly to Lobby players; executing on Survival broadcasts strictly to Survival players.
- **Rich MiniMessage Formatting:** Full support for hex gradients, bold tags, italics, colors, and font tags via Adventure MiniMessage.
- **Dedicated Quick Commands:** Pre-configured commands for frequent network operations (`/re`, `/mn`, `/ev`, `/up`, `/nw`, `/warn`).
- **Custom Announcement Command:** `/custom <message>` allows staff to broadcast custom formatted announcements with automatic tag safety and fallback.
- **Configuration Reload:** `/announcements reload` updates templates and messages live without requiring a server reboot.
- **Staff Permissions:** Granular permissions for command usage (`rizzlersannouncements.use`) and administration (`rizzlersannouncements.admin`).
- **Zero Local Tooling Requirement:** Built in cloud CI via GitHub Actions using Temurin JDK 25 and Maven.

---

## Commands & Permissions

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/re` | Announce upcoming server restart | `rizzlersannouncements.use` | OP |
| `/mn` | Announce maintenance starting soon | `rizzlersannouncements.use` | OP |
| `/ev` | Announce upcoming community event | `rizzlersannouncements.use` | OP |
| `/up` | Announce server updates being applied | `rizzlersannouncements.use` | OP |
| `/nw` | Announce community news and updates | `rizzlersannouncements.use` | OP |
| `/warn` | Broadcast high-priority staff warning | `rizzlersannouncements.use` | OP |
| `/custom <message>` | Broadcast custom MiniMessage announcement | `rizzlersannouncements.use` | OP |
| `/announcements reload` | Reload configuration templates & messages | `rizzlersannouncements.admin` | OP |

Aliases for `/announcements`: `/rannouncements`, `/rannounce`.

---

## Configuration (`config.yml`)

```yaml
# MiniMessage formatting supported: https://docs.papermc.io/adventure/minimessage/format/
# These announcements are broadcast to players on THIS backend only.
messages:
  no-permission: "<red>You do not have permission to send announcements.</red>"
  reload-success: "<green>RizzlersAnnouncements configuration reloaded successfully.</green>"
  reload-failed: "<red>Failed to reload configuration: %error%</red>"
  custom-usage: "<yellow>Usage: /custom <message></yellow>"
  no-template: "<red>No template configured for announcement '%key%'.</red>"

templates:
  restart: "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS RESTART</bold></gradient> <gray>»</gray> <white>The server will restart shortly. Please find a safe location and prepare to reconnect.</white>"
  maintenance: "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS MAINTENANCE</bold></gradient> <gray>»</gray> <white>Maintenance is starting soon. You may be disconnected while updates are applied.</white>"
  event: "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS EVENT</bold></gradient> <gray>»</gray> <white>A community event is coming up! Stay tuned for details.</white>"
  update: "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS UPDATE</bold></gradient> <gray>»</gray> <white>Server updates are being applied. Thank you for your patience.</white>"
  news: "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS NEWS</bold></gradient> <gray>»</gray> <white>Check our community channels for the latest news and announcements.</white>"
  warning: "<red><bold>RIZZLERS WARNING</bold></red> <gray>»</gray> <white>Please follow the server rules and listen to staff instructions.</white>"
  custom-prefix: "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS ANNOUNCEMENT</bold></gradient> <gray>»</gray> <white>%message%</white>"
```

---

## Installation (Pelican / Plugged.host)

1. Download `RizzlersAnnouncements-1.0.0.jar` from GitHub Releases or GitHub Actions workflow artifacts.
2. Open the **Pelican / Plugged.host** control panel.
3. For each backend server where announcements are desired (e.g. **Lobby** and **Main Survival**):
   - Navigate to **Files** > `plugins/`.
   - Upload `RizzlersAnnouncements-1.0.0.jar`.
   - Restart the backend server.
4. Verify startup in the console:
   ```text
   [INFO] [RizzlersAnnouncements] Enabling RizzlersAnnouncements v1.0.0
   [INFO] [RizzlersAnnouncements] RizzlersAnnouncements v1.0.0 enabled successfully. Announcements broadcast locally only.
   ```
5. Test a command in-game or via console (e.g. `/re` or `/custom <green>Welcome to RIZZLERS!</green>`).
