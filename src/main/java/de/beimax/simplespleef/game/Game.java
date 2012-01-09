/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 *
 */
public abstract class Game {
	/**
	 * game status constants
	 */
	protected static final int STATUS_NEW = 1;
	protected static final int STATUS_READY = 2;
	protected static final int STATUS_COUNTDOWN = 3;
	protected static final int STATUS_STARTED = 4;
	protected static final int STATUS_FINISHED = 5;

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
	public Game(String name) {
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
	 * Mark player as ready
	 * @return boolean successful?
	 */
	public abstract boolean ready(Player player);

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
	 * Watch command issued
	 * @param player
	 * @return
	 */
	public abstract boolean watch(Player player);
	
	/**
	 * Back command issued
	 * @param player
	 * @return
	 */
	public abstract boolean back(Player player);

	/**
	 * Check if player is in arena
	 * @return boolean
	 */
	public abstract boolean hasPlayer(Player player);

	/**
	 * Check if player is spectator in arena
	 * @return boolean
	 */
	public abstract boolean hasSpectator(Player player);
	
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
	 * Called when a spleefer places a block
	 * @param event
	 */
	public abstract void onBlockPlace(BlockPlaceEvent event);

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
	 * Get number of players
	 * @return something like (1/2)
	 */
	public abstract String getNumberOfPlayers();
	
	/**
	 * return a comma separated list of spleefers (or null)
	 * @return
	 */
	public abstract String getListOfSpleefers();
	
	/**
	 * return a comma separated list of spectators (or null)
	 * @return
	 */
	public abstract String getListOfSpectators();

	/**
	 * is game joinable?
	 * @return true for joinable
	 */
	public boolean isJoinable() {
		return this.status <= STATUS_READY;
	}

	/**
	 * is game ready?
	 * @return true for game ready
	 */
	public boolean isReady() {
		// game must be readied?
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("arenas." + getId() + ".useReady", false))
			return this.status == STATUS_READY;
		return this.status <= STATUS_READY; // without using ready, game is ready automatically
	}

	/**
	 * is game in progress?
	 * @return true/false
	 */
	public boolean isInProgress() {
		return this.status == STATUS_COUNTDOWN || this.status == STATUS_STARTED;
	}

	/**
	 * has game started (not countdown)?
	 * @return true/false
	 */
	public boolean isInGame() {
		return this.status == STATUS_STARTED;
	}

	/**
	 * cleaning routine called at end of game
	 */
	public abstract void clean();
}
