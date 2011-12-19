/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author mkalus
 *
 */
public abstract class Game {
	/**
	 * game status constants
	 */
	protected static final int STATUS_NEW = 1;
	protected static final int STATUS_COUNTDOWN = 2;
	protected static final int STATUS_STARTED = 3;
	protected static final int STATUS_FINISHED = 4;

	/**
	 * reference on game handler instance
	 */
	protected final GameHandler gameHandler;

	/**
	 * name of the game/arena
	 */
	private final String name;

	/**
	 * game status
	 */
	private int status;

	/**
	 * Constructor
	 * @param name
	 */
	public Game(GameHandler gameHandler, String name) {
		this.gameHandler = gameHandler;
		this.name = name;
		this.status = STATUS_NEW;
	}

	/**
	 * @return the id (normaly name to lower case)
	 */
	public String getId() {
		return this.name.toLowerCase();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the name
	 */
	public int getStatus() {
		return this.status;
	}
	
	/**
	 * get type of arena
	 * @return
	 */
	public abstract String getType();

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
	 * @return true for joinable
	 */
	public boolean isJoinable() {
		return this.status == STATUS_NEW;
	}

	/**
	 * Check if player is in arena X
	 * @return boolean
	 */
	public abstract boolean hasPlayer(Player player);
}
