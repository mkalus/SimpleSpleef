/**
 * 
 */
package de.beimax.simplespleef.util;

import java.io.Serializable;

import org.bukkit.Location;

/**
 * @author mkalus Represents cube in the world - inspired by Cuboid plugin
 */
public class Cuboid implements Serializable {
	private static final long serialVersionUID = 5378552316337311806L;

	/**
	 * coordinates
	 */
	private int[] coords; // int[]{firstX, firstY, firstZ, secondX, secondY, secondZ}

	/**
	 * Constructor
	 */
	public Cuboid() {
		this.coords = new int[6];
	}

	/**
	 * checks whether coordinates are within this cuboid
	 * 
	 * @param X
	 * @param Y
	 * @param Z
	 * @return
	 */
	public boolean contains(int X, int Y, int Z) {
		if (X >= coords[0] && X <= coords[3] && Z >= coords[2]
				&& Z <= coords[5] && Y >= coords[1] && Y <= coords[4])
			return true;
		return false;
	}

	/**
	 * checks whether coordinates are within this cuboid
	 * 
	 * @param X
	 * @param Y
	 * @param Z
	 * @return
	 */
	public boolean contains(float X, float Y, float Z) {
		return contains((int) X, (int) Y, (int) Z);
	}

	/**
	 * checks whether coordinates are within this cuboid
	 * 
	 * @param X
	 * @param Y
	 * @param Z
	 * @return
	 */
	public boolean contains(double X, double Y, double Z) {
		return contains((int) X, (int) Y, (int) Z);
	}
	
	/**
	 * checks whether coordinates are within this cuboid
	 * 
	 * @param X
	 * @param Y
	 * @param Z
	 * @return
	 */
	public boolean contains(Location location) {
		return contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}
}
