package com.operontech.redblocks;

import org.bukkit.command.CommandSender;

public enum Permission {
	USE(
			"use"),
	ANIMATIONS(
			"animations"),
	RELOAD(
			"reload"),
	WORLDEDIT(
			"worldedit"),
	DELAY(
			"delay"),
	CREATEANDDESTROY(
			"createanddestroy"),
	BYPASS_PROTECT(
			"bypass.protect"),
	BYPASS_WEMAX(
			"bypass.WEMax"),
	BYPASS_MAXBLOCKSPER(
			"bypass.maxBlocksPer"),
	BYPASS_MAXREDBLOCKSPER(
			"bypass.maxRedBlocksPer"),
	OPTIONS_PROTECT(
			"options.protect"),
	OPTIONS_INVERTED(
			"options.inverted"),
	OPTIONS_OWNER(
			"options.owner");
	private String pString;

	private Permission(final String pString) {
		this.pString = pString;
	}

	public String getPermission() {
		return "redblocks." + pString.toLowerCase();
	}

	public boolean check(final CommandSender s) {
		return s.isOp() || s.hasPermission(getPermission());
	}
}
