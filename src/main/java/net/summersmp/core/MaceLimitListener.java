package net.summersmp.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 1) Caps the number of Heavy Cores that can be picked up server-wide (= Mace cap).
 * 2) Blocks Heavy Cores and Maces from being stored in ender chests.
 */
public class MaceLimitListener implements Listener {

    private final SummerSMPCore plugin;

    public MaceLimitListener(SummerSMPCore plugin) {
        this.plugin = plugin;
    }

    private boolean isRestricted(ItemStack stack) {
        if (stack == null) return false;
        Material m = stack.getType();
        return m == Material.HEAVY_CORE || m == Material.MACE;
    }

    // ---------- Heavy core cap ----------

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        Item itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItemStack();
        if (stack.getType() != Material.HEAVY_CORE) return;

        ItemMeta meta = stack.getItemMeta();
        boolean alreadyCounted = meta != null
                && meta.getPersistentDataContainer().has(plugin.countedKey, PersistentDataType.BYTE);
        if (alreadyCounted) return; // counted before (e.g. dropped then re-picked) — let it through

        Player player = event.getPlayer();

        if (plugin.getHeavyCoresFound() >= plugin.getMaceLimit()) {
            // Limit reached — this heavy core "cannot be found".
            event.setCancelled(true);
            itemEntity.remove();
            if (player != null) {
                player.sendMessage(ChatColor.RED + "This server has reached its limit of "
                        + plugin.getMaceLimit() + " Maces, so this Heavy Core crumbles to dust.");
            }
            return;
        }

        // Count it and tag the item so it is never double-counted.
        plugin.incrementHeavyCores();
        if (meta != null) {
            meta.getPersistentDataContainer().set(plugin.countedKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
            itemEntity.setItemStack(stack);
        }
        if (plugin.announceClaims()) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "A Heavy Core has been claimed! "
                    + ChatColor.YELLOW + "(" + plugin.getHeavyCoresFound() + "/" + plugin.getMaceLimit()
                    + " Maces now exist on the server.)");
        }
    }

    // ---------- Ender chest restriction ----------

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST) return;

        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();

        // Shift-clicking a restricted item from the bottom inventory into the ender chest.
        if (event.isShiftClick() && rawSlot >= topSize) {
            if (isRestricted(event.getCurrentItem())) {
                deny(event);
            }
            return;
        }

        // Acting on a top (ender chest) slot.
        if (rawSlot >= 0 && rawSlot < topSize) {
            // Placing the held cursor item.
            if (isRestricted(event.getCursor())) {
                deny(event);
                return;
            }
            // Number-key swap from a hotbar slot.
            if (event.getClick() == ClickType.NUMBER_KEY) {
                int button = event.getHotbarButton();
                if (button >= 0) {
                    ItemStack hotbarItem = event.getView().getBottomInventory().getItem(button);
                    if (isRestricted(hotbarItem)) {
                        deny(event);
                        return;
                    }
                }
            }
            // Off-hand swap (F key).
            if (event.getClick() == ClickType.SWAP_OFFHAND && event.getWhoClicked() instanceof Player) {
                ItemStack offhand = ((Player) event.getWhoClicked()).getInventory().getItemInOffHand();
                if (isRestricted(offhand)) {
                    deny(event);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST) return;
        if (!isRestricted(event.getOldCursor())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(ChatColor.RED
                        + "You can't store Heavy Cores or Maces in an ender chest.");
                return;
            }
        }
    }

    private void deny(InventoryClickEvent event) {
        event.setCancelled(true);
        event.getWhoClicked().sendMessage(ChatColor.RED
                + "You can't store Heavy Cores or Maces in an ender chest.");
    }
}
