/**
 * 
 */
package de.beimax.simplespleef.modelold;

import java.util.HashSet;

import org.bukkit.entity.Player;

/**
 * Models games.
 * @author mkalus
 */
public class Game {
	/**
	 * arena the game is playing in
	 */
	private Arena arena;
	
	/**
	 * game has started?
	 */
	private boolean started;

	/**
	 *  list of spleefers currently spleefing
	 */
	private HashSet<Spleefer> spleefers;
	
	//TODO countdown

	/**
	 * Add a spleefer to the game
	 * @param spleefer
	 * @param team
	 * @return boolean, true if successful
	 */
	public boolean addSpleefer(Spleefer spleefer, Integer team) {
		//TODO
		return false;
	}
	
	/**
	 * Remove spleefer from game
	 * @param spleefer
	 * @param silent
	 * @return boolean, true if successful
	 */
	public boolean removeSpleefer(Spleefer spleefer, boolean silent) {
		//TODO
		return false;
	}
	
	/**
	 * Spleefer leaves game
	 * @param spleefer
	 * @return boolean, true if successful
	 */
	public boolean leaveSpleefer(Spleefer spleefer) {
		//TODO
		return false;
	}
	
	/**
	 * Attempt to start game
	 * @param player
	 * @param startsingle
	 * @return boolean, true if successful
	 */
	public boolean startGame(Player player, boolean startsingle) {
		//TODO
		return false;
	}
	
	/**
	 * Attempt to stop game
	 * @param player
	 * @return boolean, true if successful
	 */
	public boolean stopGame(Player player) {
		//TODO
		return false;
	}
	
	/**
	 * Attempt to delete game
	 * @param player
	 * @return boolean, true if successful
	 */
	public boolean deleteGame(Player player) {
		//TODO
		return false;
	}
	
	/**
	 * internal method to stop a game
	 * @return boolean, true if successful
	 */
	protected boolean haltGame() {
		//TODO
		return false;		
	}
	
	//TODO countdown
}
