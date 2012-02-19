/**
 * 
 */
package de.beimax.simplespleef.game.arenarestoring;

import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.gamehelpers.Cuboid;
import de.beimax.simplespleef.gamehelpers.SerializableBlockData;

/**
 * @author mkalus
 *
 */
public class SoftRestorer extends ArenaRestorer {
	/**
	 * cuboid to store/restore
	 */
	private Cuboid cuboid;
	//TODO: not really needed at this place, is it?

	/**
	 * keeps the data of the changed blocks
	 */
	private LinkedList<BlockChange> changedBlocks;
	
	/**
	 * iterator for the reconstruction of the arena
	 */
	Iterator<BlockChange> it;

	/**
	 * Constructor
	 * @param waitBeforeRestoring
	 */
	public SoftRestorer(int waitBeforeRestoring) {
		super(waitBeforeRestoring);
	}

	@Override
	public void setArena(Cuboid cuboid) {
		this.cuboid = cuboid;
		saveArena();
	}

	@Override
	public void saveArena() {		
		// initialize task data
		this.changedBlocks = new LinkedList<SoftRestorer.BlockChange>();
		this.it = null;
	}

	@Override
	public void restoreArena() {
		// just started
		if (it == null)
			it = changedBlocks.iterator();

		LinkedList<Chunk> chunksChanged = new LinkedList<Chunk>();

		// 200 blocks per tick max
		for (int i = 0; i < 200; i++) {
			if (it == null || !it.hasNext()) {
				if (it == null)
					SimpleSpleef.log.warning("[SimpleSpleef] Could not restore the whole arena - changed blocks iterator has been killed somehow.");
				it = null;
				break;
			} else { // restore blocks
				BlockChange changedBlock = it.next();

				Block block = changedBlock.location.getBlock();

				// load chunk if needed
				Chunk here = block.getChunk();
				if (!here.isLoaded()) here.load();

				block.setTypeIdAndData(changedBlock.blockData.getTypeId(), changedBlock.blockData.getData(), false);
				
				// add to list of changed chunks
				if (!chunksChanged.contains(here))
					chunksChanged.addFirst(here);
			}
		}

		// refresh chunks
		for (Chunk chunk : chunksChanged)
			chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
	}

	@Override
	public boolean tick() {
		if (interrupted == true) { // when interrupted, restore arena right awaw
			restoreArena(); // finish arena at once
			return true; // finish restorer
		}
		
		// wait for game to finish
		if (!game.isFinished()) return false;
		
		// before start restoring count restore ticker to 0
		if (this.waitBeforeRestoring > 0) {
			this.waitBeforeRestoring--;
			return false;
		}
		
		restoreArena(); // start restoring
		// test, if all blocks have been restored
		if (it == null) return true;
		// otherwise wait another tick
		return false;
	}

	@Override
	public boolean updateBlock(Block block, int oldType, byte oldData) {
		if (block == null || changedBlocks == null) return false; // no NPEs
		// just add original block type to list
		BlockChange change = new BlockChange();
		change.location = block.getLocation();
		change.blockData = new SerializableBlockData(oldType, oldData);
		// add at first position, so the restoration is done from the last block changed
		changedBlocks.addFirst(change);
		//System.out.println("Added!");
		return true;
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
}
