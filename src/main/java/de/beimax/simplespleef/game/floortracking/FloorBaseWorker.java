/**
 * 
 */
package de.beimax.simplespleef.game.floortracking;

import java.util.List;

import org.bukkit.block.Block;

import de.beimax.simplespleef.game.Game;

/**
 * @author mkalus
 *
 */
public abstract class FloorBaseWorker implements FloorWorker {
	/**
	 * seconds after which task actually starts doing something
	 */
	private int startAfter;
	
	/**
	 * seconds to wait each tick of the task
	 */
	private int tickTime;
	
	/**
	 * system time to start task at
	 */
	private long startAt = 0;
	
	/**
	 * system time for next tick
	 */
	private long nextTick = 0;
	
	/**
	 * reference to tracker
	 */
	private FloorTracker tracker;
	
	/**
	 * flag to stop task
	 */
	protected boolean stop = false;
	
	/**
	 * Constructor
	 * @param startAfter
	 * @param tickTime
	 */
	public FloorBaseWorker(int startAfter, int tickTime, FloorTracker tracker) {
		this.startAfter = startAfter;
		this.tickTime = tickTime;
		this.tracker = tracker;
	}
	
	/**
	 * initialize task
	 */
	@Override
	public void initialize(Game game, List<Block> floor) {
		startAt = System.currentTimeMillis() + ((long) startAfter * 1000);
		nextTick = startAt;
	}

	/**
	 * do a tick
	 */
	@Override
	public void tick() {
		long now = System.currentTimeMillis();
		// first, wait for worker to start and wait for the next tick
		if (now < startAt || now < nextTick) return;

		// now do the actual tick work
		executeTick();
		// update tick
		nextTick = nextTick + ((long) tickTime * 1000);
	}
	
	/**
	 * do a tick within the task
	 */
	public abstract void executeTick();
	
	/**
	 * Notify the tracker about a block change - called by tick() when a block changes,
	 * so other trackers can update this block
	 * @param block changed block
	 */
	protected void notifyTracker(Block block, int oldType, byte oldData) {
		tracker.notifyChangedBlock(block, oldType, oldData, this);
	}
	
	@Override
	public boolean isStopped() {
		return stop;
	}
}
