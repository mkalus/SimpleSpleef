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
public class FloorDissolveThread extends FloorBaseThread {
	/**
	 * areas of the floor that are non-air
	 */
	private HashSet<Location> nonAir = new HashSet<Location>();
	
	/**
	 * Constructor
	 * @param startAfter
	 * @param tickTime
	 */
	public FloorDissolveThread(int startAfter, int tickTime) {
		super(startAfter, tickTime);
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#initializeThread(de.beimax.simplespleef.game.Game, java.util.List)
	 */
	@Override
	public void initializeThread(Game game, List<Block> floor) {
		for (Block block : floor) {
			if (block == null) continue; // no NPEs
			if (block.getType() != Material.AIR) // add location to nonAir locations
				nonAir.add(block.getLocation());
		}
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#updateBlock(org.bukkit.block.Block)
	 */
	@Override
	public void updateBlock(Block block) {
		Location loc = block.getLocation();
		if (nonAir.contains(loc)) {
			if (block.getType() == Material.AIR) // dissolve to air
				nonAir.remove(loc); // remove because it is air anyway
		} else if (block.getType() != Material.AIR) // non air added - add to dissolvable parts of arena
			nonAir.add(loc);
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorBaseThread#tick()
	 */
	@Override
	public void tick() {
		// get a random entry
		Location location = getRandomEntry();
		if (location != null) {
			// dissolve to air
			Block block = location.getBlock();
			block.setType(Material.AIR);
			block.setData((byte) 0);
			nonAir.remove(location);
		}
	}
	
	/**
	 * picks a random entry from a set
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

}
