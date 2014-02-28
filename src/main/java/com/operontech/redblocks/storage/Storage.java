package com.operontech.redblocks.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;

import com.operontech.redblocks.ConsoleConnection;
import com.operontech.redblocks.RedBlocksMain;

public class Storage {
	private RedBlocksMain plugin;
	private ConsoleConnection console;
	private InventorySerializer invSerializer;
	private HashMap<String, RedBlock> rbSorted = new HashMap<String, RedBlock>();

	public Storage(final RedBlocksMain plugin) {
		this.plugin = plugin;
		console = plugin.getConsoleConnection();
		invSerializer = new InventorySerializer();
		loadRedBlocks();
	}

	/**
	 * Gets the HashMap (Key: Location String, Value: RedBlock) of RedBlocks.
	 * @return the HashMap of RedBlocks
	 */
	public HashMap<String, RedBlock> getRedBlocks() {
		return rbSorted;
	}

	/**
	 * Saves RedBlocks then clears RAM usage of the storage/
	 */
	public void clearRAMUsage() {
		saveRedBlocks();
		plugin = null;
		console = null;
		invSerializer = null;
		rbSorted = null;
	}

	/**
	 * Loads the RedBlocks.dat File in the RedBlocks Plugin Folder
	 * 
	 * If the file is using the old rbList (Set) format, it will be converted to the rbSorted (HashMap) format.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void loadRedBlocks() {
		ObjectInputStream blocksReader = null;
		try {
			final File blocks = new File(plugin.getDataFolder() + File.separator + "redblocks.dat");
			if (blocks.exists() && (blocks.length() != 0)) {
				blocksReader = new ObjectInputStream(new FileInputStream(blocks));
				final Object readObject = blocksReader.readObject();
				if (readObject instanceof HashMap<?, ?>) {
					rbSorted = (HashMap<String, RedBlock>) readObject;
					console.info("RedBlocks Loaded Successfully!");
				} else {
					rbSorted = convertSetToHashMap((Set<RedBlock>) readObject);
					console.info("RedBlocks Converted and Loaded Successfully!");
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			console.warning("An error occured while loading the RedBlocks file.");
		}
		try {
			if (blocksReader != null) {
				blocksReader.close();
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			console.warning("An error occured while closing the RedBlocks file input stream.");
		}
		if (rbSorted == null) {
			rbSorted = new HashMap<String, RedBlock>();
		}
	}

	/**
	 * Saves the HashSet of RedBlocks to a file.
	 * @return if saving was successful
	 */
	public boolean saveRedBlocks() {
		ObjectOutputStream blocksWriter = null;
		try {
			final File blocks = new File(plugin.getDataFolder() + File.separator + "redblocks.dat");
			if (!blocks.exists()) {
				blocks.createNewFile();
			}
			blocksWriter = new ObjectOutputStream(new FileOutputStream(blocks));
			blocksWriter.writeObject(rbSorted);
			blocksWriter.flush();
			blocksWriter.close();
			console.info("RedBlocks Saved Successfully!");
			return true;
		} catch (final Exception ex) {
			ex.printStackTrace();
			console.warning("An error occured while saving the RedBlocks file.");
		}
		try {
			if (blocksWriter != null) {
				blocksWriter.close();
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			console.warning("An error occured while closing the RedBlocks file output stream.");
		}
		return false;
	}

	/**
	 * Searches the sorted RedBlock list for a RedBlock with a matching location.
	 *
	 * @param b the block to search for
	 * @return the RedBlock (if it was found)
	 */
	public RedBlock getRedBlock(final Block b) {
		if (rbSorted.containsKey(b.getLocation().toString())) {
			return rbSorted.get(b.getLocation().toString());
		}
		return null;
	}

	/**
	 * Creates a RedBlock and adds it to the HashSet and HashMap of RedBlocks.
	 *
	 * @param loc the location of the RedBlock
	 * @param typeId the typeId of the RedBlock
	 * @param data the block data of the RedBlock
	 * @param p the owner of the RedBlock
	 * @return the created RedBlock
	 */
	public RedBlock createRedBlock(final Location loc, final int typeId, final byte data, final String p) {
		final RedBlock rb = new RedBlock(loc, typeId, data, p, true, false);
		rbSorted.put(loc.toString(), rb);
		return rb;
	}

	/**
	 * Creates a RedBlock and adds it to the HashSet and HashMap of RedBlocks.
	 *
	 * @param p the owner of the RedBlock
	 * @param b the block of the RedBlock
	 * @return the created RedBlock
	 */
	@SuppressWarnings("deprecation")
	public RedBlock createRedBlock(final String p, final Block b) {
		return createRedBlock(b.getLocation(), b.getTypeId(), b.getData(), p);
	}

	/**
	 * Removes a RedBlock from the HashMap and HashSet of RedBlocks.
	 *
	 * @param b the block to remove
	 * @return if it was removed
	 */
	public boolean removeRedBlock(final Block b) {
		if (rbSorted.containsKey(b.getLocation().toString())) {
			rbSorted.remove(b.getLocation().toString());
			return true;
		}
		return false;
	}

	/**
	 * Checks if the database (HashMap) contains a RedBlock.
	 * 
	 * @param b the block to search for
	 * @return if the RedBlock was found
	 */
	public boolean containsRedBlock(final Block b) {
		return (rbSorted.containsKey(b.getLocation().toString()));
	}

	/**
	 * Searches for the RedBlock in control of that block.
	 * @param b the block to search for the RedBlock of
	 * @return the RedBlock that owns it (if found)
	 */
	public RedBlock getRedBlockParent(final Block b) {
		final Collection<RedBlock> rbc = rbSorted.values();
		for (final RedBlock rb : rbc) {
			if (rb.contains(b)) {
				return rb;
			}
		}
		return null;
	}

	/**
	 * Runs a cleaning process that removes clutter in the RedBlock database, then resorts the HashMap.
	 * 
	 * Removes empty RedBlocks
	 * Removes children of the RedBlock if it they're AIR
	 * 
	 */
	public void cleanupRedBlocks() {
		console.info("Cleaning up RedBlocks...");
		int redBlocksRemoved = 0;
		int blocksRemoved = 0;
		List<RedBlockChild> list;
		Set<RedBlockChild> children;
		for (final Entry<String, RedBlock> entry : rbSorted.entrySet()) {
			if (!plugin.isBeingEdited(entry.getValue())) {
				if (entry.getValue().getBlockCount() == 0) {
					rbSorted.remove(entry.getKey());
					redBlocksRemoved++;
				} else {
					list = new ArrayList<RedBlockChild>();
					children = entry.getValue().getBlocks();
					for (final RedBlockChild child : children) {
						if (child.getTypeId() == 0) {
							list.add(child);
						}
					}
					blocksRemoved = entry.getValue().removeList(list);
				}
			}
		}
		if ((redBlocksRemoved != 0) || (blocksRemoved != 0)) {
			console.info("  " + redBlocksRemoved + " RedBlocks were removed!");
			console.info("  " + blocksRemoved + " blocks controlled by RedBlocks were removed!");
		}
	}

	/**
	 * Gets the InventorySerializer.
	 *
	 * @return the InventorySerializer
	 */
	public InventorySerializer getInventorySerializer() {
		return invSerializer;
	}

	private HashMap<String, RedBlock> convertSetToHashMap(final Set<RedBlock> oldSet) {
		final HashMap<String, RedBlock> newSet = new HashMap<String, RedBlock>();
		for (final RedBlock rb : oldSet) {
			newSet.put(rb.getLocation().toString(), rb);
		}
		return newSet;
	}
}
