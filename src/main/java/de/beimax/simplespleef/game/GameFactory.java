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
	 * @param handler
	 * @param type
	 * @param name
	 * @return game or null, if game type does not exist
	 */
	public static Game createGame(GameHandler handler, String type, String name) {
		// TODO: inject other game types
		return new GameImpl(handler, name);
	}
}
