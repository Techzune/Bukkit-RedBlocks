package com.operontech.redblocks.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import com.operontech.redblocks.RedBlocksMain;

public class PhysicsListener implements Listener {
	private final RedBlocksMain plugin;

	public PhysicsListener(final RedBlocksMain plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockRedstone(final BlockRedstoneEvent event) {
		final Block block = event.getBlock();
		if (block == null) {
			return;
		}
		plugin.doBlockUpdate(block);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPhysics(final BlockPhysicsEvent event) {
		if ((event.getChangedType() == Material.BED_BLOCK) || (event.getChangedType() == Material.SIGN) || (event.getChangedType() == Material.SIGN_POST) || (event.getChangedType() == Material.REDSTONE_WIRE) || (event.getChangedType() == Material.NETHER_WARTS)) {
			if (event.getBlock().getType() == Material.AIR) {
				if (plugin.getStorage().getRedBlockParent(event.getBlock()) != null) {
					event.setCancelled(true);
				}
			}
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
