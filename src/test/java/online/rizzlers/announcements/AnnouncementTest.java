package online.rizzlers.announcements;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class AnnouncementTest {

    private AnnouncementManager manager;
    private MiniMessage miniMessage;
    private List<Component> broadcasts;
    private List<String> logs;

    @BeforeEach
    public void setUp() {
        miniMessage = MiniMessage.miniMessage();
        manager = new AnnouncementManager(miniMessage, null);
        broadcasts = new ArrayList<>();
        logs = new ArrayList<>();

        Map<String, String> defaultTemplates = Map.of(
                "restart", "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS RESTART</bold></gradient> <gray>»</gray> <white>The server will restart shortly.</white>",
                "maintenance", "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS MAINTENANCE</bold></gradient> <gray>»</gray> <white>Maintenance is starting soon.</white>",
                "event", "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS EVENT</bold></gradient> <gray>»</gray> <white>A community event is coming up!</white>",
                "update", "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS UPDATE</bold></gradient> <gray>»</gray> <white>Server updates are being applied.</white>",
                "news", "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS NEWS</bold></gradient> <gray>»</gray> <white>Check our community channels for news.</white>",
                "warning", "<red><bold>RIZZLERS WARNING</bold></red> <gray>»</gray> <white>Please follow the server rules.</white>"
        );

        Map<String, String> defaultMessages = Map.of(
                "no-permission", "<red>You do not have permission to send announcements.</red>",
                "reload-success", "<green>RizzlersAnnouncements configuration reloaded successfully.</green>",
                "reload-failed", "<red>Failed to reload configuration: %error%</red>",
                "custom-usage", "<yellow>Usage: /custom <message></yellow>",
                "no-template", "<red>No template configured for announcement '%key%'.</red>"
        );

        String customPrefix = "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS ANNOUNCEMENT</bold></gradient> <gray>»</gray> <white>%message%</white>";

        manager.loadRaw(defaultTemplates, defaultMessages, customPrefix);
    }

    private static class DummyCommand extends Command {
        public DummyCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return false;
        }
    }

    private CommandSender createSender(String name, Set<String> permissions, List<Component> sentMessages) {
        return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getName" -> {
                            return name;
                        }
                        case "hasPermission" -> {
                            if (args != null && args.length > 0 && args[0] instanceof String perm) {
                                return permissions.contains(perm);
                            }
                            return false;
                        }
                        case "sendMessage" -> {
                            if (args != null && args.length > 0 && args[0] instanceof Component c) {
                                sentMessages.add(c);
                            }
                            return null;
                        }
                        case "isOp" -> {
                            return permissions.contains("*");
                        }
                        default -> {
                            Class<?> returnType = method.getReturnType();
                            if (returnType == boolean.class) return false;
                            if (returnType == int.class) return 0;
                            return null;
                        }
                    }
                }
        );
    }

    private String toPlain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    @DisplayName("Verify all 6 fixed templates produce formatted components matching their keywords")
    public void testFixedTemplates() {
        String[] commandKeys = {"re", "mn", "ev", "up", "nw", "warn"};
        String[] expectedKeywords = {"RESTART", "MAINTENANCE", "EVENT", "UPDATE", "NEWS", "WARNING"};

        for (int i = 0; i < commandKeys.length; i++) {
            String key = commandKeys[i];
            String expected = expectedKeywords[i];

            Component comp = manager.buildAnnouncement(key);
            assertNotNull(comp, "Template for " + key + " should not be null");
            String plain = toPlain(comp);
            assertTrue(plain.contains(expected), "Plain text for " + key + " should contain '" + expected + "', got: " + plain);
        }
    }

    @Test
    @DisplayName("Verify custom announcement formatting with MiniMessage tags")
    public void testCustomAnnouncementWithTags() {
        String input = "<yellow>Special Event</yellow> at <gold>8:00 PM EST</gold>!";
        Component comp = manager.buildCustomAnnouncement(input);
        assertNotNull(comp);
        String plain = toPlain(comp);
        assertTrue(plain.contains("RIZZLERS ANNOUNCEMENT"));
        assertTrue(plain.contains("Special Event at 8:00 PM EST!"));
    }

    @Test
    @DisplayName("Verify custom announcement handles malformed unclosed tags safely without crashing")
    public void testCustomAnnouncementMalformedTagsSafe() {
        String broken = "<unclosed_tag_with_no_end>Safe fallback test";
        Component comp = assertDoesNotThrow(() -> manager.buildCustomAnnouncement(broken));
        assertNotNull(comp);
        String plain = toPlain(comp);
        assertTrue(plain.contains("Safe fallback test"));
    }

    @Test
    @DisplayName("Verify commands broadcast locally when sender has permission")
    public void testAnnouncementExecutionAuthorized() {
        AnnouncementCommand cmd = new AnnouncementCommand(manager, broadcasts::add, logs::add, () -> {});
        List<Component> senderMessages = new ArrayList<>();
        CommandSender sender = createSender("StaffMember", Set.of(AnnouncementCommand.PERM_USE), senderMessages);

        boolean result = cmd.onCommand(sender, new DummyCommand("re"), "re", new String[0]);
        assertTrue(result);
        assertEquals(1, broadcasts.size(), "Broadcast should be received");
        assertTrue(toPlain(broadcasts.get(0)).contains("RESTART"));
        assertEquals(0, senderMessages.size(), "No error should be sent to sender");
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("re announcement broadcast locally by StaffMember"));
    }

    @Test
    @DisplayName("Verify unauthorized sender receives no-permission message and no broadcast happens")
    public void testAnnouncementExecutionUnauthorized() {
        AnnouncementCommand cmd = new AnnouncementCommand(manager, broadcasts::add, logs::add, () -> {});
        List<Component> senderMessages = new ArrayList<>();
        CommandSender sender = createSender("NormalPlayer", Collections.emptySet(), senderMessages);

        boolean result = cmd.onCommand(sender, new DummyCommand("re"), "re", new String[0]);
        assertTrue(result);
        assertEquals(0, broadcasts.size(), "No broadcast should occur");
        assertEquals(1, senderMessages.size(), "Sender should receive no-permission message");
        assertTrue(toPlain(senderMessages.get(0)).contains("do not have permission"));
    }

    @Test
    @DisplayName("Verify /custom with empty arguments shows usage instructions")
    public void testCustomEmptyArgsUsage() {
        AnnouncementCommand cmd = new AnnouncementCommand(manager, broadcasts::add, logs::add, () -> {});
        List<Component> senderMessages = new ArrayList<>();
        CommandSender sender = createSender("StaffMember", Set.of(AnnouncementCommand.PERM_USE), senderMessages);

        boolean result = cmd.onCommand(sender, new DummyCommand("custom"), "custom", new String[0]);
        assertTrue(result);
        assertEquals(0, broadcasts.size(), "No broadcast on empty args");
        assertEquals(1, senderMessages.size());
        assertTrue(toPlain(senderMessages.get(0)).contains("Usage: /custom <message>"));
    }

    @Test
    @DisplayName("Verify /announcements reload reloads configuration with admin permission")
    public void testAnnouncementsReloadAuthorized() {
        AtomicBoolean reloadInvoked = new AtomicBoolean(false);
        AnnouncementCommand cmd = new AnnouncementCommand(manager, broadcasts::add, logs::add, () -> reloadInvoked.set(true));

        List<Component> senderMessages = new ArrayList<>();
        CommandSender admin = createSender("AdminUser", Set.of(AnnouncementCommand.PERM_ADMIN), senderMessages);

        boolean result = cmd.onCommand(admin, new DummyCommand("announcements"), "announcements", new String[]{"reload"});
        assertTrue(result);
        assertTrue(reloadInvoked.get(), "Reload callback should have run");
        assertEquals(1, senderMessages.size());
        assertTrue(toPlain(senderMessages.get(0)).contains("reloaded successfully"));
    }

    @Test
    @DisplayName("Verify /announcements reload fails without admin permission")
    public void testAnnouncementsReloadUnauthorized() {
        AtomicBoolean reloadInvoked = new AtomicBoolean(false);
        AnnouncementCommand cmd = new AnnouncementCommand(manager, broadcasts::add, logs::add, () -> reloadInvoked.set(true));

        List<Component> senderMessages = new ArrayList<>();
        CommandSender staff = createSender("StaffMember", Set.of(AnnouncementCommand.PERM_USE), senderMessages);

        boolean result = cmd.onCommand(staff, new DummyCommand("announcements"), "announcements", new String[]{"reload"});
        assertTrue(result);
        assertFalse(reloadInvoked.get(), "Reload should not execute without admin permission");
        assertEquals(1, senderMessages.size());
        assertTrue(toPlain(senderMessages.get(0)).contains("do not have permission"));
    }

    @Test
    @DisplayName("Verify tab completion for announcements and custom commands")
    public void testTabCompletion() {
        AnnouncementCommand cmd = new AnnouncementCommand(manager, broadcasts::add, logs::add, () -> {});

        CommandSender admin = createSender("AdminUser", Set.of(AnnouncementCommand.PERM_ADMIN, AnnouncementCommand.PERM_USE), new ArrayList<>());
        CommandSender unauthorized = createSender("Player", Collections.emptySet(), new ArrayList<>());

        // Admin tab completes reload
        List<String> adminAnnounce = cmd.onTabComplete(admin, new DummyCommand("announcements"), "announcements", new String[]{""});
        assertEquals(List.of("reload"), adminAnnounce);

        // Admin tab completes reload with prefix 'rel'
        List<String> adminAnnouncePref = cmd.onTabComplete(admin, new DummyCommand("announcements"), "announcements", new String[]{"rel"});
        assertEquals(List.of("reload"), adminAnnouncePref);

        // Unauthorized gets empty list
        List<String> unauthAnnounce = cmd.onTabComplete(unauthorized, new DummyCommand("announcements"), "announcements", new String[]{""});
        assertTrue(unauthAnnounce.isEmpty());

        // Custom tab complete
        List<String> customTabs = cmd.onTabComplete(admin, new DummyCommand("custom"), "custom", new String[]{""});
        assertEquals(List.of("<message>"), customTabs);
    }

    @Test
    @DisplayName("Verify missing template reports no-template message without exception")
    public void testMissingTemplateHandling() {
        AnnouncementCommand cmd = new AnnouncementCommand(manager, broadcasts::add, logs::add, () -> {});
        List<Component> senderMessages = new ArrayList<>();
        CommandSender staff = createSender("StaffMember", Set.of(AnnouncementCommand.PERM_USE), senderMessages);

        boolean result = cmd.onCommand(staff, new DummyCommand("unknowncmd"), "unknowncmd", new String[0]);
        assertTrue(result);
        assertEquals(0, broadcasts.size());
        assertEquals(1, senderMessages.size());
        assertTrue(toPlain(senderMessages.get(0)).contains("No template configured for announcement 'unknowncmd'"));
    }
}
