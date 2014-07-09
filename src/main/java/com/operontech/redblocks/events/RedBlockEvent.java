package com.operontech.redblocks.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.operontech.redblocks.RedBlocksMain;
import com.operontech.redblocks.storage.RedBlockAnimated;

public class RedBlockEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final RedBlocksMain plugin;
	private final RedBlockCause cause;
	private final RedBlockAnimated rb;
	private final Player p;
	private boolean cancelled = false;

	public RedBlockEvent(final RedBlocksMain plugin, final RedBlockAnimated rb, final RedBlockCause cause) {
		this.plugin = plugin;
		this.rb = rb;
		this.cause = cause;
		p = null;
	}

	public RedBlockEvent(final RedBlocksMain plugin, final RedBlockAnimated rb, final RedBlockCause cause, final Player p) {
		this.plugin = plugin;
		this.rb = rb;
		this.cause = cause;
		this.p = p;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Sets the cancellation state of this event. A cancelled event will not be executed in the server, but will still pass to other plugins.
	 *
	 * @param bool true if you wish to cancel this event
	 */
	public void setCancelled(final boolean bool) {
		cancelled = bool;
	}

	/**
	 * Checks if is the RedBlockAnimated being edited.
	 *
	 * @return true if the RedBlockAnimated is being edited
	 */
	public boolean isBlockBeingEdited() {
		return plugin.isBeingEdited(rb);
	}

	/**
	 * Gets the player involved in this event.
	 *
	 * @return the player involved
	 */
	public Player getPlayer() {
		return p;
	}

	/**
	 * Gets the cause of the event being triggered.
	 *
	 * @return the cause of the event
	 */
	public RedBlockCause getCause() {
		return cause;
	}

	/**
	 * Gets the RedBlockAnimated involved in the event.
	 *
	 * @return the RedBlockAnimated involved
	 */
	public RedBlockAnimated getRedBlock() {
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
