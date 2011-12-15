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
				if (games[i] == game) return false; // one cannot add the same game twice
				newGames[i] = games[i];
			}
			games = newGames;
		}
		return true;
	}
	
	/**
	 * remove a game from the handler
	 * @param game
	 * @return
	 */
	public boolean removeGame(Game game) {
		if (games == null) return false;
		// copy array and do not add old game
		Game[] newGames = new Game[games.length-1];
		boolean found = false;
		int pos = 0;
		for (int i = 0; i < newGames.length; i++) {
			if (games[i] == game) continue;
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
