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

import java.util.Map;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditAPI;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import de.beimax.simplespleef.admin.SimpleSpleefAdmin;
import de.beimax.simplespleef.command.SimpleSpleefCommandExecutor;
import de.beimax.simplespleef.game.GameHandler;
import de.beimax.simplespleef.game.OriginalPositionKeeper;
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
	public static final Logger log = Logger.getLogger("Minecraft");
	
	/**
	 * reference to Vault economy
	 */
	public static Economy economy = null;
	
	/**
	 * reference to Vault permissions
	 */
	public static Permission permission = null;
	
	/**
	 * checks permission, either Vault-based or using builtin system
	 * @param sender
	 * @return
	 */
	public static boolean checkPermission(CommandSender sender, String permission) {
		if (SimpleSpleef.permission != null) { // use Vault to check permissions
			return SimpleSpleef.permission.has(sender, permission);
		}
		// fallback to default Bukkit permission checking system
		return sender.hasPermission(permission) || sender.hasPermission("simplespleef.*");
	}
	
	/**
	 * self reference to singleton
	 */
	private static SimpleSpleef simpleSpleef;
	
	/**
	 * get instance
	 * @return
	 */
	public static SimpleSpleef getPlugin() {
		return simpleSpleef;
	}

	/**
	 * reference to game handler
	 */
	private static GameHandler gameHandler;

	/**
	 * @return the gameHandler
	 */
	public static GameHandler getGameHandler() {
		return gameHandler;
	}

	/**
	 * world edit API
	 */
	private static WorldEditAPI worldEditAPI = null;

	/**
	 * sets the world edit API for this plugin
	 * @param worldEditAPI
	 */
	public static void setWorldEditAPI(WorldEditAPI worldEditAPI) {
		SimpleSpleef.worldEditAPI = worldEditAPI;
	}
	
	/**
	 * return WorldEditAPI
	 * @return
	 */
	public static WorldEditAPI getWorldEditAPI() {
		return SimpleSpleef.worldEditAPI;
	}
	
	/**
	 * keeper of original positions
	 */
	private static OriginalPositionKeeper originalPositionKeeper;
	
	/**
	 * get originalPositionKeeper instance (singleton)
	 * @return
	 */
	public static OriginalPositionKeeper getOriginalPositionKeeper() {
		if (originalPositionKeeper == null) originalPositionKeeper = new OriginalPositionKeeper();
		return originalPositionKeeper;
	}

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
		
		// set reference
		SimpleSpleef.simpleSpleef = this;

		// configure plugin (configuration stuff)
		configurePlugin();
		
		// check updates, if turned on
		checkForUpdate();
		
		// create new handlers
		SimpleSpleef.gameHandler = new GameHandler();
		this.admin = new SimpleSpleefAdmin();
		
		// register vault stuff
		setupEconomy();
		setupPermission();
		
		// add event listeners
		registerEvents();
		
		// check for WorldEdit 
		checkForWorldEdit();
	}

	/**
	 * Called when disabling plugin
	 */
	public void onDisable() {
		log.info(this.toString() + " is shutting down.");
		// clean memory
		SimpleSpleef.worldEditAPI = null;
		SimpleSpleef.gameHandler = null;
		SimpleSpleef.economy = null;
		SimpleSpleef.originalPositionKeeper = null;
		this.admin = null;
		this.lang = null;
		this.playerListener = null;
		this.entityListener = null;
		this.blockListener = null;
		this.commandExecutor = null;
		//TODO add if more stuff comes along

		//save config to disk
		this.saveConfig();
		
		// derefer self reference
		SimpleSpleef.simpleSpleef = null;
	}
	
	/**
	 * Run some configuration stuff at the initialization of the plugin
	 */
	public void configurePlugin() {
		// define that default config should be copied
		this.getConfig().options().copyDefaults(true);

		// create config helper
		ConfigHelper configHelper = new ConfigHelper();
		// update sample config, if needed
		configHelper.updateSampleConfig();
		// update language files
		configHelper.updateLanguageFiles();
		
		// initialize the translator
		lang = new Translator(this, this.getConfig().getString("language", "en"));
	}
	
	/**
	 * reload configuration and translation
	 */
	public void reloadSimpleSpleefConfiguration() {
		// reload the config file
		this.reloadConfig();

		// re-initialize the translator
		lang = new Translator(this, this.getConfig().getString("language", "en"));
	}

	/**
	 * Check for updates
	 */
	protected void checkForUpdate() {
		// create update checker
		final UpdateChecker checker = new UpdateChecker();
		
		// possibly check for updates in the internet on startup
		if (this.getConfig().getBoolean("settings.updateNotificationOnStart", true)) {
			(new Thread() { // create a new anonymous thread that will check the version asyncronously
				@Override
				public void run() {
					try {
						String newVersion = checker.checkForUpdate(SimpleSpleef.simpleSpleef.getDescription().getVersion());
						if (newVersion != null)
							log.info("[SimpleSpleef] Update found for SimpleSpleef - please go to http://dev.bukkit.org/server-mods/simple-spleef/ to download version " + newVersion + "!");
					} catch (Exception e) {
						log.warning("[SimpleSpleef] Could not connect to remote server to check for update. Exception said: " + e.getMessage());
					}
				}
			}).start();
		}
		
		// also check for updates in the configuration files and update them, if needed
		checker.updateConfigurationVersion(this);
	}

	/**
	 * Configure event listeners
	 */
	protected void registerEvents() {
		// add listener for other plugins
		PluginListener pluginListener = new PluginListener();
		
		// let my command handler take care of commands
		this.commandExecutor = new SimpleSpleefCommandExecutor();
		this.getCommand("spleef").setExecutor(commandExecutor);

		// Prepare listeners
		PluginManager pm = getServer().getPluginManager();
		this.blockListener = new SimpleSpleefBlockListener();
		this.entityListener = new SimpleSpleefEntityListener();
		this.playerListener = new SimpleSpleefPlayerListener();
		
		// Register our events
		pm.registerEvent(Type.PLUGIN_ENABLE, pluginListener, Priority.Normal, this);
		
		pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Low, this);
		pm.registerEvent(Type.BLOCK_PLACE, blockListener, Priority.Low, this);
		
		pm.registerEvent(Type.ENTITY_DEATH, entityListener, Priority.Highest, this);
		pm.registerEvent(Type.FOOD_LEVEL_CHANGE, entityListener, Priority.Highest, this);
		
		pm.registerEvent(Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_KICK, playerListener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Low, this);
		pm.registerEvent(Type.PLAYER_TELEPORT, playerListener, Priority.Lowest, this);
		pm.registerEvent(Type.PLAYER_GAME_MODE_CHANGE, playerListener, Priority.Lowest, this);
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
	
	/**
	 * Get translation section list
	 * @param key
	 * @return
	 */
	public Map<String, String> lls(String section) {
		return lang.lls(section);
	}

	/**
	 * set up economy - see http://dev.bukkit.org/server-mods/vault/
	 * @return
	 */
	private boolean setupEconomy() {
		if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Economy> economyProvider = getServer()
					.getServicesManager().getRegistration(
							net.milkbowl.vault.economy.Economy.class);
			if (economyProvider != null) {
				economy = economyProvider.getProvider();
			}
	
			SimpleSpleef.log.info("[SimpleSpleef] Vault hooked as economy plugin.");
			return (economy != null);
		}
		economy = null; // if the plugin is reloaded during play, possibly kill economy
		SimpleSpleef.log.info("[SimpleSpleef] Vault plugin not found - not using economy.");
		return false;
	}
	
	/**
	 * set up permissions - see http://dev.bukkit.org/server-mods/vault/
	 * @return
	 */
	private boolean setupPermission() {
		if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
	        if (permissionProvider != null) {
	            permission = permissionProvider.getProvider();
	        }
	
			SimpleSpleef.log.info("[SimpleSpleef] Vault hooked as permission plugin.");
	        return (permission != null);
		}
		permission = null; // if the plugin is reloaded during play, possibly kill permissions
		SimpleSpleef.log.info("[SimpleSpleef] Vault plugin not found - defaulting to Bukkit permission system.");
		return false;
	}
	
	/** 
	 * make sure worldEdit is loaded - inspired by MultiversePortals
	 */
	private void checkForWorldEdit() {
		 if (this.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
			 SimpleSpleef.setWorldEditAPI(new WorldEditAPI((WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit")));
				SimpleSpleef.log.info("[SimpleSpleef] Found WorldEdit. Using it for selections.");
		 }
	}
}