package com.operontech.redblocks.storage;

import java.io.Serializable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.event.inventory.InventoryType;

public class RedBlockChild implements Serializable {
	private static final long serialVersionUID = 1L;
	private int typeId;
	private byte data;
	private String location;
	private String inventory;
	private byte note;
	private String[] signData = null;

	public RedBlockChild(final int typeId, final byte data, final Location location) {
		this.typeId = typeId;
		this.data = data;
		this.location = convertLocation(location);
	}

	@SuppressWarnings("deprecation")
	public void enableBlock(final boolean blockUpdate) {
		if ((typeId == Material.WATER.getId()) || (typeId == Material.LAVA.getId())) {
			getBlock().setTypeIdAndData(typeId, data, true);
		} else {
			getBlock().setTypeIdAndData(typeId, data, blockUpdate);
		}
		if (inventory != null) {
			try {
				final BlockState state = getBlock().getState();
				if (state instanceof Chest) {
					final Chest inv = (Chest) state;
					inv.getBlockInventory().setContents(new InventorySerializer().convertToInventory(inventory, InventoryType.CHEST));
				} else if (state instanceof Furnace) {
					final Furnace inv = (Furnace) state;
					inv.getInventory().setContents(new InventorySerializer().convertToInventory(inventory, InventoryType.FURNACE));
				} else if (state instanceof Dispenser) {
					final Dispenser inv = (Dispenser) state;
					inv.getInventory().setContents(new InventorySerializer().convertToInventory(inventory, InventoryType.DISPENSER));
				} else if (state instanceof Dropper) {
					final Dropper inv = (Dropper) state;
					inv.getInventory().setContents(new InventorySerializer().convertToInventory(inventory, InventoryType.DROPPER));
				} else if (state instanceof BrewingStand) {
					final BrewingStand inv = (BrewingStand) state;
					inv.getInventory().setContents(new InventorySerializer().convertToInventory(inventory, InventoryType.BREWING));
				} else if (state instanceof Hopper) {
					final Hopper inv = (Hopper) state;
					inv.getInventory().setContents(new InventorySerializer().convertToInventory(inventory, InventoryType.HOPPER));
				} else if (state instanceof Beacon) {
					final Beacon inv = (Beacon) state;
					inv.getInventory().setContents(new InventorySerializer().convertToInventory(inventory, InventoryType.BEACON));
				} else if (state instanceof NoteBlock) {
					final NoteBlock noteblock = (NoteBlock) state;
					noteblock.setRawNote(note);
				}
				state.update(true);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		if ((signData != null) && (getBlock().getState() instanceof Sign)) {
			final Sign s = (Sign) getBlock().getState();
			for (int i = 0; i <= (signData.length - 1); i++) {
				s.setLine(i, signData[i]);
			}
			s.update();
		}

	}

	@SuppressWarnings("deprecation")
	public void disableBlock(final boolean blockUpdate) {
		if (getBlock().isEmpty()) {
			inventory = null;
			return;
		}
		try {
			final BlockState state = getBlock().getState();
			if (state instanceof Chest) {
				final Chest inv = (Chest) state;
				inventory = new InventorySerializer().convertToString(inv.getBlockInventory());
				inv.getBlockInventory().clear();
			} else if (state instanceof Furnace) {
				final Furnace inv = (Furnace) state;
				inventory = new InventorySerializer().convertToString(inv.getInventory());
				inv.getInventory().clear();
			} else if (state instanceof Dispenser) {
				final Dispenser inv = (Dispenser) state;
				inventory = new InventorySerializer().convertToString(inv.getInventory());
				inv.getInventory().clear();
			} else if (state instanceof Dropper) {
				final Dropper inv = (Dropper) state;
				inventory = new InventorySerializer().convertToString(inv.getInventory());
				inv.getInventory().clear();
			} else if (state instanceof BrewingStand) {
				final BrewingStand inv = (BrewingStand) state;
				inventory = new InventorySerializer().convertToString(inv.getInventory());
				inv.getInventory().clear();
			} else if (state instanceof Hopper) {
				final Hopper inv = (Hopper) state;
				inventory = new InventorySerializer().convertToString(inv.getInventory());
				inv.getInventory().clear();
			} else if (state instanceof Beacon) {
				final Beacon inv = (Beacon) state;
				inventory = new InventorySerializer().convertToString(inv.getInventory());
				inv.getInventory().clear();
			} else if (state instanceof NoteBlock) {
				final NoteBlock noteblock = (NoteBlock) state;
				note = noteblock.getRawNote();
			} else if (state instanceof Sign) {
				final Sign sign = (Sign) state;
				signData = sign.getLines();
			}
			state.update(true);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		getBlock().setTypeId(0, false);
	}

	/**
	 * Gets the type id.
	 *
	 * @return the type id
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public byte getData() {
		return data;
	}

	/**
	 * Gets the block.
	 *
	 * @return the block
	 */
	public Block getBlock() {
		return getLocation().getBlock();
	}

	/**
	 * Gets the location.
	 *
	 * @return the location
	 */
	public Location getLocation() {
		final String str2loc[] = location.split("\\:");
		final Location loc = new Location(Bukkit.getServer().getWorld(str2loc[0]), 0, 0, 0);
		loc.setX(Double.parseDouble(str2loc[1]));
		loc.setY(Double.parseDouble(str2loc[2]));
		loc.setZ(Double.parseDouble(str2loc[3]));
		return loc;
	}

	/**
	 * Gets the inventory right.
	 *
	 * @return the inventory right
	 */
	public String getInventoryRight() {
		return inventory;
	}

	/**
	 * Gets the sign data.
	 *
	 * @return the sign data
	 */
	public String[] getSignData() {
		return signData;
	}

	/**
	 * Sets the sign data.
	 *
	 * @param str the new sign data
	 */
	public void setSignData(final String[] str) {
		signData = str;
	}

	/**
	 * Sets the data.
	 *
	 * @param data the new data
	 */
	public void setData(final byte data) {
		this.data = data;
	}

	/**
	 * Sets the type id.
	 *
	 * @param typeId the new type id
	 */
	public void setTypeId(final int typeId) {
		this.typeId = typeId;
	}

	/**
	 * Sets the location.
	 *
	 * @param location the new location
	 */
	public void setLocation(final Location location) {
		this.location = location.toString();
	}

	/**
	 * Convert location.
	 *
	 * @param loc the loc
	 * @return the string
	 */
	private String convertLocation(final Location loc) {
		return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
	}
}
