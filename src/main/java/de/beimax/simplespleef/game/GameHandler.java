/**
 * 
 */
package de.beimax.simplespleef.game;

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
		}
		return true;
	}
	
	/**
	 * add a game by name
	 * @param type
	 * @return
	 */
	public boolean addGame(String type) {
		// let the factory handle the details
		Game game = GameFactory.createGame(type);
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
	 * Check if there are games on the server
	 * @return
	 */
	public boolean hasGames() {
		return games != null;
	}

	//TODO game handling itself
}
