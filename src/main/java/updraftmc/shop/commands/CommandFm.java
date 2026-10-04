package updraftmc.shop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import updraftmc.shop.UpdraftShop;

import java.util.List;
import java.util.Objects;

/**
 * Base for every shop command.
 *
 * <p>Subclasses declare their own command name and pass it up, rather than having this
 * constructor guess it from the class name. Two reasons: calling an overridable
 * {@code getName()} from a superclass constructor escapes {@code this} before the
 * subclass is initialised, and deriving the name from the class name silently
 * mismatches {@code plugin.yml}, where a command that is not declared makes
 * {@code getCommand(name)} return null.
 *
 * <p>Registration is best effort. A command whose entry is missing from plugin.yml is
 * left unregistered and logged once, rather than throwing during plugin enable and
 * taking the whole plugin down with it.
 */
public abstract class CommandFm implements CommandExecutor, TabCompleter {

    private final String name;
    private final UpdraftShop plugin;

    protected CommandFm(UpdraftShop plugin, String name) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * Binds this command to its plugin.yml entry.
     *
     * <p>Called from {@code onEnable} rather than the constructor, so nothing escapes
     * {@code this} before the subclass is fully initialised.
     *
     * <p>Best effort: a command missing from plugin.yml is logged and skipped rather
     * than throwing, so one typo cannot stop the plugin from loading.
     */
    public final void register() {
        PluginCommand command = plugin.getCommand(name);

        if (command == null) {
            plugin.getLogger().warning(
                    "Command '" + name + "' is not declared in plugin.yml, /" + name + " will not work.");
            return;
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    /**
     * @return the name this command is declared under in plugin.yml
     */
    public final String getName() {
        return name;
    }

    /**
     * Runs the command.
     *
     * @return false to make Bukkit print the usage line from plugin.yml
     */
    protected abstract boolean execute(CommandContext context);

    /**
     * @return completions for the argument currently being typed, never null
     */
    protected List<String> suggest(CommandContext context) {
        return List.of();
    }

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                  @NotNull String label, @NotNull String[] args) {
        return execute(new CommandContext(sender, label, List.of(args)));
    }

    @Override
    public final List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                            @NotNull String label, @NotNull String[] args) {
        // Bukkit passes a partial final argument; drop it so the index lines up.
        List<String> typed = args.length == 0 ? List.of() : List.of(args).subList(0, args.length - 1);
        String partial = args.length == 0 ? "" : args[args.length - 1];

        return CommandContext.matching(partial, suggest(new CommandContext(sender, label, typed)));
    }
}
