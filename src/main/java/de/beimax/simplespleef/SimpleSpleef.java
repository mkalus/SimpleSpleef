package de.beimax.simplespleef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
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

import com.nijiko.permissions.PermissionHandler;
import org.bukkit.Server;
import com.iConomy.iConomy;

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
	public Configuration conf; // General configuration
	public Configuration ll; // Language configuration

	private static PluginListener PluginListener = null;
	private static Server Server = null;

	/**
	 * Permissions
	 */
	private static PermissionHandler Permissions;

	/**
	 * iConomy
	 */
	private static iConomy iConomy;

	/**
	 * @return BukkitServer
	 */
	public static Server getBukkitServer() {
		return Server;
	}

	/**
	 * @return Permissions instance or null
	 */
	public static PermissionHandler getPermissions() {
		return Permissions;
	}

	/**
	 * @param plugin
	 *            Permissions plugin setter
	 * @return true if set
	 */
	public static boolean setPermissions(PermissionHandler plugin) {
		if (Permissions == null) {
			Permissions = plugin;
		} else {
			return false;
		}
		return true;
	}

	/**
	 * @return iConomy instance or null
	 */
	public static iConomy getiConomy() {
		return iConomy;
	}

	/**
	 * @return true if iConomy exists
	 */
	public static boolean checkiConomy() {
		if (iConomy != null)
			return true;
		return false;
	}

	/**
	 * @param plugin
	 *            iConomy plugin setter
	 * @return true if set
	 */
	public static boolean setiConomy(iConomy plugin) {
		if (iConomy == null) {
			iConomy = plugin;
		} else {
			return false;
		}
		return true;
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

		// config has been changed: save it
		if (changed) {
			log.info("[SimpleSpleef] Updating configuration file to new version.");
			conf.save();
		}

		// load language files
		if (!enFile.exists()) {
			log.info("[SimpleSpleef] Creating new English language file.");
			try {
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(enFile),"UTF-8");
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
						+ "announce_dropped_out: Player [PLAYER] dropped into the water and out of the game.\n"
						+ "announce_won: Player [PLAYER] has won the spleef game. Congratulations!\n"
						+ "announce_won_team: 'Team [TEAM] has won the spleef game. Congratulations go to: [PLAYERS].'\n"
						+ "announce_grats: 'You have won a prize: [WIN].'\n"
						+ "announce_grats_all: '[PLAYER] has won a prize: [WIN].'\n"
						+ "announce_gamestopped: Game has been stopped, please stay where you are.\n"
						+ "announce_gamedeleted: Game was stopped and deleted.\n"
						+ "countdown_start: Starting spleef game. There can only be one!\n"
						+ "countdown_prefix: 'Spleef: '\n"
						+ "countdown_go: Goooo!\n"
						+ "countdown_interrupted: Countdown interrupted.\n"
						+ "list: 'Dog-eats-dog game with following players: [PLAYERS].'\n"
						+ "list_team: 'Team [TEAM]: [PLAYERS].'\n"
						+ "list_nospleefers: No spleefers present.\n"
						+ "err_game_nospleefers: There are no spleefers in the game!\n"
						+ "err_gameinprogress: A game is already in progress! Please wait until it is finished.\n"
						+ "err_gameteamgame: The current game has been declared a team game. Please join one of the teams.\n"
						+ "err_minplayers_team: There must be at least one player in each team!\n"
						+ "err_leave_notjoined: You have not joined the game anyway.\n"
						+ "err_gameinprogress_short: A game is already in progress!\n"
						+ "err_gamenotinprogress: There is no game in progress!\n"
						+ "err_joined_already: You have already joined the game.\n"
						+ "err_minplayers: A minimum of two players is needed to start the game!\n"
						+ "err_gamenoteamgame: The current game has not been declared a team game. Please join by typing /spleef join.\n"
						+ "err_joined_team_already: You have already joined this team.\n"
						+ "fee_entry: You paid an entry fee of [MONEY].\n"
						+ "fee_entry_err: Insufficient funds - you need at least [MONEY].\n"
						+ "prize_money: Player [PLAYER] has won [MONEY] of prize money.\n"
						+ "prize_money_team: Each member of the team received [MONEY] prize money.\n"
						+ "prize_item_set: Prize has been set to [ITEM].\n"
						+ "prize_money_set: Prize has been set to [MONEY].\n"
						+ "prize_item_money_deleted: Prize has been deleted.\n");
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
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(deFile),"UTF-8");
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
						+ "announce_dropped_out: '[PLAYER] ist ausgeschieden!'\n"
						+ "announce_won: '[PLAYER] hat gewonnen. Herzlichen Glückwunsch!'\n"
						+ "announce_won_team: 'Team [TEAM] hat gewonnen. Herzlichen Glückwunsch an: [PLAYERS].'\n"
						+ "announce_grats: 'Herzlichen Glückwunsch zum Sieg! Du hast gewonnen: [WIN].'\n"
						+ "announce_grats_all: '[PLAYER] hat einen Preis gewonnen: [WIN].'\n"
						+ "announce_gamestopped: Spleef-Spiel wurde unterbrochen - bitte alle Stehenbleiben!\n"
						+ "announce_gamedeleted: Spleef-Spiel wurde gestoppt und gelöscht!\n"
						+ "countdown_start: 'Starte Spleef-Spiel: Es kann nur einen geben!'\n"
						+ "countdown_prefix: 'Spleef: '\n"
						+ "countdown_go: LOOOOOS!\n"
						+ "countdown_interrupted: Countdown abgebrochen.\n"
						+ "list: 'Jeder-gegen-jeden-Spiel mit folgenden Spielern: [PLAYERS].'\n"
						+ "list_team: 'Team [TEAM]: [PLAYERS].'\n"
						+ "list_nospleefers: Keine Spleefer im Spiel.\n"
						+ "err_game_nospleefers: Es gibt keine Spleefer im Spiel\n"
						+ "err_gameinprogress: Es läuft gerade ein Spiel! Bitte warte, bis es beendet ist.\n"
						+ "err_gameteamgame: Das aktuelle Spiel ist ein Team-Spiel. Bitte melde dich in einem der beiden Teams an!\n"
						+ "err_minplayers_team: In beiden Teams muss sich mindestens ein Spieler befinden!\n"
						+ "err_leave_notjoined: Du spielst ja sowieso nicht mit!\n"
						+ "err_gameinprogress_short: Das Spiel läuft bereits!\n"
						+ "err_gamenotinprogress: Es läuft gerade kein Spleef-Spiel!\n"
						+ "err_joined_already: Du bist bereits im Spiel angemeldet!\n"
						+ "err_minplayers: Mindestens zwei Spleefer müssen am Spiel teilnehmen!\n"
						+ "err_gamenoteamgame: Das aktuelle Spiel ist nicht als Team-Spiel angekündigt. Bitte melde dich einfach mit /spleef join an.\n"
						+ "err_joined_team_already: Du bist bereits in diesem Team angemeldet!\n"
						+ "fee_entry: Du hast ein Startgeld von [MONEY] gezahlt.\n"
						+ "fee_entry_err: Du hast nicht die benötigten [MONEY] Startgeld.\n"
						+ "prize_money: Spieler [PLAYER] hat ein Preisgeld in Höhe von [MONEY] gewonnen.\n"
						+ "prize_money_team: Jeder Spieler des Teams hat ein Preisgeld in Höhe von [MONEY] gewonnen.\n"
						+ "prize_item_set: Preis wurde gesetzt: [ITEM].\n"
						+ "prize_money_set: Preis wurde gesetzt: [MONEY].\n"
						+ "prize_item_money_deleted: Ausgelobter Preis wurde gelöscht.\n");
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
					if (!checkPermission(player, "play"))
						return true; // check permission
					game.addPlayer(player, null); // join no-team spleef
				} else if (command.equalsIgnoreCase("1")) {
					if (!checkPermission(player, "team"))
						return true; // check permission
					game.addPlayer(player, 1); // add player to team 1
				} else if (command.equalsIgnoreCase("2")) {
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
					game.startGame(player); // start a game
				} else if (command.equalsIgnoreCase("stop")) {
					if (!checkPermission(player, "stop"))
						return true; // check permission
					game.stopGame(player); // stop a game
				} else if (command.equalsIgnoreCase("delete")) {
					if (!checkPermission(player, "delete"))
						return true; // check permission
					game.deleteGame(player); // delete a game
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
				} else
					return false;
			}
		}
		return true;
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
		// no permissions set - everybody may do everything
		if (SimpleSpleef.Permissions == null)
			return true;
		// if op, allow!
		if (player.isOp())
			return true;
		// permission checked
		if (SimpleSpleef.Permissions.has(player, "simplespleef." + permission)) {
			// inform player
			player.sendMessage(ChatColor.RED
					+ "You do not have the permission to use this command!");
			return true;
		}
		// all others may not do this!
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
