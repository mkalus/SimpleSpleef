/**
 * 
 */

package de.beimax.simplespleef.game.floortracking;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.block.Block;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.util.Cuboid;

/**
 * @author mkalus
 * Keeps track of the floor - used by automatic arena degeneration and repair spleefs
 */
public class FloorTracker implements Runnable {
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
	 * flag to stop worker
	 */
	private boolean stop = false;
	
	/**
	 * id of the Bukkit scheduler for the tracker
	 */
	int schedulerId = -1;

	/**
	 * list of floor workers to be called
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
		//initialize floor dissolve task
		if (arenaFloorDissolvesAfter >= 0) {
			floorWorkers.add(new FloorDissolveWorker(arenaFloorDissolvesAfter, arenaFloorDissolveTick, this));
		}

		//initialize floor repair task
		if (arenaFloorRepairsAfter >= 0) {
			floorWorkers.add(new FloorRepairWorker(arenaFloorRepairsAfter, arenaFloorRepairTick, this));
		}

		// get diggable floor
		List<Block> blocks;
		if (floor != null) blocks = floor.getDiggableBlocks(game);
		else blocks = null; // this will stop some of the trackers right away
		
		// initialize tasks one by one
		for (FloorWorker floorWorker : floorWorkers) {
			floorWorker.initialize(game, blocks);
		}
		
		// start tracking
		schedulerId = SimpleSpleef.getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(SimpleSpleef.getPlugin(), this, 0L, 20L);
	}
	
	@Override
	public void run() {
		// not stopped: normal operations
		if (!stop) {
			// actual worker task: execute ticks for all workers
			for (FloorWorker floorWorker : floorWorkers) {
				floorWorker.tick();
			}
		}

		//wait for floor workers to be stopped
		Iterator<FloorWorker> i = floorWorkers.iterator(); //use iterator, because one can remove while traversing...
		while (i.hasNext()) {
			FloorWorker floorWorker = i.next();
			if (floorWorker.isStopped()) i.remove();
		}
		
		// is our list empty? If yes, cancel scheduler
		if (floorWorkers.size() == 0) {
			SimpleSpleef.getPlugin().getServer().getScheduler().cancelTask(schedulerId);
		}
	}
	
	/**
	 * stop tracking floor changes
	 */
	public void stopTracking() {
		stop = true;
		// stop tracking for all floors workers
		for (FloorWorker floorWorker : floorWorkers) {
			floorWorker.stopTracking();
		}
	}
	
	/**
	 * update a certain block location
	 * @param block
	 * @param oldType - old type of block
	 * @param oldData - old data of block
	 */
	public void updateBlock(Block block, int oldType, byte oldData) {
		if (!stop) // if stopped, do not update changes any more
			for (FloorWorker floorWorker : floorWorkers) {
				floorWorker.updateBlock(block, oldType, oldData);
			}
	}
	
	/**
	 * Called by tick()-methods of FloorWorkers when they change a block,
	 * so other trackers can update their block database. 
	 * @param block
	 * @param oldType - old type of block
	 * @param oldData - old data of block
	 * @param caller
	 */
	public void notifyChangedBlock(Block block, int oldType, byte oldData, FloorWorker caller) {
		for (FloorWorker floorWorker : floorWorkers) {
			if (floorWorker != caller) // caller is not updated - has to do this itself
				floorWorker.updateBlock(block, oldType, oldData);
		}
	}
	
	/**
	 * Add a new floor worker to tracker
	 * @param worker
	 */
	public void addFloorWorker(FloorWorker worker) {
		floorWorkers.add(worker);
	}
}
