/**
 * 
 */
package de.beimax.simplespleef.game.arenarestoring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.util.Cuboid;
import de.beimax.simplespleef.util.SerializableBlockData;

/**
 * @author mkalus
 *
 */
public class HardArenaRestorer implements ArenaRestorer {
	/**
	 * game to restore
	 */
	private Game game;
	
	/**
	 * cuboid to store/restore
	 */
	private Cuboid cuboid;

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.arenarestoring.ArenaRestorer#saveArena(de.beimax.simplespleef.game.Game)
	 */
	@Override
	public void saveArena(Game game, Cuboid cuboid) {
		if (game == null || cuboid == null) return; //ignore invalid stuff
		this.game = game;
		this.cuboid = cuboid;

		SerializableBlockData[][][] blockData = this.cuboid.getSerializedBlocks();
		
		// output file
		File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + this.game.getId() + ".save");
		// delete old file
		if (file.exists() && !file.delete()) {
			SimpleSpleef.log.warning("[SimpleSpleef] Could not delete file " + file.getName());
			return;
		}
		
		try {
			// serialize objects
			FileOutputStream fileStream = new FileOutputStream(file);
			ObjectOutputStream os = new ObjectOutputStream(fileStream);
			// write array itself
			os.writeObject(blockData);
			os.close();
		} catch (Exception e) {
			 SimpleSpleef.log.warning("[SimpleSpleef] Could not save arena file " + file.getName() + ". Reason: " + e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.arenarestoring.ArenaRestorer#restoreArena()
	 */
	@Override
	public void restoreArena() {
		// start restoration in new thread
		(new RestoreThread()).start();
	}

	/**
	 * restorer thread
	 * @author mkalus
	 *
	 */
	private class RestoreThread extends Thread {
		@Override
		public void run() {
			// input file
			File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + game.getId() + ".save");
			if (!file.exists()) {
				SimpleSpleef.log.warning("[SimpleSpleef] Could find arena file " + file.getName());
				return;
			}
			SerializableBlockData[][][] blockData;
			try {
				// deserialize objects
				FileInputStream fileInputStream = new FileInputStream(file);
				ObjectInputStream oInputStream = new ObjectInputStream(fileInputStream);
				blockData = (SerializableBlockData[][][]) oInputStream.readObject();
				oInputStream.close();
			} catch (Exception e) {
				 SimpleSpleef.log.warning("[SimpleSpleef] Could not restore arena file " + file.getName() + ". Reason: " + e.getMessage());
				 return;
			}
			// restore arena
			cuboid.setSerializedBlocks(blockData);
			// delete file at the end - cleanup work...
			if (!file.delete()) SimpleSpleef.log.warning("[SimpleSpleef] Could not delete file " + file.getName());
			
			// call game handler to finish the game off
			SimpleSpleef.getGameHandler().gameOver(game);			
		}
	}
}
