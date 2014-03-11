package com.operontech.redblocks.storage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class RedBlockAnimated implements Serializable {

	private static final long serialVersionUID = 1L;
	private final String location;
	private final int typeId;
	private final byte data;
	private String owner;
	private boolean protect;
	private boolean inverted;
	private boolean blocksEnabled;
	private boolean inTimeout = false;
	private boolean changed = false;
	private Set<RedBlockChild> blocks = new HashSet<RedBlockChild>();
	private Set<RedBlockChild> specialBlocks = new HashSet<RedBlockChild>();
	private final List<Material> specialBList = Arrays.asList(Material.ACTIVATOR_RAIL, Material.ANVIL, Material.BED_BLOCK, Material.BROWN_MUSHROOM, Material.CACTUS, Material.CAKE_BLOCK, Material.CROPS, Material.DETECTOR_RAIL, Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.FLOWER_POT, Material.ITEM_FRAME, Material.SUGAR_CANE_BLOCK, Material.REDSTONE_WIRE, Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON, Material.PAINTING, Material.POWERED_RAIL, Material.WOOD_DOOR, Material.IRON_DOOR_BLOCK);

	public RedBlockAnimated(final Location location, final int typeId, final byte data, final String owner, final boolean protect, final boolean inverted) {
		this.location = convertLocation(location);
		this.owner = owner;
		this.protect = protect;
		this.inverted = inverted;
		this.typeId = typeId;
		this.data = data;
	}

	/**
	 * Adds a block to the RedBlock.
	 *
	 * @param typeId the typeID of the block
	 * @param data the data of the block
	 * @param loc the location of the block
	 * @return if the block was added
	 */
	public boolean add(final int typeId, final byte data, final Location loc) {
		fixNulls();
		if (contains(loc.getBlock())) {
			return false;
		}
		if (specialBList.contains(loc.getBlock().getType())) {
			return specialBlocks.add(new RedBlockChild(typeId, data, loc));
		}
		return blocks.add(new RedBlockChild(typeId, data, loc));
	}

	/**
	 * Adds a block to the RedBlock.
	 *
	 * @param b the block to be added
	 * @return if the block was added
	 */
	@SuppressWarnings("deprecation")
	public boolean add(final Block b) {
		fixNulls();
		return add(b.getTypeId(), b.getData(), b.getLocation());
	}

	/**
	 * Removes a block from the RedBlock.
	 *
	 * @param b the block to be removed
	 * @return if the block was removed
	 */
	public boolean remove(final Block b) {
		fixNulls();
		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
		rbc.addAll(blocks);
		rsbc.addAll(specialBlocks);
		for (final RedBlockChild c : rbc) {
			if (c.getLocation().toString().equals(b.getLocation().toString())) {
				return blocks.remove(c);
			}
		}
		for (final RedBlockChild c : rsbc) {
			if (c.getLocation().toString().equals(b.getLocation().toString())) {
				return specialBlocks.remove(c);
			}
		}
		return false;
	}

	/**
	 * Removes a RedBlockChild from the RedBlock.
	 *
	 * @param b the block to be removed
	 * @return if the block was removed
	 */
	public boolean remove(final RedBlockChild b) {
		fixNulls();
		return blocks.remove(b) || specialBlocks.remove(b);
	}

	/**
	 * Removes a RedBlockChild in a list from the RedBlock.
	 *
	 * @param children the children
	 * @return the number of blocks removed
	 */
	public int removeList(final List<RedBlockChild> children) {
		fixNulls();
		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
		rbc.addAll(blocks);
		rsbc.addAll(specialBlocks);
		int removed = 0;
		for (final RedBlockChild c : rbc) {
			if (children.contains(c)) {
				blocks.remove(c);
				removed++;
			}
		}
		for (final RedBlockChild c : rsbc) {
			if (children.contains(c)) {
				specialBlocks.remove(c);
				removed++;
			}
		}
		return removed;
	}

	/**
	 * Enables the children of the RedBlock.
	 *
	 * @param force if true, the RedBlock will ignore if the RedBlock is already enabled and enable it again
	 */
	public void enable(final boolean force) {
		final Set<Chunk> chunks = new HashSet<Chunk>();
		if (!isEnabled() || force) {
			fixNulls();
			if ((blocks != null) && (blocks.size() > 0)) {
				for (final RedBlockChild c : blocks) {
					c.enableBlock(false);
				}
			}
			if ((specialBlocks != null) && (specialBlocks.size() > 0)) {
				for (final RedBlockChild c : specialBlocks) {
					c.enableBlock(true);
					chunks.add(c.getLocation().getChunk());
				}
			}
			for (final Chunk chunk : chunks) {
				chunk.load();
				chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
			}
			blocksEnabled = true;
		}
	}

	/**
	 * Disables the children of the RedBlock.
	 *
	 * @param force if true, the RedBlock will ignore if the RedBlock is already disabled and disable it again
	 */
	public void disable(final boolean force) {
		if (isEnabled() || force) {
			final Set<Chunk> chunks = new HashSet<Chunk>();
			fixNulls();
			if ((specialBlocks != null) && (specialBlocks.size() > 0)) {
				for (final RedBlockChild c : specialBlocks) {
					c.disableBlock(true);
					chunks.add(c.getLocation().getChunk());
				}
			}
			if ((blocks != null) && (blocks.size() > 0)) {
				for (final RedBlockChild c : blocks) {
					c.disableBlock(false);
					chunks.add(c.getLocation().getChunk());
				}
			}
			for (final Chunk chunk : chunks) {
				chunk.load();
				chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
			}
			blocksEnabled = false;
		}
	}

	/**
	 * Checks if the RedBlock controls a block.
	 *
	 * @param b the block to check for
	 * @return if the RedBlock contains that block
	 */
	public boolean contains(final Block b) {
		fixNulls();
		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
		rbc.addAll(blocks);
		rsbc.addAll(specialBlocks);
		for (final RedBlockChild c : rbc) {
			if (c.getLocation().toString().equals(b.getLocation().toString())) {
				return true;
			}
		}
		for (final RedBlockChild c : rsbc) {
			if (c.getLocation().toString().equals(b.getLocation().toString())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds or Removes multiple blocks by using List<Block>.
	 *
	 * @param block the list of blocks
	 * @param remove if true, the blocks in the list will be removed from the RedBlock, but if false, they will be added
	 * @return the amount of blocks added/removed
	 */
	@SuppressWarnings("deprecation")
	public int blockListAction(final List<Block> block, final boolean remove) {
		fixNulls();
		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
		rbc.addAll(blocks);
		rsbc.addAll(specialBlocks);
		int affected = 0;
		for (final RedBlockChild rc : rbc) {
			if (block.contains(rc.getBlock())) {
				if (remove) {
					blocks.remove(rc);
					affected++;
				} else {
					block.remove(rc.getBlock());
				}
			}
		}
		for (final RedBlockChild rc : rsbc) {
			if (block.contains(rc.getBlock())) {
				if (remove) {
					blocks.remove(rc);
					affected++;
				} else {
					block.remove(rc.getBlock());
				}
			}
		}
		if (!remove) {
			for (final Block b : block) {
				if (specialBList.contains(b.getType())) {
					specialBlocks.add(new RedBlockChild(b.getTypeId(), b.getData(), b.getLocation()));
					affected++;
					continue;
				}
				blocks.add(new RedBlockChild(b.getTypeId(), b.getData(), b.getLocation()));
				affected++;
			}
		}
		return affected;
	}

	/**
	 * Gets a set of the RedBlockChilds controlled by the RedBlock.
	 *
	 * @return the set of RedBlockChilds
	 */
	public Set<RedBlockChild> getBlocks() {
		final Set<RedBlockChild> b = new HashSet<RedBlockChild>();
		b.addAll(blocks);
		b.addAll(specialBlocks);
		return b;
	}

	/**
	 * Gets a RedBlockChild controlled by the RedBlock.
	 *
	 * @param b the block to get
	 * @return the RedBlockChild
	 */
	public RedBlockChild getChild(final Block b) {
		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
		rbc.addAll(blocks);
		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
		rsbc.addAll(specialBlocks);
		for (final RedBlockChild child : rbc) {
			if (child.getLocation().toString().equals(b.getLocation().toString())) {
				return child;
			}
		}
		for (final RedBlockChild child : rsbc) {
			if (child.getLocation().toString().equals(b.getLocation().toString())) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Gets the RedBlock's location.
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
	 * Sets the Inverted property.
	 *
	 * @param bool the new Inverted property setting
	 * @return the resulting property
	 */
	public boolean setInverted(final boolean bool) {
		inverted = bool;
		return inverted;
	}

	/**
	 * Sets the Protected property.
	 *
	 * @param bool the new Protected property setting
	 * @return the resulting property
	 */
	public boolean setProtected(final boolean bool) {
		protect = bool;
		return protect;
	}

	/**
	 * Sets the Owner property.
	 *
	 * @param str the str
	 * @return the resulting property
	 */
	public String setOwner(final String str) {
		owner = str;
		return owner;
	}

	/**
	 * Sets if the RedBlock is under Redstone Timeout.
	 *
	 * @param bool the desired Redstone Timeout setting
	 * @return the property
	 */
	public boolean setTimeout(final boolean bool) {
		inTimeout = bool;
		return bool;
	}

	/**
	 * Checks if the RedBlock is already enabled.
	 *
	 * @return if the RedBlock is enabled
	 */
	public boolean isEnabled() {
		return blocksEnabled;
	}

	/**
	 * Gets the Block of the RedBlock.
	 *
	 * @return the Block of the RedBlock
	 */
	public Block getBlock() {
		return getLocation().getBlock();
	}

	/**
	 * Gets the number of blocks controlled by the RedBlock.
	 *
	 * @return the number of blocks
	 */
	public int getBlockCount() {
		return blocks.size() + specialBlocks.size();
	}

	/**
	 * Gets the typeId of the RedBlock.
	 *
	 * @return the typeId of the RedBlock
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * Gets the data of the RedBlock.
	 *
	 * @return the data of the RedBlock
	 */
	public byte getData() {
		return data;
	}

	/**
	 * Gets the Owner property.
	 *
	 * @return the Owner property
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * Gets the Protected property.
	 *
	 * @return the protected property
	 */
	public boolean isProtected() {
		return protect;
	}

	/**
	 * Gets the Inverted property.
	 *
	 * @return the Inverted property
	 */
	public boolean isInverted() {
		return inverted;
	}

	/**
	 * Gets the inTimeOut property.
	 *
	 * @return the inTimeOut property
	 */
	public boolean isInTimeout() {
		return inTimeout;
	}

	/**
	 * Gets the changed property.
	 *
	 * @return the changed property
	 */
	public boolean hasChanged() {
		return changed;
	}

	/**
	 * Sets the changed property.
	 *
	 * @param bool the new changed property
	 */
	public void setChanged(final boolean bool) {
		changed = bool;
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

	/**
	 * Fix nulls.
	 */
	private void fixNulls() {
		if (specialBlocks == null) {
			specialBlocks = new HashSet<RedBlockChild>();
		}
		if (blocks == null) {
			blocks = new HashSet<RedBlockChild>();
		}
	}
}
