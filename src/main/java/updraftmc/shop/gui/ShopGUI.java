package updraftmc.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ShopGUI {

    public static final String TITLE = ChatColor.DARK_AQUA + "ѕʜᴏᴘ";

    private final Player player;

    public ShopGUI(Player player) {
        this.player = player;
    }

    public void open() {
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);

        fillBorder(inventory);

        inventory.setItem(20, createItem(
                Material.GRASS_BLOCK,
                ChatColor.GREEN + "ʙʟᴏᴄᴋѕ",
                List.of(
                        ChatColor.GRAY + "ᴘᴜʀᴄʜᴀѕᴇ ʙᴜɪʟᴅɪɴɢ ѕᴛᴜꜰꜰ",
                        "",
                        ChatColor.YELLOW + "ᴄʟɪᴄᴋ ᴛᴏ ʙʀᴏᴡѕᴇ"
                )
        ));

        inventory.setItem(22, createItem(
                Material.DIAMOND_SWORD,
                ChatColor.RED + "ᴄᴏᴍʙᴀᴛ",
                List.of(
                        ChatColor.GRAY + "ᴡᴇᴀᴘᴏɴѕ,ᴀʀᴍᴏᴜʀ ᴀɴᴅ ɢᴇᴀʀ",
                        "",
                        ChatColor.YELLOW + "ᴄʟɪᴄᴋ ᴛᴏ ʙʀᴏᴡѕᴇ"
                )
        ));

        inventory.setItem(24, createItem(
                Material.WHEAT,
                ChatColor.GOLD + "ꜰᴀʀᴍɪɴɢ",
                List.of(
                        ChatColor.GRAY + "ꜰᴀʀᴍɪɴɢ ɪᴛᴇᴍѕ ᴀɴᴅ ꜰᴏᴏᴅ",
                        "",
                        ChatColor.YELLOW + "ᴄʟɪᴄᴋ ᴛᴏ ʙʀᴏᴡѕᴇ"
                )
        ));

        inventory.setItem(30, createItem(
                Material.DIAMOND_ORE,
                ChatColor.AQUA + "ᴏʀᴇѕ",
                List.of(
                        ChatColor.GRAY + "ᴠᴀᴜʟᴜᴀʙʟᴇ ᴏʀᴇѕ ᴀɴᴅ ᴍᴀᴛᴇʀɪᴀʟѕ",
                        "",
                        ChatColor.YELLOW + "ᴄʟɪᴄᴋ ᴛᴏ ʙʀᴏᴡѕᴇ"
                )
        ));

        inventory.setItem(32, createItem(
                Material.NETHERRACK,
                ChatColor.DARK_RED + "ɴᴇᴛʜᴇʀ",
                List.of(
                        ChatColor.GRAY + "Nether items and resources",
                        "",
                        ChatColor.YELLOW + "ᴄʟɪᴄᴋ ᴛᴏ ʙʀᴏᴡѕᴇ"
                )
        ));

        inventory.setItem(40, createItem(
                Material.BEACON,
                ChatColor.LIGHT_PURPLE + "ᴄᴜѕᴛᴏᴍ",
                List.of(
                        ChatColor.GRAY + "ᴄᴜѕᴛᴏᴍ ᴛᴏᴏʟѕ ᴀɴᴅ ɪᴛᴇᴍѕ",
                        "",
                        ChatColor.YELLOW + "ᴄʟɪᴄᴋ ᴛᴏ ʙʀᴏᴡѕᴇ"
                )
        ));

        inventory.setItem(49, createItem(
                Material.BARRIER,
                ChatColor.RED + "ᴄʟᴏѕᴇ",
                List.of(
                        ChatColor.GRAY + "ᴄʟᴏѕᴇ ᴛʜᴇ ѕʜᴏᴘ"
                )
        ));

        player.openInventory(inventory);
    }

    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                " ",
                List.of()
        );

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }

        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
        }

        for (int i = 17; i < 54; i += 9) {
            inventory.setItem(i, border);
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static boolean isShopInventory(InventoryClickEvent event) {
        return event.getView().getTitle().equals(TITLE);
    }

    public static void handleClick(InventoryClickEvent event) {
        if (!isShopInventory(event)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        switch (event.getRawSlot()) {
            case 20 -> openCategory(player, "blocks");
            case 22 -> openCategory(player, "combat");
            case 24 -> openCategory(player, "farming");
            case 30 -> openCategory(player, "ores");
            case 32 -> openCategory(player, "nether");
            case 40 -> openCategory(player, "custom");

            case 49 -> player.closeInventory();
        }
    }

    private static void openCategory(Player player, String category) {
        player.sendMessage(
                ChatColor.DARK_AQUA + ""
                        + ChatColor.GRAY + "» "
                        + ChatColor.YELLOW + "ᴏᴘᴇɴɪɴɢ"
                        + ChatColor.WHITE + category
                        + ChatColor.YELLOW + "..."
        );
    }
}