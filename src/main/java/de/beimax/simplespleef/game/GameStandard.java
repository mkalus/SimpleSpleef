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
package de.beimax.simplespleef.game;

import java.util.*;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.arenarestoring.ArenaRestorer;
import de.beimax.simplespleef.game.arenarestoring.HardArenaRestorer;
import de.beimax.simplespleef.game.arenarestoring.SoftRestorer;
import de.beimax.simplespleef.game.floortracking.FloorWorker;
import de.beimax.simplespleef.game.floortracking.FloorTracker;
import de.beimax.simplespleef.util.*;

/**
 * @author mkalus
 * Simple Game implementation
 */
public class GameStandard extends Game {
	/**
	 * static reference to random generator
	 */
	private static Random generator = new Random();
	
	/**
	 * Reference to spleefer list
	 */
	protected SpleeferList spleefers;
	
	/**
	 * Reference to spectators
	 */
	protected LinkedList<Player> spectators;
	
	/**
	 * Reference to configuration
	 */
	protected ConfigurationSection configuration;
	
	/**
	 * to make game faster:
	 */
	protected Set<Material> loseOnTouchMaterial;

	/**
	 * private countdown class
	 */
	private Countdown countdown;
	
	/**
	 * Id of the countdown task
	 */
	private int countdownId;
	
	/**
	 * list of players which may be teleported - used by teleportPlayer and playerMayTeleport
	 */
	private Set<Player> teleportOkList;
	
	/**
	 * arena cuboid
	 */
	private Cuboid arena;

	/**
	 * floor cuboid
	 */
	private Cuboid floor;

	/**
	 * lose cuboid
	 */
	private Cuboid lose;
	
	/**
	 * set to false, if disallowDigBlocks has been chosen in config
	 */
	private boolean allowDigBlocks = true;
	
	/**
	 * inventory keeper for this game
	 */
	protected InventoryKeeper inventoryKeeper;
	
	/**
	 * block degenerator that keeps track of players standing on something, if needed
	 */
	protected PlayerOnBlockDegenerator playerOnBlockDegenerator;
	
	/**
	 * floor tracker - takes care of floor changes
	 */
	protected FloorTracker floorTracker;
	
	/**
	 * arena restorer for this game
	 */
	protected ArenaRestorer arenaRestorer;
	
	/**
	 * shortcuts for digging settings
	 */
	protected static final int DIGGING_NONE = 0;
	protected static final int DIGGING_EVERYWHERE = 1;
	protected static final int DIGGING_FLOOR_ONLY = 2;
	protected static final int DIGGING_IN_ARENA = 3;
	protected static final int DIGGING_OUTSIDE_ARENA = 4;
	
	private int diggingIfArenaUndefined;
	private int diggingIfFloorUndefined;
	
	/**
	 * blocks that can be dug or (if allowDigBlocks is false) cannot
	 */
	private LinkedList<ItemStack> digBlocks;

	/**
	 * Constructor
	 * @param gameHandler
	 * @param name
	 */
	public GameStandard(String name) {
		super(name);
		this.spleefers = new SpleeferList();
		this.spectators = new LinkedList<Player>();
		this.countdown = null;
		this.teleportOkList = new HashSet<Player>();
	}

	@Override
	public String getName() {
		// get name from config
		String name = configuration.getString("name");
		if (name != null) return name;
		// otherwise return non-fancy name
		return super.getName();
	}

	@Override
	public String getType() {
		return "standard";
	}

	@Override
	public void defineSettings(ConfigurationSection conf) {
		this.configuration = conf;
		// define defaults/shortcuts
		if (conf.getBoolean("loseOnTouchBlocks", true)) {
			// is loseBlocks a valid list?
			if (conf.isList("loseBlocks")) { //yes
				this.loseOnTouchMaterial = new HashSet<Material>();
				for (String material : conf.getStringList("loseBlocks")) {
					Material m = Material.getMaterial(material);
					// check existence of material
					if (m == null) SimpleSpleef.log.warning("[SimpleSpleef] " + getId() + " configuration warning! Unknown Material in loseBlocks: " + material);
					else this.loseOnTouchMaterial.add(m);
				}
				if (this.loseOnTouchMaterial.size() == 0) this.loseOnTouchMaterial = null; //no
			} else this.loseOnTouchMaterial = null; //no
		} else this.loseOnTouchMaterial = null; // reset
		// define arena, floor and lose cuboids
		arena = SimpleSpleef.getGameHandler().configToCuboid(getId(), "arena");
		floor = SimpleSpleef.getGameHandler().configToCuboid(getId(), "floor");
		lose = SimpleSpleef.getGameHandler().configToCuboid(getId(), "lose");
		// block destruction/keep hashes
		if (conf.isList("allowDigBlocks")) {
			allowDigBlocks = true;
			digBlocks = new LinkedList<ItemStack>();
			for (String line : conf.getStringList("allowDigBlocks")) {
				digBlocks.add(MaterialHelper.getItemStackFromString(line, true));
			}
		} else if (conf.isList("disallowDigBlocks")) {
			allowDigBlocks = false;
			digBlocks = new LinkedList<ItemStack>();
			for (String line : conf.getStringList("disallowDigBlocks")) {
				digBlocks.add(MaterialHelper.getItemStackFromString(line, true));
			}
		} else digBlocks = null; // delete previous settings

		// get dig settings
		String dig = conf.getString("diggingIfArenaUndefined", "floorOnly").toLowerCase();
		if (dig.equals("none")) diggingIfArenaUndefined = GameStandard.DIGGING_NONE;
		else if (dig.equals("everywhere")) diggingIfArenaUndefined = GameStandard.DIGGING_EVERYWHERE;
		else if (floor == null) diggingIfArenaUndefined = GameStandard.DIGGING_EVERYWHERE;
		else diggingIfArenaUndefined = GameStandard.DIGGING_FLOOR_ONLY;
		
		dig = conf.getString("diggingIfFloorUndefined", "inArena").toLowerCase();
		if (dig.equals("none")) diggingIfFloorUndefined = GameStandard.DIGGING_NONE;
		else if (dig.equals("everywhere")) diggingIfFloorUndefined = GameStandard.DIGGING_EVERYWHERE;
		else if (arena == null) diggingIfFloorUndefined = GameStandard.DIGGING_EVERYWHERE;
		else if (dig.equals("outsidearena")) diggingIfFloorUndefined = GameStandard.DIGGING_OUTSIDE_ARENA;
		else diggingIfFloorUndefined = GameStandard.DIGGING_IN_ARENA;
		
		// initialize inventory keeper if needed
		if (configuration.getBoolean("clearInventory", false))
			this.inventoryKeeper = new InventoryKeeper();
		else this.inventoryKeeper = null;
		
		// floor changes might be registered by the floor tracker
		renewFloorTracker();
		
		// block degeneration
		renewPlayerOnBlockGenerator();

		//TODO: more definitions/shortcuts
	}
	
	/**
	 * renew the floor tracker
	 */
	protected void renewFloorTracker() {
		if (floor != null) {
			int arenaFloorDissolvesAfter = configuration.getInt("arenaFloorDissolvesAfter", -1);
			int arenaFloorRepairsAfter = configuration.getInt("arenaFloorRepairsAfter", -1);
			if (arenaFloorDissolvesAfter > -1 || arenaFloorRepairsAfter > -1) {
				floorTracker = new FloorTracker();
				floorTracker.setArenaFloorDissolvesAfter(arenaFloorDissolvesAfter);
				floorTracker.setArenaFloorRepairsAfter(arenaFloorRepairsAfter);
				floorTracker.setArenaFloorDissolveTick(configuration.getInt("arenaFloorDissolveTick", 5));
				floorTracker.setArenaFloorRepairTick(configuration.getInt("arenaFloorRepairTick", 10));
			} else
				floorTracker = null; // none is needed to track floor
		} else floorTracker = null; // no floor, no floor tracker!		
	}

	/**
	 * renew the block degenerator for players
	 */
	protected void renewPlayerOnBlockGenerator() {
		if (configuration == null) playerOnBlockDegenerator = null; // fix NPE
		
		int blockDegeneration = configuration .getInt("blockDegeneration", -1);
		if (blockDegeneration >= 0) playerOnBlockDegenerator = new PlayerOnBlockDegenerator(blockDegeneration, configuration.getStringList("degeneratingBlocks"), floorTracker);
		else playerOnBlockDegenerator = null;		
	}
	
	@Override
	public boolean join(Player player) {
		//check joinable status
		if (!isJoinable()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.join", "[ARENA]", getName()));
			return false;
		}
		// max number of players?
		int maximumPlayers = configuration.getInt("maximumPlayers", 0);
		if (spleefers == null) spleefers = new SpleeferList(); //restore spleefers list if it was null for some reason - strange that this would happen...
		if (maximumPlayers > 0 && spleefers.size() >= maximumPlayers) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.joinMax", "[ARENA]", getName(), "[NUMBER]", String.valueOf(maximumPlayers)));
			return false;
		}
		// already joined this game? => is caught by GameHandler, so we do not check this here...
		// check funds of player...
		if (SimpleSpleef.economy != null) {
			double entryFee = configuration.getDouble("entryFee", 0.0);
			if (entryFee > 0.0) {
				EconomyResponse response = SimpleSpleef.economy.withdrawPlayer(player.getName(), entryFee);
				if (response.type == EconomyResponse.ResponseType.SUCCESS) { // ok, tell the player about the amount charged
					player.sendMessage(SimpleSpleef.getPlugin().ll("feedback.joinFee", "[AMOUNT]", SimpleSpleef.economy.format(entryFee)));
				} else { //insufficient funds
					player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.joinFee", "[AMOUNT]", SimpleSpleef.economy.format(entryFee)));
					return false;
				}
				// ok, is a part of the fee paid to a player?
				String playerNameGettingFees = configuration.getString("entryFeeGoesToPlayer", "none");
				if (playerNameGettingFees != null && !playerNameGettingFees.equalsIgnoreCase("none") &&  SimpleSpleef.economy.hasAccount(playerNameGettingFees))
					SimpleSpleef.economy.depositPlayer(playerNameGettingFees, configuration.getDouble("entryFeeAmountToPlayer", entryFee));
			}
		}
		// check gamemode and change it if needed
		if (player.getGameMode() != GameMode.SURVIVAL) {
			player.setGameMode(GameMode.SURVIVAL);
			player.sendMessage(ChatColor.YELLOW + SimpleSpleef.getPlugin().ll("feedback.gamemodeChanged"));
		}
		if (!spleefers.addSpleefer(player)) { // some weird error
			player.sendMessage(ChatColor.DARK_RED + "Internal error while joining occured! Please tell the SimpleSpleef creator!");
			return false;
		}
		// unready game, if needed
		if (supportsReady()) {
			this.status = Game.STATUS_NEW;
		}
		// remove from spectators, if needed
		unwatch(player);
		// inform/broadcast join is done by the game handler
		// remember player's last position
		if (configuration.getBoolean("enableBackCommand", true))
			SimpleSpleef.getOriginalPositionKeeper().keepPosition(player);
		// teleport player to lounge
		teleportPlayer(player, "lounge");
		return true;
	}

	@Override
	public boolean leave(Player player) {
		// is the player a spectator?
		if (spectators != null && spectators.contains(player)) {
			return back(player); // redirect to back command instead
		}
		// check, if player is not a spleefer
		if (!spleefers.hasSpleefer(player)) {
			player.sendMessage(ChatColor.DARK_RED + "Internal error while leave occured! Please tell the SimpleSpleef creator!");
			return false;
		}
		// inform player
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("feedback.leave"));
		// broadcast message of somebody loosing
		String broadcastMessage = ChatColor.DARK_PURPLE + SimpleSpleef.getPlugin().ll("broadcasts.leave", "[PLAYER]", player.getDisplayName(), "[ARENA]", getName());
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceJoin", true)) {
			SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
		} else {
			// send message to all receivers
			sendMessage(broadcastMessage, player);
		}
		// if degeneration keeper is on, delete player from list
		if (playerOnBlockDegenerator != null) playerOnBlockDegenerator.removePlayer(player);
		// teleport him/her back to original position, if supported
		if (configuration.getBoolean("enableBackCommand", true)) {
			// get original position
			Location originalLocation = SimpleSpleef.getOriginalPositionKeeper().getOriginalPosition(player);
			if (originalLocation == null) { // no position
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.backNoLocation"));
				return false;
			}
			// add player to teleport ok list
			this.teleportOkList.add(player);
			// teleport player to original position
			player.teleport(originalLocation);
			player.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("feedback.back"));
		}
		// check game status
		if (isJoinable() || isReady()) { //still joinable or ready state - not so bad!
			// just remove spleefer
			spleefers.removeSpleefer(player);
		} else if (countdown != null) { // during countdown - end the game...
			// set player to lost, so that the player is not teleported twice
			spleefers.setLost(player);
			endGame(); // actually end the game
		} else { // game is in progress - player loses - simple as that
			// player loses
			playerLoses(player, false); // do not teleport leaving players...
		}
		return true;
	}

	@Override
	public boolean team(Player player, String team) {
		// no team games possible in this arena
		player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.teamNotPossible", "[ARENA]", getName()));
		return false;
	}

	@Override
	public boolean ready(Player player, boolean hitBlock) {
		// game started already?
		if (isInProgress() || countdown != null) { // avoid possible memory leak
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.readyAlreadyStarted", "[ARENA]", getName()));
			return false;
		}
		// readying is not used in this game
		if (!supportsReady()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.readyNotUsed", "[ARENA]", getName()));
			return false;
		}
		// right command?
		if (!hitBlock && !supportsCommandReady()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.readyBlock", "[ARENA]", getName()));
			return false;
		}
		// get spleefer
		Spleefer spleefer = spleefers.getSpleefer(player);
		if (spleefer == null) { // internal error
			player.sendMessage(ChatColor.DARK_RED + "Internal error: Player " + player.getName() + " should be in spleefers list, but isn't!");
			return false;
		}
		// player already ready?
		if (spleefer.isReady()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.readyAlready", "[ARENA]", getName()));
			return false;
		}
		// ok, ready player now
		spleefer.setReady(true);
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("feedback.ready", "[ARENA]", getName(), "[PLAYER]", player.getDisplayName()));
		// broadcast message of somebody readying
		String broadcastMessage = ChatColor.DARK_PURPLE + SimpleSpleef.getPlugin().ll("broadcasts.ready", "[PLAYER]", player.getDisplayName(), "[ARENA]", getName());
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceReady", false)) {
			SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
		} else {
			// send message to all receivers
			sendMessage(broadcastMessage, player);
		}
		// is the game ready?
		checkReadyAndStartGame();
		return true;
	}

	/**
	 * check the readiness status of a game and possibly start it
	 */
	protected void checkReadyAndStartGame() {
		if (spleefers.countUnreadyPlayers() == 0) {
			// autostart game once all are ready
			if (configuration.getBoolean("readyAutoStart", false)) {
				// test for minumum players
				int minimumPlayers = configuration.getInt("minimumPlayers", 0);
				if (minimumPlayers > 0 && spleefers.size() < minimumPlayers) {
					// wait for more players to join and ready
					sendMessage(SimpleSpleef.getPlugin().ll("broadcasts.ready", "[NUMBER]", String.valueOf(spleefers.size() - minimumPlayers)), false);
					this.status = Game.STATUS_READY;
				}
				else startGameOrCountdown(); // start game right away
			}
			// otherwise, just set game as ready
			else this.status = Game.STATUS_READY;
		}
	}

	@Override
	public boolean countdown(CommandSender sender) {
		// game started already?
		if (isInProgress() || countdown != null) { // avoid possible memory leak
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.startDouble", "[ARENA]", getName()));
			return false;
		}
		// minimum number of players?
		int minimumPlayers = configuration.getInt("minimumPlayers", 0);
		if (minimumPlayers > 0 && spleefers.size() < minimumPlayers) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.startMin", "[ARENA]", getName(), "[NUMBER]", String.valueOf(minimumPlayers)));
			return false;
		}
		// game is not ready yet?
		if (!isReady()) {
			//get list of unready spleefers
			String unreadyList = getListOfUnreadySpleefers();
			if (unreadyList == null) unreadyList = "---"; // in any case...
			// send error message to player
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.startNotReady", "[ARENA]", getName(), "[PLAYERS]", unreadyList));
			return false;
		}
		// start game or countdown
		startGameOrCountdown();
		
		return true;
	}
	
	/**
	 * helper to start game or countdown - used by ready and countdown methods
	 */
	private void startGameOrCountdown() {
		// teleport players to arena
		teleportPlayersAtGameStart();
		// start countdown, if setting is 0 or higher
		if (configuration.getInt("countdownFrom", 10) == 0) {
			start(); // if countdown is null, start game right away
		} else {
			// create countdown and start it
			countdown = new Countdown();
			countdownId = SimpleSpleef.getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(SimpleSpleef.getPlugin(), countdown, 0L, 20L);
		}
	}

	@Override
	public boolean start() {
		// delete countdown
		deleteCountdown();
		// change game status
		status = STATUS_STARTED;
		// save arena information
		saveArena();
		// start floor tracker, if needed
		if (floorTracker != null)
			floorTracker.startTracking(this, floor);
		// start degenerator if needed
		if (playerOnBlockDegenerator != null)
			playerOnBlockDegenerator.startBlockDegenerator();
		// possibly clear inventory
		clearInventories();
		// optionally add to inventory
		addToInventories();
		// and/or give shovels
		addShovelItems();
		return true;
	}
	
	@Override
	public boolean stop(Player player) {
		// game is not in progress!
		if (!isInProgress()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.stop", "[ARENA]", getName()));
			return false;
		}
		// actually end the game
		if (!endGame()) return false;
		// send message
		sendMessage(SimpleSpleef.getPlugin().ll("feedback.stop", "[ARENA]", getName(), "[PLAYER]", player.getDisplayName()),
				SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceStop", true));
		return true;
	}
	

	@Override
	public boolean delete(CommandSender sender) {
		// end the game first, if game is started
		if (!endGame()) return false;
		// send message
		sendMessage(SimpleSpleef.getPlugin().ll("feedback.delete", "[ARENA]", getName(), "[PLAYER]", sender.getName()),
				SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceStop", true));
		// call the game handler to tell it that the game is over - only if game status has been reset
		if (status == STATUS_NEW) SimpleSpleef.getGameHandler().gameOver(this);
		return true;
	}

	/**
	 * Do end game maintenance
	 * @return
	 */
	protected boolean endGame() {
		//check what the current status is and end game then
		boolean wasInProgress = isInProgress(); 
		//isJoinable()
		//isReady()
		// + other cases?

		// still in countdown? if yes, kill it!
		if (countdown != null)
			countdown.interrupted = true;

		// if floor tracker is on, delete it and renew it
		if (floorTracker != null)
			floorTracker.stopTracking();
		renewFloorTracker(); // renew the tracker

		// if degeneration keeper is on, delete and renew it
		if (playerOnBlockDegenerator != null)
			playerOnBlockDegenerator.stopBlockDegenerator();
		renewPlayerOnBlockGenerator(); // renew the degenerator

		// change game status
		status = STATUS_FINISHED;

		// only do this when game was in progress
		if (wasInProgress) {
			// possibly take away shovel items
			removeShovelItems();
			// possibly restore inventories
			restoreAllInventories();
			// teleport remaining players to lounge
			for (Spleefer spleefer : spleefers.get()) {
				if (!spleefer.hasLost())
					teleportPlayer(spleefer.getPlayer(), "lounge");
			}
			// restore arena
			restoreArena(); // this will also eventually reset the game status and call the game handler
		} else 	
			// change game status
			status = STATUS_NEW;
		return true;
	}

	@Override
	public boolean watch(Player player) {
		// check, if player is spleefer
		if (spleefers.hasSpleefer(player)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.watchSpleefer", "[ARENA]", getName()));
			return false;
		}
		// check, if player is in spectator list already
		if (spectators.contains(player)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.watchAlready", "[ARENA]", getName()));
			return false;
		}
		// check, if we have a spectator spawn defined
		if (!configuration.isConfigurationSection("spectatorSpawn") || !configuration.getBoolean("spectatorSpawn.enabled", false)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.watchNoSpawnDefined", "[ARENA]", getName()));
			return false;
		}
		// save spectator's original position
		if (configuration.getBoolean("enableBackCommand", true))
			SimpleSpleef.getOriginalPositionKeeper().keepPosition(player);
		// teleport spectator
		Location teleportTo = LocationHelper.configToExactLocation(configuration.getConfigurationSection("spectatorSpawn"));
		player.teleport(teleportTo);
		// add to spectator list
		spectators.add(player);
		// send message to player
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("feedback.watch", "[ARENA]", getName()));
		return true;
	}
	
	@Override
	public boolean back(Player player) {
		// not allowed
		if (!configuration.getBoolean("enableBackCommand", true)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.commandNotAllowed", "[ARENA]", getName()));
			return false;
		}
		// check, if player is spleefer
		if (spleefers.hasSpleefer(player)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.backSpleefer", "[ARENA]", getName()));
			return false;
		}
		// check, if player is not a spectator
		if (spectators == null || !spectators.contains(player)) {
			player.sendMessage(ChatColor.DARK_RED + "Internal error while back occured! Please tell the SimpleSpleef creator!");
			return false;
		}
		// get original position
		Location originalLocation = SimpleSpleef.getOriginalPositionKeeper().getOriginalPosition(player);
		if (originalLocation == null) { // no position
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.backNoLocation"));
			return false;
		}
		// remove player from watch list
		unwatch(player);
		// teleport player to original position
		player.teleport(originalLocation);
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("feedback.back"));
		return true;
	}
	
	/**
	 * Helper method to remove player from spectator list
	 * @param player
	 * @return
	 */
	protected boolean unwatch(Player player) {
		// check if player is in specator list
		if (!spectators.contains(player)) return false;
		// remove from spectator list
		spectators.remove(player);
		return true;
	}

	@Override
	public boolean hasPlayer(Player player) {
		if (spleefers == null || player == null) return false;
		return spleefers.hasSpleefer(player);
	}
	
	@Override
	public boolean hasSpectator(Player player) {
		if (spectators == null) return false;
		return spectators.contains(player);
	}

	/**
	 * only delete countdown
	 */
	protected void deleteCountdown() {
		countdown = null;
	}
	
	/**
	 * Send a message to broadcast, or to players and spectators
	 * @param message
	 * @param broadcast
	 */
	public void sendMessage(String message, boolean broadcast) {
		if (message == null) {
			SimpleSpleef.log.warning("[SimpleSpleef] Message was null and could not be broadcasted!");
			return;
		}
		// global broadcast
		if (broadcast) SimpleSpleef.getPlugin().getServer().broadcastMessage(message);
		else { // only players and specators
			// players
			for (Spleefer spleefer : spleefers.get()) {
				spleefer.getPlayer().sendMessage(message);
			}
			// spectators
			for (Player player : this.spectators) {
				player.sendMessage(message);
			}
			// send to console, too
			SimpleSpleef.log.info(message);
		}
	}
	
	@Override
	public String getNumberOfPlayers() {
		// no spleefers - return empty string
		if (spleefers == null || spleefers.size() == 0) return "";

		int active;
		int max;
		// shows information depending on state
		if (isInProgress()) {
			active = spleefers.inGame();
			max = spleefers.size();
		} else { // joinable -> show number of players joined, open places
			active = spleefers.size();
			max = configuration.getInt("maximumPlayers", 0);
		}
		return "(" + active + "/" + (max>0?max:"-") + ")";
	}

	@Override
	public String getListOfSpleefers() {
		// no spleefers - return null
		if (spleefers == null || spleefers.size() == 0) return null;
		// create list of spleefers
		String comma = SimpleSpleef.getPlugin().ll("feedback.infoComma");
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (Spleefer spleefer : spleefers.get()) {
			if (i > 0 && i == spleefers.size() - 1) builder.append(SimpleSpleef.getPlugin().ll("feedback.infoAnd")); // last element with end
			else if (i > 0) builder.append(comma);  // other elements with ,
			// lost or in game?
			if (spleefer.hasLost()) builder.append(ChatColor.RED);
			else builder.append(ChatColor.GREEN);
			builder.append(spleefer.getPlayer().getDisplayName());
			builder.append(ChatColor.GRAY);
			i++;
		}
		return builder.toString();
	}

	@Override
	public String getListOfSpectators() {
		// no spectators - return null
		if (spectators == null || spectators.size() == 0) return null;
		// create list of spectators
		String comma = SimpleSpleef.getPlugin().ll("feedback.infoComma");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < spectators.size(); i++) {
			if (i > 0 && i == spectators.size() - 1) builder.append(SimpleSpleef.getPlugin().ll("feedback.infoAnd")); // last element with end
			else if (i > 0) builder.append(comma);  // other elements with ,
			builder.append(spectators.get(i).getDisplayName());
		}
		return builder.toString();
	}
	
	@Override
	public String getListOfUnreadySpleefers() {
		// no spleefers - return null
		if (spleefers == null || spleefers.size() == 0) return null;
		// get unready spleefers
		LinkedList<Spleefer> list = new LinkedList<Spleefer>();
		for (Spleefer spleefer : spleefers.get()) {
			if (!spleefer.isReady()) list.add(spleefer);
		}
		// is the list empty?
		if (list.size() == 0) return null; // no unready spleefes
		// compile list
		return SpleeferList.getPrintablePlayerList(list);
	}

	/**
	 * Send a message to broadcast, or to players and spectators
	 * @param message
	 * @param exception exception - this player does not receive message
	 */
	public void sendMessage(String message, Player exception) {
		// players
		for (Spleefer spleefer : spleefers.get()) {
			if (exception != spleefer.getPlayer())
				spleefer.getPlayer().sendMessage(message);
		}
		// spectators
		for (Player player : this.spectators) {
			if (exception != player)
				player.sendMessage(message);
		}
		// send to console, too
		SimpleSpleef.log.info(message);
	}


	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (!isInGame() || spleefers.hasLost(player)) return; // if game is not in progress or player has lost, return
		
		//player touched certain block (setting loseOnTouchBlocks)
		if (loseOnTouchMaterial != null) {
			Material touchedBlock = event.getTo().getBlock().getType();
			Material onBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
			// what happened exactly?
			boolean lostByTouching = loseOnTouchMaterial.contains(touchedBlock);
			boolean lostByStandingOn = loseOnTouchMaterial.contains(onBlock);
			if (lostByTouching || lostByStandingOn) {
				// Block name
				String blockName;
				if (lostByTouching) blockName = touchedBlock.name();
				else blockName = onBlock.name();
				// translate block name
				String translatedBlockName = SimpleSpleef.getPlugin().ll("material." + blockName);
				if (translatedBlockName != null) blockName = translatedBlockName;
				// broadcast message of somebody loosing
				String broadcastMessage = ChatColor.GREEN + SimpleSpleef.getPlugin().ll("broadcasts.lostByTouching", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[MATERIAL]", blockName);
				if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLose", true)) {
					SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
				} else {
					// send message to all receivers
					sendMessage(broadcastMessage, player);
				}
				// Ha, lost!
				playerLoses(player, true);
				return;
			}
		}

		// check location within "lose" cuboid (setting lose)
		if (lose != null && lose.contains(player.getLocation())) {
			// broadcast message of somebody loosing
			String broadcastMessage = ChatColor.GREEN + SimpleSpleef.getPlugin().ll("broadcasts.lostByCuboid", "[PLAYER]", player.getName(), "[ARENA]", getName());
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLose", true)) {
				SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
			} else {
				// send message to all receivers
				sendMessage(broadcastMessage, player);
			}
			// Ha, lost!
			playerLoses(player, true);
			return;			
		}

		// check block degeneration
		if (playerOnBlockDegenerator != null && isInGame()) {
			// get block player is standing on
			Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
			if (block != null && checkMayBreakBlockLocation(block)) // only degenerate blocks that are within the arena/floor
				playerOnBlockDegenerator.updatePlayer(player);
		}
	}

	@Override
	public boolean playerMayTeleport(Player player) {
		// at the start of game, teleportation is ok
		if (isJoinable() && (spleefers == null || spleefers.size() == 0)) return true;
		// check if the player is on the teleport-ok-list, delete him/her from list and return true
		if (this.teleportOkList.contains(player)) {
			this.teleportOkList.remove(player);
			player.setFallDistance(0.0f); // set fall distance to 0 to negate damager
			return true;
		}
		// otherwise return preventTeleportingDuringGames for this arena
		return !configuration.getBoolean("preventTeleportingDuringGames", true);
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if (block == null || event.getPlayer() == null) return; // ignore null blocks and null players

		// check instant dig and block may be broken
		if (event.getAction() == Action.LEFT_CLICK_BLOCK && configuration.getBoolean("instantDig", true) && checkMayBreakBlock(block)) {
			// cancel event
			event.setCancelled(true);
			// get block data
			Block clickedBlock = event.getClickedBlock();
			int oldType = clickedBlock.getTypeId();
			byte oldData = clickedBlock.getData();
			// set block to air
			clickedBlock.setType(Material.AIR);
			clickedBlock.setData((byte) 0);
			// notify floor tracker
			updateTrackers(clickedBlock, oldType, oldData);
		} else
		//check if player clicked on a "ready" block (e.g. iron block) and the game is readyable
			if (supportsBlockReady() && isJoinable()) {
				ItemStack readyBlockMaterial;
				try {
					readyBlockMaterial = MaterialHelper.getItemStackFromString(configuration.getString("readyBlockMaterial", null), true);
				} catch (Exception e) {
					return; // ignore exceptions
				}
				if (readyBlockMaterial == null) return; // ignore null materials
				// material has been checked, now test, if clicked block is of the same material
				if (readyBlockMaterial.getTypeId() == block.getTypeId() && MaterialHelper.isSameBlockType(block, readyBlockMaterial)) {
					ready(event.getPlayer(), true);
				}
			}
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		// call helper method
		loseOnQuitOrKick(event.getPlayer());
	}

	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		// call helper method
		loseOnQuitOrKick(event.getPlayer());
		// delete original position, because player is banned anyhow
		SimpleSpleef.getOriginalPositionKeeper().deleteOriginalPosition(event.getPlayer());
	}
	
	/**
	 * helper method for onPlayerQuit and onPlayerKick
	 * @param player
	 */
	protected void loseOnQuitOrKick(Player player) {
		if (configuration.getBoolean("loseOnLogout", true)) {
			// broadcast message of somebody loosing
			String broadcastMessage = ChatColor.GREEN + SimpleSpleef.getPlugin().ll("broadcasts.lostByQuitting", "[PLAYER]", player.getName(), "[ARENA]", getName());
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLose", true)) {
				SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
			} else {
				// send message to all receivers
				sendMessage(broadcastMessage, player);
			}
			// player loses, if set to true
			playerLoses(player, false); // do not teleport dead players...
		} // else - do nothing...
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		// right now, do nothing... later on maybe give shovel back or so (if it has been taken away before the game ended - that gets complicated...)
	}

	@Override
	public void onPlayerDeath(Player player) {
		// delete original position, because player spawns somewhere else anyhow
		SimpleSpleef.getOriginalPositionKeeper().deleteOriginalPosition(player);
		if (configuration.getBoolean("loseOnDeath", true)) {
			// broadcast message of somebody loosing
			String broadcastMessage = ChatColor.GREEN + SimpleSpleef.getPlugin().ll("broadcasts.lostByDeath", "[PLAYER]", player.getName(), "[ARENA]", getName());
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLose", true)) {
				SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
			} else {
				// send message to all receivers
				sendMessage(broadcastMessage, player);
			}
			// player loses, if set to true
			playerLoses(player, false); // do not teleport dead players...
		} // else - do nothing...
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block == null) return; // sanity check
		// get block data
		int oldType = block.getTypeId();
		byte oldData = block.getData();
		boolean floorBroken = false;
		// may the block be broken?
		if (!checkMayBreakBlock(block)) {
			// cancel event
			event.setCancelled(true);
			// message to player
			event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.noDig"));
		} else if (!configuration.getBoolean("blockDropping", true)) { // otherwise: block dropping set to false => destroy blocks
			// cancel event - because we will handle the block destruction ourselves
			event.setCancelled(true);
			floorBroken = true;
			// set block to air
			block.setType(Material.AIR);
			block.setData((byte) 0);
		} else floorBroken = true; // block was broken

		// if there is a floor tracker running, tell it about the change
		if (floorTracker != null && floorBroken)
			updateTrackers(block, oldType, oldData);
	}
	
	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		// joined players may not place blocks as long as game has not started
		// also, if allowBlockPlacing is false, disallow block placing during game
		if (!isInGame() || !configuration.getBoolean("allowBlockPlacing", false)) {
			// cancel event
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.noPlacement"));
		}

		// if there is a floor tracker running, tell it about the change
		if (floorTracker != null && !event.isCancelled())
			updateTrackers(event.getBlock(), Material.AIR.getId(), (byte) 0);
	}

	/**
	 * called when player loses a game
	 * (broadcast message has to be sent by calling method)
	 * @param player
	 * @param teleport true, if player should be teleported to lose spawn, if possible
	 */
	protected void playerLoses(Player player, boolean teleport) {
		// set player to lost
		spleefers.setLost(player);
		// if degeneration keeper is on, delete player from list
		if (playerOnBlockDegenerator != null) playerOnBlockDegenerator.removePlayer(player);
		// message to player
		player.sendMessage(ChatColor.RED + SimpleSpleef.getPlugin().ll("feedback.lost"));
		// broadcast message has to be sent by calling method
		// shovel lost, too
		removeShovelItem(player, true);
		// restore inventory
		restoreInventory(player);
		// teleport player to lose spawn
		if (teleport) teleportPlayer(player, "lose");
		// determine if game is over...
		if (checkGameOver()) gameOver();
	}
	
	/**
	 * checks whether the game is over
	 * @return true for game over conditions are all met
	 */
	protected boolean checkGameOver() {
		// check for number of players that have to remain on the field to tell it a win
		if (spleefers.inGame() <= configuration.getInt("remainingPlayersWin", 1)) return true;
		return false;
	}
	
	/**
	 * called when game is over - get winners, etc.
	 */
	protected void gameOver() {
		// possibly restore inventories
		restoreAllInventories(); // done before paying prizes...

		// determine winners
		LinkedList<Spleefer> winners = new LinkedList<Spleefer>();
		for (Spleefer spleefer : spleefers.get()) {
			Player player = spleefer.getPlayer();
			if (!spleefer.hasLost()) { // not lost?
				//this guy is a winner - send a message
				player.sendMessage(ChatColor.DARK_GREEN + SimpleSpleef.getPlugin().ll("feedback.won"));
				winners.add(spleefer); // aggregate the winners to broadcast them later on
				// pay prizes
				payPrizeMoney(player);
				payPrizeExperience(player);
				payPrizeItems(player);
				// teleport winners back to winner's point or to lounge
				if (configuration.isConfigurationSection("winnerSpawn") && configuration.getBoolean("winnerSpawn.enabled", false))
					teleportPlayer(player, "winner");
				else teleportPlayer(player, "lounge");
			}
			// update original positions
			if (configuration.getBoolean("enableBackCommand", true))
				SimpleSpleef.getOriginalPositionKeeper().updateOriginalLocationTimestamp(player);
		}
		broadcastWinners(winners);
		
		// clean up game and end it
		endGame();
		// call the game handler to tell it that the game is over - only if game status has been reset
		if (status == STATUS_NEW) SimpleSpleef.getGameHandler().gameOver(this);
	}
	
	/**
	 * broadcast the winners
	 * @param winners
	 */
	protected void broadcastWinners(LinkedList<Spleefer> winners) {
		// get the total winners from winners list
		String broadcastKey;
		String replacePlayer = "";
		// no winner?
		if (winners.size() == 0) broadcastKey = "None";
		else if (winners.size() == 1) { // one winner?
			broadcastKey = "One";
			replacePlayer = winners.getFirst().getPlayer().getDisplayName();
		} else { // multiple winners
			broadcastKey = "Multi";
			// build list of winners
			replacePlayer = SpleeferList.getPrintablePlayerList(winners);
		}
		// broadcast message
		String broadcastMessage = ChatColor.GOLD + SimpleSpleef.getPlugin().ll("broadcasts.win" + broadcastKey, "[PLAYER]", replacePlayer, "[ARENA]", getName());
		sendMessage(broadcastMessage, SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceWin", true));		
	}

	/**
	 * Pay a prize in money
	 * @param player
	 */
	protected void payPrizeMoney(Player player) {
		// get prizes
		double prizeMoneyFixed = configuration.getDouble("prizeMoneyFixed", 0.0);
		double prizeMoneyPerPlayer = configuration.getDouble("prizeMoneyPerPlayer", 5.0);
		double win = prizeMoneyFixed + prizeMoneyPerPlayer * spleefers.size();
		if (win == 0) return; // if no prize money is payed, return without telling anybody
		if (SimpleSpleef.economy != null) {
			String formated = SimpleSpleef.economy.format(win);
			// give money to player
			SimpleSpleef.economy.depositPlayer(player.getName(), win);
			// player gets message
			player.sendMessage(ChatColor.AQUA + SimpleSpleef.getPlugin().ll("feedback.prizeMoney", "[ARENA]", getName(), "[MONEY]", formated));
			// broadcast prize?
			String broadcastMessage = ChatColor.AQUA + SimpleSpleef.getPlugin().ll("broadcasts.prizeMoney", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[MONEY]", formated);
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announcePrize", true)) {
				SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
			} else {
				sendMessage(broadcastMessage, player); // send message to all receivers
			}
		}
	}
	
	/**
	 * Pay a prize in experience
	 * @param player
	 */
	protected void payPrizeExperience(Player player) {
		// get configuration and look, if we actually add experience
		int prizeExperienceFixed = configuration.getInt("prizeExperienceFixed", 0);
		int prizeExperiencePerPlayer = configuration.getInt("prizeExperiencePerPlayer", 0);
		int win = prizeExperienceFixed + prizeExperiencePerPlayer * spleefers.size();
		if (win == 0) return; // if no prize xps is payed, return without telling anybody
		// add player experience
		player.giveExp(win);
		// player gets message
		player.sendMessage(ChatColor.AQUA + SimpleSpleef.getPlugin().ll("broadcasts.prizeExperience", "[ARENA]", getName(), "[EXPERIENCE]", String.valueOf(win)));
		// broadcast prize?
		String broadcastMessage = ChatColor.AQUA + SimpleSpleef.getPlugin().ll("broadcasts.prizeExperience", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[EXPERIENCE]", String.valueOf(win));
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announcePrize", true)) {
			SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
		} else {
			sendMessage(broadcastMessage, player); // send message to all receivers
		}
	}

	/**
	 * Pay a prize in items
	 * @param player
	 */
	protected void payPrizeItems(Player player) {
		if (!configuration.getBoolean("giveItemPrizes", true)) return; // disabled?
		if (!configuration.isList("itemPrizes")) { // check if this is a list
			SimpleSpleef.log.warning("[SimpleSpleef] Item winning list is arena " + getId() + " is not a valid list!");
			return;
		}
		// get list
		List<String> prizes = configuration.getStringList("itemPrizes");
		// random entry...
		String prize;
		if (prizes.size() == 1) { // if there is only one entry...
			prize = prizes.get(0);
		} else prize = prizes.get(generator.nextInt(prizes.size() - 1));
		// interpret line to Item Stack
		ItemStack itemStack = MaterialHelper.getItemStackFromString(prize, false);
		// give prizes to player
		player.getInventory().addItem(itemStack);
		// player gets message
		player.sendMessage(ChatColor.AQUA + SimpleSpleef.getPlugin().ll("feedback.prizeItems", "[ARENA]", getName(), "[ITEM]", itemStack.getType().toString(), "[AMOUNT]", String.valueOf(itemStack.getAmount())));
		// broadcast prize?
		String broadcastMessage = ChatColor.AQUA + SimpleSpleef.getPlugin().ll("broadcasts.prizeItems", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[ITEM]", itemStack.getType().toString(), "[AMOUNT]", String.valueOf(itemStack.getAmount()));
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announcePrize", true)) {
			SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
		} else {
			sendMessage(broadcastMessage, player); // send message to all receivers
		}
	}

	/**
	 * if "clearInventory" setting of area is true, clear inventory of player and remember it
	 * @return
	 */
	protected boolean clearInventories() {
		// check setting in configuration
		if (!configuration.getBoolean("clearInventory", false)) return false;
		if (spleefers == null || inventoryKeeper == null) return false; // no NPE

		for (Spleefer spleefer : spleefers.get()) {
			Player player = spleefer.getPlayer();
			if (!inventoryKeeper.saveInventory(player)) {
				// log error
				SimpleSpleef.log.severe("[SimpleSpleef] Could not clear inventory of " + player.getName() + ". Keeping it.");
			}
		}
		return true;
	}
	
	/**
	 * if "clearInventory" setting of area is true, restore saved inventory of all players
	 * @return
	 */
	protected boolean restoreAllInventories() {
		// check setting in configuration
		if (!configuration.getBoolean("clearInventory", false)) return false;
		if (spleefers == null) return false; // no NPE

		for (Spleefer spleefer : spleefers.get()) {
			restoreInventory(spleefer.getPlayer());
		}
		return true;
	}
	
	/**
	 * if "clearInventory" setting of area is true, restore saved inventory of single player
	 * @return
	 */
	protected boolean restoreInventory(Player player) {
		// check setting in configuration
		if (!configuration.getBoolean("clearInventory", false)) return false;
		if (player == null || inventoryKeeper == null) return false; // no NPE

		return inventoryKeeper.restoreInventory(player);
	}
	
	/**
	 * if "addToInventory" setting of area is true, add items to inventory of player
	 * @return
	 */
	protected boolean addToInventories() {
		// check setting in configuration
		if (!configuration.getBoolean("addToInventory", false) || !configuration.isList("addInventoryItems")) return false;
		
		// create items for the inventory
		List<String> addInventoryItems = configuration.getStringList("addInventoryItems");
		LinkedList<ItemStack> itemStack = new LinkedList<ItemStack>();
		for (String item : addInventoryItems) {
			ItemStack stack = MaterialHelper.getItemStackFromString(item, false);
			if (stack == null)
				SimpleSpleef.log.warning("[SimpleSpleef] Could not parse addInventoryItems to item stack in arena " + getId() + ", line: " + item);
			else itemStack.add(stack);
		}
		// only continue if stack is greater 0
		if (itemStack.size() == 0) return false;
		addInventoryItems = null; // save memory
		
		// now give spleefers stack items
		for (Spleefer spleefer : spleefers.get()) {
			Player player = spleefer.getPlayer();
			for (ItemStack item : itemStack) {
				player.getInventory().addItem(item.clone());
			}
		}

		return true;
	}
	
	/**
	 * if "playersReceiveShovelAtGameStart" setting of area is true, add shovel to inventory of player
	 * @return
	 */
	protected boolean addShovelItems() {
		// check setting first
		if (!configuration.getBoolean("playersReceiveShovelAtGameStart", true)) return false; // no shovels lost
		// if yes, add shovels to all players
		for (Spleefer spleefer : spleefers.get()) {
			addShovelItem(spleefer.getPlayer(), false); // add shovel without checking setting again
		}
		return true;
	}
	
	/**
	 * remove shovel item from single player
	 * @param player
	 * @param checkSetting - if true, setting playersReceiveShovelAtGameStart is checked before addition
	 * @return
	 */
	protected boolean addShovelItem(Player player, boolean checkSetting) {
		// should setting be checked first?
		if (checkSetting && !configuration.getBoolean("playersReceiveShovelAtGameStart", true)) return false; // no shovel added
		// get material
		ItemStack shovelItem = MaterialHelper.getItemStackFromString(configuration.getString("shovelItem", "DIAMOND_SPADE"), false);
		if (shovelItem == null) {
			SimpleSpleef.log.warning("[SimpleSpleef] shovelItem of arena " + getId() + " is not a correct item id/name!");
			return false;
		}
		// give it to the player
		player.getInventory().addItem(shovelItem);
		
		return true;
	}
	
	/**
	 * if "playersLoseShovelAtGameEnd" setting of area is true, remove shovel from inventory of player
	 * @return
	 */
	protected boolean removeShovelItems() {
		// check setting first
		if (!configuration.getBoolean("playersLoseShovelAtGameEnd", true)) return false; // no shovels lost
		// if yes, remove shovels from remaining players
		for (Spleefer spleefer : spleefers.get()) {
			if (!spleefer.hasLost())
				removeShovelItem(spleefer.getPlayer(), false); // remove shovel without checking setting again
		}
		return true;
	}
	
	/**
	 * remove shovel item from single player
	 * @param player
	 * @param checkSetting - if true, setting playersLoseShovelAtGameEnd is checked before removal
	 * @return
	 */
	protected boolean removeShovelItem(Player player, boolean checkSetting) {
		// should setting be checked first?
		if (checkSetting && !configuration.getBoolean("playersLoseShovelAtGameEnd", true)) return false; // no shovel lost
		// get material
		ItemStack shovelItem = MaterialHelper.getItemStackFromString(configuration.getString("shovelItem", "DIAMOND_SPADE"), false);
		if (shovelItem == null) {
			SimpleSpleef.log.warning("[SimpleSpleef] shovelItem of arena " + getId() + " is not a correct item id/name!");
			return false;
		}
		// take if, if player still has it - yes, there are exploits, but I do
		// not want to handle them ;-)
		// We have to find the shovel item - it might have been used and its damage number be lowered due to use... So we have to
		// find the shovel that has been used most and delete that one...
		Inventory inventory = player.getInventory();
		Material shovelMaterial = MaterialHelper.getMaterialFromString(configuration.getString("shovelItem", "DIAMOND_SPADE"));
		// find all shovel items, if there are more than one to be removed
		for (int i = 0; i < shovelItem.getAmount(); i++) {
			int indexFound = -1; // keeps found shovels
			short durability = Short.MIN_VALUE; // keeps status of worst shovel found
			// get all possible shovel items
			for (Integer index : inventory.all(shovelMaterial).keySet()) {
				ItemStack current = inventory.getItem(index);
				short currentDurability = current.getDurability();
				if (currentDurability > durability) { // is this the worst shovel found?
					indexFound = index;
					durability = currentDurability;
				}
			}
			
			// worst shovel found?
			if (indexFound > -1) {
				// get item
				ItemStack stack = inventory.getItem(indexFound);
				if (stack.getAmount() > 1) { // shovels should only come in packs of one, but who knows?
					stack.setAmount(stack.getAmount() - 1); // reduce amount by one
					inventory.setItem(indexFound, stack);
				} else
					inventory.setItem(indexFound, null); // remove item completely
			}
		}
		
		return true;
	}

	/**
	 * teleport a player to named spawn, if it exists and it is enabled
	 * @param player
	 * @param string
	 */
	protected void teleportPlayer(Player player, String spawn) {
		if (!configuration.isConfigurationSection(spawn + "Spawn") || !configuration.getBoolean(spawn + "Spawn.enabled", false))
			return; // just ignore, if not set or not enabled
		// everything ok -> teleport player
		Location teleportTo = LocationHelper.configToExactLocation(configuration.getConfigurationSection(spawn + "Spawn"));
		if (teleportTo == null) SimpleSpleef.log.warning("[SimpleSpleef] Teleport error - location was null!");
		// load chunk, if needed
		teleportTo.getWorld().loadChunk(teleportTo.getChunk());
		// add player to teleport ok list
		this.teleportOkList.add(player);
		player.teleport(teleportTo);
	}

	/**
	 * check whether a certain block may be broken
	 * => player has been checked before this, so this does only concern block breaks
	 * and interactions by spleefers
	 * @param block broken/interacted (on instant-break) by spleefer
	 * @return true, if block may be destroyed
	 */
	@Override
	public boolean checkMayBreakBlock(Block block) {
		// sanity check
		if (block == null) return true;
		// joined players may not break blocks as long as game has not started
		if (!isInGame()) return false;
		return checkMayBreakBlockMaterial(block) && checkMayBreakBlockLocation(block);
	}
	
	/**
	 * helper function for the above checkMayBreakBlock method to check the material of a block
	 * @param block
	 * @return
	 */
	protected boolean checkMayBreakBlockMaterial(Block block) {
		// allowed blocks? => allowDigBlocks
		// alternatively: disallowDigBlocks
		if (digBlocks != null) {
			if (MaterialHelper.isSameBlockType(block, digBlocks)) return allowDigBlocks; // found -> return state
			else // not found
				return !allowDigBlocks; // negate
		}
		return true;
	}
	
	/**
	 * helper function for the above checkMayBreakBlock method to check the location of a block
	 * @param block
	 * @return
	 */
	protected boolean checkMayBreakBlockLocation(Block block) {
		Location blockLocation = block.getLocation();
		// is arena undefined?
		if (arena == null) {
			switch (diggingIfArenaUndefined) {
			case GameStandard.DIGGING_NONE: return false; // digging is not allowed
			case GameStandard.DIGGING_EVERYWHERE: return true; // digging is allowed everywhere
			default: // allowed in the arena floor in all other cases
				return this.floor.contains(blockLocation);
			}
		} else { // arena defined
			// is floor undefined?
			if (floor == null) {
				switch (diggingIfFloorUndefined) {
				case GameStandard.DIGGING_NONE: return false; // digging is not allowed
				case GameStandard.DIGGING_EVERYWHERE: return true; // digging is allowed everywhere
				case GameStandard.DIGGING_OUTSIDE_ARENA: return !this.arena.contains(blockLocation); // digging is allowed outside of arena
				default: // allowed within the arena in other cases
					return this.arena.contains(blockLocation);
				}
			} else { // floor and arena defined
				return this.floor.contains(blockLocation); // only arena floor can be broken during game
			}
		}
	}
	
	/**
	 * update the trackers when blocks are changed
	 * @param newBlock - new block (after change)
	 * @param oldType - old type of block
	 * @param oldData - old data of block
	 */
	protected void updateTrackers(Block newBlock, int oldType, byte oldData) {
		if (floorTracker != null)
			floorTracker.updateBlock(newBlock, oldType, oldData);
	}
	
	/**
	 * save arena information, if setting restoreArenaAfterGame has been set
	 */
	protected void saveArena() {
		// explicitly set to false
		if (configuration.isBoolean("restoreArenaAfterGame") && !configuration.getBoolean("restoreArenaAfterGame")) {
			arenaRestorer = null;
			return; // ignore, if arena not defined or setting false
		}
		
		// determine type of arena
		String type;
		if (configuration.isBoolean("restoreArenaAfterGame")) type = "soft";
		else if (configuration.isString("restoreArenaAfterGame")) type = configuration.getString("restoreArenaAfterGame");
		else type = "soft"; // fall back
		
		if (type.equals("arenahard")) { // hard arena restorer
			arenaRestorer = new HardArenaRestorer();
			arenaRestorer.saveArena(this, arena);
		} else if (type.equals("floorhard")) { // hard floor restorer
			arenaRestorer = new HardArenaRestorer();
			arenaRestorer.saveArena(this, floor);
		} else { // soft restorer
			Cuboid possibleFloor = floor==null?arena:floor;
			if (possibleFloor == null) {
				arenaRestorer = null;
				return; // freestyle spleefing does not implement any floor
			}

			// create soft restorer
			arenaRestorer = new SoftRestorer();
			arenaRestorer.saveArena(this, possibleFloor);

			if (floorTracker == null) // create floor tracker, if needed
				floorTracker = new FloorTracker();
			// create a new tracker/restorer
			floorTracker.addFloorWorker((FloorWorker) arenaRestorer);
		}
	}

	/**
	 * restore arena information, if setting restoreArenaAfterGame has been set
	 */
	protected void restoreArena() {
		if (arenaRestorer == null) {
			SimpleSpleef.getGameHandler().gameOver(this); // notify game handler that the game has stopped
			return;
		}

		final int wait = configuration.getInt("restoreArenaAfterGameTimer", 0);
		if (wait > 0) { // start in timer
			SimpleSpleef.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(SimpleSpleef.getPlugin(), new Runnable() {
				@Override
				public void run() {
					arenaRestorer.restoreArena();
				}
			}, (long) (20 * wait)); // ticks * 20 = secs
		} else arenaRestorer.restoreArena(); // restore right away
	}

	@Override
	public void clean() {
		// dereference stuff
		this.configuration = null;
		this.spleefers = null;
		this.spectators = null;
		this.loseOnTouchMaterial = null;
		this.countdown = null;
		this.teleportOkList = null;
		this.arena = null;
		this.floor = null;
		this.lose = null;
		this.inventoryKeeper = null;
		if (this.playerOnBlockDegenerator != null)
			this.playerOnBlockDegenerator.stopBlockDegenerator(); // to play it safe
		this.playerOnBlockDegenerator = null;
		if (this.floorTracker != null)
			this.floorTracker.stopTracking(); // to play it safe, too
		this.floorTracker = null;
		this.arenaRestorer = null;
		//TODO add more
	}
	
	/**
	 * teleport players to arena
	 */
	protected void teleportPlayersAtGameStart() {
		for (Spleefer spleefer : spleefers.get()) {
			teleportPlayer(spleefer.getPlayer(), "game");
		}
	}

	/**
	 * Countdown class
	 * 
	 * @author mkalus
	 * 
	 */
	private class Countdown implements Runnable {
		// flag to toggle interrupts
		private boolean interrupted = false;
		
		private int count = -1;
		
		private boolean broadcast;
		
		// start countdown
		private void start() {
			// change game status
			status = STATUS_COUNTDOWN;

			// announce countdown?
			broadcast = SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceCountdown", true);
			sendMessage(ChatColor.BLUE + SimpleSpleef.getPlugin().ll("feedback.countdownStart"), broadcast);
			
			// get time
			count = configuration.getInt("countdownFrom", 10);
		}

		/**
		 * Thread method
		 */
		@Override
		public void run() {
			if (interrupted) {
				// send message
				sendMessage(ChatColor.RED + SimpleSpleef.getPlugin().ll("feedback.countdownInterrupted"), broadcast);
				// cancel countdown
				SimpleSpleef.getPlugin().getServer().getScheduler().cancelTask(countdownId);
				// end the game
				GameStandard.this.endGame();
			}
			
			// start?
			if (count < 0) {
				start();
			}
			
			// do countdown
			if (count > 0) {
				sendMessage(ChatColor.BLUE + SimpleSpleef.getPlugin().ll("feedback.countdown", "[COUNT]", String.valueOf(count), "[ARENA]", GameStandard.this.getName()), broadcast);
				count--;
			} else {
				// send message
				sendMessage(ChatColor.BLUE + SimpleSpleef.getPlugin().ll("feedback.countdownGo"), broadcast);
				// start the game itself!
				GameStandard.this.start();
				// cancel countdown
				SimpleSpleef.getPlugin().getServer().getScheduler().cancelTask(countdownId);
			}
		}
	}
}
