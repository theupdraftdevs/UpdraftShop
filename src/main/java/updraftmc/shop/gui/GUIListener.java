package updraftmc.shop.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import updraftmc.shop.gui.ShopGUI;

public class GUIListener implements Listener {

     @EventHandler
     public void onInventoryClick(InventoryClickEvent event) {
     if (ShopGUI.isShopInventory(event)) {
         ShopGUI.handleClick(event);
         return;
     }
     if (CategoryGUI.isCategoryInventory(event)) {
         CategoryGUI.handleClick(event);
     }
     }
}