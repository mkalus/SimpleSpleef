/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.beimax.simplespleef.game.floortracking.FloorTracker;
import de.beimax.simplespleef.util.MaterialHelper;

/**
 * @author mkalus
 * Keeps tracks of blocks players are standing on etc.
 */
public class PlayerOnBlockDegenerator {
	/**
	 * seconds to wait for degeneration
	 */
	private int seconds;
	
	/**
	 * list of blocks that will degenerate when standing on them for too long
	 */
	private List<ItemStack> degeneratingBlocks;
	
	/**
	 * list of DegenerationKeeper to players
	 */
	private Map<Player, DegenerationKeeper> degenerationList;
	
	/**
	 * reference to floor tracker
	 */
	private FloorTracker floorTracker;

	/**
	 * Constructor
	 * @param numberOfSecondsToDegenerate
	 */
	public PlayerOnBlockDegenerator(int numberOfSecondsToDegenerate, List<String> degeneratingBlocks, FloorTracker floorTracker) {
		seconds = numberOfSecondsToDegenerate;
		
		// fill with degenerating blocks
		this.degeneratingBlocks = new LinkedList<ItemStack>();
		for (String string : degeneratingBlocks) {
			ItemStack stack = MaterialHelper.getItemStackFromString(string);
			if (stack != null) this.degeneratingBlocks.add(stack);
		}
		
		degenerationList = new HashMap<Player, PlayerOnBlockDegenerator.DegenerationKeeper>();
		
		this.floorTracker = floorTracker;
	}

	/**
	 * update player's position
	 * @param player
	 */
	public void updatePlayer(Player player) {
		// get block player is standing on
		Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		
		// get keeper
		DegenerationKeeper keeper = degenerationList.get(player);
		
		// add player if needed
		if (keeper == null) {
			keeper = new DegenerationKeeper();
			keeper.updateBlock(block);
			degenerationList.put(player, keeper);
			keeper.start(); // run the keeper
		} else // just update the block
			keeper.updateBlock(block);
	}
	
	public void removePlayer(Player player) {
		// get keeper
		DegenerationKeeper keeper = degenerationList.get(player);
		
		// if keeper found for player
		if (keeper != null) {
			keeper.stop = true; //stop keeper
			degenerationList.remove(player); //remove from list
		}
	}

	/**
	 * stop the block degenerator (its threads, for the most part)
	 */
	public void stopBlockDegenerator() {
		for (DegenerationKeeper keeper : degenerationList.values()) {
			// iterate through keepers and stop them one by one
			keeper.stop = true;
		}
	}
	
	/**
	 * Keeps track of player's block and its degeneration
	 * @author mkalus
	 *
	 */
	private class DegenerationKeeper extends Thread {
		/**
		 * set to true to stop keeping track
		 */
		private boolean stop = false;
		
		/**
		 * keeps track of the checked block
		 */
		private Block checkedBlock = null;
		
		/**
		 * timestamp
		 */
		private long timestamp;
		
		@Override
		public void run() {
			// set timestamp to dummy value, since we have not checked anything yet
			timestamp = Long.MAX_VALUE;
			
			while (!stop) { // run until the end of time (or stopped)
				try { // try to sleep a little
					Thread.sleep(200);
				} catch (InterruptedException e) {}

				// has timestamp been exceeded?
				if (System.currentTimeMillis() >= timestamp && checkedBlock != null) {
					checkedBlock.setType(Material.AIR); // block dissolves into thin air
					checkedBlock.setData((byte) 0);
					timestamp = Long.MAX_VALUE; // to not have this happen again
					// notify floor tracker
					if (floorTracker != null)
						floorTracker.updateBlock(checkedBlock);
				}
			}
			
			// clean up
			checkedBlock = null;
		}
		
		/**
		 * called by updatePlayer to update a block
		 * @param block
		 */
		public synchronized void updateBlock(Block block) {
			if (block == null) return; //no NPEs
			if (block.getType() != Material.AIR &&
					(checkedBlock == null || block.getX() != checkedBlock.getX() || block.getY() != checkedBlock.getY() || block.getZ() != checkedBlock.getZ())) { // ignore air...
				// only if in list of blocks
				if (degeneratingBlocks == null || degeneratingBlocks.size() == 0 || MaterialHelper.isSameBlockType(block, degeneratingBlocks)) {
					timestamp = System.currentTimeMillis() + ((long) seconds * 1000); // update timestamp
					checkedBlock = block; // update block
				}
			}
		}
	}
}
