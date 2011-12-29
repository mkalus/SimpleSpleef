package de.beimax.simplespleef.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;

/**
 * Handle events for all Player related events
 * 
 * @author maxkalus
 */
public class SimpleSpleefPlayerListener extends PlayerListener {
	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)
	 */
	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (SimpleSpleef.getGameHandler().hasGames()) {
			// tell all games about it
			for (Game game : SimpleSpleef.getGameHandler().getGames()) {
				game.onPlayerJoin(event);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerKick(org.bukkit.event.player.PlayerKickEvent)
	 */
	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		if (SimpleSpleef.getGameHandler().hasGames()) {
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(event.getPlayer());
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
		if (SimpleSpleef.getGameHandler().hasGames()) {
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(event.getPlayer());
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
		if (SimpleSpleef.getGameHandler().hasGames()) {
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(event.getPlayer());
			if (game != null) game.onPlayerMove(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)
	 */
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (SimpleSpleef.getGameHandler().hasGames()) {
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(event.getPlayer());
			if (game != null) game.onPlayerInteract(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent)
	 */
	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (SimpleSpleef.getGameHandler().hasGames()) {
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(event.getPlayer());
			if (game != null) {
				// check, if arena allows the player's teleportation
				if (!game.playerMayTeleport(event.getPlayer())) {
					event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.teleport", "[ARENA]", game.getName()));
					event.setCancelled(true); //cancel event
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerGameModeChange(org.bukkit.event.player.PlayerGameModeChangeEvent)
	 */
	@Override
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		if (SimpleSpleef.getGameHandler().hasGames()) {
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(event.getPlayer());
			if (game != null) { // generally disallow changes of game modes for spleefers
				event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.gamemodeChange"));
				event.setCancelled(true); //cancel event
			}			
		}
	}
}
