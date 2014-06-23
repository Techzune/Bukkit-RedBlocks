package com.operontech.redblocks.storage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.operontech.redblocks.RedBlocksMain.RBDisableListener;
import com.operontech.redblocks.UUIDFetcher;
import com.operontech.redblocks.Util;

public class RedBlockAnimated implements Serializable {

	private static final long serialVersionUID = 1L;
	private String location;
	private Material type;
	private byte data;
	private String owner;
	private UUID ownerUUID;
	private boolean protect;
	private boolean inverted;
	private boolean blocksActive;
	private boolean inTimeout = false;
	private boolean changed = false;

	// RedBlockChild & Delay Times (ENABLE,DISABLE)
	private final Map<RedBlockChild, List<Integer>> listOfBlocks = new TreeMap<RedBlockChild, List<Integer>>();

	/**
	 * Creates a RedBlock.
	 * @param location the location of the physical block
	 * @param type the material of the physical block
	 * @param data the byte data of the physical block
	 * @param owner the UUID of the owner of the RedBlock
	 * @param protect the "protect" option
	 * @param inverted the "inverted" option
	 */
	public RedBlockAnimated(final Location location, final Material type, final byte data, final UUID owner, final boolean protect, final boolean inverted) {
		this.location = Util.convertLocationToString(location);
		ownerUUID = owner;
		this.protect = protect;
		this.inverted = inverted;
		this.type = type;
		this.data = data;
	}

	/**
	 * Creates a RedBlock.
	 * @param b the physical block for the RedBlock
	 * @param owner the UUID of the owner of the RedBlock
	 * @param protect the "protect" option
	 * @param inverted the "inverted" option
	 */
	@SuppressWarnings("deprecation")
	public RedBlockAnimated(final Block b, final UUID owner, final boolean protect, final boolean inverted) {
		location = Util.convertLocationToString(b.getLocation());
		type = b.getType();
		data = b.getData();
		ownerUUID = owner;
		this.protect = protect;
		this.inverted = inverted;
	}

	@SuppressWarnings("deprecation")
	public RedBlockAnimated(final RedBlock rb) {
		location = Util.convertLocationToString(rb.getLocation());
		type = Material.getMaterial(rb.getTypeId());
		data = rb.getData();
		owner = rb.getOwner();
		ownerUUID = UUIDFetcher.getUUIDOf(rb.getOwner());
		protect = rb.isProtected();
		inverted = rb.isInverted();
		for (final RedBlockChild child : rb.getBlocks()) {
			listOfBlocks.put(child, Arrays.asList(0, 0));
		}
	}

	/**
	 * Enables all of the RedBlockChilds in the RedBlock's database of blocks.
	 * @param force if true, the blocks will cause block updates when enabled forcibly
	 * @param doAnimations if true, the RedBlock will pause for animations
	 */
	public void enable(final boolean force, final boolean doAnimations) {
		final Set<Chunk> chunks = new HashSet<Chunk>();
		if (!blocksActive || force) {
			final Thread thread = new Thread() {
				@Override
				public void run() {
					Map<RedBlockChild, List<Integer>> cacheMap = new TreeMap<RedBlockChild, List<Integer>>(listOfBlocks);
					for (final Entry<RedBlockChild, List<Integer>> entry : cacheMap.entrySet()) {
						if (doAnimations && (entry.getValue().get(0) > 0)) {
							try {
								Thread.sleep(entry.getValue().get(0));
							} catch (final InterruptedException e) {
							}
						}
						entry.getKey().enableBlock(Util.isSpecialBlock(entry.getKey().getType()));
						chunks.add(entry.getKey().getLocation().getChunk());
					}
					for (final Chunk chunk : chunks) {
						chunk.load();
						chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
					}
					blocksActive = true;
					cacheMap = null;
				}
			};
			thread.start();
		}
	}

	/**
	 * Enables all of the RedBlockChilds in the RedBlock's database of blocks.
	 * Will execute and pause for animations.
	 * @param force if true, the blocks will cause block updates when enabled forcibly
	 */
	public void enable(final boolean force) {
		enable(force, true);
	}

	/**
	 * Disables all of the RedBlockChilds in the RedBlock's database of blocks.
	 * @param force if true, the blocks will cause block updates when disabled forcibly
	 * @param doAnimations if true, the RedBlock will pause for animations
	 */
	public void disable(final boolean force, final boolean doAnimations, final RBDisableListener listener) {
		final Set<Chunk> chunks = new HashSet<Chunk>();
		if (blocksActive || force) {
			final Thread thread = new Thread() {
				@Override
				public void run() {
					Map<RedBlockChild, List<Integer>> cacheMap = new TreeMap<RedBlockChild, List<Integer>>(listOfBlocks);
					for (final Entry<RedBlockChild, List<Integer>> entry : cacheMap.entrySet()) {
						if (doAnimations && (entry.getValue().get(0) > 0)) {
							try {
								Thread.sleep(entry.getValue().get(0));
							} catch (final InterruptedException e) {
							}
						}
						entry.getKey().disableBlock(Util.isSpecialBlock(entry.getKey().getType()));
						chunks.add(entry.getKey().getLocation().getChunk());
					}
					for (final Chunk chunk : chunks) {
						chunk.load();
						chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
					}
					blocksActive = false;
					cacheMap = null;
					listener.threadFinished();
				}
			};
			thread.start();
		}
	}

	/**
	 * Disables all of the RedBlockChilds in the RedBlock's database of blocks.
	 * Will execute and pause for animations.
	 * @param force if true, the blocks will cause block updates when disabled forcibly
	 */
	public void disable(final boolean force, final RBDisableListener listener) {
		disable(force, true, listener);
	}

	/**
	 * Adds a RedBlockChild to the RedBlock's database of blocks.
	 * @param child the child to be added
	 * @param enableDelay the delay when enabling the block
	 * @param disableDelay the delay when removing the block
	 * @return the 
	 */
	public boolean add(final RedBlockChild child, final int enableDelay, final int disableDelay) {
		return listOfBlocks.put(child, Arrays.asList(enableDelay, disableDelay)) == null;
	}

	/**
	 * Adds a block to the RedBlock's database of blocks.
	 * 
	 * Converts it to a RedBlockChild before adding it.
	 * @param b the block to be converted then added
	 * @param enableDelay the delay when enabling the block
	 * @param disableDelay the delay when disabling the block
	 * @return the converted RedBlockChild
	 */
	@SuppressWarnings("deprecation")
	public boolean add(final Block b, final int enableDelay, final int disableDelay) {
		return add(new RedBlockChild(b.getType(), b.getData(), b.getLocation()), enableDelay, disableDelay);
	}

	/**
	 * Adds multiple RedBlockChilds to the RedBlock's database of blocks.
	 * @param list the list of RedBlockChilds
	 * @return the number of RedBlockChilds added
	 */
	public int addChildList(final List<RedBlockChild> list) {
		int i = 0;
		for (final RedBlockChild child : list) {
			if (add(child, 0, 0)) {
				i++;
			}
		}
		return i;
	}

	/**
	 * Adds multiple Blocks to the RedBlock's database of blocks.
	 * @param list the list of Blocks
	 * @return the number of Blocks added
	 */
	public int addBlockList(final List<Block> list) {
		int i = 0;
		for (final Block child : list) {
			if (add(child, 0, 0)) {
				i++;
			}
		}
		return i;
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
		Set<RedBlockChild> rbcKeys = new TreeSet<RedBlockChild>(listOfBlocks.keySet());
		for (final RedBlockChild rbc : rbcKeys) {
			if (rbc.getLocation().toString().equals(b.getLocation().toString())) {
				listOfBlocks.remove(rbc);
				return true;
			}
		}
		rbcKeys = null;
		return false;
	}

	/**
	 * Removes multiple RedBlockChilds from the RedBlock's database of blocks.
	 * @param list the list of RedBlockChilds
	 * @return the number of RedBlockChilds removed
	 */
	public int removeChildList(final List<RedBlockChild> list) {
		int i = 0;
		for (final RedBlockChild rbc : list) {
			if (remove(rbc)) {
				i++;
			}
		}
		return i;
	}

	/**
	 * Removes multiple Blocks from the RedBlock's database of blocks.
	 * @param list the list of Blocks
	 * @return the number of RedBlockChilds removed
	 */
	public int removeBlockList(final List<Block> list) {
		int i = 0;
		for (final Block rbc : list) {
			if (remove(rbc)) {
				i++;
			}
		}
		return i;
	}

	/**
	 * Searches for the RedBlockChild in the RedBlock's database of blocks
	 * @param rbc the RedBlockChild to search for
	 * @return if the RedBlockChild was found
	 */
	public boolean contains(final RedBlockChild rbc) {
		return (listOfBlocks.containsKey(rbc));
	}

	/**
	 * Searches for the block in the RedBlock's database of blocks
	 * @param b the block to search for
	 * @return if the RedBlockChild of the block was found
	 */
	public boolean contains(final Block b) {
		Map<RedBlockChild, List<Integer>> cacheMap = new TreeMap<RedBlockChild, List<Integer>>(listOfBlocks);
		for (final RedBlockChild rbc : cacheMap.keySet()) {
			if (rbc.getLocation().toString().equals(b.getLocation().toString())) {
				return true;
			}
		}
		cacheMap = null;
		return false;
	}

	/**
	 * Searches for the block in the RedBlock's database of blocks and gets the RedBlockChild
	 * @param b the block to search for
	 * @return the RedBlockChild of the block that was found
	 */
	public RedBlockChild getChild(final Block b) {
		Map<RedBlockChild, List<Integer>> cacheMap = new TreeMap<RedBlockChild, List<Integer>>(listOfBlocks);
		for (final RedBlockChild rbc : cacheMap.keySet()) {
			if (rbc.getLocation().toString().equals(b.getLocation().toString())) {
				return rbc;
			}
		}
		cacheMap = null;
		return null;
	}

	/**
	 * Sets the delay (milliseconds) for the enabling of the block.
	 * @param child the RedBlockChild to delay
	 * @param enableDelay the length of the delay in milliseconds
	 * @return if the child existed
	 */
	public boolean setEnableDelayForChild(final RedBlockChild child, final int enableDelay) {
		if (contains(child)) {
			final List<Integer> newList = listOfBlocks.get(child);
			newList.set(0, enableDelay);
			listOfBlocks.put(child, newList);
			return true;
		}
		return false;
	}

	/**
	 * Sets the delay (milliseconds) for the disabling of the RedBlockChild.
	 * @param child the RedBlockChild to delay
	 * @param disableDelay the length of the delay in milliseconds
	 * @return if the child existed
	 */
	public boolean setDisableDelayForChild(final RedBlockChild child, final int disableDelay) {
		if (contains(child)) {
			final List<Integer> newList = listOfBlocks.get(child);
			newList.set(1, disableDelay);
			listOfBlocks.put(child, newList);
			return true;
		}
		return false;
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
	 * Gets UUID of the RedBlock's owner.
	 * @return the owner's name
	 */
	public UUID getOwnerUUID() {
		if (ownerUUID == null) {
			ownerUUID = UUIDFetcher.getUUIDOf(owner);
		}
		return ownerUUID;
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
	public Map<RedBlockChild, List<Integer>> getDatabase() {
		return listOfBlocks;
	}

	public Block getBlock() {
		return getLocation().getBlock();
	}

	/**
	 * Gets the Set of blocks in the RedBlocks's database of blocks.
	 * @return the Set of blocks
	 */
	public Set<RedBlockChild> getBlocks() {
		return listOfBlocks.keySet();
	}
}