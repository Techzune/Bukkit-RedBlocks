package com.operontech.redblocks.listener;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.operontech.redblocks.ConsoleConnection;
import com.operontech.redblocks.RedBlocksMain;
import com.operontech.redblocks.Util;
import com.operontech.redblocks.playerdependent.PlayerSession;
import com.operontech.redblocks.storage.RedBlockAnimated;

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
					if (plugin.hasPermission(s, "reload")) {
						console.notify(s, "RedBlocks Reloading: " + ((plugin.reloadPlugin()) ? "Succeeded" : ChatColor.RED + "Failed to Save"));
					}
				}
				if (s instanceof Player) {
					final Player p = (Player) s;
					if (plugin.isEditing(p)) {
						final RedBlockAnimated rb = plugin.getRedBlockEditing(p);
						if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("a")) {
							if (plugin.getWE() == null) {
								console.error(s, "RedBlocks could not find World-Edit!");
								return true;
							}
							if (plugin.hasPermission(s, "worldedit")) {
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
							if (plugin.hasPermission(s, "worldedit")) {
								plugin.useWorldEdit(rb, p, (args.length > 1) ? args[1] : null, true);
							} else {
								console.error(s, "You do not have the permissions to use World-Edit with RedBlocks!");
								return true;
							}
						} else if (multiString(args[0], "stop", "quit", "s")) {
							if (plugin.isEditing(p)) {
								plugin.removeEditor(p);
							} else {
								console.error(s, "You must be editing a RedBlock to do that!");
								return true;
							}
						} else if (multiString(args[0], "delay", "d")) {
							if (!plugin.isEditing(p)) {
								console.error(s, "You must editing a RedBlock to do that!");
								return true;
							}
							if (args.length >= 3) {
								if (multiString(args[1], "place", "p", "break", "b")) {
									final PlayerSession session = plugin.getPlayerSession(p);
									String tempText;
									if (Util.isInteger(args[2])) {
										if (multiString(args[1], "place", "p")) {
											session.setEnableDelay(Integer.parseInt(args[2]));
											console.notify(s, "All future child blocks you place will delay on enabling for " + args[2] + " milliseconds.");
										} else {
											session.setDisableDelay(Integer.parseInt(args[2]));
											console.notify(s, "All future child blocks you place will delay on disabling for " + args[2] + " milliseconds.");
										}
									}
									if (args.length > 3) {
										for (int i = 3; i < args.length; i++) {
											tempText = args[i].toLowerCase();
											if (tempText.startsWith("block:") || tempText.startsWith("b:")) {
												final String[] splitText = tempText.split(":");
												if ((splitText.length == 2) && Util.isInteger(splitText[1])) {
													if (multiString(args[1], "place", "p")) {
														session.setEnableDelayBlock(Integer.parseInt(splitText[1]), 0);
														console.notify(s, "All future child blocks you place will only delay enabling if they are block id " + splitText[1]);
													} else {
														session.setDisableDelayBlock(Integer.parseInt(splitText[1]), 0);
														console.notify(s, "All future child blocks you place will only delay disabling if they are block id " + splitText[1]);
													}
												} else if ((splitText.length == 3) && Util.isInteger(splitText[1]) && Util.isInteger(splitText[2])) {
													if (multiString(args[1], "place", "p")) {
														session.setEnableDelayBlock(Integer.parseInt(splitText[1]), Integer.parseInt(splitText[2]));
														console.notify(s, "All future child blocks you place will only delay enabling if they are block id " + splitText[1] + ":" + splitText[2]);
													} else {
														session.setDisableDelayBlock(Integer.parseInt(splitText[1]), Integer.parseInt(splitText[2]));
														console.notify(s, "All future child blocks you place will only delay disabling if they are block id " + splitText[1] + ":" + splitText[2]);
													}
												} else {
													sendCMenu(s);
													return true;
												}
											}
										}
									}
								} else {
									sendCMenu(s);
									return true;
								}
							} else {
								sendCMenu(s);
								return true;
							}
						} else if (multiString(args[0], "options", "o")) {
							if (args.length <= 2) {
								sendCOptions(s);
								return true;
							}
							if (args[1].equalsIgnoreCase("inverted")) {
								if (plugin.hasPermission(s, "optionsInverted")) {
									if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
										console.notify(s, "RedBlock Option Set | inverted: " + rb.setInverted(Boolean.valueOf(args[2].toLowerCase())));
									} else {
										sendCOptions(s);
									}
								}
							} else if (args[1].equalsIgnoreCase("protect")) {
								if (plugin.hasPermission(s, "optionsProtect")) {
									if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
										console.notify(s, "RedBlock Option Set | protect: " + rb.setProtected(Boolean.valueOf(args[2].toLowerCase())));
									} else {
										sendCOptions(s);
									}
								}
							} else if (args[1].equalsIgnoreCase("owner")) {
								if (plugin.hasPermission(p, "optionsOwner")) {
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
										console.notify(s, "RedBlock Option Set | owner: " + rb.setOwner(s.getServer().getPlayer(args[2]).getName()));
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
						} else {
							sendCMenu(s);
						}
					} else {
						console.error(s, "You must be editing a RedBlock to do that!");
					}
				}
			} else {
				if (s instanceof Player) {
					sendCMenu(s);
				}
			}
			return true;
		}
		return false;
	}

	private void sendCMenu(final CommandSender s) {
		console.msg(s, ChatColor.GOLD + "   >>>>> RedBlocks Menu <<<<<   ");
		if (plugin.hasPermission(s, "reload")) {
			console.msg(s, ChatColor.GREEN + "Reload RedBlocks:" + ChatColor.LIGHT_PURPLE + " /rb reload");
		}
		console.msg(s, ChatColor.GREEN + "Stop Editing RedBlock:" + ChatColor.LIGHT_PURPLE + " /rb stop");
		console.msg(s, ChatColor.GREEN + "Set Pause For Single Child Block:" + ChatColor.LIGHT_PURPLE + " /rb p <place/break> <MILLISECONDS>");
		console.msg(s, ChatColor.GREEN + "Set Pause For Multiple Child Blocks:" + ChatColor.LIGHT_PURPLE + " /rb pm <place/break> <MILLISECONDS>");
		console.msg(s, ChatColor.GREEN + "Stop Editing RedBlock:" + ChatColor.LIGHT_PURPLE + " /rb stop");
		console.msg(s, ChatColor.GREEN + "Edit Options:" + ChatColor.LIGHT_PURPLE + " /rb options <OPTION> <VALUE>");
		console.msg(s, ChatColor.GREEN + "Set RedBlockChild Delays:" + ChatColor.LIGHT_PURPLE + " /rb delay [place/break] [MILLISECONDS] <block:ID:DATA>");
		if (plugin.hasPermission(s, "worldedit") && (plugin.getWE() != null)) {
			console.msg(s, ChatColor.GREEN + "World-Edit: AddCommand Blocks:" + ChatColor.LIGHT_PURPLE + " /rb add [TYPE:DMG]");
			console.msg(s, ChatColor.GREEN + "World-Edit: RemoveCommand Blocks:" + ChatColor.LIGHT_PURPLE + " /rb remove [TYPE:DMG]");
		}
	}

	private void sendCOptions(final CommandSender s) {
		console.msg(s, ChatColor.GOLD + "   >>>>> Options for RedBlocks <<<<<   ");
		if (plugin.hasPermission(s, "optionsInverted")) {
			console.msg(s, ChatColor.GREEN + "Inverted Redstone:" + ChatColor.LIGHT_PURPLE + " /rb options inverted [default/true/false]");
		}
		if (plugin.hasPermission(s, "optionsProtect")) {
			console.msg(s, ChatColor.GREEN + "Protect Blocks:" + ChatColor.LIGHT_PURPLE + " /rb options protect [true/false]");
		}
		if (plugin.hasPermission(s, "optionsOwner")) {
			console.msg(s, ChatColor.GREEN + "RedBlock Owner:" + ChatColor.LIGHT_PURPLE + " /rb options owner [NAME]");
			console.msg(s, ChatColor.RED + "    Warning: This cannot be undone. Both players must be online.");
		}
	}

	private boolean multiString(final String mainString, final String... args) {
		return multiEqualsString(mainString.toLowerCase(), args);
	}

	private boolean multiEqualsString(final String mainString, final String... args) {
		for (final String arg : args) {
			if (mainString.equals(arg)) {
				return true;
			}
		}
		return false;
	}
}
