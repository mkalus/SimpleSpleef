/**
 * 
 */
package de.beimax.simplespleef.modelold;

import org.bukkit.World;

/**
 * New and complex arena design.
 * 
 * @author mkalus
 */
public class ComplexArena implements Arena {
	/**
	 * name of arena
	 */
	private String name;

	/**
	 * world the arena is on
	 */
	private World world;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.beimax.simplespleef.model.Arena#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.beimax.simplespleef.model.Arena#getName()
	 */
	public String getName() {
		return this.name;
	}

	// TODO: area where stuff can be dug away
	// TODO: dropout area

	// TODO: structure that keeps track of blocks destroyed

	// TODO: point for starting the game
	// TODO: point for lobby
	// TODO: point for spectators
}
