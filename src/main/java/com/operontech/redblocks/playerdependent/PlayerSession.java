package com.operontech.redblocks.playerdependent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.operontech.redblocks.Util;
import com.operontech.redblocks.storage.RedBlockAnimated;

public class PlayerSession {
	public Map<String, String> data = new HashMap<String, String>();
	private final UUID p;
	private RedBlockAnimated rb;
	private Block rbBlock;

	/**
	 * Stores player data for the RedBlockAnimated
	 * @param playerUUID the UUID of the player
	 * @param redblock the RedBlockAnimated currently being controlled by the player
	 * @param block the block of the RedBlockAnimated currently being controlled by the player
	 */
	public PlayerSession(final UUID playerUUID, final RedBlockAnimated redblock, final Block block) {
		p = playerUUID;
		rb = redblock;
		rbBlock = block;
		setEnableDelay("0");
		setDisableDelay("0");
	}

	/**
	 * Sets the generic enable delay for future RedBlockChilds placed by the player.
	 * @param placeDelay the delay in milliseconds
	 */
	public void setEnableDelay(final String placeDelay) {
		data.put("enableDelay", placeDelay);
	}

	/**
	 * Sets the generic disable delay for future RedBlockChilds placed by the player.
	 * @param breakDelay the delay in milliseconds
	 */
	public void setDisableDelay(final String breakDelay) {
		data.put("disableDelay", breakDelay);
	}

	/**
	 * Defines a enableDelay and disableDelay for the provided blockInfo
	 * 
	 * blockInfo can be ID without ":DATA"
	 * 
	 * @param blockInfo the information for the block (ID:DATA)
	 */
	public void setBlockDelay(final String blockInfo, final String enableDelay, final String disableDelay) {
		data.put(blockInfo, enableDelay + ":" + disableDelay);
	}

	/**
	 * Sets the RedBlockAnimated that the player is editing in this session.
	 * @param redblock the RedBlockAnimated the player is editing
	 * @param block the block of the RedBlockAnimated
	 */
	public void setRedBlock(final RedBlockAnimated redblock, final Block block) {
		rb = redblock;
		rbBlock = block;
	}

	/**
	 * Gets the enable delay for the specified block
	 * @param b the block to get the delay for
	 * @return if the block is specified, the specified enable delay; otherwise, the generic enable delay
	 */
	@SuppressWarnings("deprecation")
	public int getEnableDelay(final Block b) {
		if (data.containsKey(b.getTypeId() + ":" + b.getData())) {
			final String value = data.get(b.getTypeId() + ":" + b.getData());
			if (Util.isInteger(value.split(":")[0])) {
				return Integer.parseInt(value.split(":")[0]);
			}
		} else if (data.containsKey(b.getTypeId())) {
			final String value = data.get(b.getTypeId());
			if (Util.isInteger(value)) {
				return Integer.parseInt(value);
			}
		}
		return getEnableDelay();
	}

	/**
	 * Gets the generic enable delay for any future RedBlockChilds the player places.
	 * @return the generic enable delay defined by the session
	 */
	public int getEnableDelay() {
		final String value = data.get("enableDelay");
		return (Util.isInteger(value)) ? Integer.parseInt(value) : 0;
	}

	/**
	 * Gets the disable delay for the specified block
	 * @param b the block to get the delay for
	 * @return if the block is specified, the specified disable delay; otherwise, the generic disable delay
	 */
	@SuppressWarnings("deprecation")
	public int getDisableDelay(final Block b) {
		if (data.containsKey(b.getTypeId() + ":" + b.getData())) {
			final String value = data.get(b.getTypeId() + ":" + b.getData());
			if (Util.isInteger(value.split(":")[1])) {
				return Integer.parseInt(value.split(":")[1]);
			}
		} else if (data.containsKey(b.getTypeId())) {
			final String value = data.get(b.getTypeId());
			if (Util.isInteger(value)) {
				return Integer.parseInt(value);
			}
		}
		return getDisableDelay();
	}

	/**
	 * Gets the generic disable delay for any future RedBlockChilds the player places.
	 * @return the generic disable delay defined by the session
	 */
	public int getDisableDelay() {
		final String value = data.get("disableDelay");
		return (Util.isInteger(value)) ? Integer.parseInt(value) : 0;
	}

	/**
	 * Gets a data value for this session.
	 * @param key the data's key
	 * @return the value for the provided key
	 */
	public String getValue(final String key) {
		return data.get(key);
	}

	/**
	 * Sets a data key to a value for this session.
	 * @param key the data's key
	 * @param value the data's value
	 * @return the previous value associated with the data's key
	 */
	public String setValue(final String key, final String value) {
		return data.put(key, value);
	}

	/**
	 * Gets the player defined by this PlayerSession.
	 * @return the player
	 */
	public Player getPlayer() {
		return Bukkit.getServer().getPlayer(p);
	}

	/**
	 * Gets the UUID of the player defined by this PlayerSession.
	 * @return the UUID of the player
	 */
	public UUID getUUID() {
		return p;
	}

	/**
	 * Gets if this session is editing a RedBlockAnimated.
	 * @return true if RedBlockAnimated is not null
	 */
	public boolean isEditingRedBlock() {
		return rb != null;
	}

	/**
	 * Gets the RedBlockAnimated this PlayerSession is editing.
	 * @return the RedBlockAnimated
	 */
	public RedBlockAnimated getRedBlock() {
		return rb;
	}

	/**
	 * Gets the Block for the RedBlockAnimated this PlayerSession is editing.
	 * @return the Block
	 */
	public Block getBlock() {
		return rbBlock;
	}
}
