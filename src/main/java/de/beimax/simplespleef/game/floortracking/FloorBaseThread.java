/**
 * 
 */
package de.beimax.simplespleef.game.floortracking;

/**
 * @author mkalus
 *
 */
public abstract class FloorBaseThread implements FloorThread {
	/**
	 * seconds after which thread actually starts doing something
	 */
	private int startAfter;
	
	/**
	 * seconds to wait each tick of the thread
	 */
	private int tickTime;
	
	/**
	 * system time to start thread at
	 */
	private long startAt = 0;
	
	/**
	 * system time for next tick
	 */
	private long nextTick = 0;
	
	/**
	 * flag to stop thread
	 */
	private boolean stop = false;
	
	/**
	 * Constructor
	 * @param startAfter
	 * @param tickTime
	 */
	public FloorBaseThread(int startAfter, int tickTime) {
		this.startAfter = startAfter;
		this.tickTime = tickTime;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// first, wait for thread to start
		startAt = System.currentTimeMillis() + ((long) startAfter * 1000);
		while (!stop && System.currentTimeMillis() < startAt)
			try {
				Thread.sleep(500); // sleep for half a second before doing anything else
			} catch (InterruptedException e) {}
		
		// now do the actual tick work
		nextTick = System.currentTimeMillis();
		while (!stop) {
			while (!stop && System.currentTimeMillis() < nextTick)
				try {
					Thread.sleep(200); // sleep for a fifth of a second before doing anything else
				} catch (InterruptedException e) {}
			// execute tick
			tick();
			// update tick
			nextTick = nextTick + ((long) tickTime * 1000);
		}
	}
	
	/**
	 * do a tick within the thread
	 */
	public abstract void tick();
	
	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#stopTracking()
	 */
	@Override
	public void stopTracking() {
		stop =  true;
	}
}
