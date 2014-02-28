package com.operontech.redblocks.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.operontech.redblocks.RedBlocksMain;
import com.operontech.redblocks.storage.RedBlock;

public class RedBlockEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final RedBlocksMain plugin;
	private final RedBlockCause cause;
	private final RedBlock rb;
	private final Player p;
	private boolean cancelled = false;

	public RedBlockEvent(final RedBlocksMain plugin, final RedBlock rb, final RedBlockCause cause) {
		this.plugin = plugin;
		this.rb = rb;
		this.cause = cause;
		p = null;
	}

	public RedBlockEvent(final RedBlocksMain plugin, final RedBlock rb, final RedBlockCause cause, final Player p) {
		this.plugin = plugin;
		this.rb = rb;
		this.cause = cause;
		this.p = p;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * 
	 * @param bool
	 */
	public void setCancelled(final boolean bool) {
		cancelled = bool;
	}

	/**
	 * Checks if is the RedBlock being edited.
	 *
	 * @return true if the RedBlock is being edited
	 */
	public boolean isBlockBeingEdited() {
		return plugin.isBeingEdited(rb);
	}

	/**
	 * Gets the player.
	 *
	 * @return the player
	 */
	public Player getPlayer() {
		return p;
	}

	/**
	 * Gets the cause.
	 *
	 * @return the cause
	 */
	public RedBlockCause getCause() {
		return cause;
	}

	/**
	 * Gets the RedBlock
	 *
	 * @return the RedBlock
	 */
	public RedBlock getRedBlock() {
		return rb;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
