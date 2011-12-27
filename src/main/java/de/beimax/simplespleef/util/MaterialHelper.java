/**
 * 
 */
package de.beimax.simplespleef.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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
		//TODO: implement damage numbers!
		
		Material m;
		// try to convert material from number
		try {
			m = Material.getMaterial(Integer.valueOf(material));
		} catch (Exception e) {
			m = Material.getMaterial(material); // try to convert it from name instead
		}

		return m;
	}
	
	public static ItemStack getItemStackFromString(String line) {
		// split string to stack
		String[] parts = line.split(":");
		int number;
		String item;
		if (parts.length > 3) return null; //syntax error
		else if (parts.length == 3) {
			try {
				number = Integer.valueOf(parts[0]);
			} catch (Exception e) {
				return null; //syntax error
			}
			item = parts[1] + parts[2]; // damage number included
		} else if (parts.length == 2) {
			try {
				number = Integer.valueOf(parts[0]);
			} catch (Exception e) {
				return null; //syntax error
			}
			item = parts[1];
		} else {
			number = 1;
			item = line;
		}
		// get material
		Material material = getMaterialFromString(item);
		if (material == null) return null; //syntax error
		// create stack
		ItemStack stack = new ItemStack(material);
		stack.setAmount(number);
		
		return stack;
	}
}
