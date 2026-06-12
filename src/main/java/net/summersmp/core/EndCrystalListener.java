package net.summersmp.core;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * Stops End Crystals from being placed or crafted.
 * (The crafting recipe is also removed outright in the main class; this is a backup.)
 */
public class EndCrystalListener implements Listener {

    // Block placing the crystal item onto obsidian / bedrock.
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.END_CRYSTAL) {
            event.setCancelled(true);
        }
    }

    // Backup: cancel the actual entity placement, however it was triggered.
    @EventHandler(ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (event.getEntityType() == EntityType.END_CRYSTAL) {
            event.setCancelled(true);
        }
    }

    // Backup: cancel any crafting that would produce an end crystal.
    @EventHandler(ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe != null && recipe.getResult().getType() == Material.END_CRYSTAL) {
            event.getInventory().setResult(null);
        }
    }
}
