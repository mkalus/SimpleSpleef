/**
 * 
 */
package de.beimax.simplespleef.game;

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
	 * Constructor
	 * @param name
	 */
	public Game(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
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
}
