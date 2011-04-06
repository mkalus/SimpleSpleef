package de.beimax.simplespleef;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;

/**
 * SimpleSpleef for Bukkit
 * 
 * @author maxkalus
 */
public class SimpleSpleef extends JavaPlugin {
	public static Logger log = Logger.getLogger("Minecraft");
	private final SimpleSpleefGame game;
	private final SimpleSpleefPlayerListener playerListener;
	private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
	public Configuration conf;
	public Configuration ll; //Language configuration

	/**
	 * Constructor
	 */
	public SimpleSpleef() {
		game = new SimpleSpleefGame(this);
		playerListener = new SimpleSpleefPlayerListener(this, game);
	}

	public void init() {
		// create plugin dir, if needed
		new File("plugins" + File.separator + "SimpleSpleef" + File.separator).mkdirs();
		
		// check files
		File confFile = new File("plugins" + File.separator + "SimpleSpleef", "SimpleSpleef.yml");
		File enFile = new File("plugins" + File.separator + "SimpleSpleef", "lang_en.yml");
		File deFile = new File("plugins" + File.separator + "SimpleSpleef", "lang_de.yml");
		File langFile;
		
		// load configuration
		conf = new Configuration(confFile);
		// check file
		if (!confFile.exists()) {
			log.info("Creating new configuration file for SimpleSpleef.");
			try {
				FileWriter fw = new FileWriter(confFile);
				fw.write("lang: en\nprices: true\n");
				fw.close();
			} catch (Exception e) {
				log.warning("Could not write SimpleSpleef.yml: " + e.getMessage());
			}
		}
		conf.load(); // load conf file
		
		// load language files
		if (!enFile.exists()) {
			log.info("Creating new English language file for SimpleSpleef.");
			try {
				FileWriter fw = new FileWriter(enFile);
				fw.write("teamred: red\n" +
						"teamblue: blue\n" +
						"announce_noteam: '[PLAYER] has announced a new spleef game - you are all invited to\n" +
						"    join!'\n" +
						"announce_team: '[PLAYER] has announced a new spleef game in the team [TEAM] - you\n" +
						"    are all invited to join!'\n" +
						"announce_join: '[PLAYER] joined spleef!'\n" +
						"announce_team_join: '[PLAYER] joined spleef in team [TEAM].'\n" +
						"announce_team_change: '[PLAYER] changed to to team [TEAM].'\n" +
						"announce_leave: '[PLAYER] left the game - what a whimp!'\n" +
						"announce_player_logout: Player has left the game - whimp!\n" +
						"announce_dropped_out: Player [PLAYER] dropped into the water and out of the game.\n" +
						"announce_won: Player [PLAYER] has won the spleef game. Congratulations!\n" +
						"announce_won_team: 'Team [TEAM] has won the spleef game. Congratulations go to: [PLAYERS].'\n" +
						"announce_grats: 'You have won a prize: [WIN].'\n" +
						"announce_grats_all: '[PLAYER] has won a prize: [WIN].'\n" +
						"announce_gamestopped: Game has been stopped, please stay where you are.\n" +
						"announce_gamedeleted: Game was stopped and deleted.\n" +
						"countdown_start: Starting spleef game. There can only be one!\n" +
						"countdown_prefix: 'Spleef: '\n" +
						"countdown_go: Goooo!\n" +
						"countdown_interrupted: Countdown interrupted.\n" +
						"list: 'Dog-eats-dog game with following players: [PLAYERS].'\n" +
						"list_team: 'Team [TEAM]: [PLAYERS].'\n" +
						"list_nospleefers: No spleefers present.\n" +
						"err_game_nospleefers: There are no spleefers in the game!\n" +
						"err_gameinprogress: A game is already in progress! Please wait until it is finished.\n" +
						"err_gameteamgame: The current game has been declared a team game. Please join one of the teams.\n" +
						"err_minplayers_team: There must be at least one player in each team!\n" +
						"err_leave_notjoined: You have not joined the game anyway.\n" +
						"err_gameinprogress_short: A game is already in progress!\n" +
						"err_gamenotinprogress: There is no game in progress!\n" +
						"err_joined_already: You have already joined the game.\n" +
						"err_minplayers: A minimum of two players is needed to start the game!\n" +
						"err_gamenoteamgame: The current game has not been declared a team game. Please join by typing /spleef join.\n" +
						"err_joined_team_already: You have already joined this team.\n");
				fw.close();
			} catch (Exception e) {
				log.warning("Could not write lang_en.yml: " + e.getMessage());
			}
		}

		if (!deFile.exists()) {
			log.info("Creating new German language file for SimpleSpleef.");
			try {
				FileWriter fw = new FileWriter(deFile);
				fw.write("teamred: rot\n" +
						"teamblue: blau\n" +
						"announce_noteam: '[PLAYER] hat ein neues Spleef-Spiel angekündigt - kommt alle zur\n" +
						"    Arena!'\n" +
						"announce_team: '[PLAYER] hat ein neues Spleef-Spiel im Team [TEAM] angekündigt - kommt\n" +
						"    alle zur Arena!'\n" +
						"announce_join: '[PLAYER] spielt Spleef mit!'\n" +
						"announce_team_join: '[PLAYER] spielt Spleef im Team [TEAM].'\n" +
						"announce_team_change: '[PLAYER] wechselt zum Spleef-Team [TEAM].'\n" +
						"announce_leave: '[PLAYER] verlässt das Spleef-Spiel. So ein Feigling!'\n" +
						"announce_player_logout: '[PLAYER] hat das Spleef-Spiel verlassen - Feigling!'\n" +
						"announce_dropped_out: '[PLAYER] ist ausgeschieden!'\n" +
						"announce_won: '[PLAYER] hat gewonnen. Herzlichen Glückwunsch!'\n" +
						"announce_won_team: 'Team [TEAM] hat gewonnen. Herzlichen Glückwunsch an: [PLAYERS].'\n" +
						"announce_grats: 'Herzlichen Glückwunsch zum Sieg! Du hast gewonnen: [WIN].'\n" +
						"announce_grats_all: '[PLAYER] hat einen Preis gewonnen: [WIN].'\n" +
						"announce_gamestopped: Spleef-Spiel wurde unterbrochen - bitte alle Stehenbleiben!\n" +
						"announce_gamedeleted: Spleef-Spiel wurde gestoppt und gelöscht!\n" +
						"countdown_start: 'Starte Spleef-Spiel: Es kann nur einen geben!'\n" +
						"countdown_prefix: 'Spleef: '\n" +
						"countdown_go: LOOOOOS!\n" +
						"countdown_interrupted: Countdown abgebrochen.\n" +
						"list: 'Jeder-gegen-jeden-Spiel mit folgenden Spielern: [PLAYERS].'\n" +
						"list_team: 'Team [TEAM]: [PLAYERS].'\n" +
						"list_nospleefers: Keine Spleefer im Spiel.\n" +
						"err_game_nospleefers: Es gibt keine Spleefer im Spiel\n" +
						"err_gameinprogress: Es läuft gerade ein Spiel! Bitte warte, bis es beendet ist.\n" +
						"err_gameteamgame: Das aktuelle Spiel ist ein Team-Spiel. Bitte melde dich in einem der beiden Teams an!\n" +
						"err_minplayers_team: In beiden Teams muss sich mindestens ein Spieler befinden!\n" +
						"err_leave_notjoined: Du spielst ja sowieso nicht mit!\n" +
						"err_gameinprogress_short: Das Spiel läuft bereits!\n" +
						"err_gamenotinprogress: Es läuft gerade kein Spleef-Spiel!\n" +
						"err_joined_already: Du bist bereits im Spiel angemeldet!\n" +
						"err_minplayers: Mindestens zwei Spleefer müssen am Spiel teilnehmen!\n" +
						"err_gamenoteamgame: Das aktuelle Spiel ist nicht als Team-Spiel angekündigt. Bitte melde dich einfach mit /spleef join an.\n" +
						"err_joined_team_already: Du bist bereits in diesem Team angemeldet!\n");
				fw.close();
			} catch (Exception e) {
				log.warning("Could not write lang_de.yml: " + e.getMessage());
			}
		}
		
		// Now load configuration file
		langFile = new File("plugins" + File.separator + "SimpleSpleef", "lang_" + conf.getString("lang", "en") + ".yml");
		ll = new Configuration(langFile);
		ll.load();
	}

	public void onEnable() {
		init();

		// Register our events
		PluginManager pm = getServer().getPluginManager();

		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener,
				Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener,
				Priority.Normal, this);

		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println(pdfFile.getName() + " version "
				+ pdfFile.getVersion() + " is enabled!");
	}

	/**
	 * Catch commands
	 */
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		String command = cmd.getName();

		// only players
		if (!(sender instanceof Player))
			return false;
		Player player = (Player) sender;

		// command without any parameters
		if (args.length != 1) {
			return false;
		} else {
			command = args[0];
			// check aliases
			if (command.equalsIgnoreCase("blau"))
				command = "1"; // alias for team 1
			else if (command.equalsIgnoreCase("rot"))
				command = "2"; // alias for team 2

			// check actual commands
			if (command.equalsIgnoreCase("join"))
				game.addPlayer(player, null); // join no-team spleef
			else if (command.equalsIgnoreCase("1"))
				game.addPlayer(player, 1); // add player to team 1
			else if (command.equalsIgnoreCase("2"))
				game.addPlayer(player, 2); // add player to team 2
			else if (command.equalsIgnoreCase("leave"))
				game.leavePlayer(player); // player leaves spleef
			else if (command.equalsIgnoreCase("list"))
				game.listSpleefers(player); // print a list of
														// spleefers
			else if (command.equalsIgnoreCase("start"))
				game.startGame(player); // start a game
			else if (command.equalsIgnoreCase("stop"))
				game.stopGame(player); // stop a game
			else if (command.equalsIgnoreCase("delete"))
				game.deleteGame(player); // delete a game
			else
				return false;
		}
		return true;
	}

	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();

		System.out.println(pdfFile.getName() + " version "
				+ pdfFile.getVersion() + " was disabled!");
	}

	public boolean isDebugging(final Player player) {
		if (debugees.containsKey(player)) {
			return debugees.get(player);
		} else {
			return false;
		}
	}

	public void setDebugging(final Player player, final boolean value) {
		debugees.put(player, value);
	}
}
