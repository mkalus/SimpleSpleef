/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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
				if (games[i] == game || games[i].getName().equals(game.getName())) return false; // one cannot add the same game twice
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
		return removeGame(game.getName());
	}
	
	/**
	 * remove a game from the handler (by name)
	 * @param game
	 * @return
	 */
	public boolean removeGame(String game) {
		if (games == null) return false;
		Game[] newGames = new Game[games.length-1];
		boolean found = false;
		int pos = 0;
		for (int i = 0; i < newGames.length; i++) {
			if (games[i].getName().equals(game)) continue;
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
			if (this.games[i].getName().equalsIgnoreCase(game))
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
		if (game.equalsIgnoreCase(this.getPlugin().getConfig().getString("settings.freeStyleArenaName", "freestyle")))
			return "freestyle";
		else return "standard";
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
		//TODO
		return null;
	}

	/**
	 * get list of possible games
	 * @return Map of arena names with a boolean for active/passive
	 */
	public Map<String, Boolean> getPossibleGames() {
		Map<String, Boolean> map = new TreeMap<String, Boolean>(); // sorting is maintained
		// freestyle arena allowed?
		if (this.getPlugin().getConfig().getBoolean("settings.allowFreeStylePlaying", true)) {
			map.put(this.getPlugin().getConfig().getString("settings.freeStyleArenaName", "freestyle"), true);
		}
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
		Game game = getGameByName(arena);
		arena = arena.toLowerCase();
		// does the game not exist?
		if (game == null) {
			// do players have the right to join unstarted games?
			if (this.getPlugin().getConfig().getBoolean("arenas." + arena + ".announceOnJoin", true))
				game = announce(sender, arena);
			else {
				sender.sendMessage(ChatColor.DARK_RED + this.getPlugin().ll("errors.announceBeforeJoin", "[ARENA]", arena));
				return;
			}
		}
		//game.getName();
		// TODO
		//settings.announceGame
		//settings.announceJoin
	}

	//TODO game handling itself
	
	/**
	 * helper to create new game and add it automatically to list
	 * @param arena
	 * @return new game added
	 */
	protected Game createNewGame(String arena) {
		// get type of arena
		String type = gameNameToType(arena);
		Game game = GameFactory.createGame(type, arena);
		// if not freestyle - read configuration
		if (!type.equals("freestyle"))
			game.defineSettings(this.getPlugin().getConfig().getConfigurationSection("arenas." + arena));
		// add game to list
		addGame(game);
		// return newly created game
		return game;
	}
}
