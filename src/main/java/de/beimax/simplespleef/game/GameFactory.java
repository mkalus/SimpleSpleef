/**
 * 
 */
package de.beimax.simplespleef.game;

/**
 * @author mkalus
 * Factory class to create games
 */
public abstract class GameFactory {
	/**
	 * factory create a new game instance
	 * @param type
	 * @param name
	 * @return game or null, if game type does not exist
	 */
	public static Game createGame(String type, String name) {
		//TODO create new instances and return them
		return null;
	}
}
