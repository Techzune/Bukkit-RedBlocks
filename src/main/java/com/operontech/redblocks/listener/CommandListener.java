package com.operontech.redblocks.listener;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.operontech.redblocks.ConfigValue;
import com.operontech.redblocks.ConsoleConnection;
import com.operontech.redblocks.Permission;
import com.operontech.redblocks.RedBlocksMain;
import com.operontech.redblocks.Util;
import com.operontech.redblocks.playerdependent.PlayerSession;
import com.operontech.redblocks.storage.RedBlockAnimated;
import com.operontech.redblocks.storage.RedBlockChild;

public class CommandListener {
	private final RedBlocksMain plugin;
	private final ConsoleConnection console;
	private final Map<Player, Player> changeOwner = new HashMap<Player, Player>();

	public CommandListener(final RedBlocksMain plugin) {
		this.plugin = plugin;
		console = plugin.getConsoleConnection();
	}

	@SuppressWarnings("deprecation")
	public boolean onCommand(final CommandSender s, final Command cmd, final String label, final String[] args) {
		if (cmd.getName().equalsIgnoreCase("redblocks") || cmd.getName().equalsIgnoreCase("rb")) {
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("reload")) {
					if (Permission.RELOAD.check(s)) {
						console.notify(s, ChatColor.DARK_AQUA + "RedBlocks Reloading: " + ((plugin.reloadPlugin()) ? ChatColor.GREEN + "Succeeded" : ChatColor.RED + "Failed to Save"));
						return true;
					}
				}
				if (s instanceof Player) {
					final Player p = (Player) s;
					if (plugin.isEditing(p)) {
						final RedBlockAnimated rb = plugin.getRedBlockEditing(p);
						if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("a")) {
							if (plugin.getWE() == null) {
								return true;
							}
							if (Permission.WORLDEDIT.check(s)) {
								plugin.useWorldEdit(rb, p, (args.length > 1) ? args[1] : null, false);
							} else {
								console.error(s, "You do not have the permissions to use World-Edit with RedBlocks!");
								return true;
							}
						} else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("r")) {
							if (plugin.getWE() == null) {
								console.error(s, "World-Edit is not installed on this server!");
								return true;
							}
							if (Permission.WORLDEDIT.check(s)) {
								plugin.useWorldEdit(rb, p, (args.length > 1) ? args[1] : null, true);
							} else {
								console.error(s, "You do not have the permissions to use World-Edit with RedBlocks!");
								return true;
							}
						} else if (Util.multiString(args[0], "stop", "quit", "s")) {
							plugin.removeEditor(p);
						} else if (Util.multiString(args[0], "delay", "d")) {
							if (!Permission.DELAY.check(s)) {
								console.error(s, "You don't have the permission to add delays to RedBlocks.");
								return true;
							}
							final PlayerSession session = plugin.getPlayerSession(p);
							String tempText;
							String sessionBlock = null;
							String pDelay = "0";
							String bDelay = "0";
							if (args.length > 1) {
								for (int i = 1; i < args.length; i++) {
									tempText = args[i].toLowerCase();
									if (tempText.startsWith("block:") || tempText.startsWith("b:")) {
										final String[] splitText = tempText.split(":");
										if (Util.isInteger(splitText[1])) {
											sessionBlock = splitText[1] + ((splitText.length > 2) && Util.isInteger(splitText[2]) ? ":" + splitText[2] : "");
										} else {
											sendCMenu(s);
											return true;
										}
									} else if (tempText.startsWith("time:") || tempText.startsWith("t:")) {
										final String[] splitText = tempText.split(":");
										if ((splitText.length >= 2) && Util.isInteger(splitText[1])) {
											pDelay = splitText[1];
										} else {
											sendCMenu(s);
											return true;
										}
										if ((splitText.length >= 3) && Util.isInteger(splitText[2])) {
											bDelay = splitText[2];
										}
									}
								}
								if (sessionBlock == null) {
									session.setEnableDelay(pDelay);
									session.setDisableDelay(bDelay);
									console.notify(s, ChatColor.YELLOW + "Generic delay for future placed blocks will be: ", ChatColor.YELLOW + "        Enable: " + ChatColor.GOLD + pDelay + "ms" + ChatColor.YELLOW + " | Disable: " + ChatColor.GOLD + bDelay + "ms");
								} else {
									session.setBlockDelay(sessionBlock, pDelay, bDelay);
									console.notify(s, ChatColor.YELLOW + "Delay for future placed blocks with the id " + sessionBlock + " will be: ", ChatColor.YELLOW + "        Enable: " + ChatColor.GOLD + pDelay + "ms" + ChatColor.YELLOW + " | Disable: " + ChatColor.GOLD + bDelay + "ms");
								}
							} else {
								sendCMenu(s);
								return true;
							}
						} else if (Util.multiString(args[0], "options", "o")) {
							if (args.length <= 2) {
								sendCOptions(s);
								return true;
							}
							if (args[1].equalsIgnoreCase("inverted")) {
								if (Permission.OPTIONS_INVERTED.check(s)) {
									if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
										console.notify(s, "RedBlock Option Set | inverted: " + ChatColor.GOLD + rb.setInverted(Boolean.valueOf(args[2].toLowerCase())));
									} else {
										sendCOptions(s);
									}
								}
							} else if (args[1].equalsIgnoreCase("protect")) {
								if (Permission.OPTIONS_PROTECT.check(s)) {
									if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
										console.notify(s, "RedBlock Option Set | protect: " + ChatColor.GOLD + rb.setProtected(Boolean.valueOf(args[2].toLowerCase())));
									} else {
										sendCOptions(s);
									}
								}
							} else if (args[1].equalsIgnoreCase("owner")) {
								if (Permission.OPTIONS_OWNER.check(s)) {
									if (s.getServer().getPlayer(args[2]) == null) {
										changeOwner.remove(s);
										console.error(s, "That player could not be found.");
										return true;
									}
									if (rb.getOwnerUUID().equals(p.getUniqueId())) {
										console.error(s, "You must be current owner of the RedBlock to do that!");
										return true;
									}
									if (changeOwner.containsKey(s) && (changeOwner.get(s) == s.getServer().getPlayer(args[2]))) {
										console.notify(s, "RedBlock Option Set | owner: " + ChatColor.GOLD + rb.setOwner(s.getServer().getPlayer(args[2]).getName()));
										plugin.removeEditor(p);
										changeOwner.remove(s);
										return true;
									}
									changeOwner.put((Player) s, s.getServer().getPlayer(args[2]));
									console.notify(s, ChatColor.LIGHT_PURPLE + "Say the command again to change the owner to: " + ChatColor.GOLD + s.getServer().getPlayer(args[2]).getName());
									console.notify(s, ChatColor.RED + "Warning! You cannot undo this action.");
								} else {
									console.error(s, "You do not have the permissions to change the owner of your RedBlock.");
									return true;
								}
							} else {
								sendCOptions(s);
							}
						} else if (Util.multiString(args[0], "point", "p")) {
							if (Permission.USE.check(s)) {
								final Block b = p.getTargetBlock(null, 100);
								if ((b == null) || b.isEmpty()) {
									console.error(s, "You're looking at nothing!");
									return true;
								} else if ((b.getType() == Material.BEDROCK) && plugin.getConfiguration().getBool(ConfigValue.worldedit_preventBedrock)) {
									console.error(s, "You may not add bedrock to a RedBlock.");
									return true;
								}
								if (args.length > 1) {
									String tempText;
									if (args.length > 1) {
										final RedBlockChild rbc = rb.getChild(b);
										for (int i = 1; i < args.length; i++) {
											tempText = args[i].toLowerCase();
											if (Util.multiString(tempText, "add", "a")) {
												if (!plugin.canBuildHere(p, b.getLocation())) {
													console.error(s, "You do not have the permissions to add this block.");
													return true;
												} else if (rb.contains(b)) {
													console.error(s, "This block already exists in the RedBlock!");
													return true;
												}
												plugin.addBlock(p, rb, b, true);
											} else if (Util.multiString(tempText, "remove", "r")) {
												plugin.removeBlock(p, rb, b);
											} else if (tempText.startsWith("delay:") || tempText.startsWith("d:")) {
												final String[] splitText = tempText.split(":");
												if ((splitText.length > 1) && Util.isInteger(args[1]) && rb.contains(b)) {
													rb.setEnableDelayForChild(rbc, Integer.parseInt(args[1]));
													if ((splitText.length > 2) && Util.isInteger(args[2])) {
														rb.setDisableDelayForChild(rbc, Integer.parseInt(args[2]));
													}
												} else {
													sendCMenu(s);
													return true;
												}
											} else {
												sendCMenu(s);
												return true;
											}
										}
									}
								} else {
									sendCMenu(s);
								}
							} else {
								console.error(s, "You do not have the permissions to use RedBlocks.");
							}
						} else {
							sendCMenu(s);
						}
					} else {
						console.error(s, "You must be editing a RedBlock to do that!");
					}
				} else {
					console.error(s, "You must be a Player to use that command.");
				}
			} else {
				sendCMenu(s);
			}
			return true;
		}
		return false;
	}

	private void sendCMenu(final CommandSender s) {
		console.msg(s, ChatColor.RED + "=====>>>>>{ RedBlocks Menu }<<<<<=====");
		console.msg(s, ChatColor.AQUA + "[] = Optional | <> = Required | UPPERCASE = variable");
		if (Permission.RELOAD.check(s)) {
			console.msg(s, ChatColor.GREEN + "Reload RedBlocks:", "     /rb reload");
		}
		if ((s instanceof Player) && plugin.isEditing((Player) s)) {
			console.msg(s, ChatColor.GREEN + "Stop Editing RedBlock:", "     /rb stop");
			console.msg(s, ChatColor.GREEN + "Point Commands:", "     /rb point <add/remove> [delay:PLACE:BREAK]");
			console.msg(s, ChatColor.GREEN + "Edit Options:", "     /rb options <OPTION> <VALUE>");
			if (Permission.DELAY.check(s)) {
				console.msg(s, ChatColor.GREEN + "Set RedBlockChild Delays:", "     /rb delay <time:PLACE:BREAK> [block:ID:DATA]");
			}
			if (Permission.WORLDEDIT.check(s) && (plugin.getWE() != null)) {
				console.msg(s, ChatColor.GREEN + "World-Edit: Add Child Blocks:", "     /rb add [TYPE:DMG]");
				console.msg(s, ChatColor.GREEN + "World-Edit: Remove Child Blocks:", "     /rb remove [TYPE:DMG]");
			}
		} else {
			console.msg(s, ChatColor.AQUA + "More commands are available when you are editing a RedBlock.");
		}
		console.msg(s, ChatColor.RED + "=====<<<<<{     le'End     }>>>>>=====");
	}

	private void sendCOptions(final CommandSender s) {
		console.msg(s, ChatColor.GOLD + "   >>>>> Options for RedBlocks <<<<<   ");
		if (Permission.OPTIONS_INVERTED.check(s)) {
			console.msg(s, ChatColor.GREEN + "Inverted Redstone:", "     /rb options inverted [default/true/false]");
		}
		if (Permission.OPTIONS_PROTECT.check(s)) {
			console.msg(s, ChatColor.GREEN + "Protect Blocks:", "     /rb options protect [true/false]");
		}
		if (Permission.OPTIONS_OWNER.check(s)) {
			console.msg(s, ChatColor.GREEN + "RedBlock Owner:", "     /rb options owner [NAME]");
			console.msg(s, ChatColor.RED + "     Warning: This cannot be undone. Both players must be online.");
		}
	}
}
