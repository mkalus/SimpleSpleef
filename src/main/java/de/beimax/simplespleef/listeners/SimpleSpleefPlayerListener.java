package de.beimax.simplespleef.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.game.GameHandler;

/**
 * Handle events for all Player related events
 * 
 * @author maxkalus
 */
public class SimpleSpleefPlayerListener extends PlayerListener {
	/**
	 * reference to game handler
	 */
	private final GameHandler gameHandler;
	
	/**
	 * Constructor
	 * @param gameHandler reference to game handler
	 */
	public SimpleSpleefPlayerListener(GameHandler gameHandler) {
		this.gameHandler = gameHandler;
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)
	 */
	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (gameHandler.hasGames()) {
			// tell all games about it
			for (Game game : gameHandler.getGames()) {
				game.onPlayerJoin(event);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerKick(org.bukkit.event.player.PlayerKickEvent)
	 */
	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		if (gameHandler.hasGames()) {
			// player part of a game?
			Game game = gameHandler.checkPlayerInGame(event.getPlayer());
			if (game != null) game.onPlayerKick(event);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.bukkit.event.player.PlayerListener#onPlayerQuit(org.bukkit.event.
	 * player.PlayerEvent)
	 */
	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (gameHandler.hasGames()) {
			// player part of a game?
			Game game = gameHandler.checkPlayerInGame(event.getPlayer());
			if (game != null) game.onPlayerQuit(event);
		}
	}	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.bukkit.event.player.PlayerListener#onPlayerMove(org.bukkit.event.
	 * player.PlayerMoveEvent)
	 */
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (gameHandler.hasGames()) {
			// player part of a game?
			Game game = gameHandler.checkPlayerInGame(event.getPlayer());
			if (game != null) game.onPlayerMove(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)
	 */
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (gameHandler.hasGames()) {
			// player part of a game?
			Game game = gameHandler.checkPlayerInGame(event.getPlayer());
			if (game != null) game.onPlayerInteract(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent)
	 */
	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (gameHandler.hasGames()) {
			// player part of a game?
			Game game = gameHandler.checkPlayerInGame(event.getPlayer());
			if (game != null) {
				// check, if arena allows teleportation or not (preventTeleportingDuringGames)
				if (gameHandler.getPlugin().getConfig().getBoolean("arenas." + game.getId() + ".preventTeleportingDuringGames", true)) {
					event.getPlayer().sendMessage(ChatColor.DARK_RED + gameHandler.getPlugin().ll("errors.teleport", "[ARENA]", game.getName()));
				}
			}
		}
	}
}
