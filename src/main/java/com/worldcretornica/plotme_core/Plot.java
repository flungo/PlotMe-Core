package com.worldcretornica.plotme_core;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class Plot implements Comparable<Plot> {

    private PlotMe_Core plotmecore = null;

    public String owner;
    public String world;
    private HashSet<String> allowed;
    private HashSet<String> denied;
    public Biome biome;
    public int baseY;
    public int height;
    public Date expireddate;
    public boolean finished;
    public List<String[]> comments;
    public String id;
    public double customprice;
    public boolean forsale;
    public String finisheddate;
    public boolean protect;
    public boolean auctionned;
    public String currentbidder;
    public double currentbid;
    public String auctionneddate;

    public Plot(PlotMe_Core instance) {
        plotmecore = instance;
        owner = "";
        world = "";
        id = "";
        allowed = new HashSet<String>();
        denied = new HashSet<String>();
        biome = Biome.PLAINS;

        baseY = 0;
        height = 256;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        java.util.Date utlDate = cal.getTime();
        expireddate = new java.sql.Date(utlDate.getTime());

        comments = new ArrayList<String[]>();
        customprice = 0;
        forsale = false;
        finisheddate = "";
        protect = false;
        auctionned = false;
        currentbidder = "";
        currentbid = 0;
    }

    public Plot(PlotMe_Core instance, String own, World wor, String tid, int days) {
        plotmecore = instance;
        owner = own;
        world = wor.getName();
        allowed = new HashSet<String>();
        denied = new HashSet<String>();
        biome = Biome.PLAINS;
        id = tid;

        FileConfiguration config = plotmecore.getConfig();

        ConfigurationSection plotWorldConfig = config.getConfigurationSection("worlds." + world);

        if (plotWorldConfig == null) {
            baseY = 0;
            height = 256;
        } else {
            baseY = plotWorldConfig.getInt("PlotBase", 0);
            height = plotWorldConfig.getInt("PlotHeight", 256);
        }

        if (days == 0) {
            expireddate = null;
        } else {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, days);
            java.util.Date utlDate = cal.getTime();
            expireddate = new java.sql.Date(utlDate.getTime());
        }

        comments = new ArrayList<String[]>();
        customprice = 0;
        forsale = false;
        finisheddate = "";
        protect = false;
        auctionned = false;
        currentbidder = "";
        currentbid = 0;
    }

    public Plot(PlotMe_Core instance, String o, String w, String bio, int baseY, int height, Date exp, boolean fini, HashSet<String> al,
            List<String[]> comm, String tid, double custprice, boolean sale, String finishdt, boolean prot, String bidder,
            Double bid, boolean isauctionned, HashSet<String> den, String auctdate) {
        plotmecore = instance;
        owner = o;
        world = w;
        biome = Biome.valueOf(bio);
        this.baseY = baseY;
        this.height = height;
        expireddate = exp;
        finished = fini;
        allowed = al;
        comments = comm;
        id = tid;
        customprice = custprice;
        forsale = sale;
        finisheddate = finishdt;
        protect = prot;
        auctionned = isauctionned;
        currentbidder = bidder;
        currentbid = bid;
        denied = den;
        auctionneddate = auctdate;
    }

    public void setExpire(Date date) {
        if (!expireddate.equals(date)) {
            expireddate = date;
            updateField("expireddate", expireddate);
        }
    }

    public void resetExpire(int days) {
        if (days == 0) {
            if (expireddate != null) {
                expireddate = null;
                updateField("expireddate", expireddate);
            }
        } else {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, days);
            java.util.Date utlDate = cal.getTime();
            java.sql.Date temp = new java.sql.Date(utlDate.getTime());
            if (expireddate == null || !temp.toString().equalsIgnoreCase(expireddate.toString())) {
                expireddate = temp;
                updateField("expireddate", expireddate);
            }
        }
    }

    public String getExpire() {
        return DateFormat.getDateInstance().format(expireddate);
    }

    public void setFinished() {
        finisheddate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
        finished = true;

        updateFinished(finisheddate, finished);
    }

    public void setUnfinished() {
        finisheddate = "";
        finished = false;

        updateFinished(finisheddate, finished);
    }

    public Biome getBiome() {
        return biome;
    }

    public String getOwner() {
        return owner;
    }

    public String getAllowed() {
        String list = "";

        for (String s : allowed) {
            list = list + s + ", ";
        }
        if (list.length() > 1) {
            list = list.substring(0, list.length() - 2);
        }
        return list;
    }

    public String getDenied() {
        String list = "";

        for (String s : denied) {
            list = list + s + ", ";
        }
        if (list.length() > 1) {
            list = list.substring(0, list.length() - 2);
        }
        return list;
    }

    public int getCommentsCount() {
        return comments.size();
    }

    public String[] getComments(int i) {
        return comments.get(i);
    }

    public void addAllowed(String name) {
        if (!isAllowed(name)) {
            allowed.add(name);
            plotmecore.getSqlManager().addPlotAllowed(name, plotmecore.getPlotMeCoreManager().getIdX(id), plotmecore.getPlotMeCoreManager().getIdZ(id), world);
        }
    }

    public void addDenied(String name) {
        if (!isDenied(name)) {
            denied.add(name);
            plotmecore.getSqlManager().addPlotDenied(name, plotmecore.getPlotMeCoreManager().getIdX(id), plotmecore.getPlotMeCoreManager().getIdZ(id), world);
        }
    }

    public void removeAllowed(String name) {
        String found = "";

        for (String n : allowed) {
            if (n.equalsIgnoreCase(name)) {
                found = n;
                break;
            }
        }

        if (!found.equals("")) {
            allowed.remove(found);
            plotmecore.getSqlManager().deletePlotAllowed(plotmecore.getPlotMeCoreManager().getIdX(id), plotmecore.getPlotMeCoreManager().getIdZ(id), found, world);
        }
    }

    public void removeDenied(String name) {
        String found = "";

        for (String n : denied) {
            if (n.equalsIgnoreCase(name)) {
                found = n;
                break;
            }
        }

        if (!found.equals("")) {
            denied.remove(found);
            plotmecore.getSqlManager().deletePlotDenied(plotmecore.getPlotMeCoreManager().getIdX(id), plotmecore.getPlotMeCoreManager().getIdZ(id), found, world);
        }
    }

    public void removeAllAllowed() {
        for (String n : allowed) {
            plotmecore.getSqlManager().deletePlotAllowed(plotmecore.getPlotMeCoreManager().getIdX(id), plotmecore.getPlotMeCoreManager().getIdZ(id), n, world);
        }
        allowed = new HashSet<String>();
    }

    public void removeAllDenied() {
        for (String n : denied) {
            plotmecore.getSqlManager().deletePlotDenied(plotmecore.getPlotMeCoreManager().getIdX(id), plotmecore.getPlotMeCoreManager().getIdZ(id), n, world);
        }
        denied = new HashSet<String>();
    }

    public boolean isAllowed(String name) {
        return isAllowed(name, null);
    }

    public boolean isAllowed(String name, Integer y) {
        return isAllowed(name, y, true, true);
    }

    public boolean isAllowed(String name, Integer y, boolean IncludeStar, boolean IncludeGroup) {
        if (y != null && !contains(y)) {
            return false;
        }

        if (owner.equalsIgnoreCase(name) || (IncludeStar && owner.equals("*"))) {
            return true;
        }

        if (IncludeGroup && owner.toLowerCase().startsWith("group:") && Bukkit.getServer().getPlayerExact(name) != null) {
            if (Bukkit.getServer().getPlayerExact(name).hasPermission("plotme.group." + owner.replace("Group:", ""))) {
                return true;
            }
        }

        for (String str : allowed) {
            if (str.equalsIgnoreCase(name) || (IncludeStar && str.equals("*"))) {
                return true;
            }

            if (IncludeGroup && str.toLowerCase().startsWith("group:") && Bukkit.getServer().getPlayerExact(name) != null) {
                if (Bukkit.getServer().getPlayerExact(name).hasPermission("plotme.group." + str.replace("Group:", ""))) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean contains(int y) {
        return y >= baseY && y <= baseY + height;
    }

    public boolean isDenied(String name) {
        return isDenied(name, null);
    }

    public boolean isDenied(String name, Integer y) {
        if (isAllowed(name, y, false, false)) {
            return false;
        }

        for (String str : denied) {
            if (str.equalsIgnoreCase(name) || str.equals("*")) {
                return true;
            }

            if (str.toLowerCase().startsWith("group:") && Bukkit.getServer().getPlayerExact(name) != null) {
                if (Bukkit.getServer().getPlayerExact(name).hasPermission("plotme.group." + str.replace("Group:", ""))) {
                    return true;
                }
            }
        }

        return false;
    }

    public HashSet<String> allowed() {
        return allowed;
    }

    public HashSet<String> denied() {
        return denied;
    }

    public int allowedcount() {
        return allowed.size();
    }

    public int deniedcount() {
        return denied.size();
    }

    @Override
    public int compareTo(Plot plot) {
        if (expireddate.compareTo(plot.expireddate) == 0) {
            return owner.compareTo(plot.owner);
        } else {
            return expireddate.compareTo(plot.expireddate);
        }
    }

    private void updateFinished(String finishtime, boolean isfinished) {
        updateField("finisheddate", finishtime);
        updateField("finished", isfinished);
    }

    public void updateField(String field, Object value) {
        plotmecore.getSqlManager().updatePlot(plotmecore.getPlotMeCoreManager().getIdX(id), plotmecore.getPlotMeCoreManager().getIdZ(id), world, field, value);
    }

    /*private static Map<String, Double> sortByValues(final Map<String, Double> map)
     {
     Comparator<String> valueComparator = new Comparator<String>()
     {
     public int compare(String k1, String k2)
     {
     int compare = map.get(k2).compareTo(map.get(k1));
     if (compare == 0)
     return 1;
     else
     return compare;
     }
     };

     Map<String, Double> sortedByValues = new TreeMap<String, Double>(valueComparator);
     sortedByValues.putAll(map);
     return sortedByValues;
     }*/
}
