/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
	protected int status;

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
	 * @param sender
	 * @return boolean successful?
	 */
	public abstract boolean countdown(CommandSender sender);

	/**
	 * Start command issued
	 * @return boolean successful?
	 */
	public abstract boolean start();

	/**
	 * Stop command issued
	 * @param player
	 * @return boolean successful?
	 */
	public abstract boolean stop(Player player);

	/**
	 * Delete/reset command issued
	 * @param sender
	 * @return boolean successful?
	 */
	public abstract boolean delete(CommandSender sender);

	/**
	 * Spectate game
	 * @return boolean successful?
	 */
	public abstract boolean spectate();

	/**
	 * Check if player is in arena X
	 * @return boolean
	 */
	public abstract boolean hasPlayer(Player player);
	
	/**
	 * Called when a spleefer moves in this game
	 * @param event
	 */
	public abstract void onPlayerMove(PlayerMoveEvent event);
	
	/**
	 * Called when a spleefer tries to teleport in this game
	 * may be called by the game itself in a way, so it should be able to
	 * teleport the player in certain cases
	 * @param player
	 */
	public abstract boolean playerMayTeleport(Player player);
	
	/**
	 * Called when a spleefer interacts with something in this game
	 * @param event
	 */
	public abstract void onPlayerInteract(PlayerInteractEvent event);
	
	/**
	 * Called when a spleefer quits
	 * @param event
	 */
	public abstract void onPlayerQuit(PlayerQuitEvent event);

	/**
	 * Called when a spleefer gets kicked
	 * @param event
	 */
	public abstract void onPlayerKick(PlayerKickEvent event);
	
	/**
	 * Called when a player joins - game can check if the player has quit recently, for example
	 * @param event
	 */
	public abstract void onPlayerJoin(PlayerJoinEvent event);
	
	/**
	 * Called when a spleefer dies during this game
	 * @param event
	 */
	public abstract void onPlayerDeath(Player player);
	
	/**
	 * Called when a spleefer breaks a block
	 * @param event
	 */
	public abstract void onBlockBreak(BlockBreakEvent event);

	/**
	 * Send a message to broadcast, or to players and spectators
	 * @param message
	 * @param broadcast
	 */
	public abstract void sendMessage(String message, boolean broadcast);
	
	/**
	 * Send a message to broadcast, or to players and spectators
	 * @param message
	 * @param exception exception - this player does not receive message
	 */
	public abstract void sendMessage(String message, Player exception);

	/**
	 * is game joinable?
	 * @return true for joinable
	 */
	public boolean isJoinable() {
		return this.status == STATUS_NEW;
	}

	/**
	 * is game in progress?
	 * @return true/false
	 */
	public boolean isInProgress() {
		return this.status == STATUS_COUNTDOWN || this.status == STATUS_STARTED;
	}

	/**
	 * cleaning routine called at end of game
	 */
	public abstract void clean();
}
