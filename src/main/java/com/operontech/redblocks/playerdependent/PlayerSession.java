package com.operontech.redblocks.playerdependent;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.operontech.redblocks.storage.RedBlockAnimated;

public class PlayerSession implements Serializable {
	private static final long serialVersionUID = 1L;
	private final UUID p;
	private RedBlockAnimated rb;
	private Block rbBlock;

	private int enableDelay;
	private String enableDelayBlock;
	private int disableDelay;
	private String disableDelayBlock;

	/**
	 * Stores player data for the RedBlock
	 * @param playerUUID the UUID of the player
	 * @param redblock the RedBlock currently being controlled by the player
	 * @param block the block of the RedBlock currently being controlled by the player
	 */
	public PlayerSession(final UUID playerUUID, final RedBlockAnimated redblock, final Block block) {
		p = playerUUID;
		rb = redblock;
		rbBlock = block;
		enableDelay = 0;
		disableDelay = 0;
		enableDelayBlock = "0:0";
		disableDelayBlock = "0:0";
	}

	public void setEnableDelay(final int placeDelay) {
		enableDelay = placeDelay;
	}

	public void setEnableDelayBlock(final int id, final int data) {
		enableDelayBlock = id + ":" + data;
	}

	public void setDisableDelay(final int breakDelay) {
		disableDelay = breakDelay;
	}

	public void setDisableDelayBlock(final int id, final int data) {
		disableDelayBlock = id + ":" + data;
	}

	public void setRedBlock(final RedBlockAnimated redblock, final Block block) {
		rb = redblock;
		rbBlock = block;
	}

	public int getEnableDelay() {
		return enableDelay;
	}

	public int getEnableDelayBlockId() {
		return Integer.parseInt(enableDelayBlock.split(":")[0]);
	}

	public int getEnableDelayBlockData() {
		return Integer.parseInt(enableDelayBlock.split(":")[1]);
	}

	public int getDisableDelay() {
		return disableDelay;
	}

	public int getDisableDelayBlockId() {
		return Integer.parseInt(disableDelayBlock.split(":")[0]);
	}

	public int getDisableDelayBlockData() {
		return Integer.parseInt(disableDelayBlock.split(":")[1]);
	}

	public Player getPlayer() {
		return Bukkit.getServer().getPlayer(p);
	}

	public UUID getUUID() {
		return p;
	}

	public boolean isEditingRedBlock() {
		return rb == null;
	}

	public RedBlockAnimated getRedBlock() {
		return rb;
	}

	public Block getBlock() {
		return rbBlock;
	}
}
