/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.LinkedList;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author mkalus
 * Game abstract class
 */
public abstract class Game {
	/**
	 * name of the game/arena
	 */
	private final String name;
	
	/**
	 *  list of spleefers currently spleefing
	 *  - Since we have to iterate the list most of the time anyway, we simply use a linked list
	 */
	private LinkedList<Spleefer> spleefers;

	/**
	 * Constructor
	 * @param name
	 */
	public Game(String name) {
		this.name = name;
		spleefers = new LinkedList<Spleefer>();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
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
	 * Add a spleefer to the game
	 * @param player
	 * @param team
	 * @return boolean, true if successful
	 */
	public boolean addSpleefer(Player player, Integer team) {
		//TODO
		return false;
	}
	
	/**
	 * Remove spleefer from game
	 * @param player
	 * @param silent
	 * @return boolean, true if successful
	 */
	public boolean removeSpleefer(Player player, boolean silent) {
		//TODO
		return false;
	}
	
	/**
	 * Spleefer leaves game
	 * @param player
	 * @return boolean, true if successful
	 */
	public boolean leaveSpleefer(Player player) {
		//TODO
		return false;
	}

	// read the configuration settings of this arena
	public abstract void defineSettings(ConfigurationSection conf);
	
	/**
	 * Player issues join command
	 * @param player
	 * @return boolean successful?
	 */
	public abstract boolean join(Player player);
	
	/**
	 * Player issues leave command
	 * @param player
	 * @return boolean successful?
	 */
	public abstract boolean leave(Player player);
	
	
	/**
	 * Countdown started
	 * @return boolean successful?
	 */
	public abstract boolean countdown();

	/**
	 * Start command issued
	 * @return boolean successful?
	 */
	public abstract boolean start();

	/**
	 * Stop command issued
	 * @return boolean successful?
	 */
	public abstract boolean stop();

	/**
	 * Delete/reset command issued
	 * @return boolean successful?
	 */
	public abstract boolean delete();

	/**
	 * Spectate game
	 * @return boolean successful?
	 */
	public abstract boolean spectate();

	/**
	 * is game joinable?
	 * @return
	 */
	public abstract boolean isJoinable();
}
