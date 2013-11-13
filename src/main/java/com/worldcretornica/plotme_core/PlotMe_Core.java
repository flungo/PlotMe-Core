package com.worldcretornica.plotme_core;

import com.griefcraft.model.Protection;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.worldcretornica.plotme_core.Metrics.Graph;
import com.worldcretornica.plotme_core.api.v0_14b.IPlotMe_ChunkGenerator;
import com.worldcretornica.plotme_core.api.v0_14b.IPlotMe_GeneratorManager;
import com.worldcretornica.plotme_core.listener.PlotDenyListener;
import com.worldcretornica.plotme_core.listener.PlotListener;
import com.worldcretornica.plotme_core.listener.PlotWorldEditListener;
import com.worldcretornica.plotme_core.utils.Util;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.yaml.snakeyaml.Yaml;

public class PlotMe_Core extends JavaPlugin {

    private String VERSION;

    private String configpath;
    private Boolean globalUseEconomy;
    private Boolean advancedlogging;
    private String language;
    private Boolean allowWorldTeleport;
    //private Boolean autoUpdate;
    private Boolean allowToDeny;
    private Boolean defaultWEAnywhere;
    private int nbClearSpools;
    private int nbBlocksPerClearStep;

    private Economy economy = null;
    private Boolean usinglwc = false;

    private World worldcurrentlyprocessingexpired;
    private CommandSender cscurrentlyprocessingexpired;
    private Integer counterexpired;
    private Integer nbperdeletionprocessingexpired;

    public Map<String, Map<String, String>> creationbuffer = null;

    //Spool stuff
    private ConcurrentLinkedQueue<PlotToClear> plotsToClear = null;
    public Set<PlotMeSpool> spools = null;
    public Set<BukkitTask> spoolTasks = null;

    //Global variables
    private PlotMeCoreManager plotmecoremanager = null;
    private SqlManager sqlmanager = null;
    private PlotWorldEdit plotworldedit = null;
    private Util util = null;

    public void onDisable() {
        for (PlotMeSpool spool : spools) {
            spool.Stop();
            spool = null;
        }
        spools.clear();
        for (BukkitTask bt : spoolTasks) {
            bt.cancel();
            bt = null;
        }
        spoolTasks = null;
        getSqlManager().closeConnection();
        getUtil().Dispose();
        setEconomy(null);
        setUsinglwc(null);
        getPlotMeCoreManager().setPlayersIgnoringWELimit(null);
        setWorldCurrentlyProcessingExpired(null);
        setCommandSenderCurrentlyProcessingExpired(null);
        setDefaultWEAnywhere(null);
        setAllowToDeny(null);
        creationbuffer = null;
        plotsToClear.clear();
        plotsToClear = null;

        if (creationbuffer != null) {
            creationbuffer.clear();
            creationbuffer = null;
        }
        //multiverse = null;
        //multiworld = null;
    }

    public void onEnable() {
        initialize();

        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlotListener(this), this);

        if (pm.getPlugin("Vault") != null) {
            setupEconomy();
        }

        if (pm.getPlugin("WorldEdit") != null) {
            setPlotWorldEdit(new PlotWorldEdit(this, (WorldEditPlugin) pm.getPlugin("WorldEdit")));
            pm.registerEvents(new PlotWorldEditListener(this), this);
        }

        if (pm.getPlugin("LWC") != null) {
            setUsinglwc(true);
        }

        if (getAllowToDeny()) {
            pm.registerEvents(new PlotDenyListener(this), this);
        }

        creationbuffer = new HashMap<String, Map<String, String>>();
        plotsToClear = new ConcurrentLinkedQueue<PlotToClear>();

        getCommand("plotme").setExecutor(new PMCommand(this));

        //Start the spools
        spoolTasks = new HashSet<BukkitTask>();
        spools = new HashSet<PlotMeSpool>();
        for (int i = 1; i <= getNbClearSpools(); i++) {
            PlotMeSpool pms = new PlotMeSpool(this);
            spools.add(pms);
            spoolTasks.add(Bukkit.getServer().getScheduler().runTaskAsynchronously(this, pms));
        }

        doMetric();
    }

    private void doMetric() {
        try {
            Metrics metrics = new Metrics(this);

            Graph graphNbWorlds = metrics.createGraph("Plot worlds");

            graphNbWorlds.addPlotter(new Metrics.Plotter("Number of plot worlds") {
                @Override
                public int getValue() {
                    return getPlotMeCoreManager().getPlotMaps().size();
                }
            });

            graphNbWorlds.addPlotter(new Metrics.Plotter("Average Plot size") {
                @Override
                public int getValue() {

                    if (getPlotMeCoreManager().getPlotMaps().size() > 0) {
                        int totalplotsize = 0;

                        for (String s : getPlotMeCoreManager().getPlotMaps().keySet()) {
                            if (getPlotMeCoreManager().getGenMan(s) != null) {
                                if (getPlotMeCoreManager().getGenMan(s).getPlotSize(s) != 0) {
                                    totalplotsize += getPlotMeCoreManager().getGenMan(s).getPlotSize(s);
                                }
                            }
                        }

                        return totalplotsize / getPlotMeCoreManager().getPlotMaps().size();
                    } else {
                        return 0;
                    }
                }
            });

            graphNbWorlds.addPlotter(new Metrics.Plotter("Number of plots") {
                @Override
                public int getValue() {
                    int nbplot = 0;

                    for (String map : getPlotMeCoreManager().getPlotMaps().keySet()) {
                        nbplot += getSqlManager().getPlotCount(map);
                    }

                    return nbplot;
                }
            });

            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
    }

    public boolean cPerms(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    public IPlotMe_GeneratorManager getGenManager(World w) {
        if (w.getGenerator() instanceof IPlotMe_ChunkGenerator) {
            IPlotMe_ChunkGenerator cg = (IPlotMe_ChunkGenerator) w.getGenerator();
            return cg.getManager();
        } else {
            return null;
        }
    }

    public IPlotMe_GeneratorManager getGenManager(String name) {
        World w = getServer().getWorld(name);
        if (w == null) {
            return null;
        } else {
            return getGenManager(getServer().getWorld(name));
        }
    }

    private void importOldConfigs(File newfile) {
        File oldfile;

        oldfile = new File(getDataFolder().getParentFile().getAbsolutePath() + File.separator + "PlotMe" + File.separator + "config.yml");

        if (!oldfile.exists()) {
            oldfile = new File(getDataFolder().getParentFile().getAbsolutePath() + File.separator + "PlotMe" + File.separator + "config.backup.yml");
        }

        if (oldfile.exists()) {
            getLogger().info("Importing old configurations");
            FileConfiguration oldconfig = new YamlConfiguration();
            FileConfiguration newconfig = new YamlConfiguration();

            try {
                oldconfig.load(oldfile);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                getLogger().severe("can't read configuration file");
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                getLogger().severe("invalid configuration format");
                e.printStackTrace();
            }

            newconfig.set("usemySQL", oldconfig.getBoolean("usemySQL", false));
            newconfig.set("mySQLconn", oldconfig.getString("mySQLconn", "jdbc:mysql://localhost:3306/minecraft"));
            newconfig.set("mySQLuname", oldconfig.getString("mySQLuname", "root"));
            newconfig.set("mySQLpass", oldconfig.getString("mySQLpass", "password"));
            newconfig.set("globalUseEconomy", oldconfig.getBoolean("globalUseEconomy", false));
            newconfig.set("AdvancedLogging", oldconfig.getBoolean("AdvancedLogging", false));
            newconfig.set("Language", oldconfig.getString("Language", "english"));
            newconfig.set("allowWorldTeleport", oldconfig.getBoolean("allowWorldTeleport", true));
            newconfig.set("defaultWEAnywhere", oldconfig.getBoolean("defaultWEAnywhere", false));
            newconfig.set("auto-update", oldconfig.getBoolean("auto-update", false));
            newconfig.set("allowToDeny", oldconfig.getBoolean("allowToDeny", true));

            ConfigurationSection oldworlds;
            ConfigurationSection newworlds;

            if (!oldconfig.contains("worlds")) {
                return;
            } else {
                oldworlds = oldconfig.getConfigurationSection("worlds");
            }

            newworlds = newconfig.createSection("worlds");

            for (String worldname : oldworlds.getKeys(false)) {
                ConfigurationSection oldcurrworld = oldworlds.getConfigurationSection(worldname);
                ConfigurationSection newcurrworld;

                if (newworlds.contains("worldname")) {
                    newcurrworld = newworlds.getConfigurationSection(worldname);
                } else {
                    newcurrworld = newworlds.createSection(worldname);
                }

                newcurrworld.set("PlotAutoLimit", oldcurrworld.getInt("PlotAutoLimit", 100));

                newcurrworld.set("DaysToExpiration", oldcurrworld.getInt("DaysToExpiration", 7));

                if (oldcurrworld.contains("ProtectedBlocks")) {
                    newcurrworld.set("ProtectedBlocks", oldcurrworld.getIntegerList("ProtectedBlocks"));
                }

                if (oldcurrworld.contains("PreventedItems")) {
                    newcurrworld.set("PreventedItems", oldcurrworld.getStringList("PreventedItems"));
                }

                newcurrworld.set("AutoLinkPlots", oldcurrworld.getBoolean("AutoLinkPlots", true));
                newcurrworld.set("DisableExplosion", oldcurrworld.getBoolean("DisableExplosion", true));
                newcurrworld.set("DisableIgnition", oldcurrworld.getBoolean("DisableIgnition", true));

                ConfigurationSection oldeconomysection;
                ConfigurationSection neweconomysection;

                if (oldcurrworld.getConfigurationSection("economy") != null) {
                    oldeconomysection = oldcurrworld.getConfigurationSection("economy");
                    neweconomysection = newcurrworld.createSection("economy");

                    neweconomysection.set("UseEconomy", oldeconomysection.getBoolean("UseEconomy", false));
                    neweconomysection.set("CanPutOnSale", oldeconomysection.getBoolean("CanPutOnSale", false));
                    neweconomysection.set("CanSellToBank", oldeconomysection.getBoolean("CanSellToBank", false));
                    neweconomysection.set("RefundClaimPriceOnReset", oldeconomysection.getBoolean("RefundClaimPriceOnReset", false));
                    neweconomysection.set("RefundClaimPriceOnSetOwner", oldeconomysection.getBoolean("RefundClaimPriceOnSetOwner", false));
                    neweconomysection.set("ClaimPrice", oldeconomysection.getDouble("ClaimPrice", 0));
                    neweconomysection.set("ClearPrice", oldeconomysection.getDouble("ClearPrice", 0));
                    neweconomysection.set("AddPlayerPrice", oldeconomysection.getDouble("AddPlayerPrice", 0));
                    neweconomysection.set("DenyPlayerPrice", oldeconomysection.getDouble("DenyPlayerPrice", 0));
                    neweconomysection.set("RemovePlayerPrice", oldeconomysection.getDouble("RemovePlayerPrice", 0));
                    neweconomysection.set("UndenyPlayerPrice", oldeconomysection.getDouble("UndenyPlayerPrice", 0));
                    neweconomysection.set("PlotHomePrice", oldeconomysection.getDouble("PlotHomePrice", 0));
                    neweconomysection.set("CanCustomizeSellPrice", oldeconomysection.getBoolean("CanCustomizeSellPrice", false));
                    neweconomysection.set("SellToPlayerPrice", oldeconomysection.getDouble("SellToPlayerPrice", 0));
                    neweconomysection.set("SellToBankPrice", oldeconomysection.getDouble("SellToBankPrice", 0));
                    neweconomysection.set("BuyFromBankPrice", oldeconomysection.getDouble("BuyFromBankPrice", 0));
                    neweconomysection.set("AddCommentPrice", oldeconomysection.getDouble("AddCommentPrice", 0));
                    neweconomysection.set("BiomeChangePrice", oldeconomysection.getDouble("BiomeChangePrice", 0));
                    neweconomysection.set("ProtectPrice", oldeconomysection.getDouble("ProtectPrice", 0));
                    neweconomysection.set("DisposePrice", oldeconomysection.getDouble("DisposePrice", 0));

                    newcurrworld.set("economy", neweconomysection);
                }

                newworlds.set(worldname, newcurrworld);
            }

            newconfig.set("worlds", newworlds);

            try {
                newconfig.save(newfile);

                if (!oldfile.getName().contains("config.backup.yml")) {
                    oldfile.renameTo(new File(getDataFolder().getParentFile().getAbsolutePath() + File.separator + "PlotMe" + File.separator + "config.backup.yml"));
                }
            } catch (IOException e) {
                getLogger().severe("error writting configurations");
                e.printStackTrace();
                return;
            }
        }
    }

    public void initialize() {
        setPlotMeCoreManager(new PlotMeCoreManager(this));
        setUtil(new Util(this));

        PluginDescriptionFile pdfFile = this.getDescription();
        //PREFIX = ChatColor.BLUE + "[" + NAME + "] " + ChatColor.RESET;
        VERSION = pdfFile.getVersion();
        configpath = getDataFolder().getParentFile().getAbsolutePath() + File.separator + "PlotMe";

        File configfolder = new File(getConfigPath());

        if (!configfolder.exists()) {
            configfolder.mkdirs();
        }

        File configfile = new File(getConfigPath(), "core-config.yml");

        if (!configfile.exists()) {
            importOldConfigs(configfile);
        }

        FileConfiguration config = new YamlConfiguration();

        try {
            config.load(configfile);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            getLogger().severe("can't read configuration file");
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            getLogger().severe("invalid configuration format");
            e.printStackTrace();
        }

        boolean usemySQL = config.getBoolean("usemySQL", false);
        String mySQLconn = config.getString("mySQLconn", "jdbc:mysql://localhost:3306/minecraft");
        String mySQLuname = config.getString("mySQLuname", "root");
        String mySQLpass = config.getString("mySQLpass", "password");

        setSqlManager(new SqlManager(this, usemySQL, mySQLuname, mySQLpass, mySQLconn));

        setGlobalUseEconomy(config.getBoolean("globalUseEconomy", false));
        setAdvancedLogging(config.getBoolean("AdvancedLogging", false));
        language = config.getString("Language", "english");
        setAllowWorldTeleport(config.getBoolean("allowWorldTeleport", true));
        setDefaultWEAnywhere(config.getBoolean("defaultWEAnywhere", false));
        //autoUpdate = config.getBoolean("auto-update", false);
        setAllowToDeny(config.getBoolean("allowToDeny", true));
        nbClearSpools = config.getInt("NbClearSpools", 3);
        if (nbClearSpools > 100) {
            getLogger().warning("Having more than 100 clear spools seems drastic, changing to 100");
            nbClearSpools = 100;
        }
        nbBlocksPerClearStep = config.getInt("NbBlocksPerClearStep", 50000);

        ConfigurationSection worlds;

        if (!config.contains("worlds")) {
            worlds = config.createSection("worlds");

            ConfigurationSection plotworld = worlds.createSection("plotworld");

            plotworld.set("PlotAutoLimit", 1000);
            plotworld.set("DaysToExpiration", 7);
            plotworld.set("ProtectedBlocks", getDefaultProtectedBlocks());
            plotworld.set("PreventedItems", getDefaultPreventedItems());
            plotworld.set("ProtectedWallBlockId", "44:4");
            plotworld.set("ForSaleWallBlockId", "44:1");
            plotworld.set("AuctionWallBlockId", "44:1");
            plotworld.set("AutoLinkPlots", true);
            plotworld.set("DisableExplosion", true);
            plotworld.set("DisableIgnition", true);
            plotworld.set("UseProgressiveClear", false);
            plotworld.set("NextFreed", "0;0");

            ConfigurationSection economysection = plotworld.createSection("economy");

            economysection.set("UseEconomy", false);
            economysection.set("CanPutOnSale", false);
            economysection.set("CanSellToBank", false);
            economysection.set("RefundClaimPriceOnReset", false);
            economysection.set("RefundClaimPriceOnSetOwner", false);
            economysection.set("ClaimPrice", 0);
            economysection.set("ClearPrice", 0);
            economysection.set("AddPlayerPrice", 0);
            economysection.set("DenyPlayerPrice", 0);
            economysection.set("RemovePlayerPrice", 0);
            economysection.set("UndenyPlayerPrice", 0);
            economysection.set("PlotHomePrice", 0);
            economysection.set("CanCustomizeSellPrice", false);
            economysection.set("SellToPlayerPrice", 0);
            economysection.set("SellToBankPrice", 0);
            economysection.set("BuyFromBankPrice", 0);
            economysection.set("AddCommentPrice", 0);
            economysection.set("BiomeChangePrice", 0);
            economysection.set("ProtectPrice", 0);
            economysection.set("DisposePrice", 0);

            plotworld.set("economy", economysection);

            worlds.set("plotworld", plotworld);
            config.set("worlds", worlds);
        } else {
            worlds = config.getConfigurationSection("worlds");
        }

        for (String worldname : worlds.getKeys(false)) {
            PlotMapInfo tempPlotInfo = new PlotMapInfo(this, worldname);
            ConfigurationSection currworld = worlds.getConfigurationSection(worldname);

            tempPlotInfo.PlotAutoLimit = currworld.getInt("PlotAutoLimit", 100);

            tempPlotInfo.DaysToExpiration = currworld.getInt("DaysToExpiration", 7);

            if (currworld.contains("ProtectedBlocks")) {
                tempPlotInfo.ProtectedBlocks = currworld.getIntegerList("ProtectedBlocks");
            } else {
                tempPlotInfo.ProtectedBlocks = getDefaultProtectedBlocks();
            }

            if (currworld.contains("PreventedItems")) {
                tempPlotInfo.PreventedItems = currworld.getStringList("PreventedItems");
            } else {
                tempPlotInfo.PreventedItems = getDefaultPreventedItems();
            }

            tempPlotInfo.AutoLinkPlots = currworld.getBoolean("AutoLinkPlots", true);
            tempPlotInfo.DisableExplosion = currworld.getBoolean("DisableExplosion", true);
            tempPlotInfo.DisableIgnition = currworld.getBoolean("DisableIgnition", true);
            tempPlotInfo.UseProgressiveClear = currworld.getBoolean("UseProgressiveClear", false);
            tempPlotInfo.NextFreed = currworld.getString("NextFreed", "0;0");

            ConfigurationSection economysection;

            if (currworld.getConfigurationSection("economy") == null) {
                economysection = currworld.createSection("economy");
            } else {
                economysection = currworld.getConfigurationSection("economy");
            }

            tempPlotInfo.UseEconomy = economysection.getBoolean("UseEconomy", false);
            tempPlotInfo.CanPutOnSale = economysection.getBoolean("CanPutOnSale", false);
            tempPlotInfo.CanSellToBank = economysection.getBoolean("CanSellToBank", false);
            tempPlotInfo.RefundClaimPriceOnReset = economysection.getBoolean("RefundClaimPriceOnReset", false);
            tempPlotInfo.RefundClaimPriceOnSetOwner = economysection.getBoolean("RefundClaimPriceOnSetOwner", false);
            tempPlotInfo.ClaimPrice = economysection.getDouble("ClaimPrice", 0);
            tempPlotInfo.ClearPrice = economysection.getDouble("ClearPrice", 0);
            tempPlotInfo.AddPlayerPrice = economysection.getDouble("AddPlayerPrice", 0);
            tempPlotInfo.DenyPlayerPrice = economysection.getDouble("DenyPlayerPrice", 0);
            tempPlotInfo.RemovePlayerPrice = economysection.getDouble("RemovePlayerPrice", 0);
            tempPlotInfo.UndenyPlayerPrice = economysection.getDouble("UndenyPlayerPrice", 0);
            tempPlotInfo.PlotHomePrice = economysection.getDouble("PlotHomePrice", 0);
            tempPlotInfo.CanCustomizeSellPrice = economysection.getBoolean("CanCustomizeSellPrice", false);
            tempPlotInfo.SellToPlayerPrice = economysection.getDouble("SellToPlayerPrice", 0);
            tempPlotInfo.SellToBankPrice = economysection.getDouble("SellToBankPrice", 0);
            tempPlotInfo.BuyFromBankPrice = economysection.getDouble("BuyFromBankPrice", 0);
            tempPlotInfo.AddCommentPrice = economysection.getDouble("AddCommentPrice", 0);
            tempPlotInfo.BiomeChangePrice = economysection.getDouble("BiomeChangePrice", 0);
            tempPlotInfo.ProtectPrice = economysection.getDouble("ProtectPrice", 0);
            tempPlotInfo.DisposePrice = economysection.getDouble("DisposePrice", 0);

            currworld.set("PlotAutoLimit", tempPlotInfo.PlotAutoLimit);

            currworld.set("DaysToExpiration", tempPlotInfo.DaysToExpiration);
            currworld.set("ProtectedBlocks", tempPlotInfo.ProtectedBlocks);
            currworld.set("PreventedItems", tempPlotInfo.PreventedItems);

            currworld.set("AutoLinkPlots", tempPlotInfo.AutoLinkPlots);
            currworld.set("DisableExplosion", tempPlotInfo.DisableExplosion);
            currworld.set("DisableIgnition", tempPlotInfo.DisableIgnition);
            currworld.set("UseProgressiveClear", tempPlotInfo.UseProgressiveClear);
            currworld.set("NextFreed", tempPlotInfo.NextFreed);

            economysection = currworld.createSection("economy");

            economysection.set("UseEconomy", tempPlotInfo.UseEconomy);
            economysection.set("CanPutOnSale", tempPlotInfo.CanPutOnSale);
            economysection.set("CanSellToBank", tempPlotInfo.CanSellToBank);
            economysection.set("RefundClaimPriceOnReset", tempPlotInfo.RefundClaimPriceOnReset);
            economysection.set("RefundClaimPriceOnSetOwner", tempPlotInfo.RefundClaimPriceOnSetOwner);
            economysection.set("ClaimPrice", tempPlotInfo.ClaimPrice);
            economysection.set("ClearPrice", tempPlotInfo.ClearPrice);
            economysection.set("AddPlayerPrice", tempPlotInfo.AddPlayerPrice);
            economysection.set("DenyPlayerPrice", tempPlotInfo.DenyPlayerPrice);
            economysection.set("RemovePlayerPrice", tempPlotInfo.RemovePlayerPrice);
            economysection.set("UndenyPlayerPrice", tempPlotInfo.UndenyPlayerPrice);
            economysection.set("PlotHomePrice", tempPlotInfo.PlotHomePrice);
            economysection.set("CanCustomizeSellPrice", tempPlotInfo.CanCustomizeSellPrice);
            economysection.set("SellToPlayerPrice", tempPlotInfo.SellToPlayerPrice);
            economysection.set("SellToBankPrice", tempPlotInfo.SellToBankPrice);
            economysection.set("BuyFromBankPrice", tempPlotInfo.BuyFromBankPrice);
            economysection.set("AddCommentPrice", tempPlotInfo.AddCommentPrice);
            economysection.set("BiomeChangePrice", tempPlotInfo.BiomeChangePrice);
            economysection.set("ProtectPrice", tempPlotInfo.ProtectPrice);
            economysection.set("DisposePrice", tempPlotInfo.DisposePrice);

            currworld.set("economy", economysection);

            worlds.set(worldname, currworld);

            //SqlManager.getPlots(worldname.toLowerCase());
            getPlotMeCoreManager().addPlotMap(worldname.toLowerCase(), tempPlotInfo);
        }

        config.set("usemySQL", usemySQL);
        config.set("mySQLconn", mySQLconn);
        config.set("mySQLuname", mySQLuname);
        config.set("mySQLpass", mySQLpass);
        config.set("globalUseEconomy", getGlobalUseEconomy());
        config.set("AdvancedLogging", getAdvancedLogging());
        config.set("Language", language);
        config.set("allowWorldTeleport", getAllowWorldTeleport());
        config.set("defaultWEAnywhere", getDefaultWEAnywhere());
        //config.set("auto-update", autoUpdate);
        config.set("allowToDeny", getAllowToDeny());
        config.set("NbClearSpools", getNbClearSpools());
        config.set("NbBlocksPerClearStep", getNbBlocksPerClearStep());

        try {
            config.save(configfile);
        } catch (IOException e) {
            getLogger().severe("error writting configurations");
            e.printStackTrace();
        }

        loadCaptions();
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            setEconomy(economyProvider.getProvider());
        }
    }

    public int getPlotLimit(Player p) {
        int max = -2;

        int maxlimit = 255;

        if (p.hasPermission("plotme.limit.*")) {
            return -1;
        } else {
            for (int ctr = 0; ctr < maxlimit; ctr++) {
                if (p.hasPermission("plotme.limit." + ctr)) {
                    max = ctr;
                }
            }

        }

        if (max == -2) {
            if (cPerms(p, "plotme.admin")) {
                return -1;
            } else if (cPerms(p, "plotme.use")) {
                return 1;
            } else {
                return 0;
            }
        }

        return max;
    }

    public String getDate() {
        return getDate(Calendar.getInstance());
    }

    private String getDate(Calendar cal) {
        int imonth = cal.get(Calendar.MONTH) + 1;
        int iday = cal.get(Calendar.DAY_OF_MONTH) + 1;
        String month = "";
        String day = "";

        if (imonth < 10) {
            month = "0" + imonth;
        } else {
            month = "" + imonth;
        }

        if (iday < 10) {
            day = "0" + iday;
        } else {
            day = "" + iday;
        }

        return "" + cal.get(Calendar.YEAR) + "-" + month + "-" + day;
    }

    public String getDate(java.sql.Date expireddate) {
        return expireddate.toString();
    }

    @SuppressWarnings("deprecation")
    public List<Integer> getDefaultProtectedBlocks() {
        List<Integer> protections = new ArrayList<Integer>();

        protections.add(Material.CHEST.getId());
        protections.add(Material.FURNACE.getId());
        protections.add(Material.BURNING_FURNACE.getId());
        protections.add(Material.ENDER_PORTAL_FRAME.getId());
        protections.add(Material.DIODE_BLOCK_ON.getId());
        protections.add(Material.DIODE_BLOCK_OFF.getId());
        protections.add(Material.JUKEBOX.getId());
        protections.add(Material.NOTE_BLOCK.getId());
        protections.add(Material.BED.getId());
        protections.add(Material.CAULDRON.getId());
        protections.add(Material.BREWING_STAND.getId());
        protections.add(Material.BEACON.getId());
        protections.add(Material.FLOWER_POT.getId());
        protections.add(Material.ANVIL.getId());

        return protections;
    }

    @SuppressWarnings("deprecation")
    public List<String> getDefaultPreventedItems() {
        List<String> preventeditems = new ArrayList<String>();

        preventeditems.add("" + Material.INK_SACK.getId() + ":15");
        preventeditems.add("" + Material.FLINT_AND_STEEL.getId());
        preventeditems.add("" + Material.MINECART.getId());
        preventeditems.add("" + Material.POWERED_MINECART.getId());
        preventeditems.add("" + Material.STORAGE_MINECART.getId());
        preventeditems.add("" + Material.BOAT.getId());

        return preventeditems;
    }

    public void scheduleTask(Runnable task, int eachseconds, int howmanytimes) {
        getCommandSenderCurrentlyProcessingExpired().sendMessage(getUtil().C("MsgStartDeleteSession"));

        for (int ctr = 0; ctr < (howmanytimes / getNbPerDeletionProcessingExpired()); ctr++) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, task, ctr * eachseconds * 20);
        }
    }

    private void loadCaptions() {
        File filelang = new File(getConfigPath(), "caption-english.yml");

        TreeMap<String, String> properties = new TreeMap<String, String>();
        properties.put("MsgStartDeleteSession", "Starting delete session");
        properties.put("MsgDeletedExpiredPlots", "Deleted expired plot");
        properties.put("MsgDeleteSessionFinished", "Deletion session finished, rerun to reset more plots");
        properties.put("MsgAlreadyProcessingPlots", "is already processing expired plots");
        properties.put("MsgDoesNotExistOrNotLoaded", "does not exist or is not loaded.");
        properties.put("MsgNotPlotWorld", "This is not a plot world.");
        properties.put("MsgPermissionDenied", "Permission denied");
        properties.put("MsgNoPlotFound", "No plot found");
        properties.put("MsgCannotBidOwnPlot", "You cannot bid on your own plot.");
        properties.put("MsgCannotBuyOwnPlot", "You cannot buy your own plot.");
        properties.put("MsgCannotClaimRoad", "You cannot claim the road.");
        properties.put("MsgInvalidBidMustBeAbove", "Invalid bid. Must be above");
        properties.put("MsgOutbidOnPlot", "Outbid on plot");
        properties.put("MsgOwnedBy", "owned by");
        properties.put("MsgBidAccepted", "Bid accepted.");
        properties.put("MsgPlotNotAuctionned", "This plot isn't being auctionned.");
        properties.put("MsgThisPlot", "This plot");
        properties.put("MsgThisPlotYours", "This plot is now yours.");
        properties.put("MsgThisPlotIsNow", "This plot is now ");
        properties.put("MsgThisPlotOwned", "This plot is already owned.");
        properties.put("MsgHasNoOwner", "has no owners.");
        properties.put("MsgEconomyDisabledWorld", "Economy is disabled for this world.");
        properties.put("MsgPlotNotForSale", "Plot isn't for sale.");
        properties.put("MsgAlreadyReachedMaxPlots", "You have already reached your maximum amount of plots");
        properties.put("MsgToGetToIt", "to get to it");
        properties.put("MsgNotEnoughBid", "You do not have enough to bid this much.");
        properties.put("MsgNotEnoughBuy", "You do not have enough to buy this plot.");
        properties.put("MsgNotEnoughAuto", "You do not have enough to buy a plot.");
        properties.put("MsgNotEnoughComment", "You do not have enough to comment on a plot.");
        properties.put("MsgNotEnoughBiome", "You do not have enough to change the biome.");
        properties.put("MsgNotEnoughClear", "You do not have enough to clear the plot.");
        properties.put("MsgNotEnoughDispose", "You do not have enough to dispose of this plot.");
        properties.put("MsgNotEnoughProtectPlot", "You do not have enough to protect this plot.");
        properties.put("MsgNotEnoughTp", "You do not have enough to teleport home.");
        properties.put("MsgNotEnoughAdd", "You do not have enough to add a player.");
        properties.put("MsgNotEnoughDeny", "You do not have enough to deny a player.");
        properties.put("MsgNotEnoughRemove", "You do not have enough to remove a player.");
        properties.put("MsgNotEnoughUndeny", "You do not have enough to undeny a player.");
        properties.put("MsgSoldTo", "sold to");
        properties.put("MsgPlotBought", "Plot bought.");
        properties.put("MsgBoughtPlot", "bought plot");
        properties.put("MsgClaimedPlot", "claimed plot");
        properties.put("MsgPlotHasBidsAskAdmin", "Plot is being auctionned and has bids. Ask an admin to cancel it.");
        properties.put("MsgAuctionCancelledOnPlot", "Auction cancelled on plot");
        properties.put("MsgAuctionCancelled", "Auction cancelled.");
        properties.put("MsgStoppedTheAuctionOnPlot", "stopped the auction on plot");
        properties.put("MsgInvalidAmount", "Invalid amount. Must be above or equal to 0.");
        properties.put("MsgAuctionStarted", "Auction started.");
        properties.put("MsgStartedAuctionOnPlot", "started an auction on plot");
        properties.put("MsgDoNotOwnPlot", "You do not own this plot.");
        properties.put("MsgSellingPlotsIsDisabledWorld", "Selling plots is disabled in this world.");
        properties.put("MsgPlotProtectedNotDisposed", "Plot is protected and cannot be disposed.");
        properties.put("MsgWasDisposed", "was disposed.");
        properties.put("MsgPlotDisposedAnyoneClaim", "Plot disposed. Anyone can claim it.");
        properties.put("MsgDisposedPlot", "disposed of plot");
        properties.put("MsgNotYoursCannotDispose", "is not yours. You are not allowed to dispose it.");
        properties.put("MsgPlotNoLongerSale", "Plot no longer for sale.");
        properties.put("MsgRemovedPlot", "removed the plot");
        properties.put("MsgFromBeingSold", "from being sold");
        properties.put("MsgCannotCustomPriceDefault", "You cannot customize the price. Default price is :");
        properties.put("MsgCannotSellToBank", "Plots cannot be sold to the bank in this world.");
        properties.put("MsgSoldToBank", "sold to bank.");
        properties.put("MsgPlotSold", "Plot sold.");
        properties.put("MsgSoldToBankPlot", "sold to bank plot");
        properties.put("MsgPlotForSale", "Plot now for sale.");
        properties.put("MsgPutOnSalePlot", "put on sale plot");
        properties.put("MsgPlotNoLongerProtected", "Plot is no longer protected. It is now possible to Clear or Reset it.");
        properties.put("MsgUnprotectedPlot", "unprotected plot");
        properties.put("MsgPlotNowProtected", "Plot is now protected. It won't be possible to Clear or Reset it.");
        properties.put("MsgProtectedPlot", "protected plot");
        properties.put("MsgNoPlotsFinished", "No plots are finished");
        properties.put("MsgFinishedPlotsPage", "Finished plots page");
        properties.put("MsgUnmarkFinished", "Plot is no longer marked finished.");
        properties.put("MsgMarkFinished", "Plot is now marked finished.");
        properties.put("MsgPlotExpirationReset", "Plot expiration reset");
        properties.put("MsgNoPlotExpired", "No plots are expired");
        properties.put("MsgExpiredPlotsPage", "Expired plots page");
        properties.put("MsgListOfPlotsWhere", "List of plots where");
        properties.put("MsgCanBuild", "can build:");
        properties.put("MsgListOfPlotsWhereYou", "List of plots where you can build:");
        properties.put("MsgWorldEditInYourPlots", "You can now only WorldEdit in your plots");
        properties.put("MsgWorldEditAnywhere", "You can now WorldEdit anywhere");
        properties.put("MsgNoPlotFound1", "No plot found within");
        properties.put("MsgNoPlotFound2", "plots. Contact an admin.");
        properties.put("MsgDoesNotHavePlot", "does not have a plot");
        properties.put("MsgPlotNotFound", "Could not find plot");
        properties.put("MsgYouHaveNoPlot", "You don't have a plot.");
        properties.put("MsgCommentAdded", "Comment added.");
        properties.put("MsgCommentedPlot", "commented on plot");
        properties.put("MsgNoComments", "No comments");
        properties.put("MsgYouHave", "You have");
        properties.put("MsgComments", "comments.");
        properties.put("MsgNotYoursNotAllowedViewComments", "is not yours. You are not allowed to view the comments.");
        properties.put("MsgIsInvalidBiome", "is not a valid biome.");
        properties.put("MsgBiomeSet", "Biome set to");
        properties.put("MsgChangedBiome", "changed the biome of plot");
        properties.put("MsgNotYoursNotAllowedBiome", "is not yours. You are not allowed to change it's biome.");
        properties.put("MsgPlotUsingBiome", "This plot is using the biome");
        properties.put("MsgPlotProtectedCannotReset", "Plot is protected and cannot be reset.");
        properties.put("MsgPlotProtectedCannotClear", "Plot is protected and cannot be cleared.");
        properties.put("MsgOwnedBy", "owned by");
        properties.put("MsgWasReset", "was reset.");
        properties.put("MsgPlotReset", "Plot has been reset.");
        properties.put("MsgResetPlot", "reset plot");
        properties.put("MsgPlotCleared", "Plot cleared.");
        properties.put("MsgClearedPlot", "cleared plot");
        properties.put("MsgNotYoursNotAllowedClear", "is not yours. You are not allowed to clear it.");
        properties.put("MsgNotYoursNotAllowedReset", "is not yours. You are not allowed to reset it.");
        properties.put("MsgAlreadyAllowed", "was already allowed");
        properties.put("MsgAlreadyDenied", "was already denied");
        properties.put("MsgWasNotAllowed", "was not allowed");
        properties.put("MsgWasNotDenied", "was not denied");
        properties.put("MsgNowUndenied", "now undenied.");
        properties.put("MsgNowDenied", "now denied.");
        properties.put("MsgNowAllowed", "now allowed.");
        properties.put("MsgAddedPlayer", "added player");
        properties.put("MsgDeniedPlayer", "denied player");
        properties.put("MsgRemovedPlayer", "removed player");
        properties.put("MsgUndeniedPlayer", "undenied player");
        properties.put("MsgToPlot", "to plot");
        properties.put("MsgFromPlot", "from plot");
        properties.put("MsgNotYoursNotAllowedAdd", "is not yours. You are not allowed to add someone to it.");
        properties.put("MsgNotYoursNotAllowedDeny", "is not yours. You are not allowed to deny someone from it.");
        properties.put("MsgNotYoursNotAllowedRemove", "is not yours. You are not allowed to remove someone from it.");
        properties.put("MsgNotYoursNotAllowedUndeny", "is not yours. You are not allowed to undeny someone to it.");
        properties.put("MsgNowOwnedBy", "is now owned by");
        properties.put("MsgChangedOwnerFrom", "changed owner from");
        properties.put("MsgChangedOwnerOf", "changed owner of");
        properties.put("MsgOwnerChangedTo", "Plot Owner has been set to");
        properties.put("MsgHeightChangedTo", "Plot height has been set to");
        properties.put("MsgBaseChangedTo", "Plot base has been set to");
        properties.put("MsgPlotMovedSuccess", "Plot moved successfully");
        properties.put("MsgExchangedPlot", "exchanged plot");
        properties.put("MsgAndPlot", "and plot");
        properties.put("MsgReloadedSuccess", "reloaded successfully");
        properties.put("MsgReloadedConfigurations", "reloaded configurations");
        properties.put("MsgNoPlotworldFound", "No Plot world found.");
        properties.put("MsgWorldNotPlot", "does not exist or is not a plot world.");
        properties.put("MsgCreateWorldHelp", "If no generator is specified, PlotMe-DefaultGenerator will be used.");
        properties.put("MsgWorldCreationSuccess", "World successfully created.");
        properties.put("MsgCreateWorldParameters1", "Plotworld creation preparation started");
        properties.put("MsgCreateWorldParameters2", "Default core settings :");
        properties.put("MsgCreateWorldParameters3", "Default generator settings :");
        properties.put("MsgCreateWorldParameters4", "to change settings");
        properties.put("MsgCreateWorldParameters5", "to cancel world creation");
        properties.put("MsgSettingChanged", "Setting changed");
        properties.put("MsgPlotLockedClear", "The plot is currently locked because it is being cleared.");
        properties.put("MsgPlotLockedReset", "The plot is currently locked because it is being reset.");
        properties.put("MsgPlotLockedExpired", "The plot is currently locked because it is being reset for having expired.");

        properties.put("ConsoleHelpMain", " ---==PlotMe Console Help Page==---");
        properties.put("ConsoleHelpReload", " - Reloads the plugin and its configuration files");

        properties.put("HelpTitle", "PlotMe Help Page");
        properties.put("HelpYourPlotLimitWorld", "Your plot limit in this world");
        properties.put("HelpUsedOf", "used of");
        properties.put("HelpClaim", "Claims the current plot you are standing on.");
        properties.put("HelpClaimOther", "Claims the current plot you are standing on for another player.");
        properties.put("HelpAuto", "Claims the next available free plot.");
        properties.put("HelpHome", "Teleports you to your plot, :# if you own multiple plots.");
        properties.put("HelpHomeOther", "Teleports you to other plots, :# if other people own multiple plots.");
        properties.put("HelpInfo", "Displays information about the plot you're standing on.");
        properties.put("HelpComment", "Leave comment on the current plot.");
        properties.put("HelpComments", "Lists all comments users have said about your plot.");
        properties.put("HelpList", "Lists every plot you can build on.");
        properties.put("HelpListOther", "Lists every plot <player> can build on.");
        properties.put("HelpBiomeInfo", "Shows the current biome in the plot.");
        properties.put("HelpBiome", "Changes the plots biome to the one specified.");
        properties.put("HelpBiomeList", "Lists all possible biomes.");
        properties.put("HelpDone", "Toggles a plot done or not done.");
        properties.put("HelpTp", "Teleports to a plot in the current world.");
        properties.put("HelpId", "Gets plot id and coordinates of the current plot your standing on.");
        properties.put("HelpClear", "Clears the plot to its original flat state.");
        properties.put("HelpReset", "Resets the plot to its original flat state AND remove its owner.");
        properties.put("HelpAdd", "Allows a player to have full access to the plot(This is your responsibility!)");
        properties.put("HelpDeny", "Prevents a player from moving onto your plot.");
        properties.put("HelpRemove", "Revokes a players access to the plot.");
        properties.put("HelpUndeny", "Allows a previously denied player to move onto your plot.");
        properties.put("HelpSetowner", "Sets the player provided as the owner of the plot your currently on.");
        properties.put("HelpSetHeight", "Sets the buildable height from the base of the plot your currently on.");
        properties.put("HelpAddHeight", "Adds to the buildable height from the base of the plot your currently on.");
        properties.put("HelpSubHeight", "Subtracts from the buildable height from the base of the plot your currently on.");
        properties.put("HelpSetBase", "Sets the lowest level that can be built on of the plot your currently on.");
        properties.put("HelpMove", "Swaps the plots blocks(highly experimental for now, use at your own risk).");
        properties.put("HelpWEAnywhere", "Toggles using worldedit anywhere.");
        properties.put("HelpExpired", "Lists expired plots.");
        properties.put("HelpDoneList", "Lists finished plots.");
        properties.put("HelpAddTime1", "Resets the expiration date to");
        properties.put("HelpAddTime2", "days from now.");
        properties.put("HelpReload", "Reloads the plugin and its configuration files.");
        properties.put("HelpDispose", "You will no longer own the plot but it will not get cleared.");
        properties.put("HelpBuy", "Buys a plot at the price listed.");
        properties.put("HelpSell", "Puts your plot for sale.");
        properties.put("HelpSellBank", "Sells your plot to the bank for");
        properties.put("HelpAuction", "Puts your plot for auction.");
        properties.put("HelpResetExpired", "Resets the 50 oldest plots on that world.");
        properties.put("HelpBid", "Places a bid on the current plot.");

        properties.put("WordWorld", "World");
        properties.put("WordUsage", "Usage");
        properties.put("WordExample", "Example");
        properties.put("WordAmount", "amount");
        properties.put("WordUse", "Use");
        properties.put("WordPlot", "Plot");
        properties.put("WordFor", "for");
        properties.put("WordAt", "at");
        properties.put("WordMarked", "marked");
        properties.put("WordFinished", "finished");
        properties.put("WordUnfinished", "unfinished");
        properties.put("WordAuction", "Auction");
        properties.put("WordSell", "Sell");
        properties.put("WordYours", "Yours");
        properties.put("WordHelpers", "Helpers");
        properties.put("WordInfinite", "Infinite");
        properties.put("WordPrice", "Price");
        properties.put("WordPlayer", "Player");
        properties.put("WordComment", "comment");
        properties.put("WordBiome", "biome");
        properties.put("WordHeight", "height");
        properties.put("WordId", "id");
        properties.put("WordIdFrom", "id-from");
        properties.put("WordIdTo", "id-to");
        properties.put("WordNever", "Never");
        properties.put("WordDefault", "Default");
        properties.put("WordMissing", "Missing");
        properties.put("WordYes", "Yes");
        properties.put("WordNo", "No");
        properties.put("WordText", "text");
        properties.put("WordFrom", "From");
        properties.put("WordTo", "to");
        properties.put("WordBiomes", "Biomes");
        properties.put("WordNotApplicable", "N/A");
        properties.put("WordBottom", "Bottom");
        properties.put("WordTop", "Top");
        properties.put("WordPossessive", "'s");
        properties.put("WordRemoved", "removed");
        properties.put("WordGenerator", "generator");
        properties.put("WordConfig", "config");
        properties.put("WordValue", "value");
        properties.put("WordIs", "is");
        properties.put("WordCleared", "cleared");
        properties.put("WordBlocks", "blocks");
        properties.put("WordIn", "in");

        properties.put("Unit_1000", "k");
        properties.put("Unit_1000000", "M");
        properties.put("Unit_1000000000", "G");
        properties.put("Unit_1000000000000", "T");

        properties.put("SignOwner", "Owner:");
        properties.put("SignId", "ID:");
        properties.put("SignForSale", "&9&lFOR SALE");
        properties.put("SignPrice", "Price :");
        properties.put("SignPriceColor", "&9");
        properties.put("SignOnAuction", "&9&lON AUCTION");
        properties.put("SignMinimumBid", "Minimum bid :");
        properties.put("SignCurrentBid", "Current bid :");
        properties.put("SignCurrentBidColor", "&9");

        properties.put("InfoId", "ID");
        properties.put("InfoOwner", "Owner");
        properties.put("InfoBiome", "Biome");
        properties.put("InfoExpire", "Expire date");
        properties.put("InfoFinished", "Finished");
        properties.put("InfoProtected", "Protected");
        properties.put("InfoBase", "Base");
        properties.put("InfoHeight", "Height");
        properties.put("InfoHelpers", "Helpers");
        properties.put("InfoDenied", "Denied");
        properties.put("InfoAuctionned", "Auctionned");
        properties.put("InfoBidder", "Bidder");
        properties.put("InfoBid", "Bid");
        properties.put("InfoForSale", "For sale");
        properties.put("InfoMinimumBid", "Minimum bid");

        properties.put("CommandBuy", "buy");
        properties.put("CommandBid", "bid");
        properties.put("CommandResetExpired", "resetexpired");
        properties.put("CommandHelp", "help");
        properties.put("CommandClaim", "claim");
        properties.put("CommandAuto", "auto");
        properties.put("CommandInfo", "info");
        properties.put("CommandComment", "comment");
        properties.put("CommandComments", "comments");
        properties.put("CommandBiome", "biome");
        properties.put("CommandBiomelist", "biomelist");
        properties.put("CommandId", "id");
        properties.put("CommandTp", "tp");
        properties.put("CommandClear", "clear");
        properties.put("CommandReset", "reset");
        properties.put("CommandAdd", "add");
        properties.put("CommandDeny", "deny");
        properties.put("CommandRemove", "remove");
        properties.put("CommandUndeny", "undeny");
        properties.put("CommandSetowner", "setowner");
        properties.put("CommandSetHeight", "setheight");
        properties.put("CommandAddHeight", "addheight");
        properties.put("CommandSubHeight", "subheight");
        properties.put("CommandSetBase", "setbase");
        properties.put("CommandMove", "move");
        properties.put("CommandWEAnywhere", "weanywhere");
        properties.put("CommandList", "list");
        properties.put("CommandExpired", "expired");
        properties.put("CommandAddtime", "addtime");
        properties.put("CommandDone", "done");
        properties.put("CommandDoneList", "donelist");
        properties.put("CommandProtect", "protect");
        properties.put("CommandSell", "sell");
        properties.put("CommandSellBank", "sell bank");
        properties.put("CommandDispose", "dispose");
        properties.put("CommandAuction", "auction");
        properties.put("CommandHome", "home");
        properties.put("CommandCreateWorld", "createworld");
        properties.put("CommandCreateWorld-Setting", "set");
        properties.put("CommandCreateWorld-Cancel", "cancel");

        properties.put("ErrCannotBuild", "You cannot build here.");
        properties.put("ErrCannotUseEggs", "You cannot use eggs here.");
        properties.put("ErrCannotUse", "You cannot use that.");
        properties.put("ErrCreatingPlotAt", "An error occured while creating the plot at");
        properties.put("ErrMovingPlot", "Error moving plot");
        properties.put("ErrWorldPluginNotFound", "Cannot create new world, Multiverse and Multiworld not found");
        properties.put("ErrCannotFindWorldGen", "Cannot create new world, cannot find generator");
        properties.put("ErrCannotCreateGen1", "Cannot create new world, generator");
        properties.put("ErrCannotCreateGen2", "failed to create configurations");
        properties.put("ErrCannotCreateGen3", "does not implement PlotMe_Generator");
        properties.put("ErrCannotCreateMW", "Cannot create new world, failed to create world using multiworld");
        properties.put("ErrMWDisabled", "Cannot create new world, multiworld is disabled");
        properties.put("ErrCannotCreateMV", "Cannot create new world, failed to create world using multiverse");
        properties.put("ErrMVDisabled", "Cannot create new world, multiverse is disabled");
        properties.put("ErrWorldExists", "Cannot create new world, name chosen already exists");
        properties.put("ErrInvalidWorldName", "Cannot create new world, invalid world name chosen.");
        properties.put("ErrSpoolInterrupted", "The spool sleep was interrupted");
        properties.put("ErrNotANumber", "is not a valid number");

        createConfig(filelang, properties, "PlotMe Caption configuration αω");

        if (!language.equalsIgnoreCase("english")) {
            filelang = new File(getConfigPath(), "caption-" + language + ".yml");
            createConfig(filelang, properties, "PlotMe Caption configuration");
        }

        InputStream input = null;

        try {
            input = new FileInputStream(filelang);
            Yaml yaml = new Yaml();
            Object obj = yaml.load(input);

            if (obj instanceof LinkedHashMap<?, ?>) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, String> data = (LinkedHashMap<String, String>) obj;

                Map<String, String> captions = new HashMap<String, String>();
                for (String key : data.keySet()) {
                    captions.put(key, data.get(key));
                }
                getUtil().setCaptions(captions);
            }
        } catch (FileNotFoundException e) {
            getLogger().severe("File not found: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            getLogger().severe("Error with configuration: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void createConfig(File file, TreeMap<String, String> properties, String Title) {
        if (!file.exists()) {
            BufferedWriter writer = null;

            try {
                File dir = new File(getConfigPath(), "");
                dir.mkdirs();

                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
                writer.write("# " + Title);

                for (Entry<String, String> e : properties.entrySet()) {
                    writer.write("\n" + e.getKey() + ": '" + e.getValue().replace("'", "''") + "'");
                }

                writer.close();
            } catch (IOException e) {
                getLogger().severe("Unable to create config file : " + Title + "!");
                getLogger().severe(e.getMessage());
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e2) {
                    }
                }
            }
        } else {
            OutputStreamWriter writer = null;
            InputStream input = null;

            try {
                input = new FileInputStream(file);
                Yaml yaml = new Yaml();
                Object obj = yaml.load(input);

                if (obj instanceof LinkedHashMap<?, ?>) {
                    @SuppressWarnings("unchecked")
                    LinkedHashMap<String, String> data = (LinkedHashMap<String, String>) obj;

                    writer = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");

                    for (Entry<String, String> e : properties.entrySet()) {
                        if (!data.containsKey(e.getKey())) {
                            writer.write("\n" + e.getKey() + ": '" + e.getValue().replace("'", "''") + "'");
                        }
                    }

                    writer.close();
                    input.close();
                }
            } catch (FileNotFoundException e) {
                getLogger().severe("File not found: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                getLogger().severe("Error with configuration: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e2) {
                    }
                }
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    public void scheduleProtectionRemoval(final Location bottom, final Location top) {
        int x1 = bottom.getBlockX();
        int y1 = bottom.getBlockY();
        int z1 = bottom.getBlockZ();
        int x2 = top.getBlockX();
        int y2 = top.getBlockY();
        int z2 = top.getBlockZ();
        World w = bottom.getWorld();

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    final Block block = w.getBlockAt(x, y, z);

                    Bukkit.getScheduler().runTask(this, new Runnable() {
                        public void run() {
                            Protection protection = com.griefcraft.lwc.LWC.getInstance().findProtection(block);

                            if (protection != null) {
                                protection.remove();
                            }
                        }
                    });
                }
            }
        }
    }

    public String getVersion() {
        return VERSION;
    }

    public World getWorldCurrentlyProcessingExpired() {
        return worldcurrentlyprocessingexpired;
    }

    public void setWorldCurrentlyProcessingExpired(
            World worldcurrentlyprocessingexpired) {
        this.worldcurrentlyprocessingexpired = worldcurrentlyprocessingexpired;
    }

    public Integer getCounterExpired() {
        return counterexpired;
    }

    public void setCounterExpired(Integer counterexpired) {
        this.counterexpired = counterexpired;
    }

    public String getConfigPath() {
        return configpath;
    }

    public Boolean getGlobalUseEconomy() {
        return globalUseEconomy;
    }

    private void setGlobalUseEconomy(Boolean globalUseEconomy) {
        this.globalUseEconomy = globalUseEconomy;
    }

    public Economy getEconomy() {
        return economy;
    }

    private void setEconomy(Economy economy) {
        this.economy = economy;
    }

    public Boolean getUsinglwc() {
        return usinglwc;
    }

    public void setUsinglwc(Boolean usinglwc) {
        this.usinglwc = usinglwc;
    }

    public void addPlotToClear(PlotToClear plotToClear) {
        this.plotsToClear.offer(plotToClear);
    }

    public PlotToClear pollPlotsToClear() {
        return plotsToClear.poll();
    }

    public boolean isPlotLocked(String world, String id) {
        for (PlotToClear ptc : plotsToClear.toArray(new PlotToClear[0])) {
            if (ptc.world.equalsIgnoreCase(world) && ptc.plotid.equalsIgnoreCase(id)) {
                return true;
            }
        }

        return false;
    }

    public PlotToClear getPlotLocked(String world, String id) {
        for (PlotToClear ptc : plotsToClear.toArray(new PlotToClear[0])) {
            if (ptc.world.equalsIgnoreCase(world) && ptc.plotid.equalsIgnoreCase(id)) {
                return ptc;
            }
        }

        return null;
    }

    public void saveWorldConfig(String world) {
        File configfile = new File(getConfigPath(), "core-config.yml");

        if (!configfile.exists()) {
            importOldConfigs(configfile);
        }

        FileConfiguration config = new YamlConfiguration();

        try {
            config.load(configfile);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            getLogger().severe("can't read configuration file");
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            getLogger().severe("invalid configuration format");
            e.printStackTrace();
        }

        PlotMapInfo pmi = getPlotMeCoreManager().getMap(world);

        ConfigurationSection worlds = config.getConfigurationSection("worlds");
        ConfigurationSection currworld = worlds.getConfigurationSection(world);

        ConfigurationSection economysection;

        if (currworld.getConfigurationSection("economy") == null) {
            economysection = currworld.createSection("economy");
        } else {
            economysection = currworld.getConfigurationSection("economy");
        }

        currworld.set("PlotAutoLimit", pmi.PlotAutoLimit);

        currworld.set("DaysToExpiration", pmi.DaysToExpiration);
        currworld.set("ProtectedBlocks", pmi.ProtectedBlocks);
        currworld.set("PreventedItems", pmi.PreventedItems);

        currworld.set("AutoLinkPlots", pmi.AutoLinkPlots);
        currworld.set("DisableExplosion", pmi.DisableExplosion);
        currworld.set("DisableIgnition", pmi.DisableIgnition);
        currworld.set("UseProgressiveClear", pmi.UseProgressiveClear);
        currworld.set("NextFreed", pmi.NextFreed);

        economysection = currworld.createSection("economy");

        economysection.set("UseEconomy", pmi.UseEconomy);
        economysection.set("CanPutOnSale", pmi.CanPutOnSale);
        economysection.set("CanSellToBank", pmi.CanSellToBank);
        economysection.set("RefundClaimPriceOnReset", pmi.RefundClaimPriceOnReset);
        economysection.set("RefundClaimPriceOnSetOwner", pmi.RefundClaimPriceOnSetOwner);
        economysection.set("ClaimPrice", pmi.ClaimPrice);
        economysection.set("ClearPrice", pmi.ClearPrice);
        economysection.set("AddPlayerPrice", pmi.AddPlayerPrice);
        economysection.set("DenyPlayerPrice", pmi.DenyPlayerPrice);
        economysection.set("RemovePlayerPrice", pmi.RemovePlayerPrice);
        economysection.set("UndenyPlayerPrice", pmi.UndenyPlayerPrice);
        economysection.set("PlotHomePrice", pmi.PlotHomePrice);
        economysection.set("CanCustomizeSellPrice", pmi.CanCustomizeSellPrice);
        economysection.set("SellToPlayerPrice", pmi.SellToPlayerPrice);
        economysection.set("SellToBankPrice", pmi.SellToBankPrice);
        economysection.set("BuyFromBankPrice", pmi.BuyFromBankPrice);
        economysection.set("AddCommentPrice", pmi.AddCommentPrice);
        economysection.set("BiomeChangePrice", pmi.BiomeChangePrice);
        economysection.set("ProtectPrice", pmi.ProtectPrice);
        economysection.set("DisposePrice", pmi.DisposePrice);

        currworld.set("economy", economysection);

        worlds.set(world, currworld);
        config.set("worlds", worlds);

        try {
            config.save(configfile);
        } catch (IOException e) {
            getLogger().severe("error writting configurations");
            e.printStackTrace();
        }
    }

    public Integer getNbPerDeletionProcessingExpired() {
        return nbperdeletionprocessingexpired;
    }

    public void setNbPerDeletionProcessingExpired(
            Integer nbperdeletionprocessingexpired) {
        this.nbperdeletionprocessingexpired = nbperdeletionprocessingexpired;
    }

    public CommandSender getCommandSenderCurrentlyProcessingExpired() {
        return cscurrentlyprocessingexpired;
    }

    public void setCommandSenderCurrentlyProcessingExpired(
            CommandSender cscurrentlyprocessingexpired) {
        this.cscurrentlyprocessingexpired = cscurrentlyprocessingexpired;
    }

    public PlotMeCoreManager getPlotMeCoreManager() {
        return plotmecoremanager;
    }

    private void setPlotMeCoreManager(PlotMeCoreManager plotmecoremanager) {
        this.plotmecoremanager = plotmecoremanager;
    }

    public SqlManager getSqlManager() {
        return sqlmanager;
    }

    private void setSqlManager(SqlManager sqlmanager) {
        this.sqlmanager = sqlmanager;
    }

    public PlotWorldEdit getPlotWorldEdit() {
        return plotworldedit;
    }

    private void setPlotWorldEdit(PlotWorldEdit plotworldedit) {
        this.plotworldedit = plotworldedit;
    }

    public Util getUtil() {
        return util;
    }

    private void setUtil(Util util) {
        this.util = util;
    }

    public Boolean getAllowToDeny() {
        return allowToDeny;
    }

    private void setAllowToDeny(Boolean allowToDeny) {
        this.allowToDeny = allowToDeny;
    }

    public Boolean getAdvancedLogging() {
        return advancedlogging;
    }

    public void setAdvancedLogging(Boolean advancedlogging) {
        this.advancedlogging = advancedlogging;
    }

    public Boolean getDefaultWEAnywhere() {
        return defaultWEAnywhere;
    }

    private void setDefaultWEAnywhere(Boolean defaultWEAnywhere) {
        this.defaultWEAnywhere = defaultWEAnywhere;
    }

    public Boolean getAllowWorldTeleport() {
        return allowWorldTeleport;
    }

    private void setAllowWorldTeleport(Boolean allowWorldTeleport) {
        this.allowWorldTeleport = allowWorldTeleport;
    }

    public int getNbClearSpools() {
        return nbClearSpools;
    }

    public int getNbBlocksPerClearStep() {
        return nbBlocksPerClearStep;
    }
}
