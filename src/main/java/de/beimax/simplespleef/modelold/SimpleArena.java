/**
 * 
 */
package de.beimax.simplespleef.modelold;

/**
 * Simple arena implementation that mimicks the behavior of SimplesSpleef prior to 2.0
 * @author mkalus
 */
public class SimpleArena implements Arena {
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.beimax.simplespleef.model.Arena#setName(java.lang.String)
	 */
	public void setName(String name) {  // do nothing...
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.beimax.simplespleef.model.Arena#getName()
	 */
	public String getName() {
		return "Arena"; // dummy name
	}

}
