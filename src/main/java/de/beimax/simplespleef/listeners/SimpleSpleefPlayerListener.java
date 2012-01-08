package de.beimax.simplespleef.listeners;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.command.SimpleSpleefSignCommandExecutor;
import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.util.UpdateChecker;

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
		// update checker activated
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.updateNotificationOnLogin", true)) {
			Player player = event.getPlayer();
			// Check for updates whenever an operator or user with the right simplespleef.admin joins the game
			if (player.isOp() || SimpleSpleef.checkPermission(player, "simplespleef.admin")) {
				UpdateChecker checker = new UpdateChecker();
				try {
					// compare versions
					String oldVersion = SimpleSpleef.getPlugin().getDescription().getVersion();
					String newVersion = checker.checkForUpdate(oldVersion);
					if (newVersion != null) // do we have a version update? => notify player
						player.sendMessage(SimpleSpleef.getPlugin().ll("feedback.update", "[OLDVERSION]", oldVersion, "[NEWVERSION]", newVersion));
				} catch (Exception e) {
					player.sendMessage("SimpleSpleef could not get version update - see log for details.");
					SimpleSpleef.log.warning("[SimpleSpleef] Could not connect to remote server to check for update. Exception said: " + e.getMessage());
				}
			}
		}

		// tell games about somebody joining, too
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
		// clicked on a sign and signs enabled?
		if (event.getClickedBlock().getState() instanceof Sign && SimpleSpleef.getPlugin().getConfig().getBoolean("settings.enableSigns", true)) {
			// only right click allowed?
			boolean signsOnlyRightClick = SimpleSpleef.getPlugin().getConfig().getBoolean("settings.signsOnlyRightClick", false);
			if (!signsOnlyRightClick || (signsOnlyRightClick && event.getAction() == Action.RIGHT_CLICK_BLOCK))
				// let the sign command executor do the rest
				new SimpleSpleefSignCommandExecutor().parseSimpleSpleefSign(event.getPlayer(), (Sign)event.getClickedBlock().getState());
		}
		
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
