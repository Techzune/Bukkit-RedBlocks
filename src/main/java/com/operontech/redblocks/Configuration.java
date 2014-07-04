package com.operontech.redblocks;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;

public class Configuration {
	private final String configVersion = "1.6";
	private final RedBlocksMain plugin;
	private FileConfiguration config;

	public Configuration(final RedBlocksMain plugin) {
		this.plugin = plugin;
		load();
	}

	private void load() {
		plugin.saveDefaultConfig();
		plugin.reloadConfig();
		config = plugin.getConfig();
		if ((getString(ConfigValue.configVersion) == null) || !getString(ConfigValue.configVersion).equals(configVersion)) {
			backup();
		}
	}

	private void backup() {
		final File in = new File("plugins" + File.separator + plugin.getDescription().getName() + File.separator + "config.yml");
		File out = new File("plugins" + File.separator + plugin.getDescription().getName() + File.separator + "[OLD] " + config.getString("configVersion") + "-config.yml");
		int count = 0;
		while (out.exists()) {
			count++;
			out = new File("plugins" + File.separator + plugin.getDescription().getName() + File.separator + "[OLD] " + config.getString("configVersion") + "-config_" + count + ".yml");
		}
		in.renameTo(out);
		plugin.saveDefaultConfig();
	}

	/**
	 * Gets a string value from the configuration.
	 * @param s the value to get
	 * @return the string value
	 */
	public String getString(final ConfigValue s) {
		return config.getString(s.toString());
	}

	/**
	 * Gets an integer value from the configuration.
	 * @param s the value to get
	 * @return the integer value
	 */
	public int getInt(final ConfigValue s) {
		try {
			return Integer.parseInt(config.getString(s.toString()));
		} catch (final Exception e) {
			ConsoleConnection.severe("Config: " + s + " should be a number!");
			return 0;
		}
	}

	/**
	 * Gets a boolean value from the configuration.
	 * @param s the value to get
	 * @return the boolean value
	 */
	public boolean getBool(final ConfigValue s) {
		try {
			return Boolean.valueOf(config.getString(s.toString()));
		} catch (final Exception e) {
			ConsoleConnection.severe("Config: " + s + " should be true/false!");
			return false;
		}
	}

	/**
	 * Reloads the configuration.
	 */
	public void reload() {
		load();
	}

	/**
	 * Checks if the plugin can check for an update.
	 * @return if the plugin can check for an update
	 */
	public boolean canUpdate() {
		return getBool(ConfigValue.updateCheck);
	}
}
