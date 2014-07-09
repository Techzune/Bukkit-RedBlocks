package com.operontech.redblocks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * ConsoleConnection: Second Edition
 */
public class ConsoleConnection {

	/**
	 * Sends a message to the console with the plugin's tag.
	 *
	 * Uses AQUA for text color.
	 * @param msg the message
	 */
	public static void info(final Object msg) {
		Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_AQUA + "[RedBlocks] " + ChatColor.AQUA + String.valueOf(msg));
	}

	/**
	 * Sends a message to the console with the plugin's tag.
	 *
	 * Uses RED for text color.
	 * @param msg the message
	 */
	public static void severe(final Object msg) {
		Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_AQUA + "[RedBlocks] " + ChatColor.RED + String.valueOf(msg));
	}

	/**
	 * Sends a message to the console with the plugin's tag.
	 *
	 * Uses DARK_RED for text color.
	 * @param msg the message
	 */
	public static void warning(final Object msg) {
		Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.DARK_AQUA + "[RedBlocks] " + ChatColor.DARK_RED + String.valueOf(msg));
	}

	/**
	 * Sends a message to a player.
	 * @param player player to receive the message
	 * @param msg the message
	 */
	public static void msg(final CommandSender player, final String... msg) {
		player.sendMessage(msg);
	}

	/**
	 * Sends a message to a player.
	 *
	 * Uses GREEN <!> RedBlocks tag.
	 * Uses DARK_GREEN as text color.
	 * @param player player to receive the message
	 * @param msg the message(s)
	 */
	public static void notify(final CommandSender player, final String... msgs) {
		for (final String msg : msgs) {
			msg(player, ChatColor.GREEN + "<!> " + "[RB] " + ChatColor.DARK_GREEN + msg);
		}
	}

	/**
	 * Sends a message to a player.
	 *
	 * Uses RED <!> RedBlocks tag.
	 * Uses RED as text color.
	 * @param player player to receive the message
	 * @param msg the message(s)
	 * @param string
	 */
	public static void error(final CommandSender player, final String... msgs) {
		for (final String msg : msgs) {
			msg(player, ChatColor.RED + "<!> " + "[RB] " + msg);
		}
	}
}
