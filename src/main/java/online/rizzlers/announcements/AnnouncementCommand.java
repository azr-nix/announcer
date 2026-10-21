package online.rizzlers.announcements;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Handles announcement commands (/re, /mn, /ev, /up, /nw, /warn, /custom, /announcements reload).
 */
public class AnnouncementCommand implements CommandExecutor, TabCompleter {
    public static final String PERM_USE = "rizzlersannouncements.use";
    public static final String PERM_ADMIN = "rizzlersannouncements.admin";

    private final AnnouncementManager manager;
    private final Consumer<Component> broadcaster;
    private final Consumer<String> logger;
    private final Runnable reloadAction;

    public AnnouncementCommand(AnnouncementManager manager,
                               Consumer<Component> broadcaster,
                               Consumer<String> logger,
                               Runnable reloadAction) {
        this.manager = Objects.requireNonNull(manager, "manager must not be null");
        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster must not be null");
        this.logger = logger;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String key = command.getName().toLowerCase(Locale.ROOT);

        if (key.equals("announcements")) {
            if (!sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(manager.getMessage("no-permission",
                        "<red>You do not have permission to send announcements.</red>"));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                try {
                    if (reloadAction != null) {
                        reloadAction.run();
                    }
                    sender.sendMessage(manager.getMessage("reload-success",
                            "<green>RizzlersAnnouncements configuration reloaded successfully.</green>"));
                } catch (Exception ex) {
                    sender.sendMessage(manager.getMessage("reload-failed",
                            "<red>Failed to reload configuration: %error%</red>",
                            "%error%", ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
                }
                return true;
            }

            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<yellow>Usage: /announcements reload</yellow>"));
            return true;
        }

        // Announcement commands require use permission
        if (!sender.hasPermission(PERM_USE)) {
            sender.sendMessage(manager.getMessage("no-permission",
                    "<red>You do not have permission to send announcements.</red>"));
            return true;
        }

        if (key.equals("custom")) {
            if (args.length == 0) {
                sender.sendMessage(manager.getMessage("custom-usage",
                        "<yellow>Usage: /custom <message></yellow>"));
                return true;
            }
            String userMessage = String.join(" ", args);
            Component component = manager.buildCustomAnnouncement(userMessage);
            broadcaster.accept(component);
            if (logger != null) {
                logger.accept("Custom announcement broadcast locally by " + sender.getName());
            }
            return true;
        }

        Component component = manager.buildAnnouncement(key);
        if (component == null) {
            sender.sendMessage(manager.getMessage("no-template",
                    "<red>No template configured for announcement '%key%'.</red>",
                    "%key%", key));
            return true;
        }

        broadcaster.accept(component);
        if (logger != null) {
            logger.accept(key + " announcement broadcast locally by " + sender.getName());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String key = command.getName().toLowerCase(Locale.ROOT);
        if (key.equals("announcements")) {
            if (args.length == 1 && sender.hasPermission(PERM_ADMIN)) {
                if ("reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    return List.of("reload");
                }
            }
            return Collections.emptyList();
        }
        if (key.equals("custom")) {
            if (args.length == 1 && sender.hasPermission(PERM_USE)) {
                return List.of("<message>");
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
