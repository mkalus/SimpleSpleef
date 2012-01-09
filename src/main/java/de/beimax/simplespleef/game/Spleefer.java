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
	 * team ids
	 */
	public static final int TEAM_NONE = 0;
	public static final int TEAM_BLUE = 1;
	public static final int TEAM_RED = 2;
	
	/**
	 * reference to player object
	 */
	private Player player;
	
	/**
	 * has the player lost?
	 */
	private boolean lost = false;
	
	/**
	 * is player ready to play?
	 */
	private boolean ready = false;

	/**
	 * Team of this player
	 */
	private int team = TEAM_NONE;
	
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

	/**
	 * @return the ready
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * @param ready the ready to set
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	/**
	 * @return the team
	 */
	public int getTeam() {
		return team;
	}

	/**
	 * @param team the team to set
	 */
	public void setTeam(int team) {
		this.team = team;
	}
}
