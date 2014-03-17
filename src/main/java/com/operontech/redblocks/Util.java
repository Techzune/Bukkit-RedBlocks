package com.operontech.redblocks;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

public class Util {
	private static final List<Material> specialBList = Arrays.asList(Material.ACTIVATOR_RAIL, Material.ANVIL, Material.BED_BLOCK, Material.BROWN_MUSHROOM, Material.CACTUS, Material.CAKE_BLOCK, Material.CROPS, Material.DETECTOR_RAIL, Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.FLOWER_POT, Material.ITEM_FRAME, Material.SUGAR_CANE_BLOCK, Material.REDSTONE_WIRE, Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON, Material.PAINTING, Material.POWERED_RAIL, Material.WOOD_DOOR, Material.IRON_DOOR_BLOCK);

	@SuppressWarnings("deprecation")
	public static boolean isSpecialBlock(final int typeId) {
		return isSpecialBlock(Material.getMaterial(typeId));
	}

	public static boolean isSpecialBlock(final Material m) {
		return specialBList.contains(m);
	}

	public static String convertLocationToString(final Location loc) {
		return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
	}

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
}
