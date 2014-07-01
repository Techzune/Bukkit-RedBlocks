package com.operontech.redblocks.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.operontech.redblocks.ConfigValue;
import com.operontech.redblocks.RedBlocksMain;

public class PhysicsListener implements Listener {
	private final RedBlocksMain plugin;

	public PhysicsListener(final RedBlocksMain plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
		if ((e.getEntityType() == EntityType.FALLING_BLOCK) && plugin.isActiveBlock(e.getBlock())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	@SuppressWarnings("deprecation")
	public void onPhysics(final BlockPhysicsEvent e) {
		if (e.getBlock().getTypeId() == plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID)) {
			plugin.doBlockUpdate(e.getBlock());
		}
	}

	@EventHandler
	public void onPistonExtend(final BlockPistonExtendEvent e) {
		for (final Block b : e.getBlocks()) {
			if (plugin.isActiveBlock(b)) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPistonPull(final BlockPistonRetractEvent e) {
		if (e.isSticky() && !e.getRetractLocation().getBlock().isEmpty() && (plugin.isActiveBlock((e.getRetractLocation().getBlock())))) {
			e.setCancelled(true);
		}
	}
}
