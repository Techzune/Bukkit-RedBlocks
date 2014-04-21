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
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.operontech.redblocks.events.RedBlockCause;
import com.operontech.redblocks.events.RedBlockEvent;
import com.operontech.redblocks.listener.BlockListener;
import com.operontech.redblocks.listener.CommandListener;
import com.operontech.redblocks.listener.PhysicsListener;
import com.operontech.redblocks.listener.WorldListener;
import com.operontech.redblocks.storage.RedBlockAnimated;
import com.operontech.redblocks.storage.RedBlockChild;
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
	private Map<Player, RedBlockAnimated> editMode = new HashMap<Player, RedBlockAnimated>();
	private List<String> activeBlocks = new ArrayList<String>();
	private boolean initialized = false;

	@Override
	public void onEnable() {
		console = new ConsoleConnection(this, getServer());
		config = new Configuration(this);
		storage = new Storage(this);
		clistener = new CommandListener(this);
		if ((new File(getDataFolder() + File.separator + "blocks.dat").exists() && new File(getDataFolder() + File.separator + "options.dat").exists()) || new File(getDataFolder() + File.separator + "blocks.dat").exists()) {
			console.severe("You must run RedBlocks with a version earlier than 2.2 to convert your RedBlocks!");
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
		for (final Entry<Player, RedBlockAnimated> e : editMode.entrySet()) {
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
		activeBlocks = null;
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
	 * Sends a message to the editors of the RedBlockAnimated.
	 * 
	 * Includes: "<!> RedBlocks: "
	 * @param rb the RedBlock
	 * @param msg the message
	 */
	public void notifyEditors(final RedBlockAnimated rb, final String msg) {
		if (editMode.containsValue(rb)) {
			for (final Entry<Player, RedBlockAnimated> e : editMode.entrySet()) {
				if (e.getValue() == rb) {
					console.notify(e.getKey(), msg);
				}
			}
		}
	}

	/**
	 * Adds an editor to a RedBlockAnimated.
	 * @param p the player to be added
	 * @param b the block to be added
	 */
	public void addEditor(final Player p, final Block b) {
		if (!isEditing(p)) {
			final RedBlockAnimated rb = storage.getRedBlock(b);
			if (rb.getTimeoutState()) {
				console.error(p, "That RedBlockAnimated is under redstone timeout.", "Stop all redstone powering the RedBlockAnimated and try again in a few seconds.");
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
				notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.GREEN + " is now editing the RedBlockAnimated | " + rb.getBlockCount() + " Blocks");
			}
		}
	}

	/**
	 * Removes an editor from a RedBlockAnimated.
	 * @param p the player to be removed
	 */
	public void removeEditor(final Player p) {
		removeEditor(p, true);
	}

	/**
	 * Removes an editor from a RedBlockAnimated.
	 * @param p the player to be removed
	 * @param blockUpdate if true, the RedBlockAnimated will check for redstone updates
	 */
	public void removeEditor(final Player p, final boolean blockUpdate) {
		if (isEditing(p)) {
			final RedBlockAnimated rb = getRedBlockEditing(p);
			final Block b = rb.getBlock();
			final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.LOST_EDITOR, p);
			getServer().getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				if (blockUpdate) {
					doBlockUpdate(b);
				}
				notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.RED + " is no longer editing the RedBlockAnimated | " + rb.getBlockCount() + " Blocks");
				editMode.remove(p);
				if (config.getBool(ConfigValue.redblocks_soundFX)) {
					b.getWorld().playSound(b.getLocation(), Sound.CHEST_CLOSE, 0.5f, 1f);
				}
			}
		}
	}

	/**
	 * Updates the data of a block in a RedBlock
	 * @param p the player that updated the block's data
	 * @param rb the RedBlock that was updated
	 * @param b the Block that was updated
	 */
	public void updateBlock(final Player p, final RedBlockAnimated rb, final Block b) {
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
	 * Adds a block to a RedBlockAnimated.
	 * @param p the player that placed the block
	 * @param rb the RedBlockAnimated to add the block to
	 * @param b the block to be added
	 * @param eWaitTime the milliseconds to wait on enable for the particular block
	 * @param dWaitTime the milliseconds to wait on disable for the particular block
	 */
	public void addBlock(final Player p, final RedBlockAnimated rb, final Block b, final int eWaitTime, final int dWaitTime) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.BLOCK_ADDED, p);
		getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					if (!event.isCancelled()) {
						if (rb.add(b, eWaitTime, dWaitTime)) {
							notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.DARK_GREEN + " Added A Block | " + rb.getBlockCount() + " Blocks");
						}
						if ((b.getState().getData() instanceof Bed) && !((Bed) b.getState().getData()).isHeadOfBed()) {
							addBlock(p, rb, b.getRelative(((Bed) b.getState().getData()).getFacing()), eWaitTime, dWaitTime);
						}
					}
				}
			}, 2L);
		}
	}

	/**
	 * Adds a block to a RedBlockAnimated.
	 * @param p the player that placed the block
	 * @param rb the RedBlockAnimated to add the block to
	 * @param b the block to be added
	 */
	public void addBlock(final Player p, final RedBlockAnimated rb, final Block b) {
		addBlock(p, rb, b, 0, 0);
	}

	/**
	 * Removes a block from a RedBlockAnimated.
	 * @param p the player that removed the block
	 * @param rb the RedBlockAnimated that is losing a block
	 * @param b the block to be removed
	 */
	public void removeBlock(final Player p, final RedBlockAnimated rb, final Block b) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.BLOCK_REMOVED, p);
		getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled() && (rb.remove(b))) {
			notifyEditors(rb, ChatColor.DARK_AQUA + (p != null ? p.getName() : "Environment") + ChatColor.GOLD + " Removed A Block | " + rb.getBlockCount() + " Blocks");
		}
	}

	/**
	 * Enables a RedBlockAnimated.
	 * @param rb the RedBlockAnimated to be enabled
	 * @param force if true, the RedBlockAnimated will ignore if the RedBlockAnimated is already enabled and enable it again
	 * @return if the RedBlockAnimated was enabled
	 */
	public boolean enableRedBlock(final RedBlockAnimated rb, final boolean force) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.ENABLED);
		getServer().getPluginManager().callEvent(event);
		if (!isBeingEdited(rb) && !event.isCancelled()) {
			rb.enable(force);
			for (final RedBlockChild rbc : rb.getBlocks()) {
				activeBlocks.add(rbc.getLocation().toString());
			}
			if (config.getBool(ConfigValue.gc_onEnableRedBlock)) {
				System.gc();
			}
		}
		return rb.getActiveState();
	}

	/**
	 * Disabled a RedBlockAnimated.
	 * @param rb the RedBlockAnimated to be disabled
	 * @param force if true, the RedBlockAnimated will ignore if the RedBlockAnimated is already disabled and disable it again
	 * @return if the RedBlockAnimated was disabled
	 */
	public boolean disableRedBlock(final RedBlockAnimated rb, final boolean force) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.DISABLED);
		getServer().getPluginManager().callEvent(event);
		if (!isBeingEdited(rb) && !event.isCancelled()) {
			rb.disable(force);
			for (final RedBlockChild rbc : rb.getBlocks()) {
				activeBlocks.remove(rbc.getLocation().toString());
			}
			if (config.getBool(ConfigValue.gc_onDisableRedBlock)) {
				System.gc();
			}
		}
		return !rb.getActiveState();
	}

	/**
	 * Deletes a RedBlockAnimated.
	 * @param b the block of the RedBlock
	 * @param breakBlock if the the RedBlock should be broken physically
	 */
	public boolean removeRedBlock(final Block b, final boolean breakBlock) {
		final RedBlockAnimated rb = storage.getRedBlock(b);
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.DESTROYED);
		getServer().getPluginManager().callEvent(event);
		if (!isBeingEdited(rb) && !event.isCancelled()) {
			enableRedBlock(rb, true);
			storage.removeRedBlock(b);
			if (breakBlock) {
				b.breakNaturally();
			}
			if (config.getBool(ConfigValue.gc_onDestroyRedBlock)) {
				System.gc();
			}
			return true;
		}
		return false;
	}

	/**
	 * Initiates the destruction of RedBlock by a player
	 * @param b the block of the RedBlock to be destroyed
	 * @param p the player that used the tool to destroy the RedBlock
	 * @return if the RedBlock was destroyed
	 */
	public boolean destroyRedBlock(final Block b, final Player p) {
		if (editMode.containsKey(p) && editMode.get(p).getLocation().toString().equals(b.getLocation().toString())) {
			removeEditor(p);
		}
		if (editMode.containsValue(storage.getRedBlock(b))) {
			console.error(p, "You can't destroy a RedBlockAnimated that is being edited!");
			return false;
		}
		if (removeRedBlock(b, true)) {
			p.getInventory().removeItem(new ItemStack(config.getInt(ConfigValue.redblocks_destroyItem), 1));
			console.notify(p, "RedBlockAnimated Eliminated");
			return true;
		}
		return false;
	}

	/**
	 * Created a new RedBlockAnimated.
	 * @param p the owner of the RedBlock
	 * @param b the block of the RedBlock
	 */
	public void createRedBlock(final Player p, final Block b) {
		int number = 0;
		for (final RedBlockAnimated rb : storage.getRedBlocks().values()) {
			if (rb.getOwnerUUID().equals(p.getUniqueId())) {
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
			storage.createRedBlock(p.getUniqueId(), b);
			addEditor(p, b);
		}
	}

	/**
	 * Notifies the editors of that the RedBlockAnimated was lost.
	 * @param rb the RedBlockAnimated that was lost
	 */
	public void redBlockLost(final RedBlockAnimated rb) {
		notifyEditors(rb, ChatColor.RED + "Your RedBlockAnimated was lost/destroyed.");
		for (final Player p : editMode.keySet()) {
			if (getRedBlockEditing(p) == rb) {
				removeEditor(p);
			}
		}
	}

	/**
	 * Runs the WorldEditCommand process on a RedBlockAnimated.
	 * @param rb the RedBlockAnimated receiving the WorldEditCommand process
	 * @param p the player that has the selection
	 * @param type the type of block to be added, can be null (use colon for data values)
	 * @param remove if true, the blocks in the selection will be removed, if false, blocks in the selection will be added
	 */
	public void useWorldEdit(final RedBlockAnimated rb, final Player p, final String type, final boolean remove) {
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
			notifyEditors(rb, rb.removeBlockList(cache) + " Blocks Removed By: " + p.getName() + " | " + rb.getBlockCount() + " Blocks");
		} else {
			notifyEditors(rb, rb.addBlockList(cache) + " Blocks Added By: " + p.getName() + " | " + rb.getBlockCount() + " Blocks");
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
				final RedBlockAnimated rb = getStorage().getRedBlock(block);
				if ((rb == null) || isBeingEdited(rb) || rb.getTimeoutState()) {
					return;
				}
				final boolean bp = block.getBlockPower() > 0;
				if ((bp && !rb.getOptionInverted()) || (!bp && rb.getOptionInverted())) {
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
	 * Starts the Redstone timeout timer on a RedBlockAnimated.
	 * @param rb the RedBlock
	 */
	public void startTimeout(final RedBlockAnimated rb) {
		if (!rb.getTimeoutState()) {
			rb.setTimeoutState(true);
			getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
				@Override
				public void run() {
					rb.setTimeoutState(false);
				}
			}, ((config.getInt(ConfigValue.redblocks_redstoneTimeout) / 1000) * 20) + 2L);
		}
	}

	/**
	 * Checks if the player is editing a RedBlockAnimated.
	 * @param p the player to check
	 * @return if the player is editing a RedBlock
	 */
	public boolean isEditing(final Player p) {
		return editMode.containsKey(p);
	}

	/**
	 * Checks if a RedBlockAnimated is being edited.
	 * @param redBlockAnimated the RedBlockAnimated to check
	 * @return if the RedBlockAnimated is being edited
	 */
	public boolean isBeingEdited(final RedBlockAnimated redBlockAnimated) {
		return editMode.containsValue(redBlockAnimated);
	}

	/**
	 * Gets the RedBlockAnimated that is being edited by a player.
	 * @param p the player to get the editing RedBlockAnimated of
	 * @return the REdBlock
	 */
	public RedBlockAnimated getRedBlockEditing(final Player p) {
		return editMode.get(p);
	}

	/**
	 * Checks if a player is op or has a RedBlockAnimated permission.
	 * @param p the player to check
	 * @param perm the permission node
	 * @return if the player is op or has permission
	 */
	public boolean hasPermission(final CommandSender p, final String perm) {
		return p.isOp() || p.hasPermission("redblocks." + perm);
	}

	/**
	 * Checks if the block at the location is listed as "active"
	 * @param l the location of the block to check
	 * @return if the block at the location is active
	 */
	public boolean isActiveBlock(final Location l) {
		return activeBlocks.contains(l.toString());
	}

	/**
	 * Checks if the block is listed as "active"
	 * @param b the block to check
	 * @return if the block is active
	 */
	public boolean isActiveBlock(final Block b) {
		return isActiveBlock(b.getLocation());
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
	 * Gets the RedBlockAnimated Storage.
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
