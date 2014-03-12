package com.operontech.redblocks.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.operontech.redblocks.Util;

public class RedBlockAnimated implements Serializable {

	private static final long serialVersionUID = 1L;
	private String location;
	private Material type;
	private byte data;
	private String owner;
	private boolean protect;
	private boolean inverted;
	private boolean blocksActive;
	private boolean inTimeout = false;
	private boolean changed = false;
	private final Map<RedBlockChild, Boolean> listOfBlocks = new TreeMap<RedBlockChild, Boolean>();

	public RedBlockAnimated(final Location location, final Material type, final byte data, final String owner, final boolean protect, final boolean inverted) {
		this.location = Util.convertLocationToString(location);
		this.owner = owner;
		this.protect = protect;
		this.inverted = inverted;
		this.type = type;
		this.data = data;
	}

	/**
	 * Adds a RedBlockChild to the RedBlock's database of blocks.
	 * @param child the child to be added
	 * @return the 
	 */
	@SuppressWarnings("deprecation")
	public RedBlockChild add(final RedBlockChild child) {
		if (Util.isSpecialBlock(Material.getMaterial(child.getTypeId()))) {
			listOfBlocks.put(child, true);
		} else {
			listOfBlocks.put(child, false);
		}
		return child;
	}

	/**
	 * Adds a block to the RedBlock's database of blocks.
	 * 
	 * Converts it to a RedBlockChild before adding it.
	 * @param b the block to be converted then added
	 * @return the converted RedBlockChild
	 */
	@SuppressWarnings("deprecation")
	public RedBlockChild add(final Block b) {
		return add(new RedBlockChild(b.getTypeId(), b.getData(), b.getLocation()));
	}

	/**
	 * Removes a RedBlockChild from the RedBlock's database of blocks.
	 * @param rbc the RedBlockChild to be removed
	 * @return if the child existed and was removed
	 */
	public boolean remove(final RedBlockChild rbc) {
		return listOfBlocks.remove(rbc) != null;
	}

	/**
	 * Searches for and removed a block from the RedBlock's database of blocks.
	 * 
	 * It is recommended that remove(RedBlockChild) is used instead as it uses less CPU and therefore takes less time.
	 * @param b the block to be searched for and removed
	 * @return if the block was found and removed
	 */
	public boolean remove(final Block b) {
		final Set<RedBlockChild> rbcKeys = listOfBlocks.keySet();
		for (final RedBlockChild rbc : rbcKeys) {
			if (rbc.getLocation().toString().equals(b.getLocation().toString())) {
				return listOfBlocks.remove(rbc);
			}
		}
		return false;
	}

	/**
	 * Removes multiple RedBlockChilds from the RedBlock's database of blocks.
	 * @param list the list of RedBlockChilds
	 * @return the number of RedBlockChilds removed
	 */
	public int removeList(final ArrayList<RedBlockChild> list) {
		int i = 0;
		for (final RedBlockChild rbc : list) {
			if (remove(rbc)) {
				i++;
			}
		}
		return i;
	}

	/**
	 * Enables all of the RedBlockChilds in the RedBlock's database of blocks.
	 * @param force if true, the blocks will cause block updates when enabled forcibly
	 */
	public void enable(final boolean force) {
		final Set<Chunk> chunks = new HashSet<Chunk>();
		if (!blocksActive || force) {
			for (final Entry<RedBlockChild, Boolean> entry : listOfBlocks.entrySet()) {
				entry.getKey().enableBlock(entry.getValue());
				chunks.add(entry.getKey().getLocation().getChunk());
			}
			for (final Chunk chunk : chunks) {
				chunk.load();
				chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
			}
			blocksActive = true;
		}
	}

	/**
	 * Disables all of the RedBlockChilds in the RedBlock's database of blocks.
	 * @param force if true, the blocks will cause block updates when disabled forcibly
	 */
	public void disable(final boolean force) {
		final Set<Chunk> chunks = new HashSet<Chunk>();
		if (blocksActive || force) {
			for (final Entry<RedBlockChild, Boolean> entry : listOfBlocks.entrySet()) {
				entry.getKey().disableBlock(entry.getValue());
				chunks.add(entry.getKey().getLocation().getChunk());
			}
			for (final Chunk chunk : chunks) {
				chunk.load();
				chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
			}
			blocksActive = false;
		}
	}

	/**
	 * Searches for the RedBlockChild in the RedBlock's database of blocks
	 * @param rbc the RedBlockChild to search for
	 * @return the RedBlockChild that was found
	 */
	public RedBlockChild contains(final RedBlockChild rbc) {
		return (listOfBlocks.containsKey(rbc) ? rbc : null);
	}

	/**
	 * Searches for the block in the RedBlock's database of blocks
	 * @param b the block to search for
	 * @return the RedBlockChild of the block that was found
	 */
	public RedBlockChild contains(final Block b) {
		for (final RedBlockChild rbc : listOfBlocks.keySet()) {
			if (rbc.getLocation().toString().equals(b.getLocation().toString())) {
				return rbc;
			}
		}
		return null;
	}

	/**
	 * Sets the block material of the RedBlock's physical block.
	 * @param type the new material
	 * @return the material after the change
	 */
	public Material setType(final Material type) {
		return this.type = type;
	}

	/**
	 * Sets the byte data of the RedBlock's physical block.
	 * @param data the new byte data
	 * @return the byte data after the change
	 */
	public byte setData(final byte data) {
		return this.data = data;
	}

	/**
	 * Sets the location of the RedBlock's physical block.
	 * @param loc the new location
	 * @return the location after the change
	 */
	public String setLocation(final String loc) {
		return location = loc;
	}

	/**
	 * Sets the location of the RedBlock's physical block.
	 * @param loc the new location
	 * @return the location after the change
	 */
	public String setLocation(final Location loc) {
		return setLocation(Util.convertLocationToString(loc));
	}

	/**
	 * Sets the owner of the RedBlock.
	 * @param p the new owner
	 * @return the owner's name
	 */
	public String setOwner(final Player p) {
		return owner = p.getName();
	}

	/**
	 * Sets the name of the owner of the RedBlock.
	 * @param ownerName the new owner's name
	 * @return the owner's name after the change
	 */
	public String setOwner(final String ownerName) {
		return owner = ownerName;
	}

	/**
	 * Sets the "protected" option of the RedBlock.
	 * 
	 * Protected prevents the breaking of any controlled blocks.
	 * @param protect the new "protected" option
	 * @return the "protected" option after the change
	 */
	public boolean setProtected(final boolean protect) {
		return this.protect = protect;
	}

	/**
	 * Sets the "inverted" option of the RedBlock.
	 * 
	 * Inverted switches the on/off states to off/on when activated by redstone.
	 * @param invert the new "inverted" option
	 * @return the "inverted" option after the change
	 */
	public boolean setInverted(final boolean invert) {
		return inverted = invert;
	}

	/**
	 * Sets the "changed" value of the RedBlock
	 * 
	 * Changed identifies that the RedBlock's database has changed
	 * @param changed the new "changed" value
	 * @return the "changed" value after the change
	 */
	public boolean setChanged(final boolean changed) {
		return this.changed = changed;
	}

	/**
	 * Sets the "timeout" value of the RedBlock.
	 * 
	 * Timeout is a period of time where the RedBlock is disabled after being (de)activated by redstone.
	 * @param timeout the new "timeout" value
	 * @return the "timeout" value
	 */
	public boolean setTimeoutState(final boolean timeout) {
		return inTimeout = timeout;
	}

	/**
	 * Gets the "changed" value of the RedBlock
	 * @return the "changed" value
	 */
	public boolean getChanged() {
		return changed;
	}

	/**
	 * Gets the "enabled" value of the RedBlock
	 * @return the "enabled" value
	 */
	public boolean getActiveState() {
		return blocksActive;
	}

	/**
	 * Gets the byte data of the RedBlock's physical block.
	 * @return the byte data
	 */
	public byte getData() {
		return data;
	}

	/**
	 * Gets the material of the RedBlock's physical block.
	 * @return the material
	 */
	public Material getType() {
		return type;
	}

	/**
	 * Gets the location of the Redblock's physical block.
	 * @return the location
	 */
	public Location getLocation() {
		return Util.convertStringToLocation(location);
	}

	/**
	 * Gets name of the RedBlock's owner.
	 * @return the owner's name
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * Gets the "protected" option of the RedBlock.
	 * @return the "protected" option
	 */
	public boolean getOptionProtected() {
		return protect;
	}

	/**
	 * Gets the "inverted" option of the RedBlock.
	 * @return the "inverted" option
	 */
	public boolean getOptionInverted() {
		return inverted;
	}

	/**
	 * Gets the "timeout" value of the RedBlock.
	 * @return the "timeout" value
	 */
	public boolean getTimeoutState() {
		return inTimeout;
	}

	/**
	 * Gets the size of the RedBlock's database of blocks.
	 * @return the number of blocks
	 */
	public int getBlockCount() {
		return listOfBlocks.size();
	}

	/**
	 * Gets the Map of the RedBlock's database of blocks.
	 * @return the Map
	 */
	public Map<RedBlockChild, Boolean> getDatabase() {
		return listOfBlocks;
	}

	/**
	 * Gets the Set of blocks in the RedBlocks's database of blocks.
	 * @return the Set of blocks
	 */
	public Set<RedBlockChild> getBlocks() {
		return listOfBlocks.keySet();
	}

	//
	//	/**
	//	 * Adds a block to the RedBlock.
	//	 *
	//	 * @param typeId the typeID of the block
	//	 * @param data the data of the block
	//	 * @param loc the location of the block
	//	 * @return if the block was added
	//	 */
	//	public boolean add(final int typeId, final byte data, final Location loc) {
	//		fixNulls();
	//		if (contains(loc.getBlock())) {
	//			return false;
	//		}
	//		if (specialBList.contains(loc.getBlock().getType())) {
	//			return specialBlocks.add(new RedBlockChild(typeId, data, loc));
	//		}
	//		return blocks.add(new RedBlockChild(typeId, data, loc));
	//	}
	//
	//	/**
	//	 * Adds a block to the RedBlock.
	//	 *
	//	 * @param b the block to be added
	//	 * @return if the block was added
	//	 */
	//	@SuppressWarnings("deprecation")
	//	public boolean add(final Block b) {
	//		fixNulls();
	//		return add(b.getTypeId(), b.getData(), b.getLocation());
	//	}
	//
	//	/**
	//	 * Removes a block from the RedBlock.
	//	 *
	//	 * @param b the block to be removed
	//	 * @return if the block was removed
	//	 */
	//	public boolean remove(final Block b) {
	//		fixNulls();
	//		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
	//		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
	//		rbc.addAll(blocks);
	//		rsbc.addAll(specialBlocks);
	//		for (final RedBlockChild c : rbc) {
	//			if (c.getLocation().toString().equals(b.getLocation().toString())) {
	//				return blocks.remove(c);
	//			}
	//		}
	//		for (final RedBlockChild c : rsbc) {
	//			if (c.getLocation().toString().equals(b.getLocation().toString())) {
	//				return specialBlocks.remove(c);
	//			}
	//		}
	//		return false;
	//	}
	//
	//	/**
	//	 * Removes a RedBlockChild from the RedBlock.
	//	 *
	//	 * @param b the block to be removed
	//	 * @return if the block was removed
	//	 */
	//	public boolean remove(final RedBlockChild b) {
	//		fixNulls();
	//		return blocks.remove(b) || specialBlocks.remove(b);
	//	}
	//
	//	/**
	//	 * Removes a RedBlockChild in a list from the RedBlock.
	//	 *
	//	 * @param children the children
	//	 * @return the number of blocks removed
	//	 */
	//	public int removeList(final List<RedBlockChild> children) {
	//		fixNulls();
	//		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
	//		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
	//		rbc.addAll(blocks);
	//		rsbc.addAll(specialBlocks);
	//		int removed = 0;
	//		for (final RedBlockChild c : rbc) {
	//			if (children.contains(c)) {
	//				blocks.remove(c);
	//				removed++;
	//			}
	//		}
	//		for (final RedBlockChild c : rsbc) {
	//			if (children.contains(c)) {
	//				specialBlocks.remove(c);
	//				removed++;
	//			}
	//		}
	//		return removed;
	//	}
	//
	//	/**
	//	 * Enables the children of the RedBlock.
	//	 *
	//	 * @param force if true, the RedBlock will ignore if the RedBlock is already enabled and enable it again
	//	 */
	//	public void enable(final boolean force) {
	//		final Set<Chunk> chunks = new HashSet<Chunk>();
	//		if (!isEnabled() || force) {
	//			fixNulls();
	//			if ((blocks != null) && (blocks.size() > 0)) {
	//				for (final RedBlockChild c : blocks) {
	//					c.enableBlock(false);
	//				}
	//			}
	//			if ((specialBlocks != null) && (specialBlocks.size() > 0)) {
	//				for (final RedBlockChild c : specialBlocks) {
	//					c.enableBlock(true);
	//					chunks.add(c.getLocation().getChunk());
	//				}
	//			}
	//			for (final Chunk chunk : chunks) {
	//				chunk.load();
	//				chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
	//			}
	//			blocksActive = true;
	//		}
	//	}
	//
	//	/**
	//	 * Disables the children of the RedBlock.
	//	 *
	//	 * @param force if true, the RedBlock will ignore if the RedBlock is already disabled and disable it again
	//	 */
	//	public void disable(final boolean force) {
	//		if (isEnabled() || force) {
	//			final Set<Chunk> chunks = new HashSet<Chunk>();
	//			fixNulls();
	//			if ((specialBlocks != null) && (specialBlocks.size() > 0)) {
	//				for (final RedBlockChild c : specialBlocks) {
	//					c.disableBlock(true);
	//					chunks.add(c.getLocation().getChunk());
	//				}
	//			}
	//			if ((blocks != null) && (blocks.size() > 0)) {
	//				for (final RedBlockChild c : blocks) {
	//					c.disableBlock(false);
	//					chunks.add(c.getLocation().getChunk());
	//				}
	//			}
	//			for (final Chunk chunk : chunks) {
	//				chunk.load();
	//				chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
	//			}
	//			blocksActive = false;
	//		}
	//	}
	//
	//	/**
	//	 * Checks if the RedBlock controls a block.
	//	 *
	//	 * @param b the block to check for
	//	 * @return if the RedBlock contains that block
	//	 */
	//	public boolean contains(final Block b) {
	//		fixNulls();
	//		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
	//		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
	//		rbc.addAll(blocks);
	//		rsbc.addAll(specialBlocks);
	//		for (final RedBlockChild c : rbc) {
	//			if (c.getLocation().toString().equals(b.getLocation().toString())) {
	//				return true;
	//			}
	//		}
	//		for (final RedBlockChild c : rsbc) {
	//			if (c.getLocation().toString().equals(b.getLocation().toString())) {
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
	//
	//	/**
	//	 * Adds or Removes multiple blocks by using List<Block>.
	//	 *
	//	 * @param block the list of blocks
	//	 * @param remove if true, the blocks in the list will be removed from the RedBlock, but if false, they will be added
	//	 * @return the amount of blocks added/removed
	//	 */
	//	@SuppressWarnings("deprecation")
	//	public int blockListAction(final List<Block> block, final boolean remove) {
	//		fixNulls();
	//		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
	//		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
	//		rbc.addAll(blocks);
	//		rsbc.addAll(specialBlocks);
	//		int affected = 0;
	//		for (final RedBlockChild rc : rbc) {
	//			if (block.contains(rc.getBlock())) {
	//				if (remove) {
	//					blocks.remove(rc);
	//					affected++;
	//				} else {
	//					block.remove(rc.getBlock());
	//				}
	//			}
	//		}
	//		for (final RedBlockChild rc : rsbc) {
	//			if (block.contains(rc.getBlock())) {
	//				if (remove) {
	//					blocks.remove(rc);
	//					affected++;
	//				} else {
	//					block.remove(rc.getBlock());
	//				}
	//			}
	//		}
	//		if (!remove) {
	//			for (final Block b : block) {
	//				if (specialBList.contains(b.getType())) {
	//					specialBlocks.add(new RedBlockChild(b.getTypeId(), b.getData(), b.getLocation()));
	//					affected++;
	//					continue;
	//				}
	//				blocks.add(new RedBlockChild(b.getTypeId(), b.getData(), b.getLocation()));
	//				affected++;
	//			}
	//		}
	//		return affected;
	//	}
	//
	//	/**
	//	 * Gets a set of the RedBlockChilds controlled by the RedBlock.
	//	 *
	//	 * @return the set of RedBlockChilds
	//	 */
	//	public Set<RedBlockChild> getBlocks() {
	//		final Set<RedBlockChild> b = new HashSet<RedBlockChild>();
	//		b.addAll(blocks);
	//		b.addAll(specialBlocks);
	//		return b;
	//	}
	//
	//	/**
	//	 * Gets a RedBlockChild controlled by the RedBlock.
	//	 *
	//	 * @param b the block to get
	//	 * @return the RedBlockChild
	//	 */
	//	public RedBlockChild getChild(final Block b) {
	//		final Set<RedBlockChild> rbc = new HashSet<RedBlockChild>();
	//		rbc.addAll(blocks);
	//		final Set<RedBlockChild> rsbc = new HashSet<RedBlockChild>();
	//		rsbc.addAll(specialBlocks);
	//		for (final RedBlockChild child : rbc) {
	//			if (child.getLocation().toString().equals(b.getLocation().toString())) {
	//				return child;
	//			}
	//		}
	//		for (final RedBlockChild child : rsbc) {
	//			if (child.getLocation().toString().equals(b.getLocation().toString())) {
	//				return child;
	//			}
	//		}
	//		return null;
	//	}
	//
	//	/**
	//	 * Gets the RedBlock's location.
	//	 *
	//	 * @return the location
	//	 */
	//	public Location getLocation() {
	//		final String str2loc[] = location.split("\\:");
	//		final Location loc = new Location(Bukkit.getServer().getWorld(str2loc[0]), 0, 0, 0);
	//		loc.setX(Double.parseDouble(str2loc[1]));
	//		loc.setY(Double.parseDouble(str2loc[2]));
	//		loc.setZ(Double.parseDouble(str2loc[3]));
	//		return loc;
	//	}
	//
	//	/**
	//	 * Sets the Inverted property.
	//	 *
	//	 * @param bool the new Inverted property setting
	//	 * @return the resulting property
	//	 */
	//	public boolean setInverted(final boolean bool) {
	//		inverted = bool;
	//		return inverted;
	//	}
	//
	//	/**
	//	 * Sets the Protected property.
	//	 *
	//	 * @param bool the new Protected property setting
	//	 * @return the resulting property
	//	 */
	//	public boolean setProtected(final boolean bool) {
	//		protect = bool;
	//		return protect;
	//	}
	//
	//	/**
	//	 * Sets the Owner property.
	//	 *
	//	 * @param str the str
	//	 * @return the resulting property
	//	 */
	//	public String setOwner(final String str) {
	//		owner = str;
	//		return owner;
	//	}
	//
	//	/**
	//	 * Sets if the RedBlock is under Redstone Timeout.
	//	 *
	//	 * @param bool the desired Redstone Timeout setting
	//	 * @return the property
	//	 */
	//	public boolean setTimeout(final boolean bool) {
	//		inTimeout = bool;
	//		return bool;
	//	}
	//
	//	/**
	//	 * Checks if the RedBlock is already enabled.
	//	 *
	//	 * @return if the RedBlock is enabled
	//	 */
	//	public boolean isEnabled() {
	//		return blocksActive;
	//	}
	//
	//	/**
	//	 * Gets the Block of the RedBlock.
	//	 *
	//	 * @return the Block of the RedBlock
	//	 */
	//	public Block getBlock() {
	//		return getLocation().getBlock();
	//	}
	//
	//	/**
	//	 * Gets the number of blocks controlled by the RedBlock.
	//	 *
	//	 * @return the number of blocks
	//	 */
	//	public int getBlockCount() {
	//		return blocks.size() + specialBlocks.size();
	//	}
	//
	//	/**
	//	 * Gets the typeId of the RedBlock.
	//	 *
	//	 * @return the typeId of the RedBlock
	//	 */
	//	public int getTypeId() {
	//		return typeId;
	//	}
	//
	//	/**
	//	 * Gets the data of the RedBlock.
	//	 *
	//	 * @return the data of the RedBlock
	//	 */
	//	public byte getData() {
	//		return data;
	//	}
	//
	//	/**
	//	 * Gets the Owner property.
	//	 *
	//	 * @return the Owner property
	//	 */
	//	public String getOwner() {
	//		return owner;
	//	}
	//
	//	/**
	//	 * Gets the Protected property.
	//	 *
	//	 * @return the protected property
	//	 */
	//	public boolean isProtected() {
	//		return protect;
	//	}
	//
	//	/**
	//	 * Gets the Inverted property.
	//	 *
	//	 * @return the Inverted property
	//	 */
	//	public boolean isInverted() {
	//		return inverted;
	//	}
	//
	//	/**
	//	 * Gets the inTimeOut property.
	//	 *
	//	 * @return the inTimeOut property
	//	 */
	//	public boolean isInTimeout() {
	//		return inTimeout;
	//	}
	//
	//	/**
	//	 * Gets the changed property.
	//	 *
	//	 * @return the changed property
	//	 */
	//	public boolean hasChanged() {
	//		return changed;
	//	}
	//
	//	/**
	//	 * Sets the changed property.
	//	 *
	//	 * @param bool the new changed property
	//	 */
	//	public void setChanged(final boolean bool) {
	//		changed = bool;
	//	}
	//
	//	/**
	//	 * Convert location.
	//	 *
	//	 * @param loc the loc
	//	 * @return the string
	//	 */
	//	private String convertLocation(final Location loc) {
	//		return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
	//	}
	//
	//	/**
	//	 * Fix nulls.
	//	 */
	//	private void fixNulls() {
	//		if (specialBlocks == null) {
	//			specialBlocks = new HashSet<RedBlockChild>();
	//		}
	//		if (blocks == null) {
	//			blocks = new HashSet<RedBlockChild>();
	//		}
	//	}
}
