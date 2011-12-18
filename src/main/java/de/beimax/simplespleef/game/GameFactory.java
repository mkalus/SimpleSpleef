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
	 * @return game or null, if game type does not exist
	 */
	public static Game getGame(String type) {
		//TODO create new instances and return them
		return null;
	}

	/**
	 * check, if a certain game type/name exists
	 * @param name
	 * @return
	 */
	public static boolean gameNameExists(String name) {
		// TODO Auto-generated method stub
		return false;
	}
}
