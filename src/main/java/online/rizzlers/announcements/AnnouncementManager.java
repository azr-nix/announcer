package online.rizzlers.announcements;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Manages announcement templates, MiniMessage parsing, and configuration loading.
 */
public class AnnouncementManager {
    private final MiniMessage miniMessage;
    private final Logger logger;

    private static final Map<String, String> DEFAULT_KEY_ALIASES = Map.of(
            "re", "restart",
            "mn", "maintenance",
            "ev", "event",
            "up", "update",
            "nw", "news",
            "warn", "warning"
    );

    private final Map<String, String> templates = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();
    private String customPrefixTemplate = "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS ANNOUNCEMENT</bold></gradient> <gray>»</gray> <white>%message%</white>";

    public AnnouncementManager(MiniMessage miniMessage, Logger logger) {
        this.miniMessage = Objects.requireNonNull(miniMessage, "miniMessage must not be null");
        this.logger = logger;
    }

    public synchronized void loadFromConfig(FileConfiguration config) {
        templates.clear();
        messages.clear();

        ConfigurationSection templatesSection = config.getConfigurationSection("templates");
        if (templatesSection != null) {
            for (String key : templatesSection.getKeys(false)) {
                String val = templatesSection.getString(key);
                if (val != null) {
                    templates.put(key.toLowerCase(Locale.ROOT), val);
                }
            }
            if (templatesSection.contains("custom-prefix")) {
                this.customPrefixTemplate = templatesSection.getString("custom-prefix", customPrefixTemplate);
            }
        }

        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                String val = messagesSection.getString(key);
                if (val != null) {
                    messages.put(key.toLowerCase(Locale.ROOT), val);
                }
            }
        }
    }

    public synchronized void loadRaw(Map<String, String> rawTemplates, Map<String, String> rawMessages, String customPrefix) {
        templates.clear();
        messages.clear();
        if (rawTemplates != null) {
            rawTemplates.forEach((k, v) -> templates.put(k.toLowerCase(Locale.ROOT), v));
        }
        if (rawMessages != null) {
            rawMessages.forEach((k, v) -> messages.put(k.toLowerCase(Locale.ROOT), v));
        }
        if (customPrefix != null) {
            this.customPrefixTemplate = customPrefix;
        }
    }

    public synchronized Component buildAnnouncement(String commandKey) {
        if (commandKey == null) return null;
        String lowerKey = commandKey.toLowerCase(Locale.ROOT);

        String template = templates.get(lowerKey);
        if (template == null || template.isBlank()) {
            String alias = DEFAULT_KEY_ALIASES.get(lowerKey);
            if (alias != null) {
                template = templates.get(alias);
            }
        }

        if (template == null || template.isBlank()) {
            return null;
        }

        try {
            return miniMessage.deserialize(template);
        } catch (Exception ex) {
            if (logger != null) {
                logger.warning("Failed to deserialize template for '" + commandKey + "': " + ex.getMessage());
            }
            return Component.text(template);
        }
    }

    public synchronized Component buildCustomAnnouncement(String userMessage) {
        if (userMessage == null) userMessage = "";
        String template = (customPrefixTemplate != null && !customPrefixTemplate.isBlank())
                ? customPrefixTemplate
                : "<white>%message%</white>";

        // Attempt 1: Direct deserialization allowing MiniMessage tags in custom user message
        try {
            String combined = template.replace("%message%", userMessage);
            return miniMessage.deserialize(combined);
        } catch (Exception ex) {
            // Attempt 2: Escape tags in user message safely if user had unclosed/malformed tags
            try {
                String escapedMessage = miniMessage.escapeTags(userMessage);
                String combinedEscaped = template.replace("%message%", escapedMessage);
                return miniMessage.deserialize(combinedEscaped);
            } catch (Exception fallbackEx) {
                if (logger != null) {
                    logger.warning("Failed to parse custom announcement template: " + fallbackEx.getMessage());
                }
                return Component.text(userMessage);
            }
        }
    }

    public synchronized Component getMessage(String key, String defaultTemplate, String... replacements) {
        String template = messages.getOrDefault(key.toLowerCase(Locale.ROOT), defaultTemplate);
        if (replacements != null && replacements.length > 1) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = replacements[i];
                String value = (i + 1 < replacements.length) ? replacements[i + 1] : "";
                template = template.replace(placeholder, value);
            }
        }
        try {
            return miniMessage.deserialize(template);
        } catch (Exception ex) {
            return Component.text(template);
        }
    }

    public synchronized Map<String, String> getTemplates() {
        return Collections.unmodifiableMap(new HashMap<>(templates));
    }

    public synchronized Map<String, String> getMessages() {
        return Collections.unmodifiableMap(new HashMap<>(messages));
    }

    public synchronized String getCustomPrefixTemplate() {
        return customPrefixTemplate;
    }
}
