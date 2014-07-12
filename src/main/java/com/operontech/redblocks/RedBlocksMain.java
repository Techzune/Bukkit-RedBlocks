package com.operontech.redblocks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

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
import com.operontech.redblocks.listener.WorldListener;
import com.operontech.redblocks.playerdependent.Permission;
import com.operontech.redblocks.playerdependent.PlayerSession;
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
	private Configuration config;
	private CommandListener clistener;
	private Map<UUID, PlayerSession> playerSessions = new HashMap<UUID, PlayerSession>();
	private Map<RedBlockAnimated, Thread> animationThreads = new HashMap<RedBlockAnimated, Thread>();
	private List<String> activeBlocks = new ArrayList<String>();
	private boolean initialized = false;

	@Override
	public void onEnable() {
		config = new Configuration(this);
		storage = new Storage(this);
		clistener = new CommandListener(this);
		if ((new File(getDataFolder() + File.separator + "blocks.dat").exists() && new File(getDataFolder() + File.separator + "options.dat").exists()) || new File(getDataFolder() + File.separator + "blocks.dat").exists()) {
			ConsoleConnection.severe("You must run RedBlocks with a version earlier than 2.2 to convert your RedBlocks!");
			ConsoleConnection.severe("If you don't, you'll lose your RedBlocks!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		getServer().getPluginManager().registerEvents(new BlockListener(this), this);
		getServer().getPluginManager().registerEvents(new WorldListener(this), this);
		if (config.canUpdate()) {
			checkForUpdate();
		}
		initialized = true;
	}

	@Override
	public void onDisable() {
		final Iterator<PlayerSession> it = playerSessions.values().iterator();
		PlayerSession ps;
		while (it.hasNext()) {
			ps = it.next();
			ps.getBlock().setTypeId(config.getInt(ConfigValue.redblocks_blockID));
			it.remove();
		}
		playerSessions.clear();
		animationThreads.clear();
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
		config = null;
		clistener = null;
		playerSessions = null;
		activeBlocks = null;
		animationThreads = null;
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
					ConsoleConnection.info(ChatColor.GREEN + "An update is available from BukkitDev: " + ChatColor.DARK_GREEN + version);
					return true;
				}
			}
		} catch (final IOException e) {
			ConsoleConnection.warning("An error occured while checking for updates.");
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
		for (final Entry<UUID, PlayerSession> e : playerSessions.entrySet()) {
			if (e.getValue().getRedBlock() == rb) {
				ConsoleConnection.notify(getServer().getPlayer(e.getKey()), msg);
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
				ConsoleConnection.error(p, "That RedBlock is under redstone timeout.", "Stop all redstone powering the RedBlock and try again in a few seconds.");
				rb.setTimeoutState(false);
				return;
			}
			final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.NEW_EDITOR, p);
			getServer().getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				enableRedBlock(rb, !isBeingEdited(rb), false);
				if (config.getBool(ConfigValue.redblocks_soundFX)) {
					b.getWorld().playSound(b.getLocation(), Sound.CHEST_OPEN, 0.5f, 1f);
				}
				if (!playerSessions.containsKey(p.getUniqueId())) {
					playerSessions.put(p.getUniqueId(), new PlayerSession(p.getUniqueId(), rb, b));
				}
				playerSessions.get(p.getUniqueId()).setRedBlock(rb, b);
				b.setType(Material.OBSIDIAN);
				notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.GREEN + " is now editing the RedBlock | " + rb.getBlockCount() + " Blocks");
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
				notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.RED + " is no longer editing the RedBlock | " + rb.getBlockCount() + " Blocks");
				if ((getPlayerSession(p).getDisableDelay() != 0) || (getPlayerSession(p).getEnableDelay() != 0)) {
					ConsoleConnection.notify(p, "You must redefine your place and break delays next time you edit a RedBlock.");
				}
				playerSessions.remove(p.getUniqueId());
				if (config.getBool(ConfigValue.redblocks_soundFX)) {
					b.getWorld().playSound(b.getLocation(), Sound.CHEST_CLOSE, 0.5f, 1f);
				}
				if (!isBeingEdited(rb)) {
					b.setTypeId(config.getInt(ConfigValue.redblocks_blockID));
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
	 * @param notify if true, the editors of the RedBlock will be notified of the change
	 * @param enableDelay the delay for enabling the particular block
	 * @param disableDelay the delay for disabling the particular block
	 */
	public void addBlock(final Player p, final RedBlockAnimated rb, final Block b, final boolean notify, final int enableDelay, final int disableDelay) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.BLOCK_ADDED, p);
		getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					if (!event.isCancelled()) {
						if (rb.add(b, enableDelay, disableDelay) && notify) {
							activeBlocks.add(b.getLocation().toString());
							notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.DARK_GREEN + " Added A Block | " + rb.getBlockCount() + " Blocks");
							if ((enableDelay > 0) || (disableDelay > 0)) {
								notifyEditors(rb, "        " + ChatColor.YELLOW + "Enable Delay: " + ChatColor.GOLD + enableDelay + "ms " + ChatColor.YELLOW + "| Disable Delay: " + ChatColor.GOLD + disableDelay + "ms");
							}
						}
						if ((b.getState().getData() instanceof Bed) && !((Bed) b.getState().getData()).isHeadOfBed()) {
							addBlock(p, rb, b.getRelative(((Bed) b.getState().getData()).getFacing()), notify, enableDelay, disableDelay);
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
	 * @param notify if true, the editors of the RedBlock will be notified of the change
	 */
	public void addBlock(final Player p, final RedBlockAnimated rb, final Block b, final boolean notify) {
		final PlayerSession ps = playerSessions.get(p.getUniqueId());
		addBlock(p, rb, b, notify, (ps == null) ? 0 : ps.getEnableDelay(b), (ps == null) ? 0 : ps.getDisableDelay(b));
	}

	/**
	 * Adds a list of blocks to a RedBlockAnimated and applies player delays.
	 *
	 * Used for World-Edit
	 *
	 * @param p the player to get the session from
	 * @param rb the RedBlock to add the Blocks to
	 * @param cache the list of Blocks
	 * @return the number of Blocks added
	 */
	public int addBlockList(final Player p, final RedBlockAnimated rb, final List<Block> cache) {
		int num = 0;
		final PlayerSession ps = playerSessions.get(p.getUniqueId());
		for (final Block b : cache) {
			final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.BLOCK_ADDED, p);
			getServer().getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				if (rb.add(b, (ps == null) ? 0 : ps.getEnableDelay(b), (ps == null) ? 0 : ps.getDisableDelay(b))) {
					num++;
				}
				if ((b.getState().getData() instanceof Bed) && !((Bed) b.getState().getData()).isHeadOfBed()) {
					if (rb.add(b.getRelative(((Bed) b.getState().getData()).getFacing()), (ps == null) ? 0 : ps.getEnableDelay(b), (ps == null) ? 0 : ps.getDisableDelay(b))) {
						num++;
					}
				}
			}
		}
		return num;
	}

	/**
	 * Removes a block from a RedBlockAnimated.
	 * @param p the player that removed the block
	 * @param rb the RedBlockAnimated that is losing a block
	 * @param b the block to be removed
	 */
	public void removeBlock(final Player p, final RedBlockAnimated rb, final Block b) {
		activeBlocks.remove(b.getLocation().toString());
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
	 * @param doAnimations if true, the RedBlockAnimated will pause for delays
	 * @return if the RedBlockAnimated was enabled
	 */
	public boolean enableRedBlock(final RedBlockAnimated rb, final boolean force, final boolean doAnimations) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.ENABLED);
		getServer().getPluginManager().callEvent(event);
		if (!isBeingEdited(rb) && !event.isCancelled()) {
			Thread t = animationThreads.get(rb);
			if (t != null) {
				t.interrupt();
			}
			for (final RedBlockChild rbc : rb.getBlocks()) {
				activeBlocks.add(rbc.getLocation().toString());
			}
			t = rb.enable((t == null) && !rb.getActiveState() ? force : true, doAnimations);
			if (t != null) {
				animationThreads.put(rb, t);
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
	 * @param doAnimations if true, the RedBlockAnimated will pause for delays
	 * @return if the RedBlockAnimated was disabled
	 */
	public boolean disableRedBlock(final RedBlockAnimated rb, final boolean force, final boolean doAnimations) {
		final RedBlockEvent event = new RedBlockEvent(this, rb, RedBlockCause.DISABLED);
		getServer().getPluginManager().callEvent(event);
		if (!isBeingEdited(rb) && !event.isCancelled()) {
			Thread t = animationThreads.get(rb);
			if (t != null) {
				t.interrupt();
			}
			final RBDisableListener listener = new RBDisableListener() {
				@Override
				public void threadFinished() {
					for (final RedBlockChild rbc : rb.getBlocks()) {
						activeBlocks.remove(rbc.getLocation().toString());
					}
				}
			};
			t = rb.disable((t == null) && !rb.getActiveState() ? force : true, doAnimations, listener);
			if (t != null) {
				animationThreads.put(rb, t);
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
			enableRedBlock(rb, true, false);
			storage.removeRedBlock(b);
			if (breakBlock) {
				b.setTypeId(config.getInt(ConfigValue.redblocks_blockID));
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
		if (playerSessions.containsKey(p.getUniqueId()) && playerSessions.get(p.getUniqueId()).getBlock().getLocation().toString().equals(b.getLocation().toString())) {
			removeEditor(p);
		}
		if (isBeingEdited(storage.getRedBlock(b))) {
			ConsoleConnection.error(p, "You can't destroy a RedBlock that is being edited!");
			return false;
		}
		if (removeRedBlock(b, true)) {
			p.getInventory().removeItem(new ItemStack(config.getInt(ConfigValue.redblocks_destroyItem), 1));
			ConsoleConnection.notify(p, "RedBlock Eliminated");
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
		if ((number > config.getInt(ConfigValue.redblocks_blockID)) && !Permission.BYPASS_MAXREDBLOCKSPER.check(p)) {
			ConsoleConnection.error(p, "You can't create anymore RedBlocks! Max: " + config.getInt(ConfigValue.redblocks_blockID));
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
		notifyEditors(rb, ChatColor.RED + "Your RedBlock was lost/destroyed.");
		for (final UUID pUUID : playerSessions.keySet()) {
			if (getRedBlockEditing(getServer().getPlayer(pUUID)) == rb) {
				removeEditor(getServer().getPlayer(pUUID));
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
				t = Integer.parseInt(type.split(":")[0]);
				d = Integer.parseInt(type.split(":")[1]);
			} else if (type != null) {
				t = Integer.parseInt(type);
			}
		} catch (final Exception e) {
			ConsoleConnection.error(p, "Unknown Type Format", ChatColor.LIGHT_PURPLE + "Operation Cancelled.");
			return;
		}
		if (reg == null) {
			ConsoleConnection.error(p, "No WorldEditCommand Selection Found", ChatColor.LIGHT_PURPLE + "Operation Cancelled.");
			return;
		}
		if ((reg.getArea() > config.getInt(ConfigValue.worldedit_maxAtOnce)) && !Permission.BYPASS_WEMAX.check(p)) {
			ConsoleConnection.error(p, "You have exceeded the maximum threshold of blocks: " + config.getInt(ConfigValue.worldedit_maxAtOnce), ChatColor.LIGHT_PURPLE + "Operation Cancelled.");
			return;
		}
		if (((reg.getArea() + rb.getBlockCount()) > config.getInt(ConfigValue.rules_maxBlocksPer)) && !Permission.BYPASS_MAXBLOCKSPER.check(p)) {
			ConsoleConnection.error(p, "You have exceeded the maximum threshold of blocks: " + config.getInt(ConfigValue.rules_maxBlocksPer), ChatColor.LIGHT_PURPLE + "Operation Cancelled.");
			return;
		}
		final Location min = reg.getMinimumPoint();
		final Location max = reg.getMaximumPoint();
		Block db = null;
		final List<Block> cache = new ArrayList<Block>();
		if (reg.getArea() > 10000) {
			notifyEditors(rb, ChatColor.LIGHT_PURPLE + "Please hold; 10000+ Blocks are being " + (remove ? "removed" : "added") + " via WorldEdit by " + ChatColor.DARK_AQUA + p.getName());
		}
		for (int y = max.getBlockY(); y >= min.getBlockY(); y--) {
			for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
				for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
					db = reg.getWorld().getBlockAt(x, y, z);
					if ((db.getType() == Material.AIR) || (((db.getType() == Material.BEDROCK)) && config.getBool(ConfigValue.worldedit_preventBedrock))) {
						continue;
					}
					if ((t == 0) || (db.getTypeId() == t)) {
						if ((d == 0) || (db.getData() == d)) {
							if (canBuildHere(p, db.getLocation())) {
								cache.add(db);
							}
						}
					}
				}
			}
		}
		notifyEditors(rb, ChatColor.DARK_AQUA + p.getName() + ChatColor.DARK_GREEN + (remove ? " removed " + rb.removeBlockList(cache) : " added " + addBlockList(p, rb, cache)) + " blocks with World-Edit | " + rb.getBlockCount() + " Blocks");
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
					disableRedBlock(rb, false, true);
					startTimeout(rb);
				} else {
					enableRedBlock(rb, false, true);
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
		return playerSessions.containsKey(p.getUniqueId());
	}

	/**
	 * Checks if a RedBlockAnimated is being edited.
	 * @param redBlockAnimated the RedBlockAnimated to check
	 * @return if the RedBlockAnimated is being edited
	 */
	public boolean isBeingEdited(final RedBlockAnimated redBlockAnimated) {
		for (final PlayerSession ps : playerSessions.values()) {
			if (ps.getRedBlock().equals(redBlockAnimated)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the RedBlockAnimated that is being edited by a player.
	 * @param p the player to get the editing RedBlockAnimated of
	 * @return the RedBlock
	 */
	public RedBlockAnimated getRedBlockEditing(final Player p) {
		return playerSessions.get(p.getUniqueId()).getRedBlock();
	}

	/**
	 * Gets the PlayerSession for the provided player.
	 * @param p the player
	 */
	public PlayerSession getPlayerSession(final Player p) {
		return playerSessions.get(p.getUniqueId());
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

	public interface RBDisableListener {
		public void threadFinished();
	}
}