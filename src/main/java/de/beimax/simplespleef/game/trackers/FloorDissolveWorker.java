/**
 * 
 */
package de.beimax.simplespleef.game.trackers;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

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
	public FloorDissolveWorker(int startAfter, int tickTime, List<Block> floor) {
		super(startAfter, tickTime);
		for (Block block : floor) {
			if (block == null) continue; // no NPEs
			if (block.getType() != Material.AIR) // add location to nonAir locations
				nonAir.add(block.getLocation());
		}
	}

	@Override
	public boolean updateBlock(Block block, int oldType, byte oldData) {
		if (block == null) return false; // no NPEs

		Location loc = block.getLocation();
		if (nonAir.contains(loc)) {
			if (block.getType() == Material.AIR) { // dissolve to air
				nonAir.remove(loc); // remove because it is air anyway
				return true;
			}
		} else if (block.getType() != Material.AIR) { // non air added - add to dissolveable parts of arena
			nonAir.add(loc);
			return true;
		}

		return false;
	}

	@Override
	public void executeTick() {
		// get a random entry
		Location location = getRandomEntry();
		//System.out.println("Tick FloorDissolveWorker " + location);
		if (location != null) {
			Block block = location.getBlock();
			// get old data
			int oldType = block.getTypeId();
			byte oldData = block.getData();
			// dissolve to air
			block.setTypeId(Material.AIR.getId(), false);
			block.setData((byte) 0, false);
			nonAir.remove(location);				
			// notify others - and myself
			game.trackersUpdateBlock(block, oldType, oldData);
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
}
