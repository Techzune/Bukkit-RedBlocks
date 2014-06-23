package com.operontech.redblocks;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

public class Util {
	private static final List<Material> specialBList = Arrays.asList(Material.ACTIVATOR_RAIL, Material.ANVIL, Material.BED_BLOCK, Material.BROWN_MUSHROOM, Material.CACTUS, Material.CAKE_BLOCK, Material.CROPS, Material.DETECTOR_RAIL, Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.FLOWER_POT, Material.ITEM_FRAME, Material.SUGAR_CANE_BLOCK, Material.REDSTONE_WIRE, Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON, Material.PAINTING, Material.POWERED_RAIL, Material.WOOD_DOOR, Material.IRON_DOOR_BLOCK);

	/**
	 * Checks if the typeId provided is declared "special"
	 * 
	 * A block that is defined "special" is one that requires a block under it to exist.
	 * @param typeId the typeId to check
	 * @return if the typeId was declared "special"
	 */
	@SuppressWarnings("deprecation")
	public static boolean isSpecialBlock(final int typeId) {
		return isSpecialBlock(Material.getMaterial(typeId));
	}

	/**
	 * Checks if the Material provided is declared "special"
	 * 
	 * A block that is defined "special" is one that requires a block under it to exist.
	 * @param m the material to check
	 * @return if the material was declared "special"
	 */
	public static boolean isSpecialBlock(final Material m) {
		return specialBList.contains(m);
	}

	/**
	 * Converts a Location to a String that can be converted back using convertStringToLocation(locationAsString)
	 * @param loc the location to convert to a string
	 * @return the location as a string
	 */
	public static String convertLocationToString(final Location loc) {
		return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
	}

	/**
	 * Converts a Location as a String (converted by convertLocationToString(location)) back into a Location
	 * @param str the Location as a String to be converted back into a Location
	 * @return the Location from the String, null if conversion failed
	 */
	public static Location convertStringToLocation(final String str) {
		final String splitString[] = str.split("\\:");
		final Location loc = new Location(Bukkit.getServer().getWorld(splitString[0]), 0, 0, 0);
		if ((splitString.length < 4) || (loc == null)) {
			return null;
		}
		loc.setX(Double.parseDouble(splitString[1]));
		loc.setY(Double.parseDouble(splitString[2]));
		loc.setZ(Double.parseDouble(splitString[3]));
		return loc;
	}

	/**
	 * Checks if the String is an Integer
	 * @param str the String to check if is an Integer
	 * @return if the String was an integer
	 */
	public static boolean isInteger(final String str) {
		try {
			Integer.parseInt(str);
		} catch (final NumberFormatException e) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if the String is a Double
	 * @param str the String to check
	 * @return if the String was a Double
	 */
	public static boolean isDouble(final String str) {
		try {
			Double.parseDouble(str);
		} catch (final NumberFormatException e) {
			return false;
		}
		return true;
	}
}
