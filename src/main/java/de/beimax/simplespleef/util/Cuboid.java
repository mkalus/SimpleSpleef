/**
 * 
 */
package de.beimax.simplespleef.util;

import java.io.Serializable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * @author mkalus Represents cube in the world - inspired by Cuboid plugin
 */
public class Cuboid implements Serializable {
	private static final long serialVersionUID = 4189254652090021378L;

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
	protected boolean onWorld(World world) {
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
	protected boolean contains(int X, int Y, int Z) {
		//System.out.println(coords[0] + "/" + coords[3] + " - " + X);
		//System.out.println(coords[1] + "/" + coords[4] + " - " + Y);
		//System.out.println(coords[2] + "/" + coords[5] + " - " + Z);
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
	protected boolean contains(float X, float Y, float Z) {
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
	protected boolean contains(double X, double Y, double Z) {
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
		return onWorld(location.getWorld()) && contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	/**
	 * return array of serializable blocks
	 * return serializable block data
	 * @return
	 */
	public SerializableBlockData[][][] getSerializedBlocks() {
		SerializableBlockData[][][] blockData =
			new SerializableBlockData[this.coords[3]-this.coords[0]+1][this.coords[4]-this.coords[1]+1][this.coords[5]-this.coords[2]+1];
		
		// copy data from blocks
		for (int x = this.coords[0]; x <= this.coords[3]; x++)
			for (int y = this.coords[1]; y <= this.coords[4]; y++)
				for (int z = this.coords[2]; z <= this.coords[5]; z++) {
					Block block = this.world.getBlockAt(x, y, z);
					blockData[Math.abs(this.coords[0]-x)][Math.abs(this.coords[1]-y)][Math.abs(this.coords[2]-z)] =
						new SerializableBlockData(block.getTypeId(), block.getData());
				}
		return blockData;
	}
	
	/**
	 * restore array of serializable blocks
	 * @param blockData
	 */
	public void setSerializedBlocks(SerializableBlockData[][][] blockData) {
		for (int x = 0; x < blockData.length; x++)
			for (int y = 0; y < blockData[0].length; y++)
				for (int z = 0; z < blockData[0][0].length; z++) {
					Block block = this.world.getBlockAt(this.coords[0] + x, this.coords[1] + y, this.coords[2] + z);
					block.setTypeId(blockData[x][y][z].getTypeId());
					block.setData(blockData[x][y][z].getData());
				}
	}
}
