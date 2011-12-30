/**
 * 
 */
package de.beimax.simplespleef.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import de.beimax.simplespleef.util.*;

/**
 * @author mkalus
 * Simple Game implementation
 */
public class GameImpl extends Game {
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
	protected Set<Material> looseOnTouchMaterial;

	/**
	 * private countdown class
	 */
	private Countdown countdown;
	
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
	 * loose cuboid
	 */
	private Cuboid loose;
	
	/**
	 * set to false, if disallowDigBlocks has been chosen in config
	 */
	private boolean allowDigBlocks = true;
	
	/**
	 * blocks that can be dug or (if allowDigBlocks is false) cannot
	 */
	private HashSet<ItemStack> digBlocks;

	/**
	 * Constructor
	 * @param gameHandler
	 * @param name
	 */
	public GameImpl(String name) {
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
		if (conf.getBoolean("looseOnTouchBlocks", true)) {
			// is looseBlocks a valid list?
			if (conf.isList("looseBlocks")) { //yes
				this.looseOnTouchMaterial = new HashSet<Material>();
				for (String material : conf.getStringList("looseBlocks")) {
					Material m = Material.getMaterial(material);
					// check existence of material
					if (m == null) SimpleSpleef.log.warning("[SimpleSpleef] " + getId() + " configuration warning! Unknown Material in looseBlocks: " + material);
					else this.looseOnTouchMaterial.add(m);
				}
				if (this.looseOnTouchMaterial.size() == 0) this.looseOnTouchMaterial = null; //no
			} else this.looseOnTouchMaterial = null; //no
		} else this.looseOnTouchMaterial = null; // reset
		// define arena, floor and loose cuboids
		arena = SimpleSpleef.getGameHandler().configToCuboid(getId(), "arena");
		floor = SimpleSpleef.getGameHandler().configToCuboid(getId(), "floor");
		loose = SimpleSpleef.getGameHandler().configToCuboid(getId(), "loose");
		// block destruction/keep hashes
		if (conf.isList("allowDigBlocks")) {
			allowDigBlocks = true;
			digBlocks = new HashSet<ItemStack>();
			for (String line : conf.getStringList("allowDigBlocks")) {
				digBlocks.add(MaterialHelper.getItemStackFromString(line));
			}
		} else if (conf.isList("disallowDigBlocks")) {
			allowDigBlocks = false;
			digBlocks = new HashSet<ItemStack>();
			for (String line : conf.getStringList("disallowDigBlocks")) {
				digBlocks.add(MaterialHelper.getItemStackFromString(line));
			}
		} else digBlocks = null; // delete previous settings
		//TODO: more definitions/shortcuts
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
		if (maximumPlayers > 0 && spleefers.size() >= maximumPlayers) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.joinMax", "[ARENA]", getName(), "[NUMBER]", String.valueOf(maximumPlayers)));
			return false;
		}
		// already joined this game? => is caught by GameHandler, so we do not check this here...
		// check funds of player...
		double entryFee = configuration.getDouble("entryFee", 0.0);
		if (entryFee > 0.0) {
			EconomyResponse response = SimpleSpleef.economy.withdrawPlayer(player.getName(), entryFee);
			if (response.type == EconomyResponse.ResponseType.SUCCESS) { // ok, tell the player about the amount charged
				player.sendMessage(SimpleSpleef.getPlugin().ll("feedback.joinFee", "[AMOUNT]", SimpleSpleef.economy.format(entryFee)));
			} else { //insufficient funds
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.joinFee", "[AMOUNT]", SimpleSpleef.economy.format(entryFee)));
				return false;
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
		//TODO: check status
		//TODO: player feedback
		return spleefers.removeSpleefer(player);
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
		// start countdown, if setting is 0 or higher
		if (configuration.getInt("countdownFrom", 10) == 0) {
			start(); // if countdown is null, start game right away
		} else {
			// create countdown and start it
			countdown = new Countdown();
			countdown.start();
		}
		
		return true;
	}

	@Override
	public boolean start() {
		// delete countdown
		deleteCountdown();
		// save arena information
		saveArena();
		// change game status
		status = STATUS_STARTED;
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
		// call the game handler to tell it that the game is over
		SimpleSpleef.getGameHandler().gameOver(this);
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
		// + other cases?
		// still in countdown? if yes, kill it!
		if (countdown != null)
			countdown.interrupted = true;
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
			restoreArena();
		}
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
		if (!spectators.contains(player)) {
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
		return spleefers.hasSpleefer(player);
	}
	
	@Override
	public boolean hasSpectator(Player player) {
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
		if (!isInProgress() || spleefers.hasLost(player)) return; // if game is not in progress or player has lost, return
		
		//player touched certain block (setting looseOnTouchBlocks)
		if (looseOnTouchMaterial != null) {
			Material touchedBlock = event.getTo().getBlock().getType();
			Material onBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
			// what happened exactly?
			boolean lostByTouching = looseOnTouchMaterial.contains(touchedBlock);
			boolean lostByStandingOn = looseOnTouchMaterial.contains(onBlock);
			if (lostByTouching || lostByStandingOn) {
				// Block name
				String blockName;
				if (lostByTouching) blockName = touchedBlock.name();
				else blockName = onBlock.name();
				// broadcast message of somebody loosing
				String broadcastMessage = ChatColor.GREEN + SimpleSpleef.getPlugin().ll("broadcasts.lostByTouching", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[MATERIAL]", blockName);
				if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLoose", true)) {
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
		// check location within "loose" cuboid (setting loose)
		if (loose != null && loose.contains(player.getLocation())) {
			// broadcast message of somebody loosing
			String broadcastMessage = ChatColor.GREEN + SimpleSpleef.getPlugin().ll("broadcasts.lostByCuboid", "[PLAYER]", player.getName(), "[ARENA]", getName());
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLoose", true)) {
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

	@Override
	public boolean playerMayTeleport(Player player) {
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
		// check instant dig and block may be broken
		if (event.getAction() == Action.LEFT_CLICK_BLOCK && block != null && configuration.getBoolean("instantDig", true) && checkMayBreakBlock(block)) {
			// cancel event
			event.setCancelled(true);
			// set block to air
			event.getClickedBlock().setType(Material.AIR);
			event.getClickedBlock().setData((byte) 0);
		}
		
		//TODO do sign interactions here
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		// remove shovel of player, if needed
		removeShovelItem(event.getPlayer(), true);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		// remove shovel of player, if needed
		removeShovelItem(event.getPlayer(), true);
		// delete original position, because player is banned anyhow
		SimpleSpleef.getOriginalPositionKeeper().deleteOriginalPosition(event.getPlayer());
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		// TODO: restore shovel item, if player is autojoined again
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerDeath(Player player) {
		// delete original position, because player spawns somewhere else anyhow
		SimpleSpleef.getOriginalPositionKeeper().deleteOriginalPosition(player);
		if (configuration.getBoolean("looseOnDeath", true)) {
			// broadcast message of somebody loosing
			String broadcastMessage = ChatColor.GREEN + SimpleSpleef.getPlugin().ll("broadcasts.lostByDeath", "[PLAYER]", player.getName(), "[ARENA]", getName());
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLoose", true)) {
				SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
			} else {
				// send message to all receivers
				sendMessage(broadcastMessage, player);
			}
			// player looses, if set to true
			playerLoses(player, false); // do not teleport dead players...
		} // else - do nothing...
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block == null) return; // sanity check
		// may the block be broken?
		if (!checkMayBreakBlock(block)) {
			// cancel event
			event.setCancelled(true);
			// message to player
			event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.noDig"));
		} else if (!configuration.getBoolean("blockDropping", true)) { // otherwise: block dropping set to false => destroy blocks
			// cancel event - because we will handle the block destruction ourselves
			event.setCancelled(true);
			// set block to air
			block.setType(Material.AIR);
			block.setData((byte) 0);
		}
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
	}

	/**
	 * called when player loses a game
	 * (broadcast message has to be sent by calling method)
	 * @param player
	 * @param teleport true, if player should be teleported to loose spawn, if possible
	 */
	protected void playerLoses(Player player, boolean teleport) {
		// set player to lost
		spleefers.setLost(player);
		// message to player
		player.sendMessage(ChatColor.RED + SimpleSpleef.getPlugin().ll("feedback.lost"));
		// broadcast message has to be sent by calling method
		// shovel lost, too
		removeShovelItem(player, true);
		// teleport player to loose spawn
		if (teleport) teleportPlayer(player, "loose");
		// determine if game is over...
		if (spleefers.inGame() <= configuration.getInt("remainingPlayersWin", 1))
			gameOver();
	}
	
	/**
	 * called when game is over - get winners, etc.
	 */
	protected void gameOver() {
		// determine winners
		LinkedList<Player> winners = new LinkedList<Player>();
		for (Spleefer spleefer : spleefers.get()) {
			Player player = spleefer.getPlayer();
			if (!spleefer.hasLost()) { // not lost?
				//this guy is a winner - send a message
				player.sendMessage(ChatColor.DARK_GREEN + SimpleSpleef.getPlugin().ll("feedback.won"));
				winners.add(player); // aggregate the winners to broadcast them later on
				// pay prizes
				payPrizeMoney(player);
				payPrizeItems(player);
			}
			// update original positions
			if (configuration.getBoolean("enableBackCommand", true))
				SimpleSpleef.getOriginalPositionKeeper().updateOriginalLocationTimestamp(player);
		}
		//TODO: winning message! <= get the total winners from winners list
		//announceWin
		
		// clean up game and end it
		endGame();
		// call the game handler to tell it that the game is over
		SimpleSpleef.getGameHandler().gameOver(this);
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
		String prize = prizes.get(generator.nextInt(prizes.size() - 1));
		// interpret line to Item Stack
		ItemStack itemStack = MaterialHelper.getItemStackFromString(prize);
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
		//TODO implement
		return false;
	}
	
	/**
	 * if "clearInventory" setting of area is true, restore saved inventory of all players
	 * @return
	 */
	protected boolean restoreAllInventories() {
		//TODO implement
		return false;
	}
	
	/**
	 * if "clearInventory" setting of area is true, restore saved inventory of single player
	 * @return
	 */
	protected boolean restoreInventory(Player player) {
		//TODO implement
		return false;
	}
	
	/**
	 * if "addToInventory" setting of area is true, add items to inventory of player
	 * @return
	 */
	protected boolean addToInventories() {
		//TODO implement
		return false;
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
		ItemStack shovelItem = MaterialHelper.getItemStackFromString(configuration.getString("shovelItem", "DIAMOND_SPADE"));
		if (shovelItem == null) {
			SimpleSpleef.log.warning("[SimpleSpleef] shovelItem of arena " + getId() + " is not a correct item id/name!");
			return false;
		}
		// give it to the player
		player.getInventory().addItem(shovelItem);
		
		return true;
	}
	
	/**
	 * if "playersLooseShovelAtGameEnd" setting of area is true, remove shovel from inventory of player
	 * @return
	 */
	protected boolean removeShovelItems() {
		// check setting first
		if (!configuration.getBoolean("playersLooseShovelAtGameEnd", true)) return false; // no shovels lost
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
	 * @param checkSetting - if true, setting playersLooseShovelAtGameEnd is checked before removal
	 * @return
	 */
	protected boolean removeShovelItem(Player player, boolean checkSetting) {
		// should setting be checked first?
		if (checkSetting && !configuration.getBoolean("playersLooseShovelAtGameEnd", true)) return false; // no shovel lost
		// get material
		ItemStack shovelItem = MaterialHelper.getItemStackFromString(configuration.getString("shovelItem", "DIAMOND_SPADE"));
		if (shovelItem == null) {
			SimpleSpleef.log.warning("[SimpleSpleef] shovelItem of arena " + getId() + " is not a correct item id/name!");
			return false;
		}
		// take if, if player still has it - yes, there are exploits, but I do
		// not want to handle them ;-)
		Inventory inventory = player.getInventory();
		if (inventory.contains(shovelItem)) {
			int index = inventory.first(shovelItem);
			// get the first stack
			ItemStack stack = inventory.getItem(index);
			if (stack.getAmount() > 1) { // shovels should only come in packs of one, but who knows?
				stack.setAmount(stack.getAmount() - 1); // reduce amount by one
				inventory.setItem(index, stack);
			} else
				inventory.setItem(index, null); // remove item
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
	protected boolean checkMayBreakBlock(Block block) {
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
	private boolean checkMayBreakBlockMaterial(Block block) {
		// allowed blocks? => allowDigBlocks
		// alternatively: disallowDigBlocks
		if (digBlocks != null) {
			ItemStack checkblock = new ItemStack(block.getType(), 1, block.getData());
			if (digBlocks.contains(checkblock)) return allowDigBlocks; // found -> return state
			// not found
			return !allowDigBlocks; // negate
		}
		return true;
	}
	
	/**
	 * helper function for the above checkMayBreakBlock method to check the location of a block
	 * @param block
	 * @return
	 */
	private boolean checkMayBreakBlockLocation(Block block) {
		Location blockLocation = block.getLocation();
		// arena floor defined?
		if (this.floor != null) return this.floor.contains(blockLocation); // only arena floor can be broken during game
		// otherwise, at least check, if block to be broken is outside a defined arena
		else if (this.arena != null) return this.arena.contains(blockLocation);

		// on all other cases - allow block breaks, like in original simple spleef games
		return true;		
	}
	
	/**
	 * save arena information, if setting restoreArenaAfterGame has been set
	 */
	protected void saveArena() {
		if (arena == null || !configuration.getBoolean("restoreArenaAfterGame", true)) return; // ignore, if arena not defined or setting false
		SerializableBlockData[][][] blockData = arena.getSerializedBlocks();
		
		// output file
		File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + getId() + ".save");
		// delete old file
		if (file.exists()) {
			if (!file.delete()) SimpleSpleef.log.warning("[SimpleSpleef] Could not delete file " + file.getName());
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

	/**
	 * restore arena information, if setting restoreArenaAfterGame has been set
	 */
	protected void restoreArena() {
		if (arena == null || !configuration.getBoolean("restoreArenaAfterGame", true)) return; // ignore, if arena not defined or setting false
		
		// input file
		File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + getId() + ".save");
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
		arena.setSerializedBlocks(blockData);
		// delete file at the end - cleanup work...
		if (!file.delete()) SimpleSpleef.log.warning("[SimpleSpleef] Could not delete file " + file.getName());
	}

	@Override
	public void clean() {
		// dereference stuff
		this.configuration = null;
		this.spleefers = null;
		this.spectators = null;
		this.looseOnTouchMaterial = null;
		this.countdown = null;
		this.teleportOkList = null;
		this.arena = null;
		this.floor = null;
		this.loose = null;
		//TODO add more
	}

	/**
	 * Countdown class
	 * 
	 * @author mkalus
	 * 
	 */
	private class Countdown extends Thread {
		// flag to toggle interrupts
		private boolean interrupted = false;

		/**
		 * Thread method
		 */
		@Override
		public void run() {
			// change game status
			status = STATUS_COUNTDOWN;

			// announce countdown?
			boolean broadcast = SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceCountdown", true);
			sendMessage(ChatColor.BLUE + SimpleSpleef.getPlugin().ll("feedback.countdownStart"), broadcast);
			
			// teleport players to arena
			for (Spleefer spleefer : spleefers.get()) {
				teleportPlayer(spleefer.getPlayer(), "game");
			}

			// get time
			long start = System.currentTimeMillis() + 1000;
			int count = configuration.getInt("countdownFrom", 10);
			boolean started = false; // start flag
			
			// do countdown
			do {
				if (System.currentTimeMillis() >= start) {
					// actually start game
					if (count == 0)
						started = true;
					// set next countdown event
					else {
						start = start + 1000;
						// Broadcast countdown
						sendMessage(ChatColor.BLUE + SimpleSpleef.getPlugin().ll("feedback.countdown", "[COUNT]", String.valueOf(count), "[ARENA]", GameImpl.this.getName()), broadcast);
						count--;
					}
				}
				try {
					sleep(10);
				} catch (InterruptedException e) {
				}
			} while (!started && !interrupted);
			// countdown ended due to interruption?
			if (interrupted) {
				// send message
				sendMessage(ChatColor.RED + SimpleSpleef.getPlugin().ll("feedback.countdownInterrupted"), broadcast);
				// end the game
				GameImpl.this.endGame();
			} else {
				// send message
				sendMessage(ChatColor.BLUE + SimpleSpleef.getPlugin().ll("feedback.countdownGo"), broadcast);
				// start the game itself!
				GameImpl.this.start();
			}
		}
	}
}
