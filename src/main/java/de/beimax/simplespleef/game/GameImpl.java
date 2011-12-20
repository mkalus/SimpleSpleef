/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
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

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 * Simple Game implementation
 */
public class GameImpl extends Game {
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
	 * Constructor
	 * @param gameHandler
	 * @param name
	 */
	public GameImpl(GameHandler gameHandler, String name) {
		super(gameHandler, name);
		this.spleefers = new SpleeferList();
		this.countdown = null;
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
		// TODO: check funds of player...
		// check gamemode and change it if needed
		if (player.getGameMode() != GameMode.SURVIVAL) {
			//this.gameHandler.getPlugin().getServer().dispatchCommand(this.gameHandler.getPlugin().getServer().getConsoleSender(), "gamemode maxkalus 0");
			player.setGameMode(GameMode.SURVIVAL);
			player.sendMessage(ChatColor.YELLOW + this.gameHandler.getPlugin().ll("feedback.gamemodeChanged"));
		}
		if (!spleefers.addSpleefer(player)) { // some weird error
			player.sendMessage(ChatColor.DARK_RED + "Internal error while joining occured! Please tell the SimpleSpleef creator!");
		}
		// TODO: remember player's last position
		// TODO: teleport player to lobby
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
			//TODO: meaningful
			return false;
		}
		// TODO: minimum number of players?
		// TODO: start countdown, if setting is 0 or higher
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
	public boolean stop() {
		// actually end the game
		endGame();
		// TODO: send message
		return true;
	}
	

	@Override
	public boolean delete() {
		// end the game first, if game is started
		endGame();
		// TODO: change status to delete
		// call the game handler to tell it that the game is over
		gameHandler.gameOver(this);
		return false;
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
		//TODO kill countdown, if needed
		// TODO: change game status
		// possibly take away shovel items
		removeShovelItems();
		// possibly restore inventories
		restoreAllInventories();
		// TODO: teleport players to lobby
		// change game status
		status = STATUS_NEW;
		return false;
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
		if (!isInProgress() || spleefers.hasLost(event.getPlayer())) return; // if game is not in progress or player has lost, return
		
		//player touched certain block (setting looseOnTouchBlocks)
		if (looseOnTouchMaterial != null) {
			Material touchedBlock = event.getTo().getBlock().getType();
			Material onBlock = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
			if (looseOnTouchMaterial.contains(touchedBlock) || looseOnTouchMaterial.contains(onBlock)) {
				// TODO: send message
				// Ha, lost!
				playerLoses(event.getPlayer());
			}
		}
		//TODO: check location within "loose" cuboid (setting loose)
	}

	@Override
	public boolean playerMayTeleport(Player player) {
		// TODO - check if the player is on the teleport-ok-list, delete him/her from list and return true
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerDeath(Player player) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * called when player loses a game
	 * @param player
	 */
	protected void playerLoses(Player player) {
		// set player to lost
		spleefers.setLost(player);
		// TODO Message
		player.sendMessage("You loose!");
		// determine if game is over...
		if (spleefers.inGame() <= configuration.getInt("remainingPlayersWin", 1))
			gameOver();
	}
	
	/**
	 * called when game is over - get winners, etc.
	 */
	protected void gameOver() {
		// determine winners
		for (Spleefer spleefer : spleefers.get()) {
			if (!spleefer.hasLost()) { // not lost?
				//this guy is a winner
				spleefer.getPlayer().sendMessage("You won!");
				//TODO: winning message!
				// TODO pay prizes
			}
		}
		
		// clean up game and end it
		endGame();
		// call the game handler to tell it that the game is over
		gameHandler.gameOver(this);
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
		//TODO implement
		return false;
	}
	
	/**
	 * if "playersLooseShovelAtGameEnd" setting of area is true, remove shovel from inventory of player
	 * @return
	 */
	protected boolean removeShovelItems() {
		//TODO implement
		return false;
	}
	
	@Override
	public void clean() {
		// dereference stuff
		this.configuration = null;
		this.spleefers = null;
		this.looseOnTouchMaterial = null;
		this.countdown = null;
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
			
			// TODO: teleport players to arena

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
