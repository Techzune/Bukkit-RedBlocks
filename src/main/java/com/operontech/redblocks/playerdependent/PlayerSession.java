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
		setEnableDelayBlock("0", "-1");
		setDisableDelay("0");
		setDisableDelayBlock("0", "-1");
	}

	/**
	 * Sets the enable delay for future RedBlockChilds placed by the player.
	 * @param placeDelay the delay in milliseconds
	 */
	public void setEnableDelay(final String placeDelay) {
		data.put("enableDelay", placeDelay);
	}

	/**
	 * Sets the type of RedBlockChild to apply the player's enableDelay to.
	 * 
	 * If the blockData is -1, there is no preferred blockData set.
	 * 
	 * @param blockId the blockId for the RedBlockChild
	 * @param blockData the blockData for the RedBlockChild
	 */
	public void setEnableDelayBlock(final String blockId, final String blockData) {
		data.put("enableDelayBlockId", blockId);
		data.put("enableDelayBlockData", blockData);
	}

	/**
	 * Sets the disable delay for future RedBlockChilds placed by the player.
	 * @param breakDelay the delay in milliseconds
	 */
	public void setDisableDelay(final String breakDelay) {
		data.put("disableDelay", breakDelay);
	}

	/**
	 * Sets the type of RedBlockChild to apply the player's disableDelay to.
	 * 
	 * If the blockData is -1, there is no preferred blockData set.
	 * 
	 * @param blockId the blockId for the RedBlockChild
	 * @param blockData the blockData for the RedBlockChild
	 */
	public void setDisableDelayBlock(final String blockId, final String blockData) {
		data.put("enableDelayBlockId", blockId);
		data.put("disableDelayBlockData", blockData);
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
	 * Gets the enable delay for any future RedBlockChilds the player places.
	 * @return the enable delay set for this session
	 */
	public int getEnableDelay() {
		final String value = data.get("enableDelay");
		return (Util.isInteger(value)) ? Integer.parseInt(value) : 0;
	}

	/**
	 * Gets the type (blockId) of RedBlockChild the player's enable delay will apply to.
	 * @return the type (blockId) of RedBlockChild for this session
	 */
	public int getEnableDelayBlockId() {
		final String value = data.get("enableDelayBlockId");
		return (Util.isInteger(value)) ? Integer.parseInt(value) : 0;
	}

	/**
	 * Gets the type (blockData) of RedBlockChild the player's enable delay will apply to.
	 * 
	 * If the blockData is -1, there is no preferred blockData set.
	 * 
	 * @return the type (blockData) of RedBlockChild set for this session
	 */
	public int getEnableDelayBlockData() {
		final String value = data.get("enableDelayBlockData");
		return (Util.isInteger(value)) ? Integer.parseInt(value) : -1;
	}

	/**
	 * Checks if the supplied block matches the blockId and blockData defined by their respective methods.
	 * @param b the block to match
	 * @return if the block matches
	 */
	@SuppressWarnings("deprecation")
	public boolean getEnableDelayBlockMatch(final Block b) {
		if ((getEnableDelayBlockId() == 0) || (getEnableDelayBlockId() == b.getTypeId())) {
			if ((getEnableDelayBlockData() == -1) || (getEnableDelayBlockData() == b.getData())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the disable delay for any future RedBlockChilds the player places.
	 * @return the disable delay set for this session
	 */
	public int getDisableDelay() {
		final String value = data.get("disableDelay");
		return (Util.isInteger(value)) ? Integer.parseInt(value) : 0;
	}

	/**
	 * Gets the type (blockId) of RedBlockChild the player's disable delay will apply to.
	 * @return the type (blockId) of RedBlockChild for this session
	 */
	public int getDisableDelayBlockId() {
		final String value = data.get("disableDelayBlockId");
		return (Util.isInteger(value)) ? Integer.parseInt(value) : 0;
	}

	/**
	 * Gets the type (blockData) of RedBlockChild the player's disable delay will apply to.
	 * 
	 * If the blockData is -1, there is no preferred blockData set.
	 * 
	 * @return the type (blockData) of RedBlockChild set for this session
	 */
	public int getDisableDelayBlockData() {
		final String value = data.get("disableDelayBlockData");
		return (Util.isInteger(value)) ? Integer.parseInt(value) : -1;
	}

	/**
	 * Checks if the supplied block matches the blockId and blockData defined by their respective methods.
	 * @param b the block to match
	 * @return if the block matches
	 */
	@SuppressWarnings("deprecation")
	public boolean getDisableDelayBlockMatch(final Block b) {
		if ((getDisableDelayBlockId() == 0) || (getDisableDelayBlockId() == b.getTypeId())) {
			if ((getDisableDelayBlockData() == -1) || (getDisableDelayBlockData() == b.getData())) {
				return true;
			}
		}
		return false;
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
