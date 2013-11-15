package com.worldcretornica.plotme_core;

import com.griefcraft.model.Protection;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.worldcretornica.plotme_core.api.v0_14b.IPlotMe_ChunkGenerator;
import com.worldcretornica.plotme_core.api.v0_14b.IPlotMe_GeneratorManager;
import com.worldcretornica.plotme_core.listener.PlotDenyListener;
import com.worldcretornica.plotme_core.listener.PlotListener;
import com.worldcretornica.plotme_core.listener.PlotWorldEditListener;
import com.worldcretornica.plotme_core.utils.Util;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import me.flungo.bukkit.tools.ConfigAccessor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

public class PlotMe_Core extends JavaPlugin {

    public static final String LANG_PATH = "language";
    public static final String DEFAULT_LANG = "english";
    public static final String CAPTIONS_PATTERN = "caption-%s.yml";

    private String VERSION;

    //Config accessors <lang, accessor>
    private HashMap<String, ConfigAccessor> captionsCA;

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
        setupConfig();
        setupDefaultCaptions();
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

    private void setupConfig() {
        // Get the config we will be working with
        final FileConfiguration config = getConfig();

        // Move old configs to new locations
        config.set(LANG_PATH, config.getString("Language"));
        config.set("advancedLogging", config.getString("AdvancedLogging"));

        // Delete old configs
        config.set("Language", null);
        config.set("AdvancedLogging", null);

        // Get default config sections
        ConfigurationSection defaultWorld = getDefaultWorld();
        ConfigurationSection defaultEconomy = getDefaultEconomy();

        // If no world exists add config for a world
        if (!config.contains("worlds")) {
            config.set("worlds.plotsworld", defaultWorld);

            // If economy is enabled add economy config
            if (config.getBoolean("globalUseEconomy")) {
                config.set("worlds.plotsworld.economy", defaultEconomy);
            }
        }

        // Load config-old.yml
        // config-old.yml should be used to import settings from by DefaultGenerator
        final ConfigAccessor oldConfCA = new ConfigAccessor(this, "config-old.yml");
        final FileConfiguration oldConfig = oldConfCA.getConfig();

        // Create a list of old world configs that should be moved to config-old.yml
        final Set<String> oldWorldConfigs = new HashSet<String>();
        oldWorldConfigs.add("PathWidth");
        oldWorldConfigs.add("PlotSize");
        oldWorldConfigs.add("XTranslation");
        oldWorldConfigs.add("ZTranslation");
        oldWorldConfigs.add("BottomBlockId");
        oldWorldConfigs.add("WallBlockId");
        oldWorldConfigs.add("PlotFloorBlockId");
        oldWorldConfigs.add("PlotFillingBlockId");
        oldWorldConfigs.add("RoadMainBlockId");
        oldWorldConfigs.add("RoadStripeBlockId");
        oldWorldConfigs.add("RoadHeight");
        oldWorldConfigs.add("ProtectedWallBlockId");
        oldWorldConfigs.add("ForSaleWallBlockId");
        oldWorldConfigs.add("AuctionWallBlockId");

        // Copy defaults for all worlds
        ConfigurationSection worldsCS = config.getConfigurationSection("worlds");
        for (String worldname : worldsCS.getKeys(false)) {
            final ConfigurationSection worldCS = worldsCS.getConfigurationSection(worldname);

            // Add the default values
            for (String path : defaultWorld.getKeys(true)) {
                worldCS.addDefault(path, defaultWorld.get(path));
            }

            // Find old world data an move it to oldConfig
            ConfigurationSection oldWorldCS = oldConfig.getConfigurationSection("worlds." + worldname);
            for (String path : oldWorldConfigs) {
                if (worldCS.contains(path)) {
                    oldConfig.set(path, worldCS.get(path));
                    worldCS.set(path, null);
                }
            }

            // If economy section is present add economy defaults
            // or if economy is enabled but there is no config section for it, add it.
            if (worldCS.contains("economy")) {
                ConfigurationSection economyCS = worldCS.getConfigurationSection("economy");
                for (String path : defaultEconomy.getKeys(true)) {
                    economyCS.addDefault(path, defaultEconomy.get(path));
                }
            } else if (config.getBoolean("globalUseEconomy")) {
                worldCS.set("economy", getDefaultEconomy());
            }
        }

        // Copy new values over
        config.options().copyDefaults(true);

        // Save the config file back to disk
        if (!oldConfig.getConfigurationSection("worlds").getKeys(false).isEmpty()) {
            oldConfCA.saveConfig();
        }
        saveConfig();
    }

    private String loadCaptionConfig(String lang) {
        if (!captionsCA.containsKey(lang)) {
            String configFilename = String.format(CAPTIONS_PATTERN, lang);
            ConfigAccessor ca = new ConfigAccessor(this, configFilename);
            captionsCA.put(lang, null);
        }
        if (captionsCA.get(lang).getConfig().getKeys(false).isEmpty()) {
            if (lang.equals(DEFAULT_LANG)) {
                setupDefaultCaptions();
            } else {
                getLogger().log(Level.WARNING, "Could not load caption file for {0}"
                        + " or the language file was empty. Using " + DEFAULT_LANG, lang);
                return loadCaptionConfig(DEFAULT_LANG);
            }
        }
        return lang;
    }

    private ConfigAccessor getCaptionConfigCA(String lang) {
        lang = loadCaptionConfig(lang);
        return captionsCA.get(lang);
    }

    public FileConfiguration getCaptionConfig() {
        return getCaptionConfig(getConfig().getString(LANG_PATH));
    }

    public FileConfiguration getCaptionConfig(String lang) {
        return getCaptionConfigCA(lang).getConfig();
    }

    public void reloadCaptionConfig() {
        reloadCaptionConfig(getConfig().getString(LANG_PATH));
    }

    public void reloadCaptionConfig(String lang) {
        getCaptionConfigCA(lang).reloadConfig();
    }

    public void saveCaptionConfig() {
        saveCaptionConfig(getConfig().getString(LANG_PATH));
    }

    public void saveCaptionConfig(String lang) {
        getCaptionConfigCA(lang).saveConfig();
    }

    private void setupDefaultCaptions() {
        String fileName = String.format(CAPTIONS_PATTERN, DEFAULT_LANG);
        saveResource(fileName, true);
    }

    private ConfigurationSection getDefaultWorld() {
        InputStream defConfigStream = getResource("default-world.yml");

        return YamlConfiguration.loadConfiguration(defConfigStream);
    }

    private ConfigurationSection getDefaultEconomy() {
        InputStream defConfigStream = getResource("default-economy.yml");

        return YamlConfiguration.loadConfiguration(defConfigStream);
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

    public void initialize() {
        setPlotMeCoreManager(new PlotMeCoreManager(this));
        setUtil(new Util(this));

        VERSION = getDescription().getVersion();

        FileConfiguration config = getConfig();

        boolean usemySQL = config.getBoolean("usemySQL", false);
        String mySQLconn = config.getString("mySQLconn", "jdbc:mysql://localhost:3306/minecraft");
        String mySQLuname = config.getString("mySQLuname", "root");
        String mySQLpass = config.getString("mySQLpass", "password");

        setSqlManager(new SqlManager(this, usemySQL, mySQLuname, mySQLpass, mySQLconn));

        setGlobalUseEconomy(config.getBoolean("globalUseEconomy", false));
        setAdvancedLogging(config.getBoolean("advancedLogging", false));
        language = config.getString("language", "english");
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
        FileConfiguration config = getConfig();

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

        saveConfig();
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
