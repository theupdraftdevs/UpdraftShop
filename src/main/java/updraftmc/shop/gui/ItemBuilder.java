package updraftmc.shop.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import updraftmc.shop.config.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small fluent wrapper around {@link ItemStack} metadata so the views read as a list
 * of icons instead of a wall of meta calls.
 *
 * <p>All text is legacy {@code &} colour code, same as config.yml.
 */
public final class ItemBuilder {

    private final ItemStack stack;
    private final List<Component> lore = new ArrayList<>();

    private Component name;

    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public static ItemBuilder of(ItemStack stack) {
        return new ItemBuilder(stack.clone());
    }

    public static Component color(String text) {
        return Messages.color(text);
    }

    public ItemBuilder name(String legacyText) {
        this.name = Messages.color(legacyText);
        return this;
    }

    public ItemBuilder lore(String... lines) {
        Arrays.stream(lines).forEach(line -> this.lore.add(Messages.color(line)));
        return this;
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(Math.max(1, Math.min(amount, stack.getMaxStackSize())));
        return this;
    }

    /**
     * Sets the head on a player head, so a menu can show the owner's own face.
     *
     * <p>Silently does nothing on any other item, so callers do not have to check the
     * material first.
     */
    public ItemBuilder skullOwner(Player player) {
        if (!(stack.getItemMeta() instanceof SkullMeta meta)) {
            return this;
        }

        meta.setOwningPlayer(player);

        stack.setItemMeta(meta);
        return this;
    }

    /**
     * Hides the vanilla tooltip clutter that tools and armour would otherwise show
     * on a shop icon.
     */
    public ItemBuilder hideAttributes() {
        stack.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.displayName(name);
            }

            meta.lore(lore);
            stack.setItemMeta(meta);
        }

        return stack;
    }
}