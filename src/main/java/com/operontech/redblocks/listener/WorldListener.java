package com.operontech.redblocks.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.operontech.redblocks.RedBlocksMain;
import com.operontech.redblocks.storage.RedBlockAnimated;

public class WorldListener implements Listener {
	private final RedBlocksMain plugin;

	public WorldListener(final RedBlocksMain plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemSpawn(final ItemSpawnEvent event) {
		final Block b = event.getEntity().getLocation().getBlock();
		if (plugin.isActiveBlock(b)) {
			final RedBlockAnimated parent = plugin.getStorage().getRedBlockParent(b);
			if (plugin.isBeingEdited(parent) && b.isEmpty()) {
				plugin.removeBlock(null, parent, b);
			}
			event.setCancelled(true);
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
}
