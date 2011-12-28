/**
 * 
 */
package de.beimax.simplespleef.util;

import java.io.Serializable;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * @author mkalus Represents cube in the world - inspired by Cuboid plugin
 */
public class Cuboid implements Serializable {
	private static final long serialVersionUID = 5378552316337311806L;

	/**
	 * world
	 */
	private World world;
	
	/**
	 * coordinates
	 */
	private int[] coords; // int[]{firstX, firstY, firstZ, secondX, secondY, secondZ}

	/**
	 * Constructor
	 */
	public Cuboid() {
		this.coords = new int[6];
		this.world = null;
	}
	
	/**
	 * Constructor
	 * @param world
	 * @param firstX
	 * @param firstY
	 * @param firstZ
	 * @param secondX
	 * @param secondY
	 * @param secondZ
	 */
	public Cuboid(World world, int firstX, int firstY, int firstZ, int secondX, int secondY, int secondZ) {
		this.coords = new int[]{firstX, firstY, firstZ, secondX, secondY, secondZ};
		this.world = world;
	}
	
	/**
	 * checks, if cuboid is on a certain world
	 * @param world
	 * @return
	 */
	public boolean onWorld(World world) {
		if (this.world == world) return true;
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
	public boolean contains(int X, int Y, int Z) {
		System.out.println(coords[0] + "/" + coords[3] + " - " + X);
		System.out.println(coords[1] + "/" + coords[4] + " - " + Y);
		System.out.println(coords[2] + "/" + coords[5] + " - " + Z);
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
