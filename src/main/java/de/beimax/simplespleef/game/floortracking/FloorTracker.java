/**
 * 
 */

package de.beimax.simplespleef.game.floortracking;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.block.Block;

import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.util.Cuboid;

/**
 * @author mkalus
 * Keeps track of the floor - used by automatic arena degeneration and repair spleefs
 */
public class FloorTracker {
	/**
	 * Time in seconds, after which the arena floor starts to dissolve slowly (-1 disables this)
	 */
	private int arenaFloorDissolvesAfter = -1;
	
	/**
	 * Time in seconds for new blocks to dissolve into thin air
	 */
	private int arenaFloorDissolveTick = 5;
	
	/**
	 * Time in seconds, after which the arena floor starts to repair slowly (-1 disables this)
	 */
	private int arenaFloorRepairsAfter = -1;
	
	/**
	 * Time in seconds for new blocks to get repaired
	 */
	private int arenaFloorRepairTick = 10;
	
	/**
	 * list of floor threads to be called
	 */
	LinkedList<FloorThread> floorThreads = new LinkedList<FloorThread>();
	
	/**
	 * @return the arenaFloorDissolvesAfter
	 */
	public int getArenaFloorDissolvesAfter() {
		return arenaFloorDissolvesAfter;
	}

	/**
	 * @param arenaFloorDissolvesAfter the arenaFloorDissolvesAfter to set
	 */
	public void setArenaFloorDissolvesAfter(int arenaFloorDissolvesAfter) {
		this.arenaFloorDissolvesAfter = arenaFloorDissolvesAfter;
	}

	/**
	 * @return the arenaFloorDissolveTick
	 */
	public int getArenaFloorDissolveTick() {
		return arenaFloorDissolveTick;
	}

	/**
	 * @param arenaFloorDissolveTick the arenaFloorDissolveTick to set
	 */
	public void setArenaFloorDissolveTick(int arenaFloorDissolveTick) {
		this.arenaFloorDissolveTick = arenaFloorDissolveTick;
	}

	/**
	 * @return the arenaFloorRepairsAfter
	 */
	public int getArenaFloorRepairsAfter() {
		return arenaFloorRepairsAfter;
	}

	/**
	 * @param arenaFloorRepairsAfter the arenaFloorRepairsAfter to set
	 */
	public void setArenaFloorRepairsAfter(int arenaFloorRepairsAfter) {
		this.arenaFloorRepairsAfter = arenaFloorRepairsAfter;
	}

	/**
	 * @return the arenaFloorRepairTick
	 */
	public int getArenaFloorRepairTick() {
		return arenaFloorRepairTick;
	}

	/**
	 * @param arenaFloorRepairTick the arenaFloorRepairTick to set
	 */
	public void setArenaFloorRepairTick(int arenaFloorRepairTick) {
		this.arenaFloorRepairTick = arenaFloorRepairTick;
	}

	/**
	 * start tracking floor changes
	 * @param game
	 * @param floor
	 */
	public void startTracking(Game game, Cuboid floor) {
		//initialize floor dissolve thread
		if (arenaFloorDissolvesAfter >= 0) {
			floorThreads.add(new FloorDissolveThread(arenaFloorDissolvesAfter, arenaFloorDissolveTick, this));
		}

		//initialize floor repair thread
		if (arenaFloorRepairsAfter >= 0) {
			floorThreads.add(new FloorRepairThread(arenaFloorRepairsAfter, arenaFloorRepairTick, this));
		}

		// get diggable floor
		List<Block> blocks = floor.getDiggableBlocks(game);
		
		// start threads one by one
		for (FloorThread floorThread : floorThreads) {
			floorThread.initializeThread(game, blocks);
			new Thread(floorThread).start(); // start tracking
		}
	}
	
	/**
	 * stop tracking floor changes
	 */
	public void stopTracking() {
		for (FloorThread floorThread : floorThreads) {
			floorThread.stopTracking();
		}
	}
	
	/**
	 * update a certain block location
	 * @param block
	 */
	public void updateBlock(Block block) {
		for (FloorThread floorThread : floorThreads) {
			floorThread.updateBlock(block);
		}
	}
	
	/**
	 * Called by tick()-methods of FloorThreads when they change a block,
	 * so other trackers can update their block database. 
	 * @param block
	 * @param caller
	 */
	public void notifyChangedBlock(Block block, FloorThread caller) {
		for (FloorThread floorThread : floorThreads) {
			if (floorThread != caller) // caller is not updated - has to do this itself
				floorThread.updateBlock(block);
		}
	}
	
	/**
	 * Add a new floor thread to tracker
	 * @param thread
	 */
	public void addFloorThread(FloorThread thread) {
		floorThreads.add(thread);
	}
}
