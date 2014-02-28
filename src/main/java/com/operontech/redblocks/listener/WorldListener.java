package com.operontech.redblocks.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;

import com.operontech.redblocks.ConfigValue;
import com.operontech.redblocks.RedBlocksMain;
import com.operontech.redblocks.storage.RedBlock;

public class WorldListener implements Listener {
	private final RedBlocksMain plugin;

	public WorldListener(final RedBlocksMain plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldSave(final WorldSaveEvent e) {
		if (plugin.getConfiguration().getBool(ConfigValue.saveOnWorld)) {
			plugin.getStorage().saveRedBlocks();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemSpawn(final ItemSpawnEvent event) {
		final Block b = event.getEntity().getLocation().getBlock();
		final RedBlock parent = plugin.getStorage().getRedBlockParent(b);
		if (parent != null) {
			if (plugin.isBeingEdited(parent)) {
				plugin.removeBlock(null, parent, b);
				event.setCancelled(true);
			} else {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		if (plugin.isEditing(event.getPlayer())) {
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					if (plugin.getServer().getPlayer(event.getPlayer().getName()) == null) {
						plugin.removeEditor(event.getPlayer());
					}
				}
			}, 20L);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if (event.hasBlock()) {
			if ((event.getClickedBlock().getType() == Material.FIRE) && (event.getAction() == Action.LEFT_CLICK_BLOCK) && plugin.isEditing(event.getPlayer())) {
				final RedBlock rb = plugin.getBlockEditing(event.getPlayer());
				if ((rb != null) && rb.contains(event.getClickedBlock())) {
					rb.remove(event.getClickedBlock());
				}
			}
		}
	}
}
