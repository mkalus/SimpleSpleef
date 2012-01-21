/**
 * 
 */
package de.beimax.simplespleef.game.arenarestoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.game.floortracking.FloorThread;
import de.beimax.simplespleef.util.Cuboid;
import de.beimax.simplespleef.util.SerializableBlockData;

/**
 * @author mkalus
 *
 */
public class SoftRestorer implements ArenaRestorer, FloorThread {
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
	 * keeps the data of the changed blocks
	 */
	private LinkedList<BlockChange> changedBlocks;
	
	/**
	 * keeps the data of the original blocks
	 */
	private HashMap<Location, SerializableBlockData> originalBlocks = new HashMap<Location, SerializableBlockData>();

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if (game == null || cuboid == null) return; //ignore invalid stuff

		// wait until game has stopped
		while (!startRestoring)
			try {
				Thread.sleep(500); // sleep for half a second before doing anything else
			} catch (InterruptedException e) {}
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#initializeThread(de.beimax.simplespleef.game.Game, java.util.List)
	 */
	@Override
	public void initializeThread(Game game, List<Block> floor) {
		for (Block block : floor) {
			if (block == null) continue; // no NPEs
			originalBlocks.put(block.getLocation(), new SerializableBlockData(block.getTypeId(), block.getData()));
		}
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
	public void updateBlock(Block block) {
		if (block == null) return; // no NPEs
		Location loc = block.getLocation();
		// in list of original blocks?
		SerializableBlockData originalBlock = originalBlocks.get(loc);
		if (originalBlock != null) { // add changed block data
			BlockChange change = new BlockChange();
			change.location = loc;
			change.blockData = new SerializableBlockData(originalBlock.getTypeId(), originalBlock.getData());
			changedBlocks.add(change);
		} else  { // outside original blocks? Add to list as air (placed blocks)
			if (block.getType() != Material.AIR) {
				BlockChange change = new BlockChange();
				change.location = loc;
				change.blockData = new SerializableBlockData(Material.AIR.getId(), (byte) 0);
				changedBlocks.add(change);
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.arenarestoring.ArenaRestorer#saveArena(de.beimax.simplespleef.game.Game, de.beimax.simplespleef.util.Cuboid)
	 */
	@Override
	public void saveArena(Game game, Cuboid cuboid) {
		if (game == null || cuboid == null) return; //ignore invalid stuff
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
	
	/**
	 * restorer thread
	 * @author mkalus
	 *
	 */
	private class RestoreThread extends Thread {
		@Override
		public void run() {
			int count = 0;
			
			// cycle through changes and restore
			for (BlockChange changedBlock : changedBlocks) {
				Block block = changedBlock.location.getBlock();
				block.setTypeId(changedBlock.blockData.getTypeId());
				block.setData(changedBlock.blockData.getData());
				
				// every 10 cycles, wait a little
				if ((count++ % 10) == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
			}

			// call game handler to finish the game off
			SimpleSpleef.getGameHandler().gameOver(game);
		}
	}
}
