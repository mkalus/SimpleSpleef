/**
 * This file is part of the SimpleSpleef bukkit plugin.
 * Copyright (C) 2011 Maximilian Kalus
 * See http://dev.bukkit.org/server-mods/simple-spleef/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package de.beimax.simplespleef.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * @author mkalus
 *
 */
public abstract class MaterialHelper {
	/**
	 * Returns material for entry (simple material like DIAMOND_SPADE or 277 - no damage numbers here!)
	 * @param material
	 * @return
	 */
	public static Material getMaterialFromString(String material) {
		if (material == null) return null; // ignore null strings

		Material m;
		// try to convert material from number
		try {
			m = Material.getMaterial(Integer.valueOf(material));
		} catch (Exception e) {
			m = Material.getMaterial(material); // try to convert it from name instead
		}

		return m;
	}
	
	/**
	 * 
	 * @param line
	 * @return
	 */
	public static ItemStack getItemStackFromString(String line) {
		if (line == null) return null; // ignore null strings
		// split string to stack
		String[] parts = line.split(":");
		int number;
		String item;
		short dmg = -1;
		if (parts.length > 3) return null; //syntax error
		else if (parts.length == 3) { // with damage number!
			try {
				number = Integer.valueOf(parts[0]);
			} catch (Exception e) {
				return null; //syntax error
			}
			item = parts[1];
			try { // get damage number
				dmg = Short.valueOf(parts[2]);
			} catch (Exception e) {
				return null; //syntax error
			}
		} else if (parts.length == 2) {
			try {
				number = Integer.valueOf(parts[0]);
				item = parts[1];
			} catch (Exception e) {
				// ok, then this is the material
				number = 1;
				item = parts[0];
				// second should be damage number
				try { // get damage number
					dmg = Short.valueOf(parts[1]);
				} catch (Exception ex) {
					return null; //syntax error
				}
			}
		} else {
			number = 1;
			item = line;
		}
		// get material
		Material material = getMaterialFromString(item);
		if (material == null) return null; //syntax error
		// create stack
		ItemStack stack;
		if (dmg > 0) stack = new ItemStack(material, number, dmg);
		//else stack = new ItemStack(material, number, (short) -1); //-1 to define a generic material (like all wool, not wool:0 = white wool)
		else stack = new ItemStack(material, number); //-1 to define a generic material (like all wool, not wool:0 = white wool)

		return stack;
	}
}
