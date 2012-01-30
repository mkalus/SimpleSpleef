/**
 * 
 */
package de.beimax.simplespleef.game.arenarestoring;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.game.floortracking.FloorWorker;
import de.beimax.simplespleef.util.Cuboid;
import de.beimax.simplespleef.util.SerializableBlockData;

/**
 * @author mkalus
 *
 */
public class SoftRestorer implements ArenaRestorer, FloorWorker {
	/**
	 * flag to stop thread - actually starts restoration
	 */
	private boolean startRestoring = false;

	/**
	 * game to restore
	 */
	private Game game;
	
	/**
	 * cuboid to store/restore
	 */
	private Cuboid cuboid;
	
	/**
	 * thread has finished restoring?
	 */
	private boolean isStopped = false;

	/**
	 * keeps the data of the changed blocks
	 */
	private LinkedList<BlockChange> changedBlocks;

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#initializeThread(de.beimax.simplespleef.game.Game, java.util.List)
	 */
	@Override
	public void initialize(Game game, List<Block> floor) {
		//Do nothing
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#stopTracking()
	 */
	@Override
	public void stopTracking() {
		startRestoring = true;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#updateBlock(org.bukkit.block.Block)
	 */
	@Override
	public void updateBlock(Block block, int oldType, byte oldData) {
		if (block == null || changedBlocks == null) return; // no NPEs
		// just add original block type to list
		BlockChange change = new BlockChange();
		change.location = block.getLocation();
		change.blockData = new SerializableBlockData(oldType, oldData);
		// add at first position, so the restoration is done from the last block changed
		changedBlocks.addFirst(change);
		//System.out.println("Added!");
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.arenarestoring.ArenaRestorer#saveArena(de.beimax.simplespleef.game.Game, de.beimax.simplespleef.util.Cuboid)
	 */
	@Override
	public void saveArena(Game game, Cuboid cuboid) {
		if (game == null || cuboid == null) {
			startRestoring = true; // run restore right away
			return; //ignore invalid stuff
		}
		this.game = game;
		this.cuboid = cuboid;

		// initialize thread data
		this.changedBlocks = new LinkedList<SoftRestorer.BlockChange>();
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.arenarestoring.ArenaRestorer#restoreArena()
	 */
	@Override
	public void restoreArena() {
		// start restoration in new thread
		(new RestoreThread()).start();
	}
	
	/**
	 * keeps original positions for blocks
	 * @author mkalus
	 *
	 */
	private class BlockChange {
		private Location location;
		private SerializableBlockData blockData;
	}

	@Override
	public void tick() {
		if (game == null || cuboid == null) return; //ignore invalid stuff

		// wait for the restoration process to start
		if (!startRestoring) return;
		
		restoreArena();
	}

	@Override
	public boolean isStopped() {
		return isStopped;
	}

	
	/**
	 * restorer thread
	 * @author mkalus
	 *
	 */
	private class RestoreThread extends Thread {
		@Override
		public void run() {
			if (changedBlocks == null) return; // changes are ignored...

			int count = 0;
			// sleep a little to let other threads finish
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {}

			for (BlockChange changedBlock : changedBlocks) {
				//synchronized (changedBlock) { // broke some threads
				Block block = changedBlock.location.getBlock();
				block.setTypeId(changedBlock.blockData.getTypeId());
				block.setData(changedBlock.blockData.getData());					
				//}
				
				// every 10 cycles, wait a little
				if ((count++ % 10) == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
			}				

			// say that the thread has finished
			isStopped = true;
			//System.out.println("Finished");
			// call game handler to finish the game off
			SimpleSpleef.getGameHandler().gameOver(game);
		}
	}
}
