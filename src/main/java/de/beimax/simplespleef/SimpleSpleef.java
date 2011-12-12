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
package de.beimax.simplespleef;

import java.util.logging.Logger;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import de.beimax.simplespleef.admin.SimpleSpleefAdmin;
import de.beimax.simplespleef.command.SimpleSpleefCommandExecutor;
import de.beimax.simplespleef.game.GameHandler;
import de.beimax.simplespleef.listeners.*;
import de.beimax.simplespleef.util.ConfigHelper;
import de.beimax.simplespleef.util.Translator;
import de.beimax.simplespleef.util.UpdateChecker;

/**
 * SimpleSpleef for Bukkit
 * 
 * @author mkalus
 */
public class SimpleSpleef extends JavaPlugin {
	public static Logger log = Logger.getLogger("Minecraft");
	
	/**
	 * reference to game handler
	 */
	private GameHandler gameHandler;

	/**
	 * reference to admin class
	 */
	private SimpleSpleefAdmin admin;

	/**
	 * reference to command handler
	 */
	private SimpleSpleefCommandExecutor commandExecutor;

	/**
	 * reference to translator
	 */
	private Translator lang;

	/**
	 * reference to event handlers/listeners
	 */
	private SimpleSpleefBlockListener blockListener;
	private SimpleSpleefPlayerListener playerListener;
	private SimpleSpleefEntityListener entityListener;
	
	/**
	 * Called when enabling plugin
	 */
	public void onEnable() {
		// initialize plugin
		log.info(this.toString() + " is loading.");
		
		// configure plugin (configuration stuff)
		configurePlugin();
		
		// check updates, if turned on
		checkForUpdate();
		
		// create new handlers
		this.gameHandler = new GameHandler(this);
		this.admin = new SimpleSpleefAdmin(this);
		
		// add plugin listeners to listen to the registry of other plugins
		registerPluginListener();
		
		// add event listeners
		registerEvents();
	}

	/**
	 * Called when disabling plugin
	 */
	public void onDisable() {
		log.info(this.toString() + " is shutting down.");
		//TODO: clean memory

		//save config to disk
		this.saveConfig();
	}
	
	/**
	 * Run some configuration stuff at the initialization of the plugin
	 */
	protected void configurePlugin() {
		// define that default config should be copied
		this.getConfig().options().copyDefaults(true);
		
		// create config helper
		ConfigHelper configHelper = new ConfigHelper(this);
		// update sample config, if needed
		configHelper.updateSampleConfig();
		// update language files
		configHelper.updateLanguageFiles();
		
		// initialize the translator
		lang = new Translator(this, this.getConfig().getString("language", "en"));
	}

	protected void checkForUpdate() {
		if (!this.getConfig().getBoolean("settings.updateNotificationOnStart", true)) return;
		
		UpdateChecker checker = new UpdateChecker();
		try {
			if (checker.checkForUpdate(this.getDescription().getVersion()))
				log.info("[SimpleSpleef] Update found for SimpleSpleef - please go to http://dev.bukkit.org/server-mods/simple-spleef/ to download newest version!");
		} catch (Exception e) {
			log.warning("[SimpleSpleef] Could not connect to remote server to check for update. Exception said: " + e.getMessage());
		}
	}

	/**
	 * adds a plugin listener to listen to the registry of other plugins
	 */
	protected void registerPluginListener() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Configure event listeners
	 */
	protected void registerEvents() {
		// let my command handler take care of commands
		this.commandExecutor = new SimpleSpleefCommandExecutor(this.gameHandler);
		this.getCommand("spleef").setExecutor(commandExecutor);

		// Prepare listeners
		PluginManager pm = getServer().getPluginManager();
		this.blockListener = new SimpleSpleefBlockListener(this.gameHandler);
		this.entityListener = new SimpleSpleefEntityListener(this.gameHandler);
		this.playerListener = new SimpleSpleefPlayerListener(this.gameHandler);
		
		// Register our events
		pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.High, this);
		
		pm.registerEvent(Type.ENTITY_DEATH, entityListener, Priority.Low, this);
		
		pm.registerEvent(Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_KICK, playerListener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
	}

	/**
	 * @return the gameHandler
	 */
	public GameHandler getGameHandler() {
		return gameHandler;
	}

	/**
	 * @return the admin
	 */
	public SimpleSpleefAdmin getAdminClass() {
		return admin;
	}

	/**
	 * @return the commandExecutor
	 */
	public SimpleSpleefCommandExecutor getCommandExecutor() {
		return commandExecutor;
	}

	/**
	 * @return the blockListener
	 */
	public SimpleSpleefBlockListener getBlockListener() {
		return blockListener;
	}

	/**
	 * @return the playerListener
	 */
	public SimpleSpleefPlayerListener getPlayerListener() {
		return playerListener;
	}

	/**
	 * @return the entityListener
	 */
	public SimpleSpleefEntityListener getEntityListener() {
		return entityListener;
	}

	/**
	 * @param key of translation file
	 * @param replacers an even number of key/value pairs to replace key entries
	 * @return translated string
	 */
	public String ll(String key, String... replacers) {
		return lang.ll(key, replacers);
	}
}
