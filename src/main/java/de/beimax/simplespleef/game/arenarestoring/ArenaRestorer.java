/**
 * 
 */
package de.beimax.simplespleef.game.arenarestoring;

import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.util.Cuboid;

/**
 * @author mkalus
 * Interface for classes that restore the arena eventually - these classes also have to call the
 * handler's game over method to delete the game when finished.
 */
public interface ArenaRestorer {
	/**
	 * Store the arena for one game
	 * @param game
	 * @param cuboid
	 */
	public void saveArena(Game game, Cuboid cuboid);

	/**
	 * Restore the arena for one game - please call handler's game over method to delete the game when finished restoring.
	 * @param game
	 */
	public void restoreArena();
}
