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

	@EventHandler
	public void onItemSpawn(final ItemSpawnEvent e) {
		final Block b = e.getEntity().getLocation().getBlock();
		if (plugin.isActiveBlock(b)) {
			final RedBlockAnimated parent = plugin.getStorage().getRedBlockParent(b);
			if (plugin.isBeingEdited(parent)) {
				plugin.removeBlock(null, parent, b);
			} else {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(final PlayerQuitEvent e) {
		if (plugin.isEditing(e.getPlayer())) {
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					if (!e.getPlayer().isOnline()) {
						plugin.removeEditor(e.getPlayer());
					}
				}
			}, 20L);
		}
	}
}
