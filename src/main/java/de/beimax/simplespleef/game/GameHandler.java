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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 * Actual game handler - central part of the plugin
 */
public class GameHandler {
	/**
	 * reference to plugin
	 */
	private final SimpleSpleef plugin;
	
	/**
	 * games array
	 */
	private Game[] games;
	
	//TODO states that can be called from listeners/commands
	
	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public GameHandler(SimpleSpleef plugin) {
		this.plugin = plugin;
	}

	/**
	 * @return the plugin
	 */
	public SimpleSpleef getPlugin() {
		return plugin;
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
		Game game = GameFactory.createGame(this, type, name);
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
		return this.getPlugin().getConfig().getString("arenas." + game + ".type", "standard");
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
		Set<String> arenas = this.getPlugin().getConfig().getConfigurationSection("arenas").getKeys(false);
		for (String arena : arenas) {
			Boolean enabled = this.getPlugin().getConfig().getBoolean("arenas." + arena + ".enabled", false);
			map.put(arena, enabled);
		}
		return map;
	}

	/**
	 * get the default arena name
	 * @return
	 */
	public String getDefaultArena() {
		return this.getPlugin().getConfig().getString("settings.defaultArena", "default");
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
			sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.arenaExistsAlready", "[ARENA]", game.getName()));
			return null;
		}
		game = createNewGame(arena);
		// announce new game globally?
		if (this.getPlugin().getConfig().getBoolean("settings.announceGame", true))
			this.getPlugin().getServer().broadcastMessage(ChatColor.GOLD + this.getPlugin().ll("broadcasts.announce", "[PLAYER]", sender.getName(), "[ARENA]", game.getName()));
		else
			sender.sendMessage(ChatColor.GOLD + this.getPlugin().ll("feedback.announce", "[ARENA]", game.getName()));
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
			if (!this.getPlugin().getConfig().getBoolean("arenas." + arena + ".enabled", false)) {
				sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.arenaDisabled", "[ARENA]", arena));
				return;
			}
			// do players have the right to join unstarted games?
			if (this.getPlugin().getConfig().getBoolean("arenas." + arena + ".announceOnJoin", true))
				game = announce(sender, arena);
			else { // tell player that he may not announce game
				sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.announceBeforeJoin", "[ARENA]", arena));
				return;
			}
		}
		// player already joined another arena?
		Game checkGame = checkPlayerInGame(player);
		if (checkGame != null) {
			sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.joinDouble", "[ARENA]", checkGame.getName()));
			return;			
		} else checkGame = null;
		// ok, try to join the game itself...
		if (!game.join(player)) return;
		// now we announce the joining of the player...
		String broadcastMessage = ChatColor.GREEN + this.getPlugin().ll("broadcasts.join", "[PLAYER]", sender.getName(), "[ARENA]", game.getName());
		if (this.getPlugin().getConfig().getBoolean("settings.announceJoin", true)) { // broadcast
			this.getPlugin().getServer().broadcastMessage(broadcastMessage);
		} else { // player only
			sender.sendMessage(ChatColor.GREEN + this.getPlugin().ll("feedback.join", "[ARENA]", game.getName()));
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
			if (checkGame.isJoinable() && !this.getPlugin().getConfig().getBoolean("arenas." + arena + ".spleeferStart", true))
				sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.startNoSpleefer", "[ARENA]", arena));
			else // spleeferStart is true: attempt to start countdown
				countdown(sender, arena);
			return;
		}
		// sender not part of any game
		sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.start"));
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
			if (!this.getPlugin().getConfig().getBoolean("arenas." + arena + ".enabled", false)) {
				sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.arenaDisabled", "[ARENA]", arena));
				return;
			}
			// game not announced yet...
			sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.noGameAnnounced", "[ARENA]", arena));
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
		// TODO Auto-generated method stub
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
			sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("errors.stopNoPlaying"));
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
				sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("errors.deleteNoPlaying"));
				return;
			}
		} else { // otherwise try to get arena by name
			game = getGameByName(arena);
			if (game == null) {
				// print error, arena was not found
				sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("errors.unknownArena", "[ARENA]", arena));
				return;
			}
		}
		game.delete(sender); // delete game
	}

	/**
	 * Attempt to stop a game
	 * @param sender
	 * @param arena (may not be null)
	 */
	public void watch(CommandSender sender, String arena) {
		// TODO Auto-generated method stub
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
	 * helper to create new game and add it automatically to list
	 * @param arena
	 * @return new game added
	 */
	protected Game createNewGame(String arena) {
		// get type of arena
		String type = gameNameToType(arena);
		Game game = GameFactory.createGame(this, type, arena);
		// define configuration section for this game
		game.defineSettings(this.getPlugin().getConfig().getConfigurationSection("arenas." + game.getId()));
		// add game to list
		addGame(game);
		// return newly created game
		return game;
	}

	/**
	 * check block breaks in and outside of game
	 */
	public void checkBlockBreak(BlockBreakEvent event) {
		// TODO Auto-generated method stub
		// TODO: check if player is a spleefer or not
		// TODO: otherweise check if player is in protected arena
	}
	
	/**
	 * reload the configuration of the games
	 */
	public void reloadConfig() {
		for (Game game : getGames()) { //iterate through active games
			// redefine settings
			game.defineSettings(this.getPlugin().getConfig().getConfigurationSection("arenas." + game.getId()));
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
