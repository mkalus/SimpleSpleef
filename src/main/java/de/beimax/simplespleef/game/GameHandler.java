/**
 * This file is part of the SimpleSpleef bukkit plugin.
 * Copyright (C) 2011 Maximilian Kalus
 * See http://dev.bukkit.org/server-mods/simple-spleef/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package de.beimax.simplespleef.game;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.gamehelpers.Cuboid;
import de.beimax.simplespleef.gamehelpers.CuboidImpl;
import de.beimax.simplespleef.gamehelpers.CuboidWorldGuard;

/**
 * @author mkalus
 *
 */
public class GameHandler implements Listener, Runnable {
	/**
	 * games array
	 */
	private List<Game> games;

	/**
	 * Initialize game handler - mainly read arena cubes and update game data
	 */
	public void updateGameHandlerData() {
		// recreate game list
		games = new LinkedList<Game>();
		// cycle through config to get arenas
		Set<String> arenas = SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas").getKeys(false);
		for (String arena : arenas) { // have the game factory create game instances
			String type = SimpleSpleef.getPlugin().getConfig().getString("arenas." + arena + ".type", "standard");
			Game game = GameFactory.createGame(type, arena);
			// define configuration section for this game
			game.defineSettings(SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas." + game.getId()));
			// add game to list
			games.add(game);
		}
	}

	/**
	 * Heart of the game mechanic - the ticker
	 */
	@Override
	public void run() {
		for (Game game : games) // cycle through games
			game.tick(); // call tick
	}

	/**
	 * get list of games
	 * @return List of games
	 */
	public List<Game> getGames() {
		// clone list
		LinkedList<Game> games = new LinkedList<Game>();
		for (Game game : this.games) {
			games.add(game);
		}
		return games;
	}
	
	/**
	 * return game by name
	 * @param name of game
	 * @return
	 */
	public Game getGameByName(String name) {
		for (Game game : games) {
			if (game.getId().equalsIgnoreCase(name)) return game;
		}
		return null;
	}
	
	/**
	 * check, if a certain game name exists
	 * @param name of game
	 * @return
	 */
	public boolean gameExists(String name) {
		for (Game game : games) {
			if (game.getId().equalsIgnoreCase(name)) return true;
		}
		return false;
	}

	/**
	 * get the default arena name
	 * @return
	 */
	public String getDefaultArena() {
		return SimpleSpleef.getPlugin().getConfig().getString("settings.defaultArena", "default");
	}

	/**
	 * checks whether the player is part of a game or not
	 * @param player
	 * @return Game the player is part of or null, if not
	 */
	public Game checkPlayerInGame(Player player) {
		if (player == null) return null; // sanity check
		for (Game checkGame :  games) {
			if (checkGame.hasPlayer(player)) return checkGame;
		}
		return null;
	}

	/**
	 * checks whether the player is watching a game or not
	 * @param player
	 * @return Game the player is part of or null, if not
	 */
	public Game checkSpectatorInGame(Player player) {
		if (player == null) return null; // sanity check
		for (Game checkGame :  games) {
			if (checkGame.hasSpectator(player)) return checkGame;
		}
		return null;
	}


	/**
	 * checks whether the player is either a spleefer or spectator of some game
	 * @param sender
	 * @param args
	 * @param arena
	 * @return
	 */
	public Game checkSpleeferOrSpectatorInGame(Player player) {
		Game game = checkPlayerInGame(player);
		if (game != null) return game;
		game = checkSpectatorInGame(player);
		return game;
	}

	/**
	 * try to announce a new game in an arena
	 * @param sender
	 * @param game
	 * @return announced game or null
	 */
	public void announce(CommandSender sender, Game game) {
		game.announce(sender);
	}

	/**
	 * try to join a game in an arena
	 * @param sender
	 * @param game
	 */
	public void join(CommandSender sender, Game game) {
		Player player = (Player) sender; // cast to player
		// check, if player has already joined another game
		Game checkDouble = checkPlayerInGame(player);
		if (checkDouble != null && checkDouble != game) { // player already joined another arena?
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.joinDouble", "[ARENA]", checkDouble.getName()));
			return;	
		}

		// join game - if something does not work as expected, return and ignore the rest
		if (!game.join(player)) return;

		// player already spectator in another arena?
		checkDouble = checkSpectatorInGame(player);
		if (checkDouble != null) // remove spectator from game on joining
			checkDouble.removeSpectator(player);
	}
	
	/**
	 * Attempt to join a team (spleefers only)
	 * @param sender
	 * @param team team name
	 */
	public void team(CommandSender sender, String team) {
		// find player's game
		Game game = checkPlayerInGame((Player) sender);
		if (game == null)
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.teamNoGame"));
		else game.team((Player) sender, team);
	}
	
	/**
	 * Attempt to set ready for a gamer (spleefers only)
	 * @param sender
	 */
	public void ready(CommandSender sender) {
		// find player's game
		Game game = checkPlayerInGame((Player) sender);
		if (game == null)
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.readyNoGame"));
		else game.ready((Player) sender, false);
	}
	
	/**
	 * Attempt to start a game (spleefers only)
	 * @param sender
	 */
	public void start(CommandSender sender) {
		// find player's game
		Game game = checkPlayerInGame((Player) sender);
		if (game == null) // sender not part of any game
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.start"));
		else game.countdown(sender);
	}
	
	/**
	 * Attempt to start countdown of a game
	 * @param sender
	 * @param game (may be null)
	 */
	public void countdown(CommandSender sender, Game game) {
		// start countdown for game
		game.countdown(sender);
	}
	

	/**
	 * Attempt to leave a game (spleefers only)
	 * @param sender
	 */
	public void leave(CommandSender sender) {
		// find player's game
		Game game = checkPlayerInGame((Player) sender);
		if (game == null)  // sender not part of any game
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.leave"));
		else game.leave((Player) sender);
	}
	
	/**
	 * Attempt to stop a game
	 * @param sender
	 */
	public void stop(CommandSender sender) {
		// find player's game
		Game game = checkPlayerInGame((Player) sender);
		if (game == null)  // sender not part of any game
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.stopNoPlaying"));
		else game.stop((Player) sender);
	}
	
	/**
	 * Attempt to delete a game
	 * @param sender
	 * @param game (may be null)
	 */
	public void delete(CommandSender sender, Game game) {
		game.delete(sender); // delete game
	}
	
	/**
	 * Attempt to watch a game
	 * @param sender
	 * @param game (may not be null)
	 */
	public void watch(CommandSender sender, Game game) {
		// find player's game - check doubles
		Game otherGame = checkPlayerInGame((Player) sender);
		// player part of an active game?
		if (otherGame != null) { // part of a game - may not spectate
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.watchDouble", "[ARENA]", game.getName()));
			return;
		}
		game.watch((Player) sender); // attempt to watch game
	}

	/**
	 * lists all the arenas in the game
	 * @param sender
	 */
	public void arenas(CommandSender sender) {
		//TODO: info if there are no games at all

		// cycle through possible games
		for (Game game: getGames()) {
			// color and information on game
			ChatColor color;
			// name of arena
			String fullName = game.getName(); // name of arena
			String information;
			if (game.isEnabled() == false) {
				color = ChatColor.DARK_GRAY; // arena has been disabled in the the config
				information = SimpleSpleef.ll("feedback.arenaDisabled");
			} else if (!game.isActive()) {
				color = ChatColor.GRAY; // not an active game
				information = null; // no information
			} else { // is it an active game?
				// game active or still joinable?
				if (game.isJoinable() || game.isReady()) {
					color = ChatColor.GREEN; // joinable
					information = SimpleSpleef.ll("feedback.arenaJoinable");
				}
				else {
					color = ChatColor.LIGHT_PURPLE; // not joinable - because running
					information = SimpleSpleef.ll("feedback.arenaInProgress");
				}
				// ok, gather some more information on the game to display
				information = information + " " + game.getNumberOfPlayers();
			}
			// create feedback
			StringBuilder builder = new StringBuilder();
			builder.append(color).append(game.getId());
			if (information != null) builder.append(ChatColor.GRAY).append(" - ").append(information);
			if (fullName != null && !fullName.equalsIgnoreCase(game.getId()))
				builder.append(ChatColor.GRAY).append(" (").append(fullName).append(')');
			sender.sendMessage(builder.toString());
		}
	}

	/**
	 * Show information
	 * @param sender
	 * @param game
	 */
	public void info(CommandSender sender, Game game) {
		//TODO: info if there are no games at all
		
		// ok, define information on arena and print it
		sender.sendMessage(SimpleSpleef.ll("feedback.infoHeader", "[ARENA]", ChatColor.DARK_AQUA + game.getId()));
		// full name of arena
		sender.sendMessage(SimpleSpleef.ll("feedback.infoName", "[NAME]", ChatColor.DARK_AQUA + game.getName()));
		// status of arena
		String information;
		ChatColor color;
		if (game.isActive()) { // game running
			if (game.isJoinable() || game.isReady()) {
				information = SimpleSpleef.ll("feedback.arenaJoinable");
				color = ChatColor.GREEN;
			} else {
				information = SimpleSpleef.ll("feedback.arenaInProgress");
				color = ChatColor.LIGHT_PURPLE;
			}
			// ok, gather some more information on the game to display
			information = information + " " + game.getNumberOfPlayers();
		} else if (game.isEnabled()) {
			information = SimpleSpleef.ll("feedback.arenaOff");
			color = ChatColor.GRAY;
		} else {
			information = SimpleSpleef.ll("feedback.arenaDisabled");
			color = ChatColor.DARK_GRAY;
		}
		sender.sendMessage(SimpleSpleef.ll("feedback.infoStatus", "[STATUS]", color + information));
		// list of spleefers and spectators
		game.printGamePlayersAndSpectators(sender);
	}
	
	/**
	 * List spleefers in arena
	 * @param sender
	 * @param game
	 */
	public void list(CommandSender sender, Game game) {
		// list of spleefers and spectators
		game.printGamePlayersAndSpectators(sender);
	}
	
	/**
	 * Attempt to teleport back to original position
	 * @param sender
	 */
	public void back(CommandSender sender) {
		Player player;
		try {
			player = (Player) sender; // cast to player
		} catch (Exception e) { // handle possible cast error
			sender.sendMessage(ChatColor.DARK_RED + "Internal error while issuing back command. CommandSender has to be player object if arena is null!");
			return;
		}
		// check, if player is spleefer or spectator
		Game game = checkPlayerInGame(player);
		if (game == null) game = checkSpectatorInGame(player);
		if (game != null) {
			game.back(player);
		} else { // otherwise look up player in OriginalPositionKeeper and teleport him/her back
			Location originalLocation = SimpleSpleef.getOriginalPositionKeeper().getOriginalPosition(player);
			if (originalLocation != null) {
				player.teleport(originalLocation);
				player.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.back"));
			} else {
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.backNoLocation"));
			}
		}
	}

	
	/**
	 * create a cuboid from a section
	 * @param arena
	 * @param section
	 * @return
	 */
	public Cuboid configToCuboid(String arena, String section) {
		FileConfiguration conf = SimpleSpleef.getPlugin().getConfig();
		arena = arena.toLowerCase();
		section = section.toLowerCase();
		String confbase = "arenas." + arena + "." + section; // shorten stuff later on
		// is arena protected? If not, ignore to save resources
		if (!conf.getBoolean("arenas." + arena + ".protectArena", true)) return null;
		// get arena cube, if possible
		if (!conf.isConfigurationSection(confbase) ||
				!conf.getBoolean("arenas." + arena + ".arena.enabled", false)) return null;
		// do we have a world guard region here?
		if (conf.getString(confbase + ".worldguardRegion") != null && conf.getString(confbase + ".worldguardWorld") != null && getWorldGuard() != null) {
			// get world guard region
			World world = SimpleSpleef.getPlugin().getServer().getWorld(conf.getString(confbase + ".worldguardWorld"));
			// get region manager
			RegionManager regionManager = world!=null?getWorldGuard().getRegionManager(world):null;
			// error?
			if (world == null || regionManager == null) return null;
			// get region
			ProtectedRegion region = regionManager.getRegion(conf.getString(confbase + ".worldguardRegion"));
			if (region == null) return null;
			return new CuboidWorldGuard(region, world); // create world guard cuboid - very cool!
		} else { // normal, non WorldGuard coordinates
			// now, check sane coords
			String firstWorldString = conf.getString(confbase + ".a.world");
			String secondWorldString = conf.getString(confbase + ".b.world");
			World firstWorld = firstWorldString!=null?SimpleSpleef.getPlugin().getServer().getWorld(firstWorldString):null;
			World secondWorld = secondWorldString!=null?SimpleSpleef.getPlugin().getServer().getWorld(secondWorldString):null;
			if (firstWorld == null || secondWorld == null || firstWorld != secondWorld) return null; // non-sane worlds
			int firstX = conf.getInt(confbase + ".a.x", 0);
			int firstY = conf.getInt(confbase + ".a.y", 0);
			int firstZ = conf.getInt(confbase + ".a.z", 0);
			int secondX = conf.getInt(confbase + ".b.x", 0);
			int secondY = conf.getInt(confbase + ".b.y", 0);
			int secondZ = conf.getInt(confbase + ".b.z", 0);
			if (firstX == 0 && firstY == 0 && firstZ == 0 && secondX == 0 && secondY == 0 && secondZ == 0) return null;
			// create cube
			return new CuboidImpl(firstWorld, (firstX<secondX?firstX:secondX), (firstY<secondY?firstY:secondY), (firstZ<secondZ?firstZ:secondZ),
					(firstX>secondX?firstX:secondX), (firstY>secondY?firstY:secondY), (firstZ>secondZ?firstZ:secondZ));
		}
	}

	/**
	 * get a world guard instance if it exists - see http://wiki.sk89q.com/wiki/WorldGuard/Regions/API for source
	 * @return
	 */
	private WorldGuardPlugin getWorldGuard() {
	    Plugin plugin = SimpleSpleef.getPlugin().getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	        return null; // Maybe you want throw an exception instead
	    }
	 
	    return (WorldGuardPlugin) plugin;
	}
	
	
	/**
	 * Catch block breaks
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		
		// pass event to games
		for (Game game : games) {
			game.onBlockBreak(event);
		}
	}

	/**
	 * Catch block placements
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) return;
		
		// pass event to games
		for (Game game : games) {
			game.onBlockPlace(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		
		// pass event to games
		for (Game game : games) {
			game.onPlayerDeath((Player) event.getEntity());
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.isCancelled() || !(event.getEntity() instanceof Player)) return;
		
		// pass event to games
		for (Game game : games) {
			game.onFoodLevelChange(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled() || !(event.getEntity() instanceof Player)) return;
		
		// pass event to games
		for (Game game : games) {
			game.onEntityDamage(event);
		}
	}

	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) return;
		
		// pass event to games
		for (Game game : games) {
			game.onEntityExplode(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		// pass event to games
		for (Game game : games) {
			game.onPlayerJoin(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		if (event.isCancelled()) return;
		
		// pass event to games
		for (Game game : games) {
			game.onPlayerKick(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		// pass event to games
		for (Game game : games) {
			game.onPlayerQuit(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled()) return;
		
		// pass event to games
		for (Game game : games) {
			game.onPlayerMove(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
		
		// pass event to games
		for (Game game : games) {
			game.onPlayerInteract(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.isCancelled()) return;
		
		// pass event to games
		for (Game game : games) {
			game.onPlayerTeleport(event);
		}
	}
	
	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		if (event.isCancelled()) return;
		
		// pass event to games
		for (Game game : games) {
			game.onPlayerGameModeChange(event);
		}
	}
}
