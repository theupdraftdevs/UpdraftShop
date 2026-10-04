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

public class CategoryGUI {

    private static final String PREFIX = ChatColor.DARK_AQUA + "Updraft Shop " + ChatColor.GRAY + "» ";

    private final Player player;
    private final String category;

    public CategoryGUI(Player player, String category) {
        this.player = player;
        this.category = category;
    }

    public void open() {
        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                getTitle()
        );

        fillBorder(inventory);

        switch (category.toLowerCase()) {
            case "blocks" -> openBlocks(inventory);
            case "combat" -> openCombat(inventory);
            case "farming" -> openFarming(inventory);
            case "ores" -> openOres(inventory);
            case "nether" -> openNether(inventory);
            case "special" -> openSpecial(inventory);
        }

        inventory.setItem(49, createItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(
                        ChatColor.GRAY + "Return to the main shop"
                )
        ));

        player.openInventory(inventory);
    }

    private void openBlocks(Inventory inventory) {
        inventory.setItem(20, shopItem(
                Material.STONE,
                "Stone",
                10.0,
                5.0
        ));

        inventory.setItem(22, shopItem(
                Material.COBBLESTONE,
                "Cobblestone",
                8.0,
                4.0
        ));

        inventory.setItem(24, shopItem(
                Material.DIRT,
                "Dirt",
                5.0,
                2.0
        ));

        inventory.setItem(30, shopItem(
                Material.SAND,
                "Sand",
                15.0,
                7.0
        ));

        inventory.setItem(32, shopItem(
                Material.GRAVEL,
                "Gravel",
                12.0,
                6.0
        ));
    }

    private void openCombat(Inventory inventory) {
        inventory.setItem(20, shopItem(
                Material.IRON_SWORD,
                "Iron Sword",
                150.0,
                75.0
        ));

        inventory.setItem(22, shopItem(
                Material.DIAMOND_SWORD,
                "Diamond Sword",
                1000.0,
                500.0
        ));

        inventory.setItem(24, shopItem(
                Material.SHIELD,
                "Shield",
                250.0,
                125.0
        ));

        inventory.setItem(30, shopItem(
                Material.BOW,
                "Bow",
                300.0,
                150.0
        ));

        inventory.setItem(32, shopItem(
                Material.ARROW,
                "Arrow",
                5.0,
                2.0
        ));
    }

    private void openFarming(Inventory inventory) {
        inventory.setItem(20, shopItem(
                Material.WHEAT,
                "Wheat",
                15.0,
                7.0
        ));

        inventory.setItem(22, shopItem(
                Material.CARROT,
                "Carrot",
                12.0,
                6.0
        ));

        inventory.setItem(24, shopItem(
                Material.POTATO,
                "Potato",
                12.0,
                6.0
        ));

        inventory.setItem(30, shopItem(
                Material.BREAD,
                "Bread",
                25.0,
                12.0
        ));
    }

    private void openOres(Inventory inventory) {
        inventory.setItem(20, shopItem(
                Material.COAL,
                "Coal",
                30.0,
                15.0
        ));

        inventory.setItem(22, shopItem(
                Material.IRON_INGOT,
                "Iron",
                75.0,
                37.0
        ));

        inventory.setItem(24, shopItem(
                Material.GOLD_INGOT,
                "Gold",
                125.0,
                62.0
        ));

        inventory.setItem(30, shopItem(
                Material.DIAMOND,
                "Diamond",
                500.0,
                250.0
        ));

        inventory.setItem(32, shopItem(
                Material.EMERALD,
                "Emerald",
                750.0,
                375.0
        ));
    }

    private void openNether(Inventory inventory) {
        inventory.setItem(20, shopItem(
                Material.NETHERRACK,
                "Netherrack",
                15.0,
                7.0
        ));

        inventory.setItem(22, shopItem(
                Material.SOUL_SAND,
                "Soul Sand",
                30.0,
                15.0
        ));

        inventory.setItem(24, shopItem(
                Material.QUARTZ,
                "Quartz",
                100.0,
                50.0
        ));

        inventory.setItem(30, shopItem(
                Material.GLOWSTONE,
                "Glowstone",
                75.0,
                37.0
        ));
    }

    private void openSpecial(Inventory inventory) {
        inventory.setItem(20, shopItem(
                Material.ENDER_PEARL,
                "Ender Pearl",
                500.0,
                250.0
        ));

        inventory.setItem(22, shopItem(
                Material.BLAZE_ROD,
                "Blaze Rod",
                350.0,
                175.0
        ));

        inventory.setItem(24, shopItem(
                Material.OBSIDIAN,
                "Obsidian",
                250.0,
                125.0
        ));

        inventory.setItem(30, shopItem(
                Material.EXPERIENCE_BOTTLE,
                "Experience Bottle",
                100.0,
                50.0
        ));
    }

    private ItemStack shopItem(Material material, String name, double buy, double sell) {
        return createItem(
                material,
                ChatColor.WHITE + name,
                List.of(
                        "",
                        ChatColor.GREEN + "Buy: " + ChatColor.WHITE + "$" + buy,
                        ChatColor.RED + "Sell: " + ChatColor.WHITE + "$" + sell,
                        "",
                        ChatColor.YELLOW + "Click to view"
                )
        );
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

    private String getTitle() {
        return PREFIX + capitalize(category);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    public static boolean isCategoryInventory(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        return title.startsWith(PREFIX);
    }

    public static void handleClick(InventoryClickEvent event) {
        if (!isCategoryInventory(event)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() == 49) {
            new ShopGUI(player).open();
            return;
        }

        switch (event.getRawSlot()) {
            case 20, 22, 24, 30, 32 -> {
                ItemStack clicked = event.getCurrentItem();

                if (clicked == null || clicked.getType() == Material.AIR) {
                    return;
                }

                player.sendMessage(
                        PREFIX + ChatColor.YELLOW + "Opening "
                                + ChatColor.WHITE
                                + clicked.getItemMeta().getDisplayName()
                                + ChatColor.YELLOW + "..."
                );
            }
        }
    }
}