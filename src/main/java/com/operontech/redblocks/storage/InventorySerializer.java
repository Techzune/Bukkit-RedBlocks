package com.operontech.redblocks.storage;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventorySerializer {

	/**
	 * Converts an Inventory into a String.
	 * @param invInventory the Inventory to convert
	 * @return the serialized inventory
	 */
	public String convertToString(final Inventory invInventory) {
		String serialization = invInventory.getSize() + ";";
		for (int i = 0; i < invInventory.getSize(); i++) {
			final ItemStack is = invInventory.getItem(i);
			if (is != null) {
				String serializedItemStack = new String();

				final String isType = String.valueOf(is.getType());
				serializedItemStack += "t@" + isType;

				if (is.getDurability() != 0) {
					final String isDurability = String.valueOf(is.getDurability());
					serializedItemStack += ":d@" + isDurability;
				}

				if (is.getAmount() != 1) {
					final String isAmount = String.valueOf(is.getAmount());
					serializedItemStack += ":a@" + isAmount;
				}

				final Map<Enchantment, Integer> isEnch = is.getEnchantments();
				if (isEnch.size() > 0) {
					for (final Entry<Enchantment, Integer> ench : isEnch.entrySet()) {
						serializedItemStack += ":e@" + ench.getKey().getName() + "@" + ench.getValue();
					}
				}

				serialization += i + "#" + serializedItemStack + ";";
			}
		}
		return serialization;
	}

	/**
	 * Converts a serialized Inventory string to an Inventory.
	 * @param invString the serialized Inventory
	 * @param type the type of Inventory to return
	 * @return the Inventory
	 */
	@SuppressWarnings("deprecation")
	public ItemStack[] convertToInventory(final String invString, final InventoryType type) {
		if ((invString == null) || (invString == "")) {
			return new ItemStack[] {};
		}
		final String[] serializedBlocks = invString.split(";");
		final String invInfo = serializedBlocks[0];
		Inventory deserializedInventory = null;
		if ((Integer.valueOf(invInfo) % 9) == 0) {
			deserializedInventory = Bukkit.getServer().createInventory(null, Integer.valueOf(invInfo));
		} else {
			deserializedInventory = Bukkit.getServer().createInventory(null, type);
		}

		for (int i = 1; i < serializedBlocks.length; i++) {
			final String[] serializedBlock = serializedBlocks[i].split("#");
			final int stackPosition = Integer.valueOf(serializedBlock[0]);

			if (stackPosition >= deserializedInventory.getSize()) {
				continue;
			}

			ItemStack is = null;
			Boolean createdItemStack = false;

			final String[] serializedItemStack = serializedBlock[1].split(":");
			for (final String itemInfo : serializedItemStack) {
				final String[] itemAttribute = itemInfo.split("@");
				if (itemAttribute[0].equals("t")) {
					if (isNumber(itemAttribute[1])) {
						itemAttribute[1] = Material.getMaterial(Integer.valueOf(itemAttribute[1])).name();
					}
					is = new ItemStack(Material.getMaterial(itemAttribute[1]));
					createdItemStack = true;
				} else if (itemAttribute[0].equals("d") && createdItemStack) {
					is.setDurability(Short.valueOf(itemAttribute[1]));
				} else if (itemAttribute[0].equals("a") && createdItemStack) {
					is.setAmount(Integer.valueOf(itemAttribute[1]));
				} else if (itemAttribute[0].equals("e") && createdItemStack) {
					is.addEnchantment(Enchantment.getByName(itemAttribute[1]), Integer.valueOf(itemAttribute[2]));
				}
			}
			deserializedInventory.setItem(stackPosition, is);
		}

		return deserializedInventory.getContents();
	}

	private static boolean isNumber(final String number) {
		try {
			Double.parseDouble(number);
		} catch (final NumberFormatException nfe) {
			return false;
		}
		return true;
	}
}
