/**
 * 
 */
package de.beimax.simplespleef.gamehelpers;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author mkalus
 *
 */
public class InventoryKeeper {
	/**
	 * keeps the players' inventories
	 */
	private HashMap<Player, List<InventoryEntry>> playerInventories = new HashMap<Player, List<InventoryEntry>>();
	
	/**
	 * save a player's inventory to a file
	 * @param player
	 * @return
	 */
	public boolean saveInventory(Player player) {
		if (player == null || playerInventories == null) return false; // no NPEs
		
		// create inventory entry
		LinkedList<InventoryEntry> copiedInventory = new LinkedList<InventoryKeeper.InventoryEntry>();
		
		// cycle through entries of stack
		int i = 0;
		for (ItemStack stack : player.getInventory().getContents()) {
			if (stack != null) //add new clone of stack to inventory
				copiedInventory.add(new InventoryEntry(i, stack.clone()));
			i++;
		}
		
		// remove old entry, if needed
		if (playerInventories.containsKey(player))
			playerInventories.remove(player);
		
		// add new entry to list
		playerInventories.put(player, copiedInventory);

		// finally, clear player's inventory
		player.getInventory().clear();

		return true;
	}
	
	/**
	 * restore a player's inventory to a file
	 * @param player
	 * @return
	 */
	public boolean restoreInventory(Player player) {
		if (player == null || playerInventories == null) return false; // no NPEs

		// entry not found -> return
		if (!playerInventories.containsKey(player)) return false;
			
		// clear player's inventory first
		Inventory inventory = player.getInventory();
		inventory.clear();
		
		// get contents from list and put them into player inventory
		for (InventoryEntry inventoryEntry : playerInventories.get(player)) {
			inventory.setItem(inventoryEntry.index, inventoryEntry.item.clone());
		}
		
		// finally, remove old entry
		playerInventories.remove(player);

		return false;
	}
	
	/**
	 * private class to keep track of item stack entries
	 * @author mkalus
	 *
	 */
	private class InventoryEntry {
		private int index;
		private ItemStack item;
		
		/**
		 * Constructor
		 * @param index
		 * @param item
		 */
		public InventoryEntry(int index, ItemStack item) {
			this.index = index;
			this.item = item;
		}
	}
}
