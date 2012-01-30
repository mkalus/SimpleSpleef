/**
 * 
 */
package de.beimax.simplespleef.game.floortracking;

import java.util.List;

import org.bukkit.block.Block;

import de.beimax.simplespleef.game.Game;

/**
 * @author mkalus
 * Interface for individual tracker tasks
 */
public interface FloorWorker {
	/**
	 * Initialize the tracker
	 * @param game
	 * @param floor
	 */
	public void initialize(Game game, List<Block> floor);
	
	/**
	 * do an action tick, possible do floor change or the like here
	 */
	public void tick();
	
	/**
	 * Stop tracking
	 */
	public void stopTracking();
	
	/**
	 * update a certain block location
	 * @param block - new block data
	 * @param oldType - old type of block
	 * @param oldData - old data of block
	 */
	public void updateBlock(Block block, int oldType, byte oldData);
	
	/**
	 * returns true if tracker has been stopped
	 * @return
	 */
	public boolean isStopped();
}
