package com.operontech.redblocks.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.operontech.redblocks.ConfigValue;
import com.operontech.redblocks.ConsoleConnection;
import com.operontech.redblocks.RedBlocksMain;
import com.operontech.redblocks.storage.RedBlock;

@SuppressWarnings("deprecation")
public class BlockListener implements Listener {
	private final RedBlocksMain plugin;
	private final ConsoleConnection console;

	public BlockListener(final RedBlocksMain plugin, final ConsoleConnection console) {
		this.plugin = plugin;
		this.console = console;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPlace(final BlockPlaceEvent event) {
		plugin.doBlockUpdate(event.getBlock());
		if (plugin.isEditing(event.getPlayer())) {
			final Block redb = plugin.getRedBlockEditing(event.getPlayer()).getBlock();
			if (redb.isEmpty()) {
				redb.setTypeId(plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID));
			}
			final RedBlock rb = plugin.getRedBlockEditing(event.getPlayer());
			if ((rb.getBlockCount() > plugin.getConfiguration().getInt(ConfigValue.rules_maxBlocksPer)) && !plugin.hasPermission(event.getPlayer(), "bypass.maxBlocksPer")) {
				console.error(event.getPlayer(), "You can't add anymore blocks! The maximum is: " + plugin.getConfiguration().getString(ConfigValue.rules_maxBlocksPer) + " Blocks");
				event.setCancelled(true);
			} else {
				plugin.addBlock(event.getPlayer(), rb, event.getBlock());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(final BlockBreakEvent event) {
		plugin.doBlockUpdate(event.getBlock());
		final Player p = event.getPlayer();
		final Block b = event.getBlock();
		final boolean isRedBlock = plugin.getStorage().containsRedBlock(b);
		if (isRedBlock && p.isSneaking() && (p.getItemInHand().getTypeId() == plugin.getConfiguration().getInt(ConfigValue.redblocks_destroyItem))) {
			// Destroy RedBlock
			if (plugin.hasPermission(p, "createanddestroy")) {
				if (plugin.isEditing(p) && plugin.getRedBlockEditing(p).getLocation().toString().equals(b.getLocation().toString())) {
					plugin.removeEditor(p);
				}
				if (plugin.isBeingEdited(plugin.getStorage().getRedBlock(b))) {
					console.error(p, "You can't destroy a RedBlock that is being edited!");
					event.setCancelled(true);
					return;
				}
				plugin.destroyRedBlock(b);
				b.breakNaturally();
				p.getInventory().remove(plugin.getConfiguration().getInt(ConfigValue.redblocks_destroyItem));
				console.notify(p, "RedBlock Eliminated");
				return;
			} else {
				console.error(p, "You do not have permission to destroy RedBlocks");
			}
		}
		final RedBlock parent = plugin.getStorage().getRedBlockParent(b);
		if (plugin.isEditing(p)) {
			// Stop Editing
			if (isRedBlock) {
				if (!plugin.getRedBlockEditing(p).getLocation().toString().equals(b.getLocation().toString())) {
					console.error(p, "You are already editing a RedBlock! Type " + ChatColor.GOLD + "/rb s" + ChatColor.RED + " to stop editing.");
					event.setCancelled(true);
					return;
				}
				plugin.removeEditor(p);
				event.setCancelled(true);
				return;
			}
			// Remove Command Block
			final Block controlledRB = plugin.getRedBlockEditing(p).getBlock();
			if (controlledRB.isEmpty()) {
				controlledRB.setTypeId(plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID));
			}
			if (plugin.getRedBlockEditing(p).contains(b)) {
				plugin.removeBlock(p, plugin.getRedBlockEditing(p), b);
				return;
			}
		} else {
			// Protection
			if ((parent != null) && !plugin.hasPermission(p, "bypassProtect")) {
				if (parent.isProtected()) {
					console.error(p, "That block is protected by a RedBlock!");
					event.setCancelled(true);
				}
				return;
			}
			// Start Editing
			if (isRedBlock) {
				if (plugin.hasPermission(p, "use")) {
					plugin.addEditor(p, b);
					event.setCancelled(true);
				} else {
					console.error(p, "You don't have the permissiosn to edit RedBlocks");
				}
			} else if ((b.getTypeId() == plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID)) && (b.getRelative(BlockFace.UP).getType() == Material.REDSTONE_WIRE)) {
				// Create RedBlock
				if (plugin.hasPermission(p, "createanddestroy")) {
					if (parent == null) {
						plugin.createRedBlock(p, b);
						event.setCancelled(true);
						return;
					} else {
						console.error(p, "That block is controlled by another RedBlock!");
					}
				} else {
					console.error(p, "You don't have the permissions to create RedBlocks.");
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockDamage(final BlockDamageEvent event) {
		final Player p = event.getPlayer();
		final Block b = event.getBlock();
		final boolean isRedBlock = plugin.getStorage().containsRedBlock(b);
		if (isRedBlock && plugin.isBeingEdited(plugin.getStorage().getRedBlock(b))) {
			// Stop Editing
			plugin.removeEditor(p);
			return;
		} else {
			// Start Editing
			if (isRedBlock) {
				if (plugin.hasPermission(p, "use")) {
					plugin.addEditor(p, b);
					return;
				} else {
					console.error(p, "You do not have the permissions to edit RedBlocks.");
				}
			} else if ((b.getTypeId() == plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID)) && (b.getRelative(BlockFace.UP).getType() == Material.REDSTONE_WIRE)) {
				// Create RedBlock
				if (plugin.hasPermission(p, "createanddestroy")) {
					if (plugin.getStorage().getRedBlockParent(b) == null) {
						plugin.createRedBlock(p, b);
						event.setCancelled(true);
						return;
					} else {
						console.error(p, "That block is controlled by another RedBlock!");
					}
				} else {
					console.error(p, "You don't have the permissions to create RedBlocks.");
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if ((event.getClickedBlock().getType() == Material.DIODE_BLOCK_ON) || ((event.getClickedBlock().getType() == Material.DIODE_BLOCK_OFF) || (event.getClickedBlock().getType() == Material.LEVER))) {
				if (plugin.isEditing(event.getPlayer())) {
					final RedBlock rb = plugin.getRedBlockEditing(event.getPlayer());
					if (rb.contains(event.getClickedBlock())) {
						plugin.updateBlock(event.getPlayer(), plugin.getRedBlockEditing(event.getPlayer()), event.getClickedBlock());
					}
				}
			}
		}
	}
}