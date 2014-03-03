package com.operontech.redblocks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.Bed;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.operontech.redblocks.events.RedBlockCause;
import com.operontech.redblocks.events.RedBlockEvent;
import com.operontech.redblocks.listener.BlockListener;
import com.operontech.redblocks.listener.PhysicsListener;
import com.operontech.redblocks.listener.WorldListener;
import com.operontech.redblocks.storage.RedBlock;
import com.operontech.redblocks.storage.Storage;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

@SuppressWarnings("deprecation")
public class RedBlocksMain extends JavaPlugin {
	private String projectID = "48951";
	private Storage storage;
	private ConsoleConnection console;
	private Configuration config;
	private CommandListener clistener;
	private Map<Player, RedBlock> editMode = new HashMap<Player, RedBlock>();
	private boolean initialized = false;

	@Override
	public void onEnable() {
		console = new ConsoleConnection(this, getServer());
		config = new Configuration(this);
		storage = new Storage(this);
		clistener = new CommandListener(this);
		if ((new File(getDataFolder() + File.separator + "blocks.dat").exists() && new File(getDataFolder() + File.separator + "options.dat").exists()) || new File(getDataFolder() + File.separator + "blocks.dat").exists()) {
			console.severe("You must run RedBlocks version 2.2 or earlier first!");
			console.severe("If you don't, you'll lose your RedBlocks!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		getServer().getPluginManager().registerEvents(new BlockListener(this, console), this);
		getServer().getPluginManager().registerEvents(new PhysicsListener(this), this);
		getServer().getPluginManager().registerEvents(new WorldListener(this), this);
		if (config.canUpdate()) {
			checkForUpdate();
		}
		initialized = true;
	}

	@Override
	public void onDisable() {
		for (final Entry<Player, RedBlock> e : editMode.entrySet()) {
			editMode.remove(e.getKey());
		}
		if (initialized) {
			storage.saveRedBlocks();
			storage.cleanupRedBlocks();
			storage.saveRedBlocks();
			clearRAM();
		}
	}

	/**
	 * Reloads the Configuration and Redblocks.
	 * @return if the saving process was successful
	 */
	public boolean reloadPlugin() {
		config.reload();
		final boolean re = storage.saveRedBlocks();
		storage.loadRedBlocks();
		return re;
	}

	private void clearRAM() {
		projectID = null;
		storage = null;
		console = null;
		config = null;
		clistener = null;
		editMode = null;
	}

	private boolean checkForUpdate() {
		try {
			final URLConnection conn = new URL("https://api.curseforge.com/servermods/files?projectIds=" + projectID).openConnection();
			conn.addRequestProperty("User-Agent", "RedBlocks Update Checker");
			final JSONArray array = (JSONArray) JSONValue.parse(new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine());
			if (array.size() > 0) {
				final JSONObject latest = (JSONObject) array.get(array.size() - 1);
				final String version = ((String) latest.get("name")).replaceAll("[a-zA-Z ]", "");
				if (!getDescription().getVersion().endsWith("SNAPSHOT") && !getDescription().getVersion().equals(version)) {
					console.info(ChatColor.GREEN + "An update is available from BukkitDev: " + ChatColor.DARK_GREEN + version);
					return true;
				}
			}
		} catch (final IOException e) {
			console.warning("An error occured while checking for updates.");
		}
		return false;
	}

	@Override
	public boolean onCommand(final CommandSender s, final Command cmd, final String label, final String[] args) {
		return clistener.onCommand(s, cmd, label, args);
	}

	/**
	 * Sends a message to the editors of the RedBlock.
	 * 
	 * Includes: "<!> RedBlocks: "
	 * @param rb the RedBlock
	 * @param msg the message
	 */
	public void notifyEditors(final RedBlock rb, final String msg) {
		if (editMode.containsValue(rb)) {
			for (final Entry<Player, RedBlock> e : editMode.entrySet()) {
				if (e.getValue() == rb) {
					console.notify(e.getKey(), msg);
				}
			}
		}
	}

	/**
	 * Adds an editor to a RedBlock.
	 * @param p the player to be added
	 * @param b the block to be added
	 */
	public void addEditor(final Player p, final Block b) {
		if (!isEditing(p)) {
			final RedBlock rb = storage.getRedBlock(b);
			if (rb.isInTimeout()) {
				console.error(p, "That RedBlock is under redstone timeout. Stop all redstone powering the RedBlock and try again in a few seconds!");
				return;
			}
			final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.NEW_EDITOR, p);
			getServer().getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				enableRedBlock(rb, true);
				if (config.getBool(ConfigValue.redblocks_soundFX)) {
					b.getWorld().playSound(b.getLocation(), Sound.CHEST_OPEN, 0.5f, 1f);
				}
				editMode.put(p, rb);
				notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.GREEN + " is now editing the RedBlock | " + rb.getBlockCount() + " Blocks");
			}
		}
	}

	/**
	 * Removes an editor from a RedBlock.
	 * @param p the player to be removed
	 */
	public void removeEditor(final Player p) {
		removeEditor(p, true);
	}

	/**
	 * Removes an editor from a RedBlock.
	 * @param p the player to be removed
	 * @param blockUpdate if true, the RedBlock will check for redstone updates
	 */
	public void removeEditor(final Player p, final boolean blockUpdate) {
		if (isEditing(p)) {
			final RedBlock rb = getRedBlockEditing(p);
			final Block b = rb.getBlock();
			final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.LOST_EDITOR, p);
			getServer().getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				if (blockUpdate) {
					doBlockUpdate(b);
				}
				notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.RED + " is no longer editing the RedBlock | " + rb.getBlockCount() + " Blocks");
				editMode.remove(p);
				if (config.getBool(ConfigValue.redblocks_soundFX)) {
					b.getWorld().playSound(b.getLocation(), Sound.CHEST_CLOSE, 0.5f, 1f);
				}
			}
		}
	}

	/**
	 * Updates the data of a block in a RedBlock
	 */
	public void updateBlock(final Player p, final RedBlock rb, final Block b) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.BLOCK_UPDATED, p);
		getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					if (!event.isCancelled()) {
						rb.getChild(b).setData(b.getData());
						notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.DARK_GREEN + " Updated A Block's Data | " + b.getType());
					}
				}
			}, 2L);
		}
	}

	/**
	 * Adds a block to a RedBlock.
	 * @param p the player that placed the block
	 * @param rb the RedBlock to add the block to
	 * @param b the block to be added
	 */
	public void addBlock(final Player p, final RedBlock rb, final Block b) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.BLOCK_ADDED, p);
		getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					if (!event.isCancelled()) {
						if (rb.add(b)) {
							notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.DARK_GREEN + " Added A Block | " + rb.getBlockCount() + " Blocks");
						}
						if ((b.getState().getData() instanceof Bed) && !((Bed) b.getState().getData()).isHeadOfBed()) {
							addBlock(p, rb, b.getRelative(((Bed) b.getState().getData()).getFacing()));
						}
					}
				}
			}, 2L);
		}
	}

	/**
	 * Removes a block from a RedBlock.
	 * @param p the player that removed the block
	 * @param rb the RedBlock that is losing a block
	 * @param b the block to be removed
	 */
	public void removeBlock(final Player p, final RedBlock rb, final Block b) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.BLOCK_REMOVED, p);
		getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled() && (rb.remove(b))) {
			notifyEditors(rb, ChatColor.DARK_AQUA + (p != null ? p.getName() : "Environment") + ChatColor.GOLD + " Removed A Block | " + rb.getBlockCount() + " Blocks");
		}
	}

	/**
	 * Enables a RedBlock.
	 * @param rb the RedBlock to be enabled
	 * @param force if true, the RedBlock will ignore if the RedBlock is already enabled and enable it again
	 * @return if the RedBlock was enabled
	 */
	public boolean enableRedBlock(final RedBlock rb, final boolean force) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.ENABLED);
		getServer().getPluginManager().callEvent(event);
		if (!isBeingEdited(rb) && !event.isCancelled()) {
			rb.enable(force);
			if (config.getBool(ConfigValue.gc_onEnableRedBlock)) {
				System.gc();
			}
		}
		return rb.isEnabled();
	}

	/**
	 * Disabled a RedBlock.
	 * @param rb the RedBlock to be disabled
	 * @param force if true, the RedBlock will ignore if the RedBlock is already disabled and disable it again
	 * @return if the RedBlock was disabled
	 */
	public boolean disableRedBlock(final RedBlock rb, final boolean force) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.DISABLED);
		getServer().getPluginManager().callEvent(event);
		if (!isBeingEdited(rb) && !event.isCancelled()) {
			rb.disable(force);
			if (config.getBool(ConfigValue.gc_onDisableRedBlock)) {
				System.gc();
			}
		}
		return !rb.isEnabled();
	}

	/**
	 * Deletes a RedBlock.
	 * @param b the block of the RedBlock
	 */
	public void destroyRedBlock(final Block b) {
		final RedBlock rb = storage.getRedBlock(b);
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.DESTROYED);
		getServer().getPluginManager().callEvent(event);
		if (!isBeingEdited(rb) && !event.isCancelled()) {
			enableRedBlock(rb, true);
			storage.removeRedBlock(b);
			if (config.getBool(ConfigValue.gc_onDestroyRedBlock)) {
				System.gc();
			}
		}
	}

	/**
	 * Created a new RedBlock.
	 * @param p the owner of the RedBlock
	 * @param b the block of the RedBlock
	 */
	public void createRedBlock(final Player p, final Block b) {
		int number = 0;
		for (final RedBlock rb : storage.getRedBlocks().values()) {
			if (rb.getOwner().equalsIgnoreCase(p.getName())) {
				number++;
			}
		}
		if ((number > config.getInt(ConfigValue.redblocks_blockID)) && !hasPermission(p, "bypass.maxRedBlocksPer")) {
			console.error(p, "You can't create anymore RedBlocks! Max: " + config.getInt(ConfigValue.redblocks_blockID));
			return;
		}
		final RedBlockEvent event = new RedBlockEvent(this, storage.getRedBlock(b), RedBlockCause.CREATED, p);
		getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			storage.createRedBlock(p.getName(), b);
			addEditor(p, b);
		}
	}

	/**
	 * Notifies the editors of that the RedBlock was lost.
	 * @param rb the RedBlock that was lost
	 */
	public void redBlockLost(final RedBlock rb) {
		notifyEditors(rb, ChatColor.RED + "Your RedBlock was lost/destroyed.");
		for (final Player p : editMode.keySet()) {
			if (getRedBlockEditing(p) == rb) {
				removeEditor(p);
			}
		}
	}

	/**
	 * Runs the WorldEditCommand process on a RedBlock.
	 * @param rb the RedBlock receiving the WorldEditCommand process
	 * @param p the player that has the selection
	 * @param type the type of block to be added, can be null (use colon for data values)
	 * @param remove if true, the blocks in the selection will be removed, if false, blocks in the selection will be added
	 */
	public void useWorldEdit(final RedBlock rb, final Player p, final String type, final boolean remove) {
		final Selection reg = getWE().getSelection(p);
		int t = 0;
		int d = 0;
		try {
			if ((type != null) && type.contains(":")) {
				t = Integer.valueOf(type.split(":")[0]);
				d = Integer.valueOf(type.split(":")[1]);
			} else if (type != null) {
				t = Integer.valueOf(type);
			}
		} catch (final Exception e) {
			console.error(p, "Unknown Type Format");
			console.error(p, ChatColor.LIGHT_PURPLE + "Operation Cancelled.");
			return;
		}
		if (reg == null) {
			console.error(p, "No WorldEditCommand Selection Found");
			console.error(p, ChatColor.LIGHT_PURPLE + "Operation Cancelled.");
			return;
		}
		if ((reg.getArea() > config.getInt(ConfigValue.worldedit_maxAtOnce)) && !hasPermission(p, "bypassWEMax")) {
			console.error(p, "You have exceeded the maximum threshold of blocks: " + config.getInt(ConfigValue.worldedit_maxAtOnce));
			console.error(p, ChatColor.LIGHT_PURPLE + "Operation Cancelled.");
			return;
		}
		if (((reg.getArea() + rb.getBlockCount()) > config.getInt(ConfigValue.rules_maxBlocksPer)) && !hasPermission(p, "bypass.maxBlocksPer")) {
			console.error(p, "You have exceeded the maximum threshold of blocks: " + config.getInt(ConfigValue.rules_maxBlocksPer));
			console.error(p, ChatColor.LIGHT_PURPLE + "Operation Cancelled.");
			return;
		}
		final Location min = reg.getMinimumPoint();
		final Location max = reg.getMaximumPoint();
		Block db = null;
		final List<Block> cache = new ArrayList<Block>();
		if (reg.getArea() > 5000) {
			notifyEditors(rb, ChatColor.LIGHT_PURPLE + "Please Wait! 5000+ Blocks are being added via WorldEdit by " + p.getName());
		}
		for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
			for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
				for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
					db = reg.getWorld().getBlockAt(x, y, z);
					if (db.getTypeId() == 0) {
						continue;
					}
					if ((t == 0) || (db.getTypeId() == t)) {
						if ((d == 0) || (db.getData() == d)) {
							if (config.getBool(ConfigValue.worldedit_preventBedrock) && (db.getType() == Material.BEDROCK)) {
								continue;
							}
							if (canBuildHere(p, db.getLocation())) {
								cache.add(db);
							}
						}
					}
				}
			}
		}
		if (remove) {
			notifyEditors(rb, rb.blockListAction(cache, true) + " Blocks Removed By: " + p.getName() + " | " + rb.getBlockCount() + " Blocks");
		} else {
			notifyEditors(rb, rb.blockListAction(cache, false) + " Blocks Added By: " + p.getName() + " | " + rb.getBlockCount() + " Blocks");
		}
		if (config.getBool(ConfigValue.gc_onWorldEdit)) {
			System.gc();
		}
	}

	/**
	 * Runs a Redstone check on a block.
	 * @param block the block that will check around itself for a RedBlock
	 */
	public void doBlockUpdate(final Block block) {
		getServer().getScheduler().runTaskLater(this, new Runnable() {
			@Override
			public void run() {
				if (block == null) {
					return;
				}
				final RedBlock rb = getStorage().getRedBlock(block);
				if ((rb == null) || isBeingEdited(rb) || rb.isInTimeout()) {
					return;
				}
				final boolean bp = block.getBlockPower() > 0;
				if ((bp && !rb.isInverted()) || (!bp && rb.isInverted())) {
					disableRedBlock(rb, false);
					startTimeout(rb);
				} else {
					enableRedBlock(rb, false);
					startTimeout(rb);
				}
			}
		}, 2L);
	}

	/**
	 * Starts the Redstone timeout timer on a RedBlock.
	 * @param rb the RedBlock
	 */
	public void startTimeout(final RedBlock rb) {
		if (!rb.isInTimeout()) {
			rb.setTimeout(true);
			getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
				@Override
				public void run() {
					rb.setTimeout(false);
				}
			}, ((config.getInt(ConfigValue.redblocks_redstoneTimeout) / 1000) * 20) + 2L);
		}
	}

	/**
	 * Checks if the player is editing a RedBlock.
	 * @param p the player to check
	 * @return if the player is editing a RedBlock
	 */
	public boolean isEditing(final Player p) {
		return editMode.containsKey(p);
	}

	/**
	 * Checks if a RedBlock is being edited.
	 * @param rb the RedBlock to check
	 * @return if the RedBlock is being edited
	 */
	public boolean isBeingEdited(final RedBlock rb) {
		return editMode.containsValue(rb);
	}

	/**
	 * Gets the RedBlock that is being edited by a player.
	 * @param p the player to get the editing RedBlock of
	 * @return the REdBlock
	 */
	public RedBlock getRedBlockEditing(final Player p) {
		return editMode.get(p);
	}

	/**
	 * Checks if a player is op or has a RedBlock permission.
	 * @param p the player to check
	 * @param perm the permission node
	 * @return if the player is op or has permission
	 */
	public boolean hasPermission(final CommandSender p, final String perm) {
		return p.isOp() || p.hasPermission("redblocks." + perm);
	}

	/**
	 * Gets the ConsoleConnection.
	 * @return RedBlock's ConsoleConnection
	 */
	public ConsoleConnection getConsoleConnection() {
		return console;
	}

	/**
	 * Gets the Configuration.
	 * @return RedBlock's Configuration
	 */
	public Configuration getConfiguration() {
		return config;
	}

	/**
	 * Gets the RedBlock Storage.
	 * @return RedBlock's Storage
	 */
	public Storage getStorage() {
		return storage;
	}

	/**
	 * Gets the WorldEditPlugin.
	 * @return WorldEditPlugin
	 */
	public WorldEditPlugin getWE() {
		return (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
	}

	/**
	 * Checks if the player has permission to edit a block at the location.
	 * @param player the player
	 * @param loc the location of the block
	 * @return if the player has permission to edit the block at the location
	 */
	public boolean canBuildHere(final Player player, final Location loc) {
		return canBuildWorldGuard(player, loc) && canBuildGriefPrevention(player, loc);
	}

	private boolean canBuildWorldGuard(final Player player, final Location loc) {
		final Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
		if ((plugin == null) || !(plugin instanceof WorldGuardPlugin)) {
			return true;
		}
		return ((WorldGuardPlugin) plugin).canBuild(player, loc);
	}

	private boolean canBuildGriefPrevention(final Player player, final Location loc) {
		final Plugin plugin = getServer().getPluginManager().getPlugin("GriefPrevention");
		if ((plugin == null) || !(plugin instanceof GriefPrevention)) {
			return true;
		}
		final Claim claim = ((GriefPrevention) plugin).dataStore.getClaimAt(loc, true, null);
		return (claim == null) || (claim.allowEdit(player) == null);
	}
}
