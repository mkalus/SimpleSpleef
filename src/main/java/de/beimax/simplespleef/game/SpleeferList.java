/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.LinkedList;

import org.bukkit.entity.Player;

/**
 * @author mkalus
 *
 */
public class SpleeferList {
	/**
	 *  list of spleefers currently spleefing
	 *  - Since we have to iterate the list most of the time anyway, we simply use a linked list
	 */
	private LinkedList<Spleefer> spleefers;

	/**
	 * Constructor
	 */
	public SpleeferList() {
		spleefers = new LinkedList<Spleefer>();
	}

	/**
	 * Get a spleefer from the list
	 * @param player
	 * @return spleefer or null
	 */
	public Spleefer getSpleefer(Player player) {
		for (Spleefer spleefer : spleefers) {
			if (spleefer.getPlayer() == player) return spleefer;
		}
		return null;
	}

	/**
	 * Add a spleefer to the list
	 * @param player
	 * @return boolean, true if successful
	 */
	public boolean addSpleefer(Player player) {
		if (hasSpleefer(player)) return false;
		spleefers.add(new Spleefer(player));
		return true;
	}
	
	/**
	 * Remove spleefer from list
	 * @param player
	 * @return boolean, true if successful
	 */
	public boolean removeSpleefer(Player player) {
		for (Spleefer spleefer : spleefers) {
			if (spleefer.getPlayer() == player) {
				spleefers.remove(spleefer);
				return true;
			}
		}
		return false;
	}

	/**
	 * checks if a player is in the list
	 * @param player
	 * @return
	 */
	public boolean hasSpleefer(Player player) {
		for (Spleefer spleefer : spleefers) {
			if (spleefer.getPlayer() == player) return true;
		}
		return false;
	}
	
	/**
	 * checks if a player has lost
	 * @param player
	 * @return
	 */
	public boolean hasLost(Player player) {
		for (Spleefer spleefer : spleefers) {
			if (spleefer.getPlayer() == player) return true;
		}
		return false;
	}
	
	/**
	 * counts the spleefers still in the game
	 * @return
	 */
	public int inGame() {
		int inGame = 0;
		for (Spleefer spleefer : spleefers) {
			if (!spleefer.hasLost()) inGame++;
		}
		return inGame;
	}
	
	/**
	 * counts the size of the spleefers
	 * @return
	 */
	public int size() {
		return spleefers.size();
	}
}
