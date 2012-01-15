/**
 * 
 */
package de.beimax.simplespleef.util;


import java.io.File;

import org.bukkit.entity.Player;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 *
 */
public class InventoryKeeper {
	private File saveDir;
	/**
	 * Constructor - creates data folder subdir, if needed
	 */
	public InventoryKeeper() {
		//check, if directory exists
		saveDir = new File(SimpleSpleef.getPlugin().getDataFolder(), "playerinventories");
		if (!saveDir.exists()) saveDir.mkdir(); // create, if not exists
		if (!saveDir.exists() || !saveDir.isDirectory()) {
			SimpleSpleef.log.severe("[SimpleSpleef] Could not create directory " + saveDir + ". Check file permissions, etc.!");
			saveDir = null;
		}
	}
	
	/**
	 * save a player's inventory to a file
	 * @param player
	 * @return
	 */
	public boolean saveInventory(Player player) {
		/*if (player == null || saveDir == null) return false; // no NPE
		
		// file name
		File file = new File(saveDir, player.getName());
		
		YamlConfiguration playerInv = new YamlConfiguration();
		
		int i = 0;
		for(ItemStack itemStack : player.getInventory().getContents()) {
			playerInv.set(String.valueOf(i++), itemStack);
		}
		
		try {
			playerInv.save(file);
		} catch (Exception e) {
			SimpleSpleef.log.severe("[SimpleSpleef] Could not create save player's inventory " + file + "! Skipping save...");
			return false;
		}*/
		SimpleSpleef.log.warning("[SimpleSpleef] ItemStack serialization is broken right now - skipping inventory saving.");
		return true;
	}
	
	/**
	 * restore a player's inventory to a file
	 * @param player
	 * @return
	 */
	public boolean restoreInventory(Player player) {
		/*if (player == null || saveDir == null) return false; // no NPE
		
		// file name
		File file = new File(saveDir, player.getName());
		YamlConfiguration playerInv = new YamlConfiguration();
		try {
			playerInv.load(file);
		} catch (Exception e) {
			SimpleSpleef.log.severe("[SimpleSpleef] Could not create restore player's inventory " + file + "! Skipping save...");
			e.printStackTrace();
			return false;
		}*/
		SimpleSpleef.log.warning("[SimpleSpleef] ItemStack deserialization is broken right now - skipping inventory restoration.");
		return false;
	}
}
