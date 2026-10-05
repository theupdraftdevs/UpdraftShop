package updraftmc.shop.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The parsed form of a command invocation, passed to {@link CommandFm#execute}.
 *
 * <p>Keeps the argument juggling in one place so each command reads as its own logic
 * rather than repeating {@code instanceof Player} checks and index guards.
 */
public record CommandContext(CommandSender sender,
                             String label,
                             List<String> args) {

    public CommandContext {
        args = List.copyOf(args);
    }

    /**
     * @return the sender as a player, or null if the console ran the command
     */
    public Player player() {
        return sender instanceof Player player ? player : null;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public int size() {
        return args.size();
    }

    public boolean isEmpty() {
        return args.isEmpty();
    }

    /**
     * @return the argument at {@code index} lowercased, or {@code fallback} if absent
     */
    public String arg(int index, String fallback) {
        return index < args.size() ? args.get(index).toLowerCase(Locale.ROOT) : fallback;
    }

    /**
     * @return the argument at {@code index} with any trailing "." removed, so item
     *         ids work whether the player types {@code diamond_sword} or
     *         {@code diamond_sword.}
     */
    public String raw(int index) {
        if (index >= args.size()) {
            return "";
        }

        String value = args.get(index);
        return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * @return the argument at {@code index}, or {@code fallback} if absent. Unlike
     *         {@link #arg} this preserves case, for things where case is meaningful.
     */
    public String text(int index, String fallback) {
        return index < args.size() ? args.get(index) : fallback;
    }

    /**
     * @return the argument at {@code index} parsed as an int, or {@code fallback}
     */
    public int number(int index, int fallback) {
        if (index >= args.size()) {
            return fallback;
        }

        try {
            return Integer.parseInt(args.get(index));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /**
     * Resolves an online player by name, case insensitively.
     *
     * @return the player, or null when nobody online has that name
     */
    public Player playerAt(int index) {
        String name = text(index, "");

        if (name.isEmpty()) {
            return null;
        }

        Player exact = Bukkit.getPlayerExact(name);

        if (exact != null) {
            return exact;
        }

        return Bukkit.getPlayerExact(name.toLowerCase(Locale.ROOT));
    }

    /**
     * @return every argument joined with spaces, for echoing usage back
     */
    public String join() {
        return String.join(" ", args);
    }

    /**
     * Case insensitive prefix filter, for tab completion.
     */
    public static List<String> matching(String prefix, List<String> options) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();

        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }

        return matches;
    }

    /**
     * Names of everyone currently online, for tab completion.
     */
    public static List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @NotNull
    @Override
    public String toString() {
        return "/" + label + " " + join();
    }
}