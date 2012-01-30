/**
 * 
 */
package de.beimax.simplespleef.game.floortracking;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import de.beimax.simplespleef.game.Game;

/**
 * @author mkalus
 *
 */
public class FloorDissolveWorker extends FloorBaseWorker {
	/**
	 * areas of the floor that are non-air
	 */
	private HashSet<Location> nonAir = new HashSet<Location>();
	
	/**
	 * Constructor
	 * @param startAfter
	 * @param tickTime
	 */
	public FloorDissolveWorker(int startAfter, int tickTime, FloorTracker tracker) {
		super(startAfter, tickTime, tracker);
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorWorker#initialize(de.beimax.simplespleef.game.Game, java.util.List)
	 */
	@Override
	public void initialize(Game game, List<Block> floor) {
		if (floor == null) {
			stop = true;
			return; // ignore null floors
		}

		super.initialize(game, floor);
		for (Block block : floor) {
			if (block == null) continue; // no NPEs
			if (block.getType() != Material.AIR) // add location to nonAir locations
				nonAir.add(block.getLocation());
		}
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorWorker#updateBlock(org.bukkit.block.Block)
	 */
	@Override
	public void updateBlock(Block block, int oldType, byte oldData) {
		if (block == null) return; // no NPEs
		if (stop) return;

		Location loc = block.getLocation();
		if (nonAir.contains(loc)) {
			if (block.getType() == Material.AIR) // dissolve to air
				nonAir.remove(loc); // remove because it is air anyway
		} else if (block.getType() != Material.AIR) // non air added - add to dissolveable parts of arena
			nonAir.add(loc);			
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorBaseWorker#executeTick()
	 */
	@Override
	public void executeTick() {
		// get a random entry
		Location location = getRandomEntry();
		if (location != null) {
			Block block = location.getBlock();
			// get old data
			int oldType = block.getTypeId();
			byte oldData = block.getData();
			// dissolve to air
			block.setType(Material.AIR);
			block.setData((byte) 0);
			nonAir.remove(location);				
			// notify others
			notifyTracker(block, oldType, oldData);
		}
	}
	
	/**
	 * picks a random entry from non-air set
	 */
	private Location getRandomEntry() {
		int size = nonAir.size();
		if (size == 0) return null;
		int item = new Random().nextInt(size);
		int i = 0;
		for(Location location : nonAir) {
			if (i == item) return location;
			i++;
		}
		
		return null; // if all locations have been dissolved
	}

	@Override
	public void stopTracking() {
		nonAir = null;
		stop = true;
	}
}
