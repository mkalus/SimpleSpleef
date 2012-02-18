/**
 * 
 */
package de.beimax.simplespleef.game.arenarestoring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.bukkit.block.Block;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.gamehelpers.Cuboid;
import de.beimax.simplespleef.gamehelpers.SerializableBlockData;

/**
 * @author mkalus
 *
 */
public class HardArenaRestorer extends ArenaRestorer {
	public static final int STATUS_NONE = 0;
	public static final int STATUS_SAVED = 1;
	public static final int STATUS_RESTORED = 2;
	
	private int status = HardArenaRestorer.STATUS_NONE;
	
	/**
	 * cuboid to store/restore
	 */
	private Cuboid cuboid;

	/**
	 * Constructor
	 * @param waitBeforeRestoring
	 */
	public HardArenaRestorer(int waitBeforeRestoring) {
		super(waitBeforeRestoring);
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.arenarestoring.ArenaRestorer#setArena(de.beimax.simplespleef.game.Game)
	 */
	@Override
	public void setArena(Cuboid cuboid) {
		this.cuboid = cuboid;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.arenarestoring.ArenaRestorer#saveArena()
	 */
	@Override
	public void saveArena() {
		if (game == null || cuboid == null) return; //ignore invalid stuff

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
		// restore arena - this is quite heavy on the server...
		cuboid.setSerializedBlocks(blockData);
		// delete file at the end - cleanup work...
		if (!file.delete()) SimpleSpleef.log.warning("[SimpleSpleef] Could not delete file " + file.getName());
	}

	@Override
	public boolean tick() {
		if (interrupted == true) { // when interrupted, restore arena right awaw
			restoreArena(); // finish arena at once
			return true; // finish restorer
		}
		
		// wait for game to start
		if (status == HardArenaRestorer.STATUS_NONE && game.isInGame()) { // game started and not saved yet
			saveArena();
			status = HardArenaRestorer.STATUS_SAVED;
			return false;
		}
		
		// wait for game to finish - or return, if already restored
		if (!game.isFinished() || status == HardArenaRestorer.STATUS_RESTORED) return false;

		// before start restoring count restore ticker to 0
		if (this.waitBeforeRestoring > 0) {
			this.waitBeforeRestoring--;
			return false;
		}

		restoreArena(); // finish arena at once
		status = HardArenaRestorer.STATUS_RESTORED;
		return true; // finish restorer
	}

	@Override
	public boolean updateBlock(Block block, int oldType, byte oldData) { // ignore
		return false;
	}
	
	@Override
	public void initialize(Game game) {
		this.game = game;

		// input file
		File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + game.getId() + ".save");
		if (file.exists()) {
			SimpleSpleef.log.info("[SimpleSpleef] Restoring arena for " + game.getId() + " - apparently there was a server crash during the game.");
			restoreArena();
		}
	}

	/**
	 * try to restore the arena after a server crash
	 * @param game
	 * @return
	 */
	public boolean restoreArenaAfterServerCrash(Game game) {
		// method is called before initialization, so we
		this.game = game;

		// input file
		File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + game.getId() + ".save");
		if (file.exists()) {
			restoreArena();
			return true;
		}
		return false;
	}
}
