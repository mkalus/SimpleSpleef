/**
 * 
 */
package de.beimax.simplespleef.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * Config helper/utility class
 * @author mkalus
 */
public class ConfigHelper {
	/**
	 * available languages
	 */
	public final static String[] languagesAvailable = {"en", "de"};
	
	/**
	 * reference to plugin
	 */
	private SimpleSpleef plugin;

	/**
	 * Constructor
	 * @param plugin
	 */
	public ConfigHelper(SimpleSpleef plugin) {
		this.plugin = plugin;
	}

	/**
	 * Check for the existence of sample_config.yml in the plugin folder and copy it if it is newer/changed
	 * @return boolean true, if everything went ok
	 */
	public boolean updateSampleConfig() {
		// check, if "sample" config file has to be updated
		try {
			// output sample file
			File file = new File(this.plugin.getDataFolder(), "sample_config.yml");
			// input default file
			InputStream is = this.plugin.getResource("config.yml");
			
			boolean copyDefault;
			if (file.exists()) { // file exists - check crc32 sums of files
				if (crc32Sum(new FileInputStream(file)) == crc32Sum(is)) copyDefault =  false;
				else {
					copyDefault =  true;
					is = this.plugin.getResource("config.yml"); // reopen stream
				}
			} else { // file does not exist and can be copied right away
				copyDefault = true;
			}

			// shall we copy the resource?
			if (copyDefault) {
				// open output stream
				OutputStream out = new FileOutputStream(file);
				int read = 0;
				byte[] bytes = new byte[1024];

				// write input to output stream (aka copy file)
				while ((read = is.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}

				// close everything
				is.close();
				out.flush();
				out.close();
				SimpleSpleef.log.info("[SimpleSpleef] Updated sample_config.yml to new version.");
				return true;
			}
		} catch(Exception e) {
			SimpleSpleef.log.warning("[SimpleSpleef] Warning: Could not write sample_config.yml - reason: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Calculate crc of input stream - stream is read, but not closed!
	 * @param is
	 * @return crc32 sum of stream
	 * @throws Exception
	 */
	protected long crc32Sum(InputStream is) throws Exception {
		CRC32 crc = new CRC32();
		byte[] ba = new byte[(int) is.available()];
		is.read(ba);
		crc.update(ba);
		is.close();
		
		return crc.getValue();
	}
	
	/**
	 * update language files to comply to defaults
	 */
	public void updateLanguageFiles() {
		// load language files one by one
		for (String language : ConfigHelper.languagesAvailable) {
			// load saved file from data folder
			File languageFile = new File(this.plugin.getDataFolder(), "lang_" + language + ".yml");
			FileConfiguration languageConfig = YamlConfiguration.loadConfiguration(languageFile);
			
			// get default config from resource
			InputStream languageConfigStream = this.plugin.getResource("lang_" + language + ".yml");
		    if (languageConfigStream != null) {
		        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(languageConfigStream);
		        // update config
		        languageConfig.setDefaults(defConfig);
		        languageConfig.options().copyDefaults(true); // copy defaults, too
		        try {
		        	// save updated config
		        	languageConfig.save(languageFile);
		        } catch (Exception e) {
		        	SimpleSpleef.log.warning("[SimpleSpleef] Warning: Could not write lang_" + language + ".yml - reason: " + e.getMessage());
		        }
		    }
		}
	}
}
