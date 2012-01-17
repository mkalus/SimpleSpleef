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
public class FloorRepairThread extends FloorBaseThread {
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
	public FloorRepairThread(int startAfter, int tickTime, FloorTracker tracker) {
		super(startAfter, tickTime, tracker);
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#initializeThread(de.beimax.simplespleef.game.Game, java.util.List)
	 */
	@Override
	public synchronized void initializeThread(Game game, List<Block> floor) {
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
	public synchronized void updateBlock(Block block) {
		if (block == null) return; // no NPEs
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
	public synchronized void tick() {
		// get a random entry
		Location location = getRandomEntry();
		if (location != null) {
			SerializableBlockData blockData = originalBlocks.get(location); // get original block data
			if (blockData == null) return; // no NPEs, should not happen here, but just to make sure...
			// repair it
			Block block = location.getBlock();
			block.setTypeId(blockData.getTypeId());
			block.setData(blockData.getData());
			air.remove(location);
			// notify others
			notifyTracker(block);
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
}
