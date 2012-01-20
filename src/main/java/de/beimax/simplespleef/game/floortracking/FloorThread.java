/**
 * 
 */
package de.beimax.simplespleef.game.floortracking;

import java.util.List;

import org.bukkit.block.Block;

import de.beimax.simplespleef.game.Game;

/**
 * @author mkalus
 * Interface for individual tracker threads
 */
public interface FloorThread extends Runnable {
	/**
	 * Initialize the thread
	 * @param game
	 * @param floor
	 */
	public void initializeThread(Game game, List<Block> floor);
	
	/**
	 * Stop tracking
	 */
	public void stopTracking();
	
	/**
	 * update a certain block location
	 */
	public void updateBlock(Block block);
}