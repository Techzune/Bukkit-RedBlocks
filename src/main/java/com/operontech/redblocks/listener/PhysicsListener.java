package com.operontech.redblocks.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import com.operontech.redblocks.ConfigValue;
import com.operontech.redblocks.RedBlocksMain;

public class PhysicsListener implements Listener {
	private final RedBlocksMain plugin;

	public PhysicsListener(final RedBlocksMain plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPhysics(final BlockPhysicsEvent event) {
		if (event.getBlock().getTypeId() == plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID)) {
			plugin.doBlockUpdate(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPistonExtend(final BlockPistonExtendEvent e) {
		for (final Block b : e.getBlocks()) {
			if (plugin.getStorage().getRedBlockParent(b) != null) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPistonPull(final BlockPistonRetractEvent e) {
		if (e.isSticky() && !e.getRetractLocation().getBlock().isEmpty() && (plugin.getStorage().getRedBlockParent(e.getRetractLocation().getBlock()) != null)) {
			e.setCancelled(true);
		}
	}
}
