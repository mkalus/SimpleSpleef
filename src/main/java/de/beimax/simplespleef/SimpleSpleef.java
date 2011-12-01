package de.beimax.simplespleef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import org.bukkit.Server;

import com.fernferret.allpay.AllPay;

/**
 * SimpleSpleef for Bukkit
 * 
 * @author maxkalus
 */
public class SimpleSpleef extends JavaPlugin {
	public static Logger log = Logger.getLogger("Minecraft");
	private SimpleSpleefGame game;
	private SimpleSpleefPlayerListener playerListener;
	private SimpleSpleefEntityListener entityListener;
	private SimpleSpleefBlockListener blockListener;
	public Configuration conf; // General configuration
	public Configuration ll; // Language configuration

	private static PluginListener PluginListener = null;
	private static Server Server = null;

	/**
	 * @return BukkitServer
	 */
	public static Server getBukkitServer() {
		return Server;
	}

	/**
	 * AllPay singleton
	 */
	private static AllPay allPay = null;

	/**
	 * @return allPay instance (singleton)
	 */
	public AllPay getAllPay() {
		if (SimpleSpleef.allPay == null) SimpleSpleef.allPay = new AllPay(this, "SimpleSpleef: ");
		return SimpleSpleef.allPay;
	}

	/**
	 * initialize object, load config mainly
	 */
	public void init() {
		// create plugin dir, if needed
		new File("plugins" + File.separator + "SimpleSpleef" + File.separator)
				.mkdirs();

		// check files
		File confFile = new File("plugins" + File.separator + "SimpleSpleef",
				"SimpleSpleef.yml");
		File enFile = new File("plugins" + File.separator + "SimpleSpleef",
				"lang_en.yml");
		File deFile = new File("plugins" + File.separator + "SimpleSpleef",
				"lang_de.yml");
		File langFile;

		// load configuration
		conf = new Configuration(confFile);
		// check file
		if (!confFile.exists()) {
			log.info("[SimpleSpleef] Creating new configuration file.");
			try {
				FileWriter fw = new FileWriter(confFile);
				fw.write("lang: en\ngiveprizes: true\nprizes:\n- APPLE\n- BED\n- BOOK\n- CAKE\n- CHAINMAIL_HELMET\n- CLAY_BALL\n- COMPASS\n- COOKED_FISH\n- DIAMOND\n- FLINT_AND_STEEL\n- GOLD_INGOT\n- GLOWSTONE\n- GOLD_HELMET\n- INK_SACK\n- IRON_INGOT\n- IRON_HELMET\n- OBSIDIAN\n- RED_ROSE\n- SNOW_BLOCK\n- STRING\n- SULPHUR\n- TNT\n- YELLOW_FLOWER\n");
				fw.close();
			} catch (Exception e) {
				log.warning("[SimpleSpleef] Could not write SimpleSpleef.yml: "
						+ e.getMessage());
			}
		}
		conf.load(); // load conf file
		// check config file
		boolean changed = false; // flag to set config change

		// new elements
		if (conf.getProperty("giveprizes") == null) { // no giveprizes property
														// exists
			conf.setProperty("giveprizes", true);
			changed = true;
		} else if (conf.getProperty("prices") != null) { // change prices to
															// giveprizes
			conf.setProperty("giveprizes", conf.getBoolean("prices", true));
			conf.removeProperty("prices"); // delete property
			changed = true;
		}
		if (conf.getProperty("prizes") == null) { // define default wins
			String[] defaultWins = { "APPLE", "BED", "BOOK", "CAKE",
					"CHAINMAIL_HELMET", "CLAY_BALL", "COMPASS", "COOKED_FISH",
					"DIAMOND", "FLINT_AND_STEEL", "GOLD_INGOT", "GLOWSTONE",
					"GOLD_HELMET", "INK_SACK", "IRON_INGOT", "IRON_HELMET",
					"OBSIDIAN", "RED_ROSE", "SNOW_BLOCK", "STRING", "SULPHUR",
					"TNT", "YELLOW_FLOWER" };
			conf.setProperty("prizes", defaultWins);
			changed = true;
		}
		if (conf.getProperty("entryfee") == null) { // define entry fee
			conf.setProperty("entryfee", 5);
			changed = true;
		}
		if (conf.getProperty("entryitem") == null) { // define entry item (or -1 for money)
			conf.setProperty("entryitem", -1);
			changed = true;
		}
		if (conf.getProperty("prizemoney_fixed") == null) { // define fixed prize money
			conf.setProperty("prizemoney_fixed", 0);
			changed = true;
		}
		if (conf.getProperty("prizemoney_perplayer") == null) { // define prize money per player
			conf.setProperty("prizemoney_perplayer", 5);
			changed = true;
		}
		if (conf.getProperty("players_get_shovel") == null) { // define if players receive a shovel
			conf.setProperty("players_get_shovel", true);
			changed = true;
		}
		if (conf.getProperty("players_loose_shovel") == null) { // define if players loose a shovel after the game
			conf.setProperty("players_loose_shovel", true);
			changed = true;
		}
		if (conf.getProperty("shovel_item") == null) { // define the item id of the shovel
			conf.setProperty("shovel_item", Material.STONE_SPADE.getId()); // 273 as default
			changed = true;
		}
		if (conf.getProperty("dead_players_leavegame") == null) { // define if dead players should leave the game
			conf.setProperty("dead_players_leavegame", true);
			changed = true;
		}
		if (conf.getProperty("also_loose_in_lava") == null) { // define if lava should be defined as material to drop out
			conf.setProperty("also_loose_in_lava", true);
			changed = true;
		}
		if (conf.getProperty("restore_blocks_after_game") == null) { // define if game should keep track of changed blocks and restore them after the game
			conf.setProperty("restore_blocks_after_game", true);
			changed = true;
		}
		if (conf.getProperty("teleport_players_to_first") == null) { // should the game teleport all players to the first after starting game
			conf.setProperty("teleport_players_to_first", false);
			changed = true;
		}
		if (conf.getProperty("countdown_from") == null) { // countdown starts at
			conf.setProperty("countdown_from", 10);
			changed = true;
		}		
		if (conf.getProperty("protect_arena") == null) { // protect arena from being changed by anyone except during games (arena needs to be set up!)
			conf.setProperty("protect_arena", true);
			changed = true;
		}		
		if (conf.getProperty("disallow_breaking_outside_arena") == null) { // disallow breaking of blocks out of the arena once the game has started (arena needs to be set up!)
			conf.setProperty("disallow_breaking_outside_arena", true);
			changed = true;
		}		
		if (conf.getProperty("keep_original_locations_seconds") == null) { // remember original locations
			conf.setProperty("keep_original_locations_seconds", 1200);
			changed = true;
		}		

		// config has been changed: save it
		if (changed) {
			log.info("[SimpleSpleef] Updating configuration file to new version.");
			conf.save();
		}

		// load language files
		if (!enFile.exists()) {
			log.info("[SimpleSpleef] Creating new English language file.");
			try {
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(enFile), "UTF-8");
				//FileWriter fw = new FileWriter(enFile);
				fw.write("teamred: red\n"
						+ "teamblue: blue\n"
						+ "announce_noteam: '[PLAYER] has announced a new spleef game - you are all invited to\n"
						+ "    join!'\n"
						+ "announce_team: '[PLAYER] has announced a new spleef game in the team [TEAM] - you\n"
						+ "    are all invited to join!'\n"
						+ "announce_join: '[PLAYER] joined spleef!'\n"
						+ "announce_team_join: '[PLAYER] joined spleef in team [TEAM].'\n"
						+ "announce_team_change: '[PLAYER] changed to to team [TEAM].'\n"
						+ "announce_leave: '[PLAYER] left the game - what a whimp!'\n"
						+ "announce_player_logout: '[PLAYER] has left the game - whimp!'\n"
						+ "announce_player_death: '[PLAYER] has opted to get himself killed instead of playing - whimp!'\n"
						+ "announce_dropped_out: 'Player [PLAYER] dropped into the [WATER] and out of the game.'\n"
						+ "water: 'water'\n"
						+ "lava: 'lava'\n"
						+ "announce_won: 'Player [PLAYER] has won the spleef game. Congratulations!'\n"
						+ "announce_won_team: 'Team [TEAM] has won the spleef game. Congratulations go to: [PLAYERS].'\n"
						+ "announce_grats: 'You have won a prize: [WIN].'\n"
						+ "announce_grats_all: '[PLAYER] has won a prize: [WIN].'\n"
						+ "announce_gamestopped: 'Game has been stopped, please stay where you are.'\n"
						+ "announce_gamedeleted: 'Game was stopped and deleted.'\n"
						+ "countdown_start: 'Starting spleef game. There can only be one!'\n"
						+ "countdown_prefix: 'Spleef: '\n"
						+ "countdown_go: 'Goooo!'\n"
						+ "countdown_interrupted: 'Countdown interrupted.'\n"
						+ "list: 'Dog-eats-dog game with following players: [PLAYERS].'\n"
						+ "list_team: 'Team [TEAM]: [PLAYERS].'\n"
						+ "list_nospleefers: 'No spleefers present.'\n"
						+ "err_game_nospleefers: 'There are no spleefers in the game!'\n"
						+ "err_gameinprogress: 'A game is already in progress! Please wait until it is finished.'\n"
						+ "err_gameteamgame: 'The current game has been declared a team game. Please join one of the teams.'\n"
						+ "err_minplayers_team: 'There must be at least one player in each team!'\n"
						+ "err_leave_notjoined: 'You have not joined the game anyway.'\n"
						+ "err_gameinprogress_short: 'A game is already in progress!'\n"
						+ "err_gamenotinprogress: 'There is no game in progress!'\n"
						+ "err_joined_already: 'You have already joined the game.'\n"
						+ "err_minplayers: 'A minimum of two players is needed to start the game!'\n"
						+ "err_gamenoteamgame: 'The current game has not been declared a team game. Please join by typing /spleef join.'\n"
						+ "err_joined_team_already: 'You have already joined this team.'\n"
						+ "fee_entry: 'You paid an entry fee of [MONEY].'\n"
						+ "fee_entry_err: 'Insufficient funds - you need at least [MONEY].'\n"
						+ "prize_money: 'Player [PLAYER] has won [MONEY] of prize money.'\n"
						+ "prize_money_team: 'Each member of the team received [MONEY] prize money.'\n"
						+ "prize_item_set: 'Prize has been set to [ITEM].'\n"
						+ "prize_money_set: 'Prize has been set to [MONEY].'\n"
						+ "prize_item_money_deleted: 'Prize has been deleted.'\n"
						+ "block_break_prohibited: 'You are not allowed to break this block.'\n"
						+ "no_spectator_spawn_defined: 'No spectator spawn has been defined.'\n"
						+ "err_wait_for_game_to_finish: 'Please wait for the game to finish!'\n"
						+ "err_could_not_find_location: 'Could not find a location to teleport back to - maybe you waited too long!'\n"
						+ "err_nogameinprogress: 'No game is in progress.'\n"
						+ "err_you_are_spleefer: 'You are a spleefer, not a spectator!'\n");
				fw.close();
			} catch (Exception e) {
				log.warning("[SimpleSpleef] Could not write lang_en.yml: "
						+ e.getMessage());
			}
		}

		if (!deFile.exists()) {
			log.info("[SimpleSpleef] Creating new German language file.");
			try {
				//FileWriter fw = new FileWriter(deFile);
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(deFile), "UTF-8");
				fw.write("teamred: rot\n"
						+ "teamblue: blau\n"
						+ "announce_noteam: '[PLAYER] hat ein neues Spleef-Spiel angekündigt - kommt alle zur\n"
						+ "    Arena!'\n"
						+ "announce_team: '[PLAYER] hat ein neues Spleef-Spiel im Team [TEAM] angekündigt - kommt\n"
						+ "    alle zur Arena!'\n"
						+ "announce_join: '[PLAYER] spielt Spleef mit!'\n"
						+ "announce_team_join: '[PLAYER] spielt Spleef im Team [TEAM].'\n"
						+ "announce_team_change: '[PLAYER] wechselt zum Spleef-Team [TEAM].'\n"
						+ "announce_leave: '[PLAYER] verlässt das Spleef-Spiel. So ein Feigling!'\n"
						+ "announce_player_logout: '[PLAYER] hat das Spleef-Spiel verlassen - Feigling!'\n"
						+ "announce_player_death: '[PLAYER] ist lieber gestorben als zu spielen - Feigling!'\n"
						+ "announce_dropped_out: '[PLAYER] ist [WATER] gefallen und ausgeschieden!'\n"
						+ "water: 'ins Wasser'\n"
						+ "lava: 'in die Lava'\n"
						+ "announce_won: '[PLAYER] hat gewonnen. Herzlichen Glückwunsch!'\n"
						+ "announce_won_team: 'Team [TEAM] hat gewonnen. Herzlichen Glückwunsch an: [PLAYERS].'\n"
						+ "announce_grats: 'Herzlichen Glückwunsch zum Sieg! Du hast gewonnen: [WIN].'\n"
						+ "announce_grats_all: '[PLAYER] hat einen Preis gewonnen: [WIN].'\n"
						+ "announce_gamestopped: 'Spleef-Spiel wurde unterbrochen - bitte alle Stehenbleiben!'\n"
						+ "announce_gamedeleted: 'Spleef-Spiel wurde gestoppt und gelöscht!'\n"
						+ "countdown_start: 'Starte Spleef-Spiel: Es kann nur einen geben!'\n"
						+ "countdown_prefix: 'Spleef: '\n"
						+ "countdown_go: 'LOOOOOS!'\n"
						+ "countdown_interrupted: 'Countdown abgebrochen.'\n"
						+ "list: 'Jeder-gegen-jeden-Spiel mit folgenden Spielern: [PLAYERS].'\n"
						+ "list_team: 'Team [TEAM]: [PLAYERS].'\n"
						+ "list_nospleefers: 'Keine Spleefer im Spiel.'\n"
						+ "err_game_nospleefers: 'Es gibt keine Spleefer im Spiel!'\n"
						+ "err_gameinprogress: 'Es läuft gerade ein Spiel! Bitte warte, bis es beendet ist.'\n"
						+ "err_gameteamgame: 'Das aktuelle Spiel ist ein Team-Spiel. Bitte melde dich in einem der beiden Teams an!'\n"
						+ "err_minplayers_team: 'In beiden Teams muss sich mindestens ein Spieler befinden!'\n"
						+ "err_leave_notjoined: 'Du spielst ja sowieso nicht mit!'\n"
						+ "err_gameinprogress_short: 'Das Spiel läuft bereits!'\n"
						+ "err_gamenotinprogress: 'Es läuft gerade kein Spleef-Spiel!'\n"
						+ "err_joined_already: 'Du bist bereits im Spiel angemeldet!'\n"
						+ "err_minplayers: 'Mindestens zwei Spleefer müssen am Spiel teilnehmen!'\n"
						+ "err_gamenoteamgame: 'Das aktuelle Spiel ist nicht als Team-Spiel angekündigt. Bitte melde dich einfach mit /spleef join an.'\n"
						+ "err_joined_team_already: 'Du bist bereits in diesem Team angemeldet!'\n"
						+ "fee_entry: 'Du hast ein Startgeld von [MONEY] gezahlt.'\n"
						+ "fee_entry_err: 'Du hast nicht die benötigten [MONEY] Startgeld.'\n"
						+ "prize_money: 'Spieler [PLAYER] hat ein Preisgeld in Höhe von [MONEY] gewonnen.'\n"
						+ "prize_money_team: 'Jeder Spieler des Teams hat ein Preisgeld in Höhe von [MONEY] gewonnen.'\n"
						+ "prize_item_set: 'Preis wurde gesetzt: [ITEM].'\n"
						+ "prize_money_set: 'Preis wurde gesetzt: [MONEY].'\n"
						+ "prize_item_money_deleted: 'Ausgelobter Preis wurde gelöscht.'\n"
						+ "block_break_prohibited: 'Du darfst diesen Block nicht zerstören.'\n"
						+ "no_spectator_spawn_defined: 'Es wurde kein Spawnpunkt für Zuschauer definiert.'\n"
						+ "err_wait_for_game_to_finish: 'Bitte warte auf das Ende des Spiels!'\n"
						+ "err_could_not_find_location: 'Konnte keinen Ort für das Zurückteleportieren mehr finden - vielleicht hast du zu lange gewartet!'\n"
						+ "err_nogameinprogress: 'Momentan kein Spiel.'\n"
						+ "err_you_are_spleefer: 'Du bist ein Spleefer, kein Zuschauer!'\n");
				fw.close();
			} catch (Exception e) {
				log.warning("[SimpleSpleef] Could not write lang_de.yml: "
						+ e.getMessage());
			}
		}

		// Now load configuration file
		langFile = new File("plugins" + File.separator + "SimpleSpleef",
				"lang_" + conf.getString("lang", "en") + ".yml");
		ll = new Configuration(langFile);
		ll.load();
	}

	/**
	 * called on enable
	 */
	public void onEnable() {
		init(); // load init
		if (!createGame()) { // try to create game
			log.warning("[SimpleSpleef] could not correctly initialize plugin. Disabling it.");
			return;
		}

		// set server
		Server = getServer();

		// register enable events from other plugins
		PluginListener = new PluginListener();

		getServer().getPluginManager().registerEvent(Event.Type.PLUGIN_ENABLE,
				PluginListener, Priority.Monitor, this);

		// Register our events
		PluginManager pm = getServer().getPluginManager();

		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener,
				Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener,
				Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DEATH, entityListener, // deaths are handled by entity
				Priority.Normal, this);
		
		// Register block breaks, if accounting is set on
		//if (conf.getBoolean("restore_blocks_after_game", false)) {
			pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener,
					Priority.Normal, this);
		//	System.out.println("[SimpleSpleef] restore_blocks_after_game is enabled - listening to block breaks when game is on.");
		//}

		
		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println("[SimpleSpleef] " + pdfFile.getName() + " version "
				+ pdfFile.getVersion() + " is enabled!");
	}

	/**
	 * tries to create a game
	 * 
	 * @return true, if correctly created, false if there was a problem
	 */
	protected boolean createGame() {
		// try to load list of wins
		List<String> wins = conf.getStringList("prizes", null);

		// list empty - should not happen, but might be...
		if (wins == null) {
			log.warning("[SimpleSpleef] Could not load prize list - it was empty or malformed.");
			return false;
		}

		// load materials into list
		LinkedList<Material> winsMat = new LinkedList<Material>();
		// iterate through string and transform them to a material
		for (String win : wins) {
			Material m = Material.getMaterial(win);
			if (m == null)
				log.warning("[SimpleSpleef] Could not find a block or material called "
						+ win + ". Ignoring it!");
			else
				winsMat.add(m); // add to list
		}

		// check, if list is still empty after transformation
		if (winsMat.isEmpty()) {
			log.warning("[SimpleSpleef] Could not load prize list - it was empty in the end.");
			return false;
		}

		// turn list into array
		Material[] randomWins = new Material[winsMat.size()];
		randomWins = winsMat.toArray(randomWins);

		// ok, now load game
		game = new SimpleSpleefGame(this, randomWins);
		playerListener = new SimpleSpleefPlayerListener(this, game);
		entityListener = new SimpleSpleefEntityListener(this, game);
		blockListener = new SimpleSpleefBlockListener(this, game);
		return true;
	}

	/**
	 * Catch commands
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		// ignore commands if disabled
		if (game == null)
			return true;

		String command = cmd.getName();

		// only players
		if (!(sender instanceof Player))
			return false;
		Player player = (Player) sender;

		// command without any parameters
		if (args.length < 1) {
			return false;
		} else {
			command = args[0];
			// one argument
			if (args.length == 1) {
				// check aliases
				if (command.equalsIgnoreCase("blue"))
					command = "1"; // alias for team 1
				else if (command.equalsIgnoreCase("red"))
					command = "2"; // alias for team 2
	
				// check actual commands
				if (command.equalsIgnoreCase("join")) {
					// has first player permission on empty game?
					if (game.isEmpty() && !checkPermission(player, "initialize"))
						return true;
					if (!checkPermission(player, "play"))
						return true; // check permission
					game.addPlayer(player, null); // join no-team spleef
				} else if (command.equalsIgnoreCase("announce")) {
					// announce game without actually joining
					if (!checkPermission(player, "announce"))
						return true; // check permission
					game.announceNewGame(player);
				} else if (command.equalsIgnoreCase("1")) {
					// has first player permission on empty game?
					if (game.isEmpty() && !checkPermission(player, "initializeteam"))
						return true;
					if (!checkPermission(player, "team"))
						return true; // check permission
					game.addPlayer(player, 1); // add player to team 1
				} else if (command.equalsIgnoreCase("2")) {
					// has first player permission on empty game?
					if (game.isEmpty() && !checkPermission(player, "initializeteam"))
						return true;
					if (!checkPermission(player, "team"))
						return true; // check permission
					game.addPlayer(player, 2); // add player to team 2
				} else if (command.equalsIgnoreCase("leave")) {
					if (!checkPermission(player, "leave"))
						return true; // check permission
					game.leavePlayer(player); // player leaves spleef
				} else if (command.equalsIgnoreCase("list")) {
					if (!checkPermission(player, "list"))
						return true; // check permission
					game.listSpleefers(player); // print a list of
												// spleefers
				} else if (command.equalsIgnoreCase("start")) {
					if (!checkPermission(player, "start"))
						return true; // check permission
					game.startGame(player, false); // start a game
				} else if (command.equalsIgnoreCase("startsingle")) {
					if (!checkPermission(player, "startsingle"))
						return true; // check permission
					game.startGame(player, true); // start a game
				} else if (command.equalsIgnoreCase("stop")) {
					if (!checkPermission(player, "stop"))
						return true; // check permission
					game.stopGame(player); // stop a game
				} else if (command.equalsIgnoreCase("delete")) {
					if (!checkPermission(player, "delete"))
						return true; // check permission
					game.deleteGame(player); // delete a game
				} else if (command.equalsIgnoreCase("spectate")) {
					if (!checkPermission(player, "spectate"))
						return true; // check permission
					game.spectateGame(player); // jump to spectation spawn point
				} else if (command.equalsIgnoreCase("back")) {
					if (!checkPermission(player, "back"))
						return true; // check permission
					game.teleportPlayerBack(player); // jump back to saved point
				} else if (command.equalsIgnoreCase("reload")) {
					if (!checkPermission(player, "reload"))
						return true; // check permission
					reloadConfiguration(player); // reload configuration
				} else
					return false;
			} else { // multiple arguments
				if (command.equalsIgnoreCase("prize")) {
					if (!checkPermission(player, "prize"))
						return true; // check permission
					// get second argument
					String second = args[1];
					if (second.equalsIgnoreCase("money")) { // money prize
						if (args.length != 3) return false;
						try {
							Integer money = Integer.parseInt(args[2]);
							if (money == null) return false;
							game.setMoneyPrize(player, (int) money);
						} catch (NumberFormatException e) {
							return false;
						}
					} else { // item prize, possibly
						if (args.length != 2) return false;
						try {
							int prizeId = Integer.parseInt(second);
							game.setItemPrize(player, prizeId);
						} catch (NumberFormatException e) {
							return false;
						}
					}
				} else if (command.equalsIgnoreCase("admin")) {
					if (!checkPermission(player, "admin"))
						return true; // check permission
					// get second argument
					String second = args[1];
					if (second.equalsIgnoreCase("setspawn")) {
						try {
							conf.setProperty("spawn", getExactLocation(player.getLocation()));
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "Spleef spawn could not be set!");
						}
						if (conf.save()) player.sendMessage(ChatColor.GREEN + "Spleef spawn location set.");
						else player.sendMessage(ChatColor.RED + "Spleef spawn could not be saved!");
					} else if (second.equalsIgnoreCase("setbluespawn")) {
						try {
							conf.setProperty("bluespawn", getExactLocation(player.getLocation()));
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "Spleef blue spawn could not be set!");
						}
						if (conf.save()) player.sendMessage(ChatColor.GREEN + "Spleef blue spawn location set.");
						else player.sendMessage(ChatColor.RED + "Spleef blue spawn could not be saved!");						
					} else if (second.equalsIgnoreCase("setredspawn")) {
						try {
							conf.setProperty("redspawn", getExactLocation(player.getLocation()));
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "Spleef red spawn could not be set!");
						}
						if (conf.save()) player.sendMessage(ChatColor.GREEN + "Spleef red spawn location set.");
						else player.sendMessage(ChatColor.RED + "Spleef red spawn could not be saved!");
					} else if (second.equalsIgnoreCase("a")) {
						try {
							conf.setProperty("arenaa", getStandingOnLocation(player.getLocation()));
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "Spleef arena point could not be set!");
						}
						if (conf.save()) player.sendMessage(ChatColor.GREEN + "Spleef arena point set.");
						else player.sendMessage(ChatColor.RED + "Spleef arena point could not be saved!");
					} else if (second.equalsIgnoreCase("b")) {
						try {
							conf.setProperty("arenab", getStandingOnLocation(player.getLocation()));
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "Spleef arena point could not be set!");
						}
						if (conf.save()) player.sendMessage(ChatColor.GREEN + "Spleef arena point set.");
						else player.sendMessage(ChatColor.RED + "Spleef arena point could not be saved!");
					} else if (second.equalsIgnoreCase("spectatorspawn")) {
						try {
							conf.setProperty("spectatorspawn", getExactLocation(player.getLocation()));
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "Spectator spawn could not be set!");
						}
						if (conf.save()) player.sendMessage(ChatColor.GREEN + "Spectator spawn location set.");
						else player.sendMessage(ChatColor.RED + "Spectator spawn could not be saved!");						
					} else if (second.equalsIgnoreCase("loungespawn")) {
						try {
							conf.setProperty("loungespawn", getExactLocation(player.getLocation()));
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "Lounge spawn could not be set!");
						}
						if (conf.save()) player.sendMessage(ChatColor.GREEN + "Lounge spawn location set.");
						else player.sendMessage(ChatColor.RED + "Lounge spawn could not be saved!");						
					} else
						return false;
				} else
					return false;
			}
		}
		return true;
	}
	
	/**
	 * get an exact location
	 * @param location
	 * @return
	 */
	private Map<String, Object> getExactLocation(Location location) {
		ConfigurationNode locNode = Configuration.getEmptyNode();
		locNode.setProperty("world", location.getWorld().getName());
		locNode.setProperty("x", location.getX());
		locNode.setProperty("y", location.getY());
		locNode.setProperty("z", location.getZ());
		locNode.setProperty("yaw", location.getYaw());
		locNode.setProperty("pitch", location.getPitch());
		
		return locNode.getAll();
	}

	/**
	 * get an exact location
	 * @param location
	 * @return
	 */
	private Map<String, Object> getStandingOnLocation(Location location) {
		ConfigurationNode locNode = Configuration.getEmptyNode();
		locNode.setProperty("world", location.getWorld().getName());
		locNode.setProperty("x", location.getBlockX());
		locNode.setProperty("y", location.getBlockY() - 1); // one below
		locNode.setProperty("z", location.getBlockZ());
		
		return locNode.getAll();
	}

	/**
	 * reload configuration
	 * @param Player player calling reload
	 */
	private void reloadConfiguration(Player player) {
		File confFile = new File("plugins" + File.separator + "SimpleSpleef",
		"SimpleSpleef.yml");
		// check if the conf file has disappeared somehow
		if (!confFile.exists() || !confFile.canRead()) {
			player.sendMessage(ChatColor.RED + "ERROR: Configuration file does not exist or is not readable!");
			return;
		}
		// reload configuration
		conf = new Configuration(confFile);
		conf.load();
		game.loadFromConfiguration(); // reload stuff
		player.sendMessage(ChatColor.GOLD + "Spleef configuration refreshed.");
	}

	/**
	 * helper method to check permission
	 * 
	 * @param player
	 *            player to check
	 * @param permission
	 *            String of part of permission like "join"
	 * @return
	 */
	private boolean checkPermission(Player player, String permission) {
		// the new permission system is very easy
		if (player.hasPermission("simplespleef." + permission)
				|| player.hasPermission("simplespleef.*"))
			return true;
		player.sendMessage(ChatColor.RED
				+ "You do not have the permission to use this command!");
		return false;
	}

	/**
	 * called on disable
	 */
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();

		System.out.println("[SimpleSpleef] " + pdfFile.getName() + " version "
				+ pdfFile.getVersion() + " was disabled!");
	}
}
