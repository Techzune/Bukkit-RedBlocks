package com.operontech.redblocks;

public enum ConfigValue {
	configVersion("configVersion"),
	updateCheck("updateCheck"),
	rules_maxBlocksPer("rules.maxBlocksPer"),
	rules_maxRedBlocksPer("rules.maxRedBlocksPer"),
	redblocks_blockID("redblocks.blockID"),
	redblocks_drops("redblocks.drops"),
	redblocks_soundFX("redblocks.soundFX"),
	redblocks_destroyItem("redblocks.destroyItem"),
	redblocks_redstoneTimeout("redblocks.redstoneTimeout"),
	worldedit_maxAtOnce("worldedit.maxAtOnce"),
	worldedit_preventBedrock("worldedit.preventBedrock"),
	gc_onWorldEdit("gc.onWorldEdit"),
	gc_onDisableRedBlock("gc.onDisableRedBlock"),
	gc_onEnableRedBlock("gc.onEnableRedBlock"),
	gc_onDestroyRedBlock("gc.onDestroyRedBlock");

	private String configString;

	private ConfigValue(final String configString) {
		this.configString = configString;
	}

	@Override
	public String toString() {
		return configString;
	}
}
