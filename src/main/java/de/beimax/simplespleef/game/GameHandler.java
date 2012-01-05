/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.util.Cuboid;

/**
 * @author mkalus
 * Actual game handler - central part of the plugin
 */
public class GameHandler {
	/**
	 * games array
	 */
	private Game[] games;
	
	/**
	 * List of cuboids for arenas - help check arena protection
	 */
	private List<Cuboid> arenaCubes;
	
	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public GameHandler() {
		updateGameHandlerData();
	}
	
	/**
	 * Initialize game handler - mainly read arena cubes and update game data
	 */
	public void updateGameHandlerData() {
		// define cubes as linked list
		arenaCubes = new LinkedList<Cuboid>();
		// get possible games
		for (String game : getPossibleGames().keySet()) {
			// game enabled?
			if (!SimpleSpleef.getPlugin().getConfig().getBoolean("arenas." + game + ".enabled", false)) continue;
			Cuboid cuboid = configToCuboid(game, "arena");
			if (cuboid != null)
				arenaCubes.add(cuboid); // add to list
			else
				SimpleSpleef.log.warning("[SimpleSpleef] Unable to load coordinates of arena for arena " + game + ". Maybe the arena is not finished yet, or its world was deleted.");
		}
	}
	
	/**
	 * add a game
	 * @param game
	 * @return boolean if successful 
	 */
	public boolean addGame(Game game) {
		if (games == null) {
			games = new Game[1];
			games[0] = game;
		} else {
			// copy array and add new game
			Game[] newGames = new Game[games.length+1];
			for (int i = 0; i < games.length; i++) {
				if (games[i] == game || games[i].getId().equals(game.getId())) return false; // one cannot add the same game twice
				newGames[i] = games[i];
			}
			games = newGames;
			// add the new game, too
			games[games.length-1] = game;
		}
		return true;
	}
	
	/**
	 * add a game by name
	 * @param type
	 * @return
	 */
	public boolean addGame(String type, String name) {
		// let the factory handle the details
		Game game = GameFactory.createGame(type, name);
		if (game == null) return false; // no game created?
		
		return addGame(game); // add game
	}
	
	/**
	 * remove a game from the handler
	 * @param game
	 * @return
	 */
	public boolean removeGame(Game game) {
		return removeGame(game.getId());
	}
	
	/**
	 * remove a game from the handler (by name)
	 * @param game
	 * @return
	 */
	public boolean removeGame(String game) {
		if (games == null) return false;
		// only one element left
		if (games.length == 1) {
			if (games[0].getId().equalsIgnoreCase(game)) {
				games = null;
				return true;
			}
			return false;
		} 
		// reduce array
		Game[] newGames = new Game[games.length-1];
		boolean found = false;
		int pos = 0;
		for (int i = 0; i < newGames.length; i++) {
			if (games[i].getId().equalsIgnoreCase(game)) continue;
			newGames[pos++] = games[i];
		}
		if (found) games = newGames;
		return found;
	}

	/**
	 * check, if a certain game name exists
	 * @param game name of game
	 * @return
	 */
	public boolean gameExists(String game) {
		return getGameByName(game)==null?false:true;
	}

	/**
	 * return game by name
	 * @param game name of game
	 * @return
	 */
	public Game getGameByName(String game) {
		// no games present
		if (this.games == null) return null;
		// try to find game
		for (int i = 0; i < this.games.length; i++)
			if (this.games[i].getId().equalsIgnoreCase(game))
				return this.games[i];
		return null;
	}

	/**
	 * check, if a certain game type or name exists generally
	 * @param type
	 * @return
	 */
	public boolean gameTypeOrNameExists(String type) {
		Map<String, Boolean> arenas = getPossibleGames();
		if (arenas == null) return false; // none - unlikely, but possible...
		// cycle through possible games
		for (Entry<String, Boolean> arena: arenas.entrySet()) {
			if (arena.getKey().equalsIgnoreCase(type)) return true;
		}

		return false;
	}
	
	/**
	 * returns type of arena from game name
	 * @param game
	 * @return
	 */
	public String gameNameToType(String game) {
		return SimpleSpleef.getPlugin().getConfig().getString("arenas." + game + ".type", "standard");
	}

	/**
	 * Check if there are games on the server
	 * @return
	 */
	public boolean hasGames() {
		return games != null;
	}
	
	/**
	 * get list of games
	 * @return List of games
	 */
	public List<Game> getGames() {
		LinkedList<Game> games = new LinkedList<Game>();
		if (this.games == null) return games; // no games present?
		for (Game game : this.games) {
			games.add(game);
		}
		return games;
	}

	/**
	 * get list of games
	 * @return List of games
	 */
	public List<String> getGameNames() {
		LinkedList<String> games = new LinkedList<String>();
		if (this.games == null) return games; // no games present?
		for (Game game : this.games) {
			games.add(game.getName());
		}
		return games;
	}

	/**
	 * get list of game ids
	 * @return List of games
	 */
	public List<String> getGameIds() {
		LinkedList<String> games = new LinkedList<String>();
		if (this.games == null) return games; // no games present?
		for (Game game : this.games) {
			games.add(game.getId());
		}
		return games;
	}

	/**
	 * get list of possible games
	 * @return Map of arena names with a boolean for active/passive
	 */
	public Map<String, Boolean> getPossibleGames() {
		Map<String, Boolean> map = new TreeMap<String, Boolean>(); // sorting is maintained
		// get arenas from config and check if they are enabled
		Set<String> arenas = SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas").getKeys(false);
		for (String arena : arenas) {
			Boolean enabled = SimpleSpleef.getPlugin().getConfig().getBoolean("arenas." + arena + ".enabled", false);
			map.put(arena, enabled);
		}
		return map;
	}
	
	/**
	 * check, if arena has been disabled
	 * @param arena
	 * @return
	 */
	public boolean isArenaDisabled(String arena) {
		arena = arena.toLowerCase();
		return !SimpleSpleef.getPlugin().getConfig().getBoolean("arenas." + arena + ".enabled", false);
	}

	/**
	 * get the default arena name
	 * @return
	 */
	public String getDefaultArena() {
		return SimpleSpleef.getPlugin().getConfig().getString("settings.defaultArena", "default");
	}
	
	/**
	 * try to announce a new game in an arena
	 * @param sender
	 * @param arena
	 * @return announced game or null
	 */
	public Game announce(CommandSender sender, String arena) {
		Game game = getGameByName(arena);
		// does the game exist already?
		if (game != null) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.arenaExistsAlready", "[ARENA]", game.getName()));
			return null;
		}
		// check if game is disabled
		if (isArenaDisabled(arena)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.arenaDisabled", "[ARENA]", arena));
			return null;
		}
		game = createNewGame(arena);
		// announce new game globally?
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceGame", true))
			SimpleSpleef.getPlugin().getServer().broadcastMessage(ChatColor.GOLD + SimpleSpleef.getPlugin().ll("broadcasts.announce", "[PLAYER]", sender.getName(), "[ARENA]", game.getName()));
		else
			sender.sendMessage(ChatColor.GOLD + SimpleSpleef.getPlugin().ll("feedback.announce", "[ARENA]", game.getName()));
		return game;
	}
	
	/**
	 * try to join a game in an arena
	 * @param sender
	 * @param arena
	 */
	public void join(CommandSender sender, String arena) {
		arena = arena.toLowerCase();
		Game game = getGameByName(arena);
		Player player = (Player) sender; // cast to player
		// does the game not exist?
		if (game == null) {
			// check if game is disabled
			if (isArenaDisabled(arena)) {
				sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.arenaDisabled", "[ARENA]", arena));
				return;
			}
			// do players have the right to join unstarted games?
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("arenas." + arena + ".announceOnJoin", true))
				game = announce(sender, arena);
			else { // tell player that he may not announce game
				sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.announceBeforeJoin", "[ARENA]", arena));
				return;
			}
		}
		// player already joined another arena?
		Game checkGame = checkPlayerInGame(player);
		if (checkGame != null) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.joinDouble", "[ARENA]", checkGame.getName()));
			return;			
		} else checkGame = null;
		// ok, try to join the game itself...
		if (!game.join(player)) return;
		// now we announce the joining of the player...
		String broadcastMessage = ChatColor.GREEN + SimpleSpleef.getPlugin().ll("broadcasts.join", "[PLAYER]", sender.getName(), "[ARENA]", game.getName());
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceJoin", true)) { // broadcast
			SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage);
		} else { // player only
			sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("feedback.join", "[ARENA]", game.getName()));
			game.sendMessage(broadcastMessage, player); // notify players and spectators
		}
	}
	
	/**
	 * Attempt to start a game (spleefers only)
	 * @param sender
	 */
	public void start(CommandSender sender) {
		// only senders in a game may start a game
		Player player = (Player) sender; // cast to player
		// find player in arena
		Game checkGame = checkPlayerInGame(player);
		if (checkGame != null) {
			String arena = checkGame.getId();
			//is config "spleeferStart" of arena is set to true? - isJoinable added to avoid error message and let game do this instead
			if (checkGame.isJoinable() && !SimpleSpleef.getPlugin().getConfig().getBoolean("arenas." + arena + ".spleeferStart", true))
				sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.startNoSpleefer", "[ARENA]", arena));
			else // spleeferStart is true: attempt to start countdown
				countdown(sender, arena);
			return;
		}
		// sender not part of any game
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.start"));
	}

	/**
	 * Attempt to start countdown of a game
	 * @param sender
	 * @param arena (may be null)
	 */
	public void countdown(CommandSender sender, String arena) {
		arena = arena.toLowerCase();
		Game game = getGameByName(arena);
		// does the game not exist?
		if (game == null) {
			// check if game is disabled
			if (isArenaDisabled(arena)) {
				sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.arenaDisabled", "[ARENA]", arena));
				return;
			}
			// game not announced yet...
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.noGameAnnounced", "[ARENA]", arena));
			return;
		}
		// start countdown for game
		game.countdown(sender);
	}

	/**
	 * Attempt to leave a game (spleefers only)
	 * @param sender
	 */
	public void leave(CommandSender sender) {
		Player player;
		try {
			player = (Player) sender; // cast to player
		} catch (Exception e) { // handle possible cast error
			sender.sendMessage(ChatColor.DARK_RED + "Internal error while leaving game. CommandSender has to be player object if arena is null!");
			return;
		}
		if (player == null) {
			sender.sendMessage(ChatColor.DARK_RED + "Internal error while leaving game. Player was null!");
			return;			
		}
		// player part of an active game or he/she is a spectator?
		Game game = checkPlayerInGame(player);
		if (game == null) game = checkSpectatorInGame(player);
		if (game != null) {
			game.leave(player);
		} else
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.leave"));
	}

	/**
	 * Attempt to stop a game
	 * @param sender
	 */
	public void stop(CommandSender sender) {
		// only senders in a game may start a game
		Player player = (Player) sender; // cast to player
		// find player in arena
		Game checkGame = checkPlayerInGame(player);
		if (checkGame != null) {
			checkGame.stop(player); // stop game
		} else //print error, since player not part of a game
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.stopNoPlaying"));
	}

	/**
	 * Attempt to delete a game
	 * @param sender
	 * @param arena (may be null)
	 */
	public void delete(CommandSender sender, String arena) {
		Game game;
		// if arena is null, get it from sender
		if (arena == null) {
			Player player;
			try {
				player = (Player) sender; // cast to player
			} catch (Exception e) { // handle possible cast error
				sender.sendMessage(ChatColor.DARK_RED + "Internal error while deleting game. CommandSender has to be player object if arena is null!");
				return;
			}
			game = checkPlayerInGame(player);
			if (game == null) {
				sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.deleteNoPlaying"));
				return;
			}
		} else { // otherwise try to get arena by name
			game = getGameByName(arena);
			if (game == null) { // print error, arena was not found
				sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
				return;
			}
		}
		game.delete(sender); // delete game
	}

	/**
	 * Attempt to watch a game
	 * @param sender
	 * @param arena (may not be null)
	 */
	public void watch(CommandSender sender, String arena) {
		Player player;
		try {
			player = (Player) sender; // cast to player
		} catch (Exception e) { // handle possible cast error
			sender.sendMessage(ChatColor.DARK_RED + "Internal error while deleting game. CommandSender has to be player object if arena is null!");
			return;
		}
		// player part of an active game?
		Game game = checkPlayerInGame(player);
		if (game != null) { // part of a game - may not spectate
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.watchDouble", "[ARENA]", game.getName()));
			return;
		}
		if (!gameTypeOrNameExists(arena)) { // print error, arena was not found
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
			return;
		}
		Game activeGame = getGameByName(arena);
		if (activeGame == null) { // game is not active
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.watchNoGame", "[ARENA]", arena));
			return;
		}
		// ok, player not in active game, game exists, let's watch!
		activeGame.watch(player); // attempt to watch game
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
			sender.sendMessage(ChatColor.DARK_RED + "Internal error while deleting game. CommandSender has to be player object if arena is null!");
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
				player.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("feedback.back"));
			} else {
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.backNoLocation"));
			}
		}
	}

	/**
	 * checks whether the player is part of a game or not
	 * @param player
	 * @return Game the player is part of or null, if not
	 */
	public Game checkPlayerInGame(Player player) {
		for (Game checkGame :  getGames()) {
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
		for (Game checkGame :  getGames()) {
			if (checkGame.hasSpectator(player)) return checkGame;
		}
		return null;
	}
	
	/**
	 * helper to create new game and add it automatically to list
	 * @param arena
	 * @return new game added
	 */
	protected Game createNewGame(String arena) {
		// get type of arena
		String type = gameNameToType(arena);
		Game game = GameFactory.createGame(type, arena);
		// define configuration section for this game
		game.defineSettings(SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas." + game.getId()));
		// add game to list
		addGame(game);
		// return newly created game
		return game;
	}

	/**
	 * check block breaks in- and outside of game
	 * @param event
	 */
	public void checkBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		// check if player is a spleefer or not
		Game game = checkPlayerInGame(player);
		if (game != null) { // game is spleefer!
			//send block break to game
			game.onBlockBreak(event);
		} else if (inProtectedArenaCube(event.getBlock())) {
			// cancel event
			event.setCancelled(true);
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.noDig"));
		}
	}
	
	/**
	 * check block placement in- and outside of game
	 * @param event
	 */
	public void checkBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		// check if player is a spleefer or not
		Game game = checkPlayerInGame(player);
		if (game != null) { // game is spleefer!
			//send block place to game
			game.onBlockPlace(event);
		} else if (inProtectedArenaCube(event.getBlock())) {
			// cancel event
			event.setCancelled(true);
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.noPlacement"));
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
		// is arena protected? If not, ignore to save resources
		if (!conf.getBoolean("arenas." + arena + ".protectArena", true)) return null;
		// get arena cube, if possible
		if (!conf.isConfigurationSection("arenas." + arena + "." + section) ||
				!conf.getBoolean("arenas." + arena + ".arena.enabled", false)) return null;
		// now, check sane coords
		String firstWorldString = conf.getString("arenas." + arena + "." + section + ".a.world");
		String secondWorldString = conf.getString("arenas." + arena + "." + section + ".b.world");
		World firstWorld = firstWorldString!=null?SimpleSpleef.getPlugin().getServer().getWorld(firstWorldString):null;
		World secondWorld = secondWorldString!=null?SimpleSpleef.getPlugin().getServer().getWorld(secondWorldString):null;
		if (firstWorld == null || secondWorld == null || firstWorld != secondWorld) return null; // non-sane worlds
		int firstX = conf.getInt("arenas." + arena + "." + section + ".a.x", 0);
		int firstY = conf.getInt("arenas." + arena + "." + section + ".a.y", 0);
		int firstZ = conf.getInt("arenas." + arena + "." + section + ".a.z", 0);
		int secondX = conf.getInt("arenas." + arena + "." + section + ".b.x", 0);
		int secondY = conf.getInt("arenas." + arena + "." + section + ".b.y", 0);
		int secondZ = conf.getInt("arenas." + arena + "." + section + ".b.z", 0);
		if (firstX == 0 && firstY == 0 && firstZ == 0 && secondX == 0 && secondY == 0 && secondZ == 0) return null;
		// create cube
		return new Cuboid(firstWorld, (firstX<secondX?firstX:secondX), (firstY<secondY?firstY:secondY), (firstZ<secondZ?firstZ:secondZ),
				(firstX>secondX?firstX:secondX), (firstY>secondY?firstY:secondY), (firstZ>secondZ?firstZ:secondZ));
	}
	
	/**
	 * Check if a block is in protected arena
	 * @param block
	 * @return
	 */
	public boolean inProtectedArenaCube(Block block) {
		if (arenaCubes == null) return false;
		Location blockLoc = block.getLocation();
		// check position in each cube
		for (Cuboid cuboid : arenaCubes)
			if (cuboid.contains(blockLoc)) return true; // in protected cube!
		
		return false;
	}

	/**
	 * reload the configuration of the games
	 */
	public void reloadConfig() {
		// update game handler data
		updateGameHandlerData();
		// iterate through active games
		for (Game game : getGames()) {
			// redefine settings
			game.defineSettings(SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas." + game.getId()));
		}
	}

	/**
	 * called by a game that has ended
	 * @param game
	 */
	public void gameOver(Game game) {
		// call cleaning routine of game
		game.clean();
		// remove game from active list
		removeGame(game);
	}
}
