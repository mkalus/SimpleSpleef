/**
 * 
 */
package de.beimax.simplespleef.game.floortracking;

import java.util.List;

import org.bukkit.block.Block;

import de.beimax.simplespleef.game.Game;

/**
 * @author mkalus
 *
 */
public class FloorRepairThread extends FloorBaseThread {
	/**
	 * Constructor
	 * @param startAfter
	 * @param tickTime
	 */
	public FloorRepairThread(int startAfter, int tickTime) {
		super(startAfter, tickTime);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#initializeThread(de.beimax.simplespleef.game.Game, java.util.List)
	 */
	@Override
	public void initializeThread(Game game, List<Block> floor) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorThread#updateBlock(org.bukkit.block.Block)
	 */
	@Override
	public void updateBlock(Block block) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.floortracking.FloorBaseThread#tick()
	 */
	@Override
	public void tick() {
		// TODO Auto-generated method stub
		//System.out.println("FloorRepairThread tick");
	}

}
