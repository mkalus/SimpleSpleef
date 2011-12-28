/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.util.MaterialHelper;

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
	 * Constructor
	 * @param gameHandler
	 * @param name
	 */
	public GameImpl(GameHandler gameHandler, String name) {
		super(gameHandler, name);
		this.spleefers = new SpleeferList();
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
		
		//TODO: more definitions/shortcuts
	}

	@Override
	public boolean join(Player player) {
		//check joinable status
		if (!isJoinable()) {
			player.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.join", "[ARENA]", getName()));
			return false;
		}
		// max number of players?
		int maximumPlayers = configuration.getInt("maximumPlayers", 0);
		if (maximumPlayers > 0 && spleefers.size() >= maximumPlayers) {
			player.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.joinMax", "[ARENA]", getName(), "[NUMBER]", String.valueOf(maximumPlayers)));
			return false;
		}
		// already joined this game? => is caught by GameHandler, so we do not check this here...
		// check funds of player...
		double entryFee = configuration.getDouble("entryFee", 0.0);
		if (entryFee > 0.0) {
			EconomyResponse response = SimpleSpleef.economy.withdrawPlayer(player.getName(), entryFee);
			if (response.type == EconomyResponse.ResponseType.SUCCESS) { // ok, tell the player about the amount charged
				player.sendMessage(this.gameHandler.getPlugin().ll("feedback.joinFee", "[AMOUNT]", SimpleSpleef.economy.format(entryFee)));
			} else { //insufficient funds
				player.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.joinFee", "[AMOUNT]", SimpleSpleef.economy.format(entryFee)));
				return false;
			}
		}
		// check gamemode and change it if needed
		if (player.getGameMode() != GameMode.SURVIVAL) {
			player.setGameMode(GameMode.SURVIVAL);
			player.sendMessage(ChatColor.YELLOW + this.gameHandler.getPlugin().ll("feedback.gamemodeChanged"));
		}
		if (!spleefers.addSpleefer(player)) { // some weird error
			player.sendMessage(ChatColor.DARK_RED + "Internal error while joining occured! Please tell the SimpleSpleef creator!");
			return false;
		}
		// inform/broadcast join is done by the game handler
		// TODO: remember player's last position
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
			sender.sendMessage(ChatColor.DARK_RED + gameHandler.getPlugin().ll("errors.startDouble", "[ARENA]", getName()));
			return false;
		}
		// minimum number of players?
		int minimumPlayers = configuration.getInt("minimumPlayers", 0);
		if (minimumPlayers > 0 && spleefers.size() < minimumPlayers) {
			sender.sendMessage(ChatColor.DARK_RED + gameHandler.getPlugin().ll("errors.startMin", "[ARENA]", getName(), "[NUMBER]", String.valueOf(minimumPlayers)));
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
			player.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.stop", "[ARENA]", getName()));
			return false;
		}
		// actually end the game
		if (!endGame()) return false;
		// send message
		sendMessage(this.gameHandler.getPlugin().ll("feedback.stop", "[ARENA]", getName(), "[PLAYER]", player.getDisplayName()),
				this.gameHandler.getPlugin().getConfig().getBoolean("settings.announceStop", true));
		return true;
	}
	

	@Override
	public boolean delete(CommandSender sender) {
		// end the game first, if game is started
		if (!endGame()) return false;
		// send message
		sendMessage(this.gameHandler.getPlugin().ll("feedback.delete", "[ARENA]", getName(), "[PLAYER]", sender.getName()),
				this.gameHandler.getPlugin().getConfig().getBoolean("settings.announceStop", true));
		// call the game handler to tell it that the game is over
		gameHandler.gameOver(this);
		return true;
	}

	/**
	 * Do end game maintenance
	 * @return
	 */
	protected boolean endGame() {
		//TODO => check what the current status is and end game then
		//isInProgress() 
		//isJoinable()
		// + other cases?
		// still in countdown? if yes, kill it!
		if (countdown != null)
			countdown.interrupted = true;
		// change game status
		status = STATUS_FINISHED;
		// possibly take away shovel items
		removeShovelItems();
		// possibly restore inventories
		restoreAllInventories();
		// teleport remaining players to lounge
		for (Spleefer spleefer : spleefers.get()) {
			if (!spleefer.hasLost())
				teleportPlayer(spleefer.getPlayer(), "lounge");
		}
		// change game status
		status = STATUS_NEW;
		return true;
	}

	@Override
	public boolean spectate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasPlayer(Player player) {
		return spleefers.hasSpleefer(player);
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
		if (broadcast) gameHandler.getPlugin().getServer().broadcastMessage(message);
		else { // only players and specators
			// players
			for (Spleefer spleefer : spleefers.get()) {
				spleefer.getPlayer().sendMessage(message);
			}
			// TODO: spectators
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
		// TODO: spectators
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
				String broadcastMessage = ChatColor.GREEN + gameHandler.getPlugin().ll("broadcasts.lostByTouching", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[MATERIAL]", blockName);
				if (gameHandler.getPlugin().getConfig().getBoolean("settings.announceLoose", true)) {
					gameHandler.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
				} else {
					// send message to all receivers
					sendMessage(broadcastMessage, player);
				}
				// Ha, lost!
				playerLoses(player);
			}
		}
		//TODO: check location within "loose" cuboid (setting loose)
	}

	@Override
	public boolean playerMayTeleport(Player player) {
		// check if the player is on the teleport-ok-list, delete him/her from list and return true
		if (this.teleportOkList.contains(player)) {
			this.teleportOkList.remove(player);
			return true;
		}
		// otherwise return preventTeleportingDuringGames for this arena
		return !configuration.getBoolean("preventTeleportingDuringGames", true);
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		// TODO Auto-generated method stub
		//TODO also check block playing here
		
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		// TODO: restore shovel item, if player is autojoined again
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerDeath(Player player) {
		// remove shovel of player, if needed
		removeShovelItem(player, true);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * called when player loses a game
	 * (broadcast message has to be sent by calling method)
	 * @param player
	 */
	protected void playerLoses(Player player) {
		// set player to lost
		spleefers.setLost(player);
		// message to player
		player.sendMessage(ChatColor.RED + this.gameHandler.getPlugin().ll("feedback.lost"));
		// broadcast message has to be sent by calling method
		// shovel lost, too
		removeShovelItem(player, true);
		// teleport player to loose spawn
		teleportPlayer(player, "loose");
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
			if (!spleefer.hasLost()) { // not lost?
				Player player = spleefer.getPlayer();
				//this guy is a winner - send a message
				player.sendMessage(ChatColor.DARK_GREEN + this.gameHandler.getPlugin().ll("feedback.won"));
				winners.add(player); // aggregate the winners to broadcast them later on
				// pay prizes
				payPrizeMoney(player);
				payPrizeItems(player);
			}
		}
		//TODO: winning message! <= get the total winners from winners list
		
		// clean up game and end it
		endGame();
		// call the game handler to tell it that the game is over
		gameHandler.gameOver(this);
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
		player.sendMessage(ChatColor.AQUA + gameHandler.getPlugin().ll("feeback.prizeMoney", "[ARENA]", getName(), "[MONEY]", formated));
		// broadcast prize?
		String broadcastMessage = ChatColor.AQUA + gameHandler.getPlugin().ll("broadcasts.prizeMoney", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[MONEY]", formated);
		if (gameHandler.getPlugin().getConfig().getBoolean("settings.announcePrize", true)) {
			gameHandler.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
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
		player.sendMessage(ChatColor.AQUA + gameHandler.getPlugin().ll("feeback.prizeItems", "[ARENA]", getName(), "[ITEM]", itemStack.getType().toString(), "[AMOUNT]", String.valueOf(itemStack.getAmount())));
		// broadcast prize?
		String broadcastMessage = ChatColor.AQUA + gameHandler.getPlugin().ll("broadcasts.prizeItems", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[ITEM]", itemStack.getType().toString(), "[AMOUNT]", String.valueOf(itemStack.getAmount()));
		if (gameHandler.getPlugin().getConfig().getBoolean("settings.announcePrize", true)) {
			gameHandler.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
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
		Location teleportTo = configToExactLocation(configuration.getConfigurationSection(spawn + "Spawn"));
		if (teleportTo == null) SimpleSpleef.log.warning("[SimpleSpleef] Teleport error - location was null!");
		// add player to teleport ok list
		this.teleportOkList.add(player);
		player.teleport(teleportTo);
	}

	/**
	 * get exact location from config section
	 * @param config
	 * @return
	 */
	private Location configToExactLocation(ConfigurationSection config) {
		try {
			World world = gameHandler.getPlugin().getServer().getWorld(config.getString("world"));
			return new Location(world, config.getDouble("x"), config.getDouble("y"), config.getDouble("z"), (float) config.getDouble("yaw"), (float) config.getDouble("pitch"));
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void clean() {
		// dereference stuff
		this.configuration = null;
		this.spleefers = null;
		this.looseOnTouchMaterial = null;
		this.countdown = null;
		this.teleportOkList = null;
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
			boolean broadcast = gameHandler.getPlugin().getConfig().getBoolean("settings.announceCountdown", true);
			sendMessage(ChatColor.BLUE + gameHandler.getPlugin().ll("feedback.countdownStart"), broadcast);
			
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
						sendMessage(ChatColor.BLUE + gameHandler.getPlugin().ll("feedback.countdown", "[COUNT]", String.valueOf(count), "[ARENA]", GameImpl.this.getName()), broadcast);
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
				sendMessage(ChatColor.RED + gameHandler.getPlugin().ll("feedback.countdownInterrupted"), broadcast);
				// end the game
				GameImpl.this.endGame();
			} else {
				// send message
				sendMessage(ChatColor.BLUE + gameHandler.getPlugin().ll("feedback.countdownGo"), broadcast);
				// start the game itself!
				GameImpl.this.start();
			}
		}
	}
}
