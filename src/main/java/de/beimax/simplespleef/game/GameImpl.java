/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

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
		if (!spleefers.addSpleefer(player)) { // some weird error
			player.sendMessage(ChatColor.DARK_RED + "Internal error while joining occured! Please tell the SimpleSpleef creator!");
		}
		// TODO clear inventory
		// TODO: add to inventory
		// TODO: alternatively give shovels
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
		// TODO start game
		return false;
	}

	@Override
	public boolean stop() {
		// TODO: change game status
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete() {
		// TODO Auto-generated method stub
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
				// change status back
				status = STATUS_NEW;
				// TODO: teleport players back to lobby
				// send message
				sendMessage(ChatColor.RED + gameHandler.getPlugin().ll("feedback.countdownInterrupted"), broadcast);
				deleteCountdown();
			} else {
				// send message
				sendMessage(ChatColor.BLUE + gameHandler.getPlugin().ll("feedback.countdownGo"), broadcast);
				// start the game itself!
				GameImpl.this.start();
			}
		}
	}
}
