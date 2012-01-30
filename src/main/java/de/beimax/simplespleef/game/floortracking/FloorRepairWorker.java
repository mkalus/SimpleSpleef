/**
 * 
 */
package de.beimax.simplespleef.game.floortracking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.util.SerializableBlockData;

/**
 * @author mkalus
 *
 */
public class FloorRepairWorker extends FloorBaseWorker {
	/**
	 * keeps the data of the original blocks
	 */
	private HashMap<Location, SerializableBlockData> originalBlocks = new HashMap<Location, SerializableBlockData>();
	
	/**
	 * areas of the floor that are air (changed areas)
	 */
	private HashSet<Location> air = new HashSet<Location>();
	
	/**
	 * Constructor
	 * @param startAfter
	 * @param tickTime
	 */
	public FloorRepairWorker(int startAfter, int tickTime, FloorTracker tracker) {
		super(startAfter, tickTime, tracker);
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#initializeThread(de.beimax.simplespleef.game.Game, java.util.List)
	 */
	@Override
	public void initialize(Game game, List<Block> floor) {
		if (floor == null) {
			stop = true;
			return; // ignore null floors
		}

		super.initialize(game, floor);
		//TODO: do we really need an original block memory or can we do something like in the SoftRestorer?
		for (Block block : floor) {
			if (block == null) continue; // no NPEs
			if (block.getType() != Material.AIR) // add location to original locations
				originalBlocks.put(block.getLocation(), new SerializableBlockData(block.getTypeId(), block.getData()));
		}
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#updateBlock(org.bukkit.block.Block)
	 */
	@Override
	public void updateBlock(Block block, int oldType, byte oldData) {
		if (block == null) return; // no NPEs
		if (stop) return;

		Location loc = block.getLocation();
		if (originalBlocks.containsKey(loc)) { // only locations that are contained in the original block database
			if (block.getType() == Material.AIR) // dissolved to air?
				air.add(loc); //add location to repair it later on
			else air.remove(loc); // if not dissolved, remove from repair list to prevent repairs
		}			
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorBaseThread#tick()
	 */
	@Override
	public void executeTick() {
		// get a random entry
		Location location = getRandomEntry();
		if (location != null) {
			SerializableBlockData blockData = originalBlocks.get(location); // get original block data
			if (blockData == null) return; // no NPEs, should not happen here, but just to make sure...
			// repair it
			Block block = location.getBlock();
			// get old data
			int oldType = block.getTypeId();
			byte oldData = block.getData();
			block.setTypeId(blockData.getTypeId());
			block.setData(blockData.getData());
			air.remove(location);				
			// notify others
			notifyTracker(block, oldType, oldData);
		}
	}

	/**
	 * picks a random entry from air set
	 */
	private Location getRandomEntry() {
		int size = air.size();
		if (size == 0) return null;
		int item = new Random().nextInt(size);
		int i = 0;
		for(Location location : air) {
			if (i == item) return location;
			i++;
		}
		
		return null; // if all locations have been dissolved
	}

	@Override
	public void stopTracking() {
		originalBlocks = null;
		air = null;
		stop = true;
	}
}
