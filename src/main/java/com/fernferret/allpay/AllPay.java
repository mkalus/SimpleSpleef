package com.fernferret.allpay;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;

import cosine.boseconomy.BOSEconomy;
import fr.crafter.tickleman.RealEconomy.RealEconomy;
import fr.crafter.tickleman.RealPlugin.RealPlugin;

/**
 * AllPay is a nifty little payment wrapper class that takes the heavy lifting out of integrating payments into your plugin!
 * 
 * @author Eric Stokes
 */
public class AllPay {
    private final double version = 3;
    protected final String logPrefix = "[AllPay] - Version " + version;

    protected static final Logger log = Logger.getLogger("Minecraft");
    private String prefix;
    private Plugin plugin;
    private GenericBank bank;
    public final static String[] validEconPlugins = { "Essentials", "RealShop", "BOSEconomy", "iConomy", "MultiCurrency" };
    public static List<String> pluginsThatUseUs = new ArrayList<String>();

    public AllPay(Plugin plugin, String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
        pluginsThatUseUs.add(this.plugin.getDescription().getName());
    }

    /**
     * Load an econ plugin. Plugins are loaded in this order: iConomy, BOSEconomy, RealShop, Essentials and simple items
     * 
     * @return The GenericBank object to process payments.
     */
    public GenericBank loadEconPlugin() {
        loadiConomy(); // Supports both 4.x and 5.x
        loadBOSEconomy();
        loadRealShopEconomy();
        loadEssentialsEconomoy();
        loadDefaultItemEconomy();
        this.bank.setPrefix(this.prefix);
        return this.bank;
    }

    /**
     * Returns the AllPay GenericBank object that you can issue calls to and from
     * 
     * @return The GenericBank object to process payments.
     */
    public GenericBank getEconPlugin() {
        return this.bank;
    }

    public double getVersion() {
        return this.version;
    }

    private void loadEssentialsEconomoy() {
        if (this.bank == null) {
            try {
                Plugin essentialsPlugin = this.plugin.getServer().getPluginManager().getPlugin("Essentials");
                if (essentialsPlugin != null) {
                    this.bank = new EssentialsBank();
                    log.info(logPrefix + " - hooked into Essentials Economy for " + this.plugin.getDescription().getFullName());
                }
            } catch (Exception e) {
                log.warning(logPrefix + "You are using a VERY old version of Essentials. Please upgrade it.");
            }
        }
    }

    private void loadRealShopEconomy() {
        if (this.bank == null && !(this.bank instanceof EssentialsBank)) {
            Plugin realShopPlugin = this.plugin.getServer().getPluginManager().getPlugin("RealShop");
            if (realShopPlugin != null) {
                RealEconomy realEconPlugin = new RealEconomy((RealPlugin) realShopPlugin);
                log.info(logPrefix + " - hooked into RealEconomy for " + this.plugin.getDescription().getFullName());
                this.bank = new RealEconomyBank(realEconPlugin);
            }
        }
    }

    private void loadBOSEconomy() {
        if (this.bank == null && !(this.bank instanceof EssentialsBank)) {
            Plugin boseconPlugin = this.plugin.getServer().getPluginManager().getPlugin("BOSEconomy");
            if (boseconPlugin != null) {

                this.bank = new BOSEconomyBank((BOSEconomy) boseconPlugin);
                log.info(logPrefix + " - hooked into BOSEconomy " + this.plugin.getDescription().getFullName());
            }
        }
    }

    private void loadDefaultItemEconomy() {
        if (this.bank == null) {
            this.bank = new ItemBank();
            log.info(logPrefix + " - using only an item based economy for " + this.plugin.getDescription().getFullName());
        }
    }

    private void loadiConomy() {
        if (this.bank == null && !(this.bank instanceof EssentialsBank)) {
            Plugin iConomyTest = this.plugin.getServer().getPluginManager().getPlugin("iConomy");
            try {
                if (iConomyTest != null && iConomyTest instanceof com.iCo6.iConomy) {
                    this.bank = new iConomyBank6X();
                    log.info(logPrefix + " - hooked into iConomy 6 for " + this.plugin.getDescription().getFullName());
                }
            } catch (NoClassDefFoundError e) {
                loadiConomy5X(iConomyTest);
            }
        }
    }

    private void loadiConomy5X(Plugin iConomyTest) {
        try {
            if (iConomyTest != null && iConomyTest instanceof com.iConomy.iConomy) {
                this.bank = new iConomyBank5X();
                log.info(logPrefix + " - hooked into iConomy 5 for " + this.plugin.getDescription().getFullName());
            }
        } catch (NoClassDefFoundError ex) {
            if (iConomyTest != null) {
                loadiConomy4X();
            }
        }
    }

    private void loadiConomy4X() {
        com.nijiko.coelho.iConomy.iConomy iConomyPlugin = (com.nijiko.coelho.iConomy.iConomy) this.plugin.getServer().getPluginManager().getPlugin("iConomy");
        if (iConomyPlugin != null) {
            this.bank = new iConomyBank4X();
            log.info(logPrefix + " - hooked into iConomy 4 for " + this.plugin.getDescription().getFullName());
        }
    }

}
