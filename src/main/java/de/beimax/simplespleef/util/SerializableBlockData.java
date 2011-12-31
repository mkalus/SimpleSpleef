/**
 * 
 */
package de.beimax.simplespleef.util;

import java.io.Serializable;

/**
 * @author mkalus
 * Keep block data of a cuboid for saving it to disk
 */
public class SerializableBlockData implements Serializable {
	private static final long serialVersionUID = 8057033565492517528L;

	/**
	 * type of block
	 */
	private int typeId;
	/**
	 * date value of block
	 */
	private byte data;
	
	/**
	 * Constructor
	 * @param typeId
	 * @param data
	 */
	public SerializableBlockData(int typeId, byte data) {
		this.typeId = typeId;
		this.data = data;
	}

	/**
	 * @return the typeId
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * @return the data
	 */
	public byte getData() {
		return data;
	}
}
