/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.entity.Player;

/**
 * Models spleefers
 * @author mkalus
 */
public class Spleefer {
	/**
	 * reference to player object
	 */
	private Player player;
	
	/**
	 * has the player lost?
	 */
	private boolean lost = false;
	
	//TODO: teams
	
	/**
	 * Constructor
	 * @param player
	 */
	public Spleefer(Player player) {
		this.player = player;
	}

	/**
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @param player the player to set
	 */
	public void setPlayer(Player player) {
		this.player = player;
	}

	/**
	 * @return the lost
	 */
	public boolean hasLost() {
		return lost;
	}

	/**
	 * @param lost the lost to set
	 */
	public void setLost(boolean lost) {
		this.lost = lost;
	}
}
