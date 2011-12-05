/**
 * 
 */
package de.beimax.simplespleefold;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.ConfigurationNode;

import com.fernferret.allpay.GenericBank;

/**
 * @author mkalus
 * 
 */
public class SimpleSpleefGame {
	/**
	 * random number generator
	 */
	private static Random generator = new Random();

	/**
	 * random gifts
	 */
	private static Material[] randomWins;

	/**
	 * return next random element from Material Array
	 */
	public static Material get(Material[] array) {
		int rnd = generator.nextInt(array.length);
		return array[rnd];
	}

	/**
	 * reference to plugin
	 */
	private final SimpleSpleef plugin;

	/**
	 *  list of spleefers currently spleefing
	 */
	private HashSet<Player> spleefers;
	
	/**
	 * reference to first player (for teleport)
	 */
	private Player firstPlayer;

	/**
	 * team list, only taken if needed
	 */
	private HashMap<Integer, HashSet<Player>> teams;

	/**
	 * list of players lost
	 */
	private HashSet<Player> lost;

	/**
	 * game has started?
	 */
	private boolean started;

	/**
	 * private countdown class
	 */
	private Countdown countdown;
	
	/**
	 * accumulated players at start of game - to count fees at the end
	 */
	private int numberOfPlayers = 0;

	/**
	 * prize money
	 */
	private int prizeMoney = 0;

	/**
	 * prize item
	 */
	private int prizeItemId = 0;

	/**
	 * also loose when touching lava - defined in the constructor by calling
	 * <code>plugin.conf.getBoolean("also_loose_in_lava", true);</code>
	 */
	private boolean alsoLooseInLava;
	
	/**
	 * remember blocks broken - defined in the constructor by calling
	 * <code>plugin.conf.getBoolean("restore_blocks_after_game", true);</code>
	 */
	private boolean restoreBlocksAfterGame;
	
	/**
	 * saved coordinates of the arena
	 */
	private int arenaMinX;
	private int arenaMinY;
	private int arenaMinZ;
	private int arenaMaxX;
	private int arenaMaxY;
	private int arenaMaxZ;
	private World arenaWorld;
	
	/**
	 * true if arena is defiend
	 */
	private boolean arenaDefined = false;
	
	/**
	 * true if arena should be protected outside games
	 */
	private boolean protectArena;
	
	/**
	 * true if spleefers may only break stuff inside the arena while playing
	 */
	private boolean disallowBreakingOutsideArena;
	
	/**
	 * remembers blocks broken
	 */
	private HashMap<Block, RememberedBlock> brokenBlocks;

	/**
	 * Constructor
	 * 
	 * @param instance
	 * @param randomWins
	 *            , defined random wins
	 */
	public SimpleSpleefGame(SimpleSpleef instance, Material[] randomWins) {
		plugin = instance;
		spleefers = null;
		teams = null;
		lost = null;
		started = false;
		countdown = null;
		SimpleSpleefGame.randomWins = randomWins;
		loadFromConfiguration();
	}
	
	/**
	 * load defaults from configuration
	 */
	public void loadFromConfiguration() {
		// to make this work faster
		alsoLooseInLava = plugin.conf.getBoolean("also_loose_in_lava", true);
		restoreBlocksAfterGame = plugin.conf.getBoolean("restore_blocks_after_game", false);
		// remember blocks broken, if config says so - create hashset to remember
		if (restoreBlocksAfterGame) brokenBlocks = new HashMap<Block, RememberedBlock>();
		else brokenBlocks = null;
		protectArena = plugin.conf.getBoolean("protect_arena", true);
		disallowBreakingOutsideArena = plugin.conf.getBoolean("disallow_breaking_outside_arena", true);
		// check if arena is defined
		defineArena();		
	}
	
	/**
	 * Define arena to make cube checks faster later
	 */
	private void defineArena() {
		ConfigurationNode a = plugin.conf.getNode("arenaa");
		ConfigurationNode b = plugin.conf.getNode("arenab");
		if (b == null || a == null) return; // one of them is undefined
		// same world?
		if (b.getString("world") == null || !b.getString("world").equals(a.getString("world"))) return; //nope
		arenaWorld = plugin.getServer().getWorld(a.getString("world"));
		if (arenaWorld == null) return;
		// define minimums to make checks faster later
		arenaMinX = Math.min(a.getInt("x", 0), b.getInt("x", 0));
		arenaMinY = Math.min(a.getInt("y", 0), b.getInt("y", 0));
		arenaMinZ = Math.min(a.getInt("z", 0), b.getInt("z", 0));
		arenaMaxX = Math.max(a.getInt("x", 0), b.getInt("x", 0));
		arenaMaxY = Math.max(a.getInt("y", 0), b.getInt("y", 0));
		arenaMaxZ = Math.max(a.getInt("z", 0), b.getInt("z", 0));
		arenaDefined = true;
	}
	
	/**
	 * returns true, if there are no spleefers present in the game
	 * @return
	 */
	public boolean isEmpty() {
		if (spleefers == null) return true;
		return false;
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
	 * Announce a new game without actually joining
	 */
	public void announceNewGame(Player player) {
		// game started, countdown, game already declared?
		if (started || countdown != null || spleefers != null) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll
							.getString("err_gameinprogress",
									"A game is already in progress! Please wait until it is finished."));
			return;
		}
		
		// create a new game
		spleefers = new HashSet<Player>();
		plugin.getServer()
				.broadcastMessage(
						ChatColor.GOLD
								+ plugin.ll
										.getString("announce_noteam",
												"[PLAYER] has announced a new spleef game - you are all invited to join!")
										.replaceAll("\\[PLAYER\\]",
												player.getName()));
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
			// set first player
			firstPlayer = player;
			// create set
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
		} else { // add new player to spleefers, if he or she has enough money
			// check account of player
			double fee = plugin.conf.getDouble("entryfee", 5.0);
			int feeitem = plugin.conf.getInt("entryitem", -1);
			if (fee != 0) {
				GenericBank bank = plugin.getAllPay().getEconPlugin();
				if (bank.hasEnough(player, fee, feeitem, null)) {
					bank.pay(player, fee, feeitem);
					player.sendMessage(ChatColor.AQUA
							+ plugin.ll.getString("fee_entry",
									"You paid an entry fee of [MONEY].")
									.replaceAll("\\[MONEY\\]",
											bank.getFormattedAmount(player, fee, feeitem)));					
				} else {
					player.sendMessage(ChatColor.RED
							+ plugin.ll
									.getString("fee_entry_err",
											"Insufficient funds - you need at least [MONEY].")
									.replaceAll("\\[MONEY\\]",
											bank.getFormattedAmount(player, fee, feeitem)));
					// team empty after this?
					if (spleefers.size() == 0)
						spleefers = null; // to prevent exploit that people with insufficient funds actually create empty games
					return;
				}
			}

			//save player's original position - or update timestamp if playing again...
			addOriginalLocation(player);
			
			// add player to spleefer list
			spleefers.add(player);

			//teleport to lounge, if point is set
			Location loungespawn = getExactLocationFromNode("loungespawn");
			if (loungespawn != null)
				player.teleport(loungespawn);

			// add first player if empty
			if (firstPlayer == null)
				firstPlayer = player;
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

		// remove shovel from player
		playerLoosesShovel(player);

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
		if (lost != null && lost.contains(player)) {
			lost.remove(player);
		}
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
		} else
			// replace First player if needed
			checkAndReplaceFirstPlayer(player);
	}

	/**
	 * Attempt to start a spleef game
	 * 
	 * @param player
	 * @param startsingle
	 *            true for starting the game with only one player (for testing)
	 */
	public void startGame(Player player, boolean startsingle) {
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

		// too few gamers
		if (!startsingle && spleefers.size() < 2) {
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
		// count players at start of game
		numberOfPlayers = spleefers.size();
		// receive shovels if setting has been activated in yaml file
		playersReceiveShovels();
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

		// remove shovels if needed
		playersLooseShovels();

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

		// remove shovels if needed
		playersLooseShovels();
		
		// restore blocks if needed
		restoreBlocks();

		// message and stop game
		started = false;
		spleefers = null;
		teams = null;
		lost = null;
		firstPlayer = null;
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
	 * possibly remove player on quit or on death
	 * 
	 * @param player
	 * @param died
	 *            true on a death event
	 */
	public void removePlayerOnQuitorDeath(Player player, boolean died) {
		// check, if player is in spleef list - if not, return
		if (spleefers == null || !spleefers.contains(player))
			return;

		// death of player and not have config set to kick out players after
		// death?
		if (died && !plugin.conf.getBoolean("dead_players_leavegame", true))
			return;

		// this spleefer automatically looses
		if (started) {
			playerLoses(player, null);
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
		if (died) {
			plugin.getServer()
					.broadcastMessage(
							ChatColor.DARK_PURPLE
									+ plugin.ll
											.getString("announce_player_death",
													"[PLAYER] has opted to get himself killed instead of playing - whimp!")
											.replaceAll("\\[PLAYER\\]",
													player.getName()));
		} else {
			plugin.getServer().broadcastMessage(
					ChatColor.DARK_PURPLE
							+ plugin.ll.getString("announce_player_logout",
									"[PLAYER] has left the game - whimp!")
									.replaceAll("\\[PLAYER\\]",
											player.getName()));
		}

		// are there players left?
		if (spleefers.size() == 0) {
			// reset game
			spleefers = null;
			teams = null;
			lost = null;
			return;
		} else // replace First player if needed
			checkAndReplaceFirstPlayer(player);
	}

	/**
	 * check player move, possibly losing a game
	 * 
	 * @param event
	 */
	public void checkPlayerMove(PlayerMoveEvent event) {
		// need to check?
		Player player = event.getPlayer();
		if (!needCheck(player)) return;
		
		// check current spleefer if he/she has touched water... and maybe lava
		Material touchedBlock = event.getTo().getBlock().getType();
		if (touchedBlock.compareTo(Material.STATIONARY_WATER) == 0
				|| (alsoLooseInLava && touchedBlock
						.compareTo(Material.STATIONARY_LAVA) == 0)) {
			// Ha, lost!
			playerLoses(player, touchedBlock);
		}
	}

	/**
	 * check player interactions
	 * 
	 * @param event
	 */
	public void checkPlayerInteract(PlayerInteractEvent event) {
		// need to check?
		Player player = event.getPlayer();
		if (!needCheck(player)) return;
		
		// player's action
		Action action = event.getAction();
		
		// check if block placing is disallowed
		if (plugin.conf.getBoolean("disallow_block_placing", true) && (action.compareTo(Action.RIGHT_CLICK_BLOCK) == 0 || action.compareTo(Action.RIGHT_CLICK_AIR) == 0)) {
			event.setCancelled(true);
			return;
		}

		// check instant destruction of blocks
		if (plugin.conf.getBoolean("instant_block_destroy", false) && action.compareTo(Action.LEFT_CLICK_BLOCK) == 0) {
			// check if block is inside arena
			Block clickedBlock = event.getClickedBlock();
			if (checkInsideArena(clickedBlock.getLocation())) {
				// remember block
				if (restoreBlocksAfterGame) {
					RememberedBlock rBlock = new RememberedBlock();
					rBlock.material = clickedBlock.getType();
					rBlock.data = clickedBlock.getData();
					brokenBlocks.put(clickedBlock, rBlock);
				}
				clickedBlock.setType(Material.AIR);
				clickedBlock.setData((byte) 0);
			}
			if (!disallowBreakingOutsideArena) return;
			// disallow event outside arena and inside to avoid complications
			event.setCancelled(true);
		}
	}
	
	/**
	 * Helper to check whether check function should be checked at all
	 * @param player
	 * @return
	 */
	private boolean needCheck(Player player) {
		// no checks, if no game has started
		if (!started)
			return false;
		// check, if player is in spleef list - if not, return
		if (!spleefers.contains(player))
			return false;
		// already on looser list?
		if (lost != null && lost.contains(player))
			return false;
		return true;
	}

	/**
	 * called when a player loses
	 * 
	 * @param player
	 */
	protected void playerLoses(Player player, Material material) {
		// does the looser list exist already?
		if (lost == null)
			lost = new HashSet<Player>();

		// get lava or water?
		if (material != null) { // only message if the player actually lost
			String fellInto;
			if (material.compareTo(Material.STATIONARY_LAVA) == 0)
				fellInto = plugin.ll.getString("LAVA", "lava");
			else fellInto = plugin.ll.getString("WATER", "water");
			// send message
			plugin.getServer()
					.broadcastMessage(
							ChatColor.AQUA
									+ plugin.ll
											.getString("announce_dropped_out",
													"Player [PLAYER] dropped into the [WATER] and out of the game.")
											.replaceAll("\\[PLAYER\\]",
													player.getName()).replaceAll("\\[WATER\\]", fellInto));
		}

		// add to list
		lost.add(player);

		// take away shovel from player if needed
		playerLoosesShovel(player);

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
	 *            for team number - 0 is no team
	 */
	private void declareVictory(int team) {
		// do we have to calculate prize money?
		double win = 0;
		GenericBank bank = plugin.getAllPay().getEconPlugin();
		if (prizeMoney > 0) {
			win = (double) prizeMoney;
			prizeMoney = 0; // reset prize money
		} else {
			double fixed = plugin.conf.getDouble("prizemoney_fixed", 0.0);
			double perplayer = plugin.conf.getDouble(
					"prizemoney_perplayer", 5.0);
			win = fixed + perplayer * numberOfPlayers;
		}

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
					if (plugin.conf.getBoolean("giveprizes", true)) giveRandomGift(player);
					// player wins all the money
					if (win > 0) {
						plugin.getServer()
								.broadcastMessage(
										ChatColor.GOLD
												+ plugin.ll
														.getString(
																"prize_money",
																"Player [PLAYER] has won [MONEY] of prize money.")
														.replaceAll(
																"\\[PLAYER\\]",
																player.getName())
														.replaceAll(
																"\\[MONEY\\]",
																bank.getFormattedAmount(player, win, -1)));
						bank.give(player, win, -1);
					}
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
			// each player get a fraction of the money
			win = win / teams.get(team).size();

			// give gifts, if activated in config
			if (plugin.conf.getBoolean("giveprizes", true) && plugin.conf.getBoolean("prizes", true))
				for (Player player : teams.get(team)) {
					giveRandomGift(player);
					// give money
					bank.give(player, win, -1);
				}
			if (win > 0)
				plugin.getServer()
						.broadcastMessage(
								ChatColor.GOLD
										+ plugin.ll
												.getString("prize_money_team",
														"Each member of the team received [MONEY] prize money.")
												.replaceAll("\\[MONEY\\]",
														bank.getFormattedAmount(this.firstPlayer, win, -1))); // bot so nice to have the first player here, but what the heck...
		}

		//update players' original position timestamps
		long maxTime = plugin.conf.getInt("keep_original_locations_seconds", 1200);
		if (maxTime > 0) { // only if setting is higher than -1
			// prune locations first
			pruneOriginalLocations();
			for (Player player : spleefers)
				updateOriginalLocationTimestamp(player);
		}
		
		// remove shovels if needed
		playersLooseShovels();

		// restore blocks if needed
		restoreBlocks();

		if (spleefers.size() == lost.size())
			plugin.getServer()
					.broadcastMessage(
							"No players left in game - seems to be a single player game or a bug.");

		// stop and reset game
		started = false;
		teams = null;
		spleefers = null;
		lost = null;
		numberOfPlayers = 0;
	}

	/**
	 * player gets random gift
	 * 
	 * @param player
	 */
	private void giveRandomGift(Player player) {
		Material random;
		// determine win
		if (prizeItemId != 0) { // fixed item id?
			random = Material.getMaterial(prizeItemId);
			prizeItemId = 0; // reset one time prize
		} else { // get random Material
			random = get(randomWins);
		}
		// create an item stack of the win
		ItemStack itemStack = new ItemStack(random);
		itemStack.setAmount(1);

		// add to player inventory
		player.getInventory().addItem(itemStack);
		if (player.isOnline()) { // only online players are gifted
			player.sendMessage(plugin.ll.getString("announce_grats",
					"You have won a prize: [WIN].").replaceAll("\\[WIN\\]",
					random.name()));
			plugin.getServer().broadcastMessage(
					ChatColor.GOLD
							+ plugin.ll
									.getString("announce_grats_all",
											"[PLAYER] has won a prize: [WIN].")
									.replaceAll("\\[WIN\\]", random.name())
									.replaceAll("\\[PLAYER\\]",
											player.getName()));
		}
	}

	/**
	 * if the setting has been activated, give all players a shovel
	 */
	private void playersReceiveShovels() {
		// if no shovels should be given - return
		if (!plugin.conf.getBoolean("players_get_shovel", true))
			return;
		int shovelItem = plugin.conf.getInt("shovel_item",
				Material.STONE_SPADE.getId()); // STONE_SPADE as default
		// each player receives a shovel
		for (Player player : spleefers) {
			// create an item stack
			ItemStack itemStack = new ItemStack(shovelItem);
			itemStack.setAmount(1);
			// give it to the player
			player.getInventory().addItem(itemStack);
		}
	}

	/**
	 * called at stop of game - all remaining players might loose their shovels
	 */
	private void playersLooseShovels() {
		for (Player player : spleefers) {
			// only loose shovel, if have not lost already
			if (lost == null || !lost.contains(player))
				playerLoosesShovel(player);
		}
	}

	/**
	 * called if a player drops out of the game or it stops
	 */
	private void playerLoosesShovel(Player player) {
		// if no shovels should be taken - return
		if (!plugin.conf.getBoolean("players_loose_shovel", true))
			return;
		int shovelItem = plugin.conf.getInt("shovel_item",
				Material.STONE_SPADE.getId()); // STONE_SPADE as default
		// take if, if player still has it - yes, there are exploits, but I do
		// not want to handle them ;-)
		Inventory inventory = player.getInventory();
		if (inventory.contains(shovelItem)) {
			int index = inventory.first(shovelItem);
			// get the first stack
			ItemStack stack = inventory.getItem(index);
			if (stack.getAmount() > 1) { // shovels should only come in packs of
											// one, but who knows?
				stack.setAmount(stack.getAmount() - 1); // reduce amount by one
				inventory.setItem(index, stack);
			} else {
				inventory.setItem(index, null); // remove item
			}
		}
	}

	/**
	 * sets the price money
	 * 
	 * @param player
	 * @param amount
	 */
	public void setMoneyPrize(Player player, int amount) {
		// sanity check
		if (amount < 0) {
			player.sendMessage(ChatColor.RED
					+ "ERROR: Prize money must be positive or 0!");
			return;
		}

		// reset prizeItemId on setting money
		prizeItemId = 0;

		// set or reset prize?
		if (amount == 0) {
			player.sendMessage(plugin.ll.getString("prize_item_money_deleted",
					"Prize has been deleted."));
		} else {
			player.sendMessage(plugin.ll.getString("prize_money_set",
					"Prize has been set to [MONEY].").replaceAll("\\[MONEY\\]",
					String.valueOf(amount)));
		}
		prizeMoney = amount;
	}

	/**
	 * sets the prize item
	 * 
	 * @param player
	 * @param itemId
	 */
	public void setItemPrize(Player player, int itemId) {
		// sanity check
		if (itemId != 0 && Material.getMaterial(itemId) == null) {
			player.sendMessage(ChatColor.RED + "ERROR: No such item id!");
			return;
		}

		// reset prizeMoney on setting item
		prizeMoney = 0;

		// set or reset prize?
		if (itemId == 0) {
			player.sendMessage(plugin.ll.getString("prize_item_money_deleted",
					"Prize has been deleted."));
		} else {
			player.sendMessage(plugin.ll.getString("prize_item_set",
					"Prize has been set to [ITEM].").replaceAll("\\[ITEM\\]",
					Material.getMaterial(itemId).name()));
		}
		prizeItemId = itemId;
	}
	
	/**
	 * Check block breaks to restore them after the game
	 * @param event
	 */
	public void checkBlockBreak(BlockBreakEvent event) {
		// check if arena is defined and has to be protected
		if (arenaDefined && protectArena) {
			// check if block is inside arena
			if (checkInsideArena(event.getBlock().getLocation())) {
				// inside: check if a game is running and the player is spleefer
				if (!started || spleefers == null || spleefers.size() == 0 || !spleefers.contains(event.getPlayer()) || (lost != null && lost.contains(event.getPlayer()))) {
					event.getPlayer().sendMessage(ChatColor.RED + plugin.ll.getString("block_break_prohibited",
						"You are not allowed to break this block."));
					event.setCancelled(true);
					return;
				}
			}
		}
		
		// check if arena is defined and game is running and spleefers may not change outside blocks
		if (arenaDefined && started && disallowBreakingOutsideArena && spleefers.contains(event.getPlayer()) && (lost == null || !lost.contains(event.getPlayer()))) {
			if (!checkInsideArena(event.getBlock().getLocation())) {
				event.getPlayer().sendMessage(ChatColor.RED + plugin.ll.getString("block_break_prohibited",
					"You are not allowed to break this block."));
				event.setCancelled(true);
				return;
			}
		}
		
		// no checks, if no game has started or no block breaking should be remembered
		if (started && restoreBlocksAfterGame) {
			// check, if player is in spleef list - if not, return
			Player player = event.getPlayer();
			if (!spleefers.contains(player))
				return;
			// already on looser list?
			if (lost != null && lost.contains(player))
				return;
			
			// remember block
			Block block = event.getBlock();
			RememberedBlock rBlock = new RememberedBlock();
			rBlock.material = block.getType();
			rBlock.data = block.getData();
			brokenBlocks.put(block, rBlock);
		}
	}
	
	/**
	 * main checking routine if location is within the arena
	 * @param location
	 * @return
	 */
	private boolean checkInsideArena(Location location) {
		if (location.getWorld() != arenaWorld) return false;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		if (arenaMinX <= x && x <= arenaMaxX && arenaMinY <= y && y <= arenaMaxY && arenaMinZ <= z && z <= arenaMaxZ) return true;
		return false;
	}
	
	/**
	 * Restore blocks after the game - starts waiter thread only
	 */
	protected void restoreBlocks() {
		if (!restoreBlocksAfterGame) return;
		// start a new game by starting countdown
		BlockRestoreWaiter blockRestorer = new BlockRestoreWaiter();
		blockRestorer.start();
	}
	
	/**
	 * teleport players to first one or to spawn points if set so in configuration
	 */
	protected void teleportPlayers() {
		// check spawn points
		if (teams != null) { // teleport team players to spawn
			Location bluespawn = getExactLocationFromNode("bluespawn");
			Location redspawn = getExactLocationFromNode("redspawn");
			if (redspawn != null && bluespawn != null) {
				for (Player cPlayer : teams.get(1))
					cPlayer.teleport(bluespawn);
				for (Player cPlayer : teams.get(2))
					cPlayer.teleport(redspawn);
				return;
			}
		} else { // teleport single player
			Location spawn = getExactLocationFromNode("spawn");
			if (spawn != null)
				for (Player cPlayer : spleefers)
					cPlayer.teleport(spawn);
			return;
		}
		
		// check configuration
		if (!plugin.conf.getBoolean("teleport_players_to_first", false)) return;
		// sanity check
		if (firstPlayer == null || spleefers == null) return;
		
		// cycle through players and teleport to first player
		for (Player cPlayer : spleefers) {
			if (firstPlayer != cPlayer) {
				cPlayer.teleport(firstPlayer);
			}
		}
	}
	
	/**
	 * Get an exact location from a node name
	 * @param nodeName
	 * @return
	 */
	private Location getExactLocationFromNode(String nodeName) {
		ConfigurationNode node = plugin.conf.getNode(nodeName);
		if (node != null) {
			Location location;
			try {
				location = new Location(plugin.getServer().getWorld(node.getString("world")), node.getDouble("x", 0.0), node.getDouble("y", 0.0),
						node.getDouble("z", 0.0), (float) node.getDouble("yaw", 0.0), (float) node.getDouble("pitch", 0.0));
				return location;
			} catch (Exception e) {
				plugin.getServer().broadcastMessage(ChatColor.RED + "Could not load spawn location from config node " + nodeName + ". Error in config!");
			}
		}
		return null;		
	}
	
	/**
	 * check if player is first player and change first player if needed
	 * @param player to be checked
	 */
	protected void checkAndReplaceFirstPlayer(Player player) {
		if (player != firstPlayer || spleefers == null) return;
		
		for (Player cPlayer : spleefers) {
			if (!(lost == null || lost.contains(cPlayer))) {
				if (cPlayer != firstPlayer) {
					firstPlayer = cPlayer;
					return;
				}
			}
		}
		
		firstPlayer = null; // set first player to null, if none was found
	}
	
	/**
	 * Jump to spectation point
	 * @param spectator
	 */
	public void spectateGame(Player spectator) {
		Location spectatorspawn = getExactLocationFromNode("spectatorspawn");
		if (spectatorspawn == null) {
			spectator.sendMessage(ChatColor.RED
					+ plugin.ll.getString("block_break_prohibited",
							"No spectator spawn has been defined."));
			return;
		}
		// is there a game in progress or has started? Prohibit spectation if there is no game
		if (spleefers == null) {
			spectator.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_nogameinprogress",
							"No game is in progress."));
			return;
		}
		// players may not spectate...
		if (spleefers.contains(spectator)) {
			spectator.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_you_are_spleefer",
							"You are a spleefer, not a spectator!"));
			return;
		}
		
		// save spectator's original position
		addOriginalLocation(spectator);
		
		// teleport spectator to specation point 
		spectator.teleport(spectatorspawn);
	}
	
	/**
	 * Teleport player back to where he/she started, provided the point is not too old
	 * @param player
	 */
	public void teleportPlayerBack(Player player) {
		// is the player a spleefer and the game has started?
		if (spleefers != null && spleefers.contains(player)) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_wait_for_game_to_finish",
							"Please wait for the game to finish!"));
			return;
		}
		// either game is finished or the player has left the game or he/she is a spectator
		Location loc = getAndRemoveOriginalLocation(player);
		if (loc == null) {
			player.sendMessage(ChatColor.RED
					+ plugin.ll.getString("err_could_not_find_location",
							"Could not find a location to teleport back to - maybe you waited too long!"));
			return;
		}
		// everything ok => teleport back
		player.teleport(loc);
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
		 * Thread method
		 */
		public void run() {
			plugin.getServer()
					.broadcastMessage(
							ChatColor.BLUE
									+ plugin.ll
											.getString("countdown_start",
													"Starting spleef game. There can only be one!"));
			
			teleportPlayers();

			// get time
			long start = System.currentTimeMillis() + 1000;
			int count = plugin.conf.getInt("countdown_from", 10);
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
	
	/**
	 * Remembered Block structure used by the block restore mechanism
	 * @author mkalus
	 */
	private class RememberedBlock {
		Material material;
		byte data;
	}

	/**
	 * Class that waits 2 secs before blocks are restored
	 * 
	 * @author mkalus
	 */
	private class BlockRestoreWaiter extends Thread {
		/**
		 * Thread method
		 */
		public void run() {
			try {
				sleep(2000);
			} catch (InterruptedException e) {
			}

			for (Entry<Block, RememberedBlock> entry : brokenBlocks.entrySet()) {
				Block block = entry.getKey();
				RememberedBlock rBlock = entry.getValue();
				block.setType(rBlock.material);
				block.setData(rBlock.data);
			}
			brokenBlocks = new HashMap<Block, SimpleSpleefGame.RememberedBlock>(); // create new
		}
	}
	
	/**
	 * Simple keeper class for timestamps and locations of players
	 * @author mkalus
	 *
	 */
	private class PlayerOriginalLocation {
		long timestamp;
		
		Location location;
	}

	/**
	 * Keeps data of players teleported
	 */
	private HashMap<String, PlayerOriginalLocation> playerOriginalLocations = new HashMap<String, SimpleSpleefGame.PlayerOriginalLocation>();
	
	/**
	 * Add an original location - or update old one
	 * @param player
	 */
	public void addOriginalLocation(Player player) {
		long maxTime = plugin.conf.getInt("keep_original_locations_seconds", 1200);
		if (maxTime < 0) return; // ignore -1 and lower

		// prune first
		pruneOriginalLocations();
		
		String playerName = player.getName();
		// already in list? => update old etry
		if (playerOriginalLocations.containsKey(playerName))
			updateOriginalLocationTimestamp(player);
		else {
			PlayerOriginalLocation loc = new PlayerOriginalLocation();
			loc.timestamp = System.currentTimeMillis() / 1000;
			loc.location = player.getLocation().clone();
			
			playerOriginalLocations.put(player.getName(), loc);
		}
	}
	
	/**
	 * Return original location and remove entry
	 * @param player
	 * @return
	 */
	public Location getAndRemoveOriginalLocation(Player player) {
		// prune first
		pruneOriginalLocations();

		PlayerOriginalLocation loc = playerOriginalLocations.get(player.getName());
		if (loc == null) return null;
		playerOriginalLocations.remove(player.getName());
		return loc.location;
	}
	
	/**
	 * Update timestamp of of an original location
	 * @param player
	 */
	public void updateOriginalLocationTimestamp(Player player) {
		PlayerOriginalLocation loc = playerOriginalLocations.get(player.getName());
		if (loc != null)
			loc.timestamp = System.currentTimeMillis() / 1000;
	}
	
	/**
	 * called by above methods to clean out original locations list periodically
	 */
	protected void pruneOriginalLocations() {
		long maxTime = plugin.conf.getInt("keep_original_locations_seconds", 1200);
		if (maxTime < 0) return; // should not happen...
		long checkTime = (System.currentTimeMillis() / 1000) - maxTime;
		for (Map.Entry<String, PlayerOriginalLocation> entry : playerOriginalLocations.entrySet()) {
			if (entry.getValue().timestamp < checkTime) // remove entries that are too old
				playerOriginalLocations.remove(entry.getKey());
		}
	}
}
