package online.rizzlers.announcements;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class RizzlersAnnouncements extends JavaPlugin {
    private AnnouncementManager manager;
    private AnnouncementCommand commandHandler;

    private static final List<String> COMMANDS = List.of(
            "re", "mn", "ev", "up", "nw", "warn", "custom", "announcements"
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();

        MiniMessage miniMessage = MiniMessage.miniMessage();
        this.manager = new AnnouncementManager(miniMessage, getLogger());
        this.manager.loadFromConfig(getConfig());

        this.commandHandler = new AnnouncementCommand(
                manager,
                Bukkit::broadcast,
                getLogger()::info,
                this::reloadPlugin
        );

        for (String cmdName : COMMANDS) {
            PluginCommand cmd = getCommand(cmdName);
            if (cmd != null) {
                cmd.setExecutor(commandHandler);
                cmd.setTabCompleter(commandHandler);
            } else {
                getLogger().warning("Command '" + cmdName + "' not registered in plugin.yml");
            }
        }

        getLogger().info("RizzlersAnnouncements v1.0.0 enabled successfully. Announcements broadcast locally only.");
    }

    public void reloadPlugin() {
        reloadConfig();
        manager.loadFromConfig(getConfig());
        getLogger().info("RizzlersAnnouncements configuration reloaded.");
    }

    public AnnouncementManager getManager() {
        return manager;
    }

    public AnnouncementCommand getCommandHandler() {
        return commandHandler;
    }
}
