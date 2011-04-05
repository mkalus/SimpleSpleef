/**
 * 
 */
package de.beimax.simplespleef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author mkalus
 * 
 */
public class SimpleSpleefGame {
	// random number generator
	private static Random generator = new Random();

	// random gifts
	private static final Material[] randomWins = { Material.APPLE,
			Material.BED, Material.BOOK, Material.CAKE,
			Material.CHAINMAIL_HELMET, Material.CLAY_BALL, Material.COMPASS,
			Material.COOKED_FISH, Material.DIAMOND, Material.FLINT_AND_STEEL,
			Material.GOLD_INGOT, Material.GLOWSTONE, Material.GOLD_HELMET,
			Material.INK_SACK, Material.IRON_INGOT, Material.IRON_HELMET,
			Material.OBSIDIAN, Material.RED_ROSE, Material.SNOW_BLOCK,
			Material.STRING, Material.SULPHUR, Material.TNT,
			Material.YELLOW_FLOWER };

	// return next random element from Material Array
	public static Material get(Material[] array) {
		int rnd = generator.nextInt(array.length);
		return array[rnd];
	}

	private final SimpleSpleef plugin;

	// list of spleefers currently spleefing
	private HashSet<Player> spleefers;

	// team list, only taken if needed
	private HashMap<Integer, HashSet<Player>> teams;

	// list of players lost
	private HashSet<Player> lost;

	// game has started?
	private boolean started;

	// private countdown class
	private Countdown countdown;

	/**
	 * Constructor
	 * 
	 * @param instance
	 */
	public SimpleSpleefGame(SimpleSpleef instance) {
		plugin = instance;
		spleefers = null;
		teams = null;
		lost = null;
		started = false;
		countdown = null;
	}

	/**
	 * return team name
	 * 
	 * @return
	 */
	public String getTeamName(int team) {
		String teamname = team == 1 ? "blue" : "red";
		return plugin.ll.getString("team" + teamname, teamname);
	}

	/**
	 * Add a player to a team
	 * 
	 * @param player
	 *            to be added
	 * @param team
	 *            to add the player to - may be null
	 */
	public void addPlayer(Player player, Integer team) {
		if (started || countdown != null) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll
							.getString("err_gameinprogress",
									"A game is already in progress! Please wait until it is finished."));
			return;
		}

		// create list of spleefers, if needed
		if (spleefers == null) {
			spleefers = new HashSet<Player>();
			// tell everyone
			if (team == null)
				plugin.getServer()
						.broadcastMessage(
								ChatColor.GOLD
										+ plugin.ll
												.getString("announce_noteam",
														"[PLAYER] has announced a new spleef game - you are all invited to join!")
												.replaceAll("\\[PLAYER\\]",
														player.getName()));
			else
				plugin.getServer()
						.broadcastMessage(
								ChatColor.GOLD
										+ plugin.ll
												.getString(
														"announce_team",
														"[PLAYER] has announced a new spleef game in the team [TEAM] - you are all invited to join!")
												.replaceAll("\\[PLAYER\\]",
														player.getName())
												.replaceAll("\\[TEAM\\]",
														getTeamName(team)));
		} else { // do some sanity checks on commands trying to add players to
					// teams without teams, etc.
			// trying to add player to team, but there is no team play defined
			if (teams == null && team != null) {
				player.sendMessage(ChatColor.RED
						+ plugin.ll
								.getString(
										"err_gamenoteamgame",
										"The current game has not been declared a team game. Please join by typing /spleef join."));
				return;
			}
			// opposite case - trying to add player without a team
			if (teams != null && team == null) {
				player.sendMessage(ChatColor.RED
						+ plugin.ll
								.getString("err_gamenoteamgame",
										"The current game has been declared a team game. Please join one of the teams."));
				return;
			}
		}

		// ok, spleef might have been created, now test if player needs to be
		// added or change team - first check, if player is already registered
		if (spleefers.contains(player)) {
			// check for teams?
			if (teams == null) { // easy case - display error
				player.sendMessage(ChatColor.RED
						+ plugin.ll.getString("err_joined_already",
								"You have already joined the game."));
				return;
			} else {
				// check team affiliation
				HashSet<Player> likeToJoinTeam = teams.get(team); // get defined
																	// team
				if (likeToJoinTeam.contains(player)) {
					player.sendMessage(ChatColor.RED
							+ plugin.ll.getString("err_joined_team_already",
									"You have already joined this team."));
					return;
				}
				// change team affiliation
				Integer otherTeamId = team == 1 ? 2 : 1;
				HashSet<Player> otherTeam = teams.get(otherTeamId); // get other
																	// team
				likeToJoinTeam.add(player);
				otherTeam.remove(player);
				plugin.getServer()
						.broadcastMessage(
								ChatColor.GREEN
										+ plugin.ll
												.getString(
														"announce_team_change",
														"[PLAYER] changed to to team [TEAM].")
												.replaceAll("\\[PLAYER\\]",
														player.getName())
												.replaceAll("\\[TEAM\\]",
														getTeamName(team)));
			}
		} else { // add new player to spleefers
			spleefers.add(player);
			// also add to team?
			if (team != null) {
				if (teams == null) { // if empty create new hash map set
					teams = new HashMap<Integer, HashSet<Player>>();
					teams.put(1, new HashSet<Player>());
					teams.put(2, new HashSet<Player>());
				}
				HashSet<Player> myTeam = teams.get(team);
				myTeam.add(player);
				// send message
				plugin.getServer()
						.broadcastMessage(
								ChatColor.GREEN
										+ plugin.ll
												.getString(
														"announce_team_join",
														"[PLAYER] joined spleef in team [TEAM].")
												.replaceAll("\\[PLAYER\\]",
														player.getName())
												.replaceAll("\\[TEAM\\]",
														getTeamName(team)));
			} else {
				// send message
				plugin.getServer().broadcastMessage(
						ChatColor.GREEN
								+ plugin.ll.getString("announce_join",
										"[PLAYER] joined spleef!").replaceAll(
										"\\[PLAYER\\]", player.getName()));
			}
		}
	}

	/**
	 * Leave a game
	 * 
	 * @param player
	 */
	public void leavePlayer(Player player) {
		// game has already begun?
		if (started || countdown != null) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll
							.getString("err_gameinprogress",
									"A game is already in progress! Please wait until it is finished."));
			return;
		}

		// sanity check
		if (spleefers == null || !spleefers.contains(player)) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_leave_notjoined",
							"You have not joined the game anyway."));
			return;
		}

		// delete player from list
		spleefers.remove(player);
		// remove from possible team, too
		if (teams != null) {
			if (teams.get(1).contains(player))
				teams.get(1).remove(player);
			else
				teams.get(2).remove(player);
		}
		// possibly remove player from loser list
		if (lost != null && lost.contains(player))
			lost.remove(player);
		plugin.getServer().broadcastMessage(
				ChatColor.GREEN
						+ plugin.ll.getString("announce_leave",
								"[PLAYER] left the game - what a whimp!")
								.replaceAll("\\[PLAYER\\]", player.getName()));

		// are there players left?
		if (spleefers.size() == 0) {
			// reset game
			spleefers = null;
			teams = null;
			lost = null;
		}
	}

	/**
	 * Attempt to start a spleef game
	 * 
	 * @param player
	 */
	public void startGame(Player player) {
		// game already started
		if (started || countdown != null) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_gameinprogress_short",
							"A game is already in progress!"));
			return;
		}

		// no spleefers in game
		if (spleefers == null) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_game_nospleefers",
							"There are no spleefers in the game!"));
			return;
		}

		// too few games
		if (spleefers.size() < 2) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll
							.getString("err_minplayers",
									"A minimum of two players is needed to start the game!"));
			return;
		}

		// No members in one team
		if (teams != null
				&& (teams.get(1).size() == 0 || teams.get(2).size() == 0)) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_minplayers_team",
							"There must be at least one player in each team!"));
			return;
		}

		// start a new game by starting countdown
		countdown = new Countdown(plugin);
		countdown.start();
	}

	/**
	 * starts a game and deletes the countdown
	 */
	private void startGameAndDeleteCountdown() {
		started = true;
		countdown = null;
	}

	/**
	 * only delete countdown
	 */
	private void deleteCountdown() {
		countdown = null;
	}

	/**
	 * Attempt to stop a spleef game
	 * 
	 * @param player
	 */
	protected void stopGame(Player player) {
		// no game started
		if (!started && countdown == null) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_gamenotinprogress",
							"There is no game in progress!"));
			return;
		}

		// still in countdown?
		if (countdown != null)
			countdown.interrupted = true;

		// message and stop game
		started = false;
		plugin.getServer()
				.broadcastMessage(
						ChatColor.DARK_PURPLE
								+ plugin.ll
										.getString("announce_gamestopped",
												"Game has been stopped, please stay where you are."));
	}

	/**
	 * Attempt to delete a spleef game
	 * 
	 * @param player
	 */
	protected void deleteGame(Player player) {
		if (spleefers == null)
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_gamenotinprogress",
							"There is no game in progress!"));

		// still in countdown?
		if (countdown != null)
			countdown.interrupted = true;

		// message and stop game
		started = false;
		spleefers = null;
		teams = null;
		lost = null;
		plugin.getServer().broadcastMessage(
				ChatColor.DARK_PURPLE
						+ plugin.ll.getString("announce_gamedeleted",
								"Game was stopped and deleted."));
	}

	/**
	 * List all spleefers
	 * 
	 * @param player
	 */
	protected void listSpleefers(Player player) {
		// no spleefers in the game
		if (spleefers == null) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("list_nospleefers",
							"No spleefers present."));
			return;
		}

		// print spleefers with or without team list
		if (teams == null) {
			player.sendMessage(plugin.ll.getString("list",
					"Dog-eats-dog game with following players: [PLAYERS].")
					.replaceAll("\\[PLAYERS\\]", compileList(spleefers)));
		} else { // team lists
			player.sendMessage(plugin.ll
					.getString("list_team", "Team [TEAM]: [PLAYERS].")
					.replaceAll("\\[TEAM\\]", getTeamName(1))
					.replaceAll("\\[PLAYERS\\]", compileList(teams.get(1))));
			player.sendMessage(plugin.ll
					.getString("list_team", "Team [TEAM]: [PLAYERS].")
					.replaceAll("\\[TEAM\\]", getTeamName(2))
					.replaceAll("\\[PLAYERS\\]", compileList(teams.get(2))));
		}
	}

	/**
	 * compile a player list
	 * 
	 * @param players
	 * @return formated String of players in list
	 */
	private String compileList(HashSet<Player> players) {
		// no players - e.g. in empty teams?
		if (players.size() == 0)
			return "---";

		// compile string
		StringBuffer list = new StringBuffer();
		boolean first = true;
		for (Player player : players) {
			// only first has no , before
			if (!first)
				list.append(ChatColor.WHITE + ", ");
			else
				first = false;
			// color player names red or green, depending on whether they still
			// play or not
			if (lost != null && lost.contains(player))
				list.append(ChatColor.RED);
			else
				list.append(ChatColor.GREEN);
			list.append(player.getName());
		}

		return list.toString();
	}

	/**
	 * possibly remove player on quit
	 * 
	 * @param player
	 */
	public void removePlayerOnQuit(Player player) {
		// check, if player is in spleef list - if not, return
		if (spleefers == null || !spleefers.contains(player))
			return;

		// this spleefers automatically looses
		if (started) {
			playerLoses(player);
			return;
		}
		// interrupt countdown
		if (countdown != null) {
			countdown.interrupted = true;
		}
		// not started - do stuff
		// already on looser list - does not matter
		if (lost != null && lost.contains(player))
			return;
		// remove player from list otherwise
		spleefers.remove(player);
		// remove from possible team, too
		if (teams != null) {
			if (teams.get(1).contains(player))
				teams.get(1).remove(player);
			else
				teams.get(2).remove(player);
		}
		// possibly remove player from loser list
		if (lost != null && lost.contains(player))
			lost.remove(player);

		// inform world
		plugin.getServer().broadcastMessage(
				ChatColor.DARK_PURPLE
						+ plugin.ll.getString("announce_player_logout",
								"Player has left the game - whimp!")
								.replaceAll("\\[PLAYER\\]", player.getName()));

		// are there players left?
		if (spleefers.size() == 0) {
			// reset game
			spleefers = null;
			teams = null;
			lost = null;
			return;
		}
	}

	/**
	 * check player move, possibly losing a game
	 * 
	 * @param event
	 */
	public void checkPlayerMove(PlayerMoveEvent event) {
		// no checks, if no game has started
		if (!started)
			return;
		// check, if player is in spleef list - if not, return
		Player player = event.getPlayer();
		if (!spleefers.contains(player))
			return;
		// already on looser list?
		if (lost != null && lost.contains(player))
			return;

		// check current spleefer if he/she has touched water...
		if (event.getTo().getBlock().getType()
				.compareTo(Material.STATIONARY_WATER) == 0) {
			// Ha, lost!
			playerLoses(player);
		}
	}

	/**
	 * called when a player loses
	 * 
	 * @param player
	 */
	protected void playerLoses(Player player) {
		// does the looser list exist already?
		if (lost == null)
			lost = new HashSet<Player>();

		// send message
		plugin.getServer()
				.broadcastMessage(
						ChatColor.AQUA
								+ plugin.ll
										.getString("announce_dropped_out",
												"Player [PLAYER] dropped into the water and out of the game.")
										.replaceAll("\\[PLAYER\\]",
												player.getName()));

		// add to list
		lost.add(player);

		// check victory conditions
		if (teams == null) { // easy
			if (spleefers.size() - 1 <= lost.size())
				declareVictory(0);
		} else { // in teams, checking is more complicated
			HashSet<Player> myTeam = teams.get(1).contains(player) ? teams
					.get(1) : teams.get(2);
			// check drowned player's team member for loss
			for (Player playerToCheck : myTeam) {
				// ok, one member still in team -> return
				if (!lost.contains(playerToCheck))
					return;
			}
			// team lost - other team wins
			declareVictory(teams.get(1).contains(player) ? 2 : 1);
		}
	}

	/**
	 * called on a victory
	 * 
	 * @param team
	 *            for team numnber - 0 is no team
	 */
	private void declareVictory(int team) {
		if (team == 0) {
			// who has won?
			for (Player player : spleefers) {
				if (!lost.contains(player)) {
					plugin.getServer()
							.broadcastMessage(
									ChatColor.GOLD
											+ plugin.ll
													.getString("announce_won",
															"Player [PLAYER] has won the spleef game. Congratulations!")
													.replaceAll("\\[PLAYER\\]",
															player.getName()));
					// give gift
					giveRandomGift(player);
					break;
				}
			}
		} else { // team win
			plugin.getServer()
					.broadcastMessage(
							ChatColor.GOLD
									+ plugin.ll
											.getString("announce_won_team",
													"Team [TEAM] has won the spleef game. Congratulations go to: [PLAYERS].")
											.replaceAll("\\[TEAM\\]",
													getTeamName(team))
											.replaceAll(
													"\\[PLAYERS\\]",
													compileList(teams.get(team))));
			// give gifts
			for (Player player : teams.get(team)) {
				giveRandomGift(player);
			}
		}

		// stop and reset game
		started = false;
		teams = null;
		spleefers = null;
		lost = null;
	}

	/**
	 * player gets random gift
	 * 
	 * @param player
	 */
	private void giveRandomGift(Player player) {
		// get random Material
		Material random = get(randomWins);

		// add to player inventory
		player.getInventory().addItem(new ItemStack(random));
		if (player.isOnline()) { // only online players are gifted
			player.sendMessage(plugin.ll.getString("announce_grats",
					"You have won a price: [WIN].").replaceAll("\\[WIN\\]",
					random.name()));
			plugin.getServer().broadcastMessage(
					ChatColor.GOLD
							+ plugin.ll
									.getString("announce_grats_all",
											"[PLAYER] has won a price: [WIN].")
									.replaceAll("\\[WIN\\]", random.name())
									.replaceAll("\\[PLAYER\\]",
											player.getName()));
		}
	}

	private class Countdown extends Thread {
		// flag to toggle interrupts
		private boolean interrupted = false;

		// reference to plugin
		private SimpleSpleef plugin;

		/**
		 * Constructor
		 * 
		 * @param plugin
		 */
		public Countdown(SimpleSpleef plugin) {
			this.plugin = plugin;
		}

		/**
		 * Thread class
		 */
		public void run() {
			plugin.getServer()
					.broadcastMessage(
							ChatColor.BLUE
									+ plugin.ll
											.getString("countdown_start",
													"Starting spleef game. There can only be one!"));

			// get time
			long start = System.currentTimeMillis() + 1000;
			int count = 10;
			boolean started = false; // start flag

			do {
				if (System.currentTimeMillis() >= start) {
					// actually start game
					if (count == 0)
						started = true;
					// set next countdown event
					else {
						start = start + 1000;
						// Broadcast countdown
						plugin.getServer().broadcastMessage(
								ChatColor.BLUE
										+ plugin.ll.getString(
												"countdown_prefix", "Spleef: ")
										+ count);
						count--;
					}
				}
				try {
					sleep(10);
				} catch (InterruptedException e) {
				}
			} while (!started && !interrupted);

			if (interrupted) {
				plugin.getServer().broadcastMessage(
						ChatColor.BLUE
								+ plugin.ll.getString("countdown_interrupted",
										"Countdown interrupted."));
				deleteCountdown();
			} else { // started
				plugin.getServer()
						.broadcastMessage(
								ChatColor.BLUE
										+ plugin.ll.getString("countdown_go",
												"Goooo!"));
			}
			startGameAndDeleteCountdown();
		}
	}
}
