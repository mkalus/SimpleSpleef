/**
 * 
 */

package de.beimax.simplespleef.game.floortracking;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.block.Block;

import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.util.Cuboid;

/**
 * @author mkalus
 * Keeps track of the floor - used by automatic arena degeneration and repair spleefs
 */
public class FloorTracker extends Thread {
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
	 * flag to stop thread
	 */
	private boolean stop = false;

	/**
	 * list of floor threads to be called
	 */
	LinkedList<FloorWorker> floorWorkers = new LinkedList<FloorWorker>();
	
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
			floorWorkers.add(new FloorDissolveWorker(arenaFloorDissolvesAfter, arenaFloorDissolveTick, this));
		}

		//initialize floor repair thread
		if (arenaFloorRepairsAfter >= 0) {
			floorWorkers.add(new FloorRepairWorker(arenaFloorRepairsAfter, arenaFloorRepairTick, this));
		}

		// get diggable floor
		List<Block> blocks = floor.getDiggableBlocks(game);
		
		// start threads one by one
		for (FloorWorker floorThread : floorWorkers) {
			floorThread.initialize(game, blocks);
		}
		
		// start tracking thread
		start();
	}
	
	@Override
	public void run() {
		// actual worker thread
		while (!stop) {
			try {
				Thread.sleep(200); // sleep for a fifth of a second before doing anything else
			} catch (InterruptedException e) {}
			//execute ticks for all workers
			for (FloorWorker floorThread : floorWorkers) {
				floorThread.tick();
			}
		}
		
		// stop tracking for all floors workers
		for (FloorWorker floorThread : floorWorkers) {
			floorThread.stopTracking();
		}

		//wait for floor workers to be stopped
		do {
			Iterator<FloorWorker> i = floorWorkers.iterator(); //use iterator, because one can remove while traversing...
			while (i.hasNext()) {
				FloorWorker floorThread = i.next();
				if (floorThread.isStopped()) i.remove();
			}
		} while (floorWorkers.size() > 0);
	}
	
	/**
	 * stop tracking floor changes
	 */
	public void stopTracking() {
		stop = true;
	}
	
	/**
	 * update a certain block location
	 * @param block
	 * @param oldType - old type of block
	 * @param oldData - old data of block
	 */
	public void updateBlock(Block block, int oldType, byte oldData) {
		for (FloorWorker floorThread : floorWorkers) {
			floorThread.updateBlock(block, oldType, oldData);
		}
	}
	
	/**
	 * Called by tick()-methods of FloorThreads when they change a block,
	 * so other trackers can update their block database. 
	 * @param block
	 * @param oldType - old type of block
	 * @param oldData - old data of block
	 * @param caller
	 */
	public void notifyChangedBlock(Block block, int oldType, byte oldData, FloorWorker caller) {
		for (FloorWorker floorThread : floorWorkers) {
			if (floorThread != caller) // caller is not updated - has to do this itself
				floorThread.updateBlock(block, oldType, oldData);
		}
	}
	
	/**
	 * Add a new floor thread to tracker
	 * @param thread
	 */
	public void addFloorThread(FloorWorker thread) {
		floorWorkers.add(thread);
	}
}
