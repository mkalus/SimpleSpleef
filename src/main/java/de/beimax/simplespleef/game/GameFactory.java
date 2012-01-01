/**
 * 
 */
package de.beimax.simplespleef.game;

import java.lang.reflect.Constructor;


import de.beimax.simplespleef.SimpleSpleef;

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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Game createGame(String type, String name) {
		// standard games
		if (type == null || type.equalsIgnoreCase("standard"))
			return new GameImpl(name);
		// inject other game types
		try {
			// define constructor parameters
			Class[] argsClass = new Class[] { String.class };
			// define arguments for this constructor
			Object[] args = new Object[] { new String(name) };
			// use reflection to create new instance
			Class gameClass = Class.forName(name); // get class
			Constructor constructor = gameClass.getConstructor(argsClass); // get constructor
			// call constructor and return game instance
			return (Game) constructor.newInstance(args);
		} catch (Exception e) {
			SimpleSpleef.log.severe("[SimpleSpleef] Could not load class " + name);
			e.printStackTrace();
		}
		return null;
	}
}
