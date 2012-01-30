/**
 * 
 */
package de.beimax.simplespleef.game.arenarestoring;

import java.util.Iterator;
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
		if (changedBlocks == null) return;
		RestoreWorker worker = new RestoreWorker();

		// start restore thread called every tick
		worker.schedulerId = SimpleSpleef.getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(SimpleSpleef.getPlugin(), worker, 0L, 1L);
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
	 * restorer task - called every server tick
	 * @author mkalus
	 *
	 */
	private class RestoreWorker implements Runnable {
		private int schedulerId = -1;
		
		Iterator<BlockChange> it = null;
		
		@Override
		public void run() {
			// just started
			if (it == null)
				it = changedBlocks.iterator();

			// 40 blocks per tick max
			for (int i = 0; i < 40; i++) {
				if (!it.hasNext()) {
					isStopped = true; // yes, we are finished
					break;
				} else { // restore blocks
					BlockChange changedBlock = it.next();
					Block block = changedBlock.location.getBlock();
					block.setTypeId(changedBlock.blockData.getTypeId());
					block.setData(changedBlock.blockData.getData());	
				}
			}
			
			// have we finished?
			if (isStopped) {
				// stop scheduler task
				SimpleSpleef.getPlugin().getServer().getScheduler().cancelTask(schedulerId);
				// call game handler to finish the game off
				SimpleSpleef.getGameHandler().gameOver(game);
			}
		}
	}
}
