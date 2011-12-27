/**
 * 
 */
package de.beimax.simplespleef.listeners;

import org.bukkit.Material;

/**
 * @author mkalus
 *
 */
public abstract class MaterialHelper {
	/**
	 * Returns material for entry (simple material like DIAMOND_SPADE or 277)
	 * @param material
	 * @return
	 */
	public static Material getMaterialFromString(String material) {
		Material m;
		// try to convert material from number
		try {
			m = Material.getMaterial(Integer.valueOf(material));
		} catch (Exception e) {
			m = Material.getMaterial(material); // try to convert it from name instead
		}

		return m;
	}
}
