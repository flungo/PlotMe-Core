package com.worldcretornica.plotme_core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.api.v0_14.IPlotMe_GeneratorManager;

public class PlotMeCoreManager 
{
	public static int getIdX(String id)
	{
		return Integer.parseInt(id.substring(0, id.indexOf(";")));
	}
	
	public static int getIdZ(String id)
	{
		return Integer.parseInt(id.substring(id.indexOf(";") + 1));
	}
	
	public static int getNbOwnedPlot(Player p)
	{
		return getNbOwnedPlot(p.getName(), p.getWorld());
	}
	
	public static int getNbOwnedPlot(Player p, World w)
	{
		return getNbOwnedPlot(p.getName(), w);
	}

	public static int getNbOwnedPlot(String name, World w)
	{
		int nbfound = 0;
		if(getPlots(w) != null)
		{
			for(Plot plot : getPlots(w).values())
			{
				if(plot.owner.equalsIgnoreCase(name))
				{
					nbfound++;
				}
			}
		}
		return nbfound;
	}
	
	public static boolean isEconomyEnabled(World w)
	{
		PlotMapInfo pmi = getMap(w);
		
		if(pmi == null)
			return false;
		else
			return pmi.UseEconomy && PlotMe_Core.globalUseEconomy && PlotMe_Core.economy != null;
	}
	
	public static boolean isEconomyEnabled(String name)
	{
		PlotMapInfo pmi = getMap(name);
		
		if(pmi == null)
			return false;
		else
			return pmi.UseEconomy && PlotMe_Core.globalUseEconomy;
	}
	
	public static boolean isEconomyEnabled(Player p)
	{
		if(PlotMe_Core.economy == null) return false;
		
		PlotMapInfo pmi = getMap(p);
		
		if(pmi == null)
			return false;
		else
			return pmi.UseEconomy && PlotMe_Core.globalUseEconomy;
	}
	
	public static boolean isEconomyEnabled(Block b)
	{
		PlotMapInfo pmi = getMap(b);
		
		if(pmi == null)
			return false;
		else
			return pmi.UseEconomy && PlotMe_Core.globalUseEconomy;
	}
	
	public static PlotMapInfo getMap(World w)
	{
		if(w == null)
			return null;
		else
		{			
			String worldname = w.getName().toLowerCase();
			
			if(PlotMe_Core.plotmaps.containsKey(worldname))
				return PlotMe_Core.plotmaps.get(worldname);
			else
				return null;
		}
	}
	
	public static PlotMapInfo getMap(String name)
	{
		String worldname = name.toLowerCase();
		
		if(PlotMe_Core.plotmaps.containsKey(worldname))
			return PlotMe_Core.plotmaps.get(worldname);
		else
			return null;
	}
	
	public static PlotMapInfo getMap(Location l)
	{
		if(l == null)
			return null;
		else
		{
			String worldname = l.getWorld().getName().toLowerCase();
			
			if(PlotMe_Core.plotmaps.containsKey(worldname))
				return PlotMe_Core.plotmaps.get(worldname);
			else
				return null;
		}
	}
	
	public static PlotMapInfo getMap(Player p)
	{
		if(p == null)
			return null;
		else
		{
			String worldname = p.getWorld().getName().toLowerCase();
			
			if(PlotMe_Core.plotmaps.containsKey(worldname))
				return PlotMe_Core.plotmaps.get(worldname);
			else
				return null;
		}
	}
	
	public static PlotMapInfo getMap(Block b)
	{
		if(b == null)
			return null;
		else
		{
			String worldname = b.getWorld().getName().toLowerCase();
			
			if(PlotMe_Core.plotmaps.containsKey(worldname))
				return PlotMe_Core.plotmaps.get(worldname);
			else
				return null;
		}
	}
	
	public static HashMap<String, Plot> getPlots(World w)
	{
		PlotMapInfo pmi = getMap(w);
		
		if(pmi == null)
			return null;
		else
			return pmi.plots;
	}
	
	public static HashMap<String, Plot> getPlots(String name)
	{		
		PlotMapInfo pmi = getMap(name);
		
		if(pmi == null)
			return null;
		else
			return pmi.plots;
	}
	
	public static HashMap<String, Plot> getPlots(Player p)
	{		
		PlotMapInfo pmi = getMap(p);
		
		if(pmi == null)
			return null;
		else
			return pmi.plots;
	}
	
	public static HashMap<String, Plot> getPlots(Block b)
	{	
		PlotMapInfo pmi = getMap(b);
		
		if(pmi == null)
			return null;
		else
			return pmi.plots;
	}
	
	public static HashMap<String, Plot> getPlots(Location l)
	{
		PlotMapInfo pmi = getMap(l);
		
		if(pmi == null)
			return null;
		else
			return pmi.plots;
	}
	
	public static Plot getPlotById(World w, String id)
	{
		HashMap<String, Plot> plots = getPlots(w);
		
		if(plots == null)
			return null;
		else
			return plots.get(id);
	}
	
	public static Plot getPlotById(String name, String id)
	{
		HashMap<String, Plot> plots = getPlots(name);
		
		if(plots == null)
			return null;
		else
			return plots.get(id);
	}
	
	public static Plot getPlotById(Player p, String id)
	{
		HashMap<String, Plot> plots = getPlots(p);
		
		if(plots == null)
			return null;
		else
			return plots.get(id);
	}
	
	public static Plot getPlotById(Player p)
	{
		HashMap<String, Plot> plots = getPlots(p);
		String plotid = getPlotId(p.getLocation());
		
		if(plots == null || plotid == "")
			return null;
		else
			return plots.get(plotid);
	}
	
	public static Plot getPlotById(Location l)
	{
		HashMap<String, Plot> plots = getPlots(l);
		String plotid = getPlotId(l);
		
		if(plots == null || plotid == "")
			return null;
		else
			return plots.get(plotid);
	}
	
	public static Plot getPlotById(Block b, String id)
	{
		HashMap<String, Plot> plots = getPlots(b);
		
		if(plots == null)
			return null;
		else
			return plots.get(id);
	}
	
	public static Plot getPlotById(Block b)
	{
		HashMap<String, Plot> plots = getPlots(b);
		String plotid = getPlotId(b.getLocation());
		
		if(plots == null || plotid == "")
			return null;
		else
			return plots.get(plotid);
	}
	
	public static void deleteNextExpired(World w, CommandSender sender)
	{
		List<Plot> expiredplots = new ArrayList<Plot>();
		HashMap<String, Plot> plots = getPlots(w);
		String date = PlotMe_Core.getDate();
		Plot expiredplot;
		
		for(String id : plots.keySet())
		{
			Plot plot = plots.get(id);
			
			if(!plot.protect && !plot.finished && plot.expireddate != null && PlotMe_Core.getDate(plot.expireddate).compareTo(date.toString()) < 0)
			{
				expiredplots.add(plot);
			}
		}
		
		plots = null;
		
		Collections.sort(expiredplots);
		
		expiredplot = expiredplots.get(0);
		
		expiredplots = null;
		
		clear(w, expiredplot);
		
		String id = expiredplot.id;
		
		getPlots(w).remove(id);
			
		getGenMan(w).removeOwnerDisplay(w, id);
		getGenMan(w).removeSellerDisplay(w, id);
		
		SqlManager.deletePlot(getIdX(id), getIdZ(id), w.getName().toLowerCase());
	}

	public static World getFirstWorld()
	{
		if(PlotMe_Core.plotmaps != null)
		{
			if(PlotMe_Core.plotmaps.keySet() != null)
			{
				if(PlotMe_Core.plotmaps.keySet().toArray().length > 0)
				{
					return Bukkit.getWorld((String) PlotMe_Core.plotmaps.keySet().toArray()[0]);
				}
			}
		}
		return null;
	}
	
	public static World getFirstWorld(String player)
	{
		if(PlotMe_Core.plotmaps != null)
		{
			if(PlotMe_Core.plotmaps.keySet() != null)
			{
				if(PlotMe_Core.plotmaps.keySet().toArray().length > 0)
				{
					for(String mapkey : PlotMe_Core.plotmaps.keySet())
					{
						for(String id : PlotMe_Core.plotmaps.get(mapkey).plots.keySet())
						{
							if(PlotMe_Core.plotmaps.get(mapkey).plots.get(id).owner.equalsIgnoreCase(player))
							{
								return Bukkit.getWorld(mapkey);
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public static Plot getFirstPlot(String player)
	{
		if(PlotMe_Core.plotmaps != null)
		{
			if(PlotMe_Core.plotmaps.keySet() != null)
			{
				if(PlotMe_Core.plotmaps.keySet().toArray().length > 0)
				{
					for(String mapkey : PlotMe_Core.plotmaps.keySet())
					{
						for(String id : PlotMe_Core.plotmaps.get(mapkey).plots.keySet())
						{
							if(PlotMe_Core.plotmaps.get(mapkey).plots.get(id).owner.equalsIgnoreCase(player))
							{
								return PlotMe_Core.plotmaps.get(mapkey).plots.get(id);
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public static boolean isPlotWorld(World w)
	{
		if(w == null)
			return false;
		else
			return PlotMe_Core.plotmaps.containsKey(w.getName().toLowerCase());
	}
	
	public static boolean isPlotWorld(String name)
	{
		return PlotMe_Core.plotmaps.containsKey(name.toLowerCase());
	}
	
	public static boolean isPlotWorld(Location l)
	{
		if(l == null)
			return false;
		else
			return PlotMe_Core.plotmaps.containsKey(l.getWorld().getName().toLowerCase());
	}
	
	public static boolean isPlotWorld(Player p)
	{
		if(p == null)
			return false;
		else
			return PlotMe_Core.plotmaps.containsKey(p.getWorld().getName().toLowerCase());
	}
	
	public static boolean isPlotWorld(Block b)
	{
		if(b == null)
			return false;
		else
			return PlotMe_Core.plotmaps.containsKey(b.getWorld().getName().toLowerCase());
	}
	
	public static boolean isPlotWorld(BlockState b)
	{
		if(b == null)
			return false;
		else
			return PlotMe_Core.plotmaps.containsKey(b.getWorld().getName().toLowerCase());
	}
	
	public static Plot createPlot(World w, String id, String owner)
	{
		if(isPlotAvailable(id, w) && id != "")
		{
			Plot plot = new Plot(owner, w, id, getMap(w).DaysToExpiration);

			setOwnerSign(w, plot);
			
			getPlots(w).put(id, plot);
			SqlManager.addPlot(plot, getIdX(id), getIdZ(id), w);
			return plot;
		}
		else
		{
			return null;
		}
	}
	
	public static boolean movePlot(World w, String idFrom, String idTo)
	{
		Location plot1Bottom = getPlotBottomLoc(w, idFrom);
		Location plot2Bottom = getPlotBottomLoc(w, idTo);
		Location plot1Top = getPlotTopLoc(w, idFrom);
		
		int distanceX = plot1Bottom.getBlockX() - plot2Bottom.getBlockX();
		int distanceZ = plot1Bottom.getBlockZ() - plot2Bottom.getBlockZ();
		
		for(int x = plot1Bottom.getBlockX(); x <= plot1Top.getBlockX(); x++)
		{
			for(int z = plot1Bottom.getBlockZ(); z <= plot1Top.getBlockZ(); z++)
			{
				Block plot1Block = w.getBlockAt(new Location(w, x, 0, z));
				Block plot2Block = w.getBlockAt(new Location(w, x - distanceX, 0, z - distanceZ));
				
				String plot1Biome = plot1Block.getBiome().name();
				String plot2Biome = plot2Block.getBiome().name();
				
				plot1Block.setBiome(Biome.valueOf(plot2Biome));
				plot2Block.setBiome(Biome.valueOf(plot1Biome));
				
				for(int y = 0; y < w.getMaxHeight() ; y++)
				{
					plot1Block = w.getBlockAt(new Location(w, x, y, z));
					int plot1Type = plot1Block.getTypeId();
					byte plot1Data = plot1Block.getData();
					
					plot2Block = w.getBlockAt(new Location(w, x - distanceX, y, z - distanceZ));
					int plot2Type = plot2Block.getTypeId();
					byte plot2Data = plot2Block.getData();
					
					plot1Block.setTypeIdAndData(plot2Type, plot2Data, false);
					plot1Block.setData(plot2Data);
					
					plot2Block.setTypeIdAndData(plot1Type, plot1Data, false);
					plot2Block.setData(plot1Data);
				}
			}
		}
		
		HashMap<String, Plot> plots = getPlots(w);
		
		if(plots.containsKey(idFrom))
		{
			if(plots.containsKey(idTo))
			{
				Plot plot1 = plots.get(idFrom);
				Plot plot2 = plots.get(idTo);
								
				int idX = getIdX(idTo);
				int idZ = getIdZ(idTo);
				SqlManager.deletePlot(idX, idZ, plot2.world);
				plots.remove(idFrom);
				plots.remove(idTo);
				idX = getIdX(idFrom);
				idZ = getIdZ(idFrom);
				SqlManager.deletePlot(idX, idZ, plot1.world);
				
				plot2.id = "" + idX + ";" + idZ;
				SqlManager.addPlot(plot2, idX, idZ, w);
				plots.put(idFrom, plot2);
				
				for(int i = 0 ; i < plot2.comments.size() ; i++)
				{
					SqlManager.addPlotComment(plot2.comments.get(i), i, idX, idZ, plot2.world);
				}
				
				for(String player : plot2.allowed())
				{
					SqlManager.addPlotAllowed(player, idX, idZ, plot2.world);
				}
				
				idX = getIdX(idTo);
				idZ = getIdZ(idTo);
				plot1.id = "" + idX + ";" + idZ;
				SqlManager.addPlot(plot1, idX, idZ, w);
				plots.put(idTo, plot1);
				
				for(int i = 0 ; i < plot1.comments.size() ; i++)
				{
					SqlManager.addPlotComment(plot1.comments.get(i), i, idX, idZ, plot1.world);
				}
				
				for(String player : plot1.allowed())
				{
					SqlManager.addPlotAllowed(player, idX, idZ, plot1.world);
				}
				
				setOwnerSign(w, plot1);
				setSellSign(w, plot1);
				setOwnerSign(w, plot2);
				setSellSign(w, plot2);
				
			}
			else
			{
				Plot plot = plots.get(idFrom);
				
				int idX = getIdX(idFrom);
				int idZ = getIdZ(idFrom);
				SqlManager.deletePlot(idX, idZ, plot.world);
				plots.remove(idFrom);
				idX = getIdX(idTo);
				idZ = getIdZ(idTo);
				plot.id = "" + idX + ";" + idZ;
				SqlManager.addPlot(plot, idX, idZ, w);
				plots.put(idTo, plot);
				
				for(int i = 0 ; i < plot.comments.size() ; i++)
				{
					SqlManager.addPlotComment(plot.comments.get(i), i, idX, idZ, plot.world);
				}
				
				for(String player : plot.allowed())
				{
					SqlManager.addPlotAllowed(player, idX, idZ, plot.world);
				}
				
				setOwnerSign(w, plot);
				setSellSign(w, plot);
				getGenMan(w).removeOwnerDisplay(w, idFrom);
				getGenMan(w).removeSellerDisplay(w, idFrom);
				
			}
		}else{
			if(plots.containsKey(idTo))
			{
				Plot plot = plots.get(idTo);
				
				int idX = getIdX(idTo);
				int idZ = getIdZ(idTo);
				SqlManager.deletePlot(idX, idZ, plot.world);
				plots.remove(idTo);
				
				idX = getIdX(idFrom);
				idZ = getIdZ(idFrom);
				plot.id = "" + idX + ";" + idZ;
				SqlManager.addPlot(plot, idX, idZ, w);
				plots.put(idFrom, plot);
				
				for(int i = 0 ; i < plot.comments.size() ; i++)
				{
					SqlManager.addPlotComment(plot.comments.get(i), i, idX, idZ, plot.world);
				}
				
				for(String player : plot.allowed())
				{
					SqlManager.addPlotAllowed(player, idX, idZ, plot.world);
				}
				
				setOwnerSign(w, plot);
				setSellSign(w, plot);
				getGenMan(w).removeOwnerDisplay(w, idTo);
				getGenMan(w).removeSellerDisplay(w, idTo);
			}
		}
		
		return true;
	}
	
	public static void RemoveLWC(World w, Plot plot)
	{
		if(PlotMe_Core.usinglwc)
		{
			Location bottom = getGenMan(w).getBottom(w, plot.id);
			Location top = getGenMan(w).getTop(w, plot.id);
			final int x1 = bottom.getBlockX();
			final int y1 = bottom.getBlockY();
	    	final int z1 = bottom.getBlockZ();
	    	final int x2 = top.getBlockX();
	    	final int y2 = top.getBlockY();
	    	final int z2 = top.getBlockZ();
			final String wname = w.getName();
	    	
			Bukkit.getScheduler().runTaskAsynchronously(PlotMe_Core.self, new Runnable() 
			{	
				public void run() 
				{
					LWC lwc = com.griefcraft.lwc.LWC.getInstance();
					List<Protection> protections = lwc.getPhysicalDatabase().loadProtections(wname, x1, x2, y1, y2, z1, z2);

					for (Protection protection : protections) {
					    protection.remove();
					}
				}
			});
	    }
	}
	
	public static void setOwnerSign(World w, Plot plot)
	{			
		String line1 = "";
		String line2 = "";
		String line3 = "";
		String line4 = "";
		String id = plot.id;
				
		if((PlotMe_Core.caption("SignId") + id).length() > 16)
		{
			line1 = (PlotMe_Core.caption("SignId") + id).substring(0, 16);
			if((PlotMe_Core.caption("SignId") + id).length() > 32)
			{
				line2 = (PlotMe_Core.caption("SignId") + id).substring(16, 32);
			}
			else
			{
				line2 = (PlotMe_Core.caption("SignId") + id).substring(16);
			}
		}
		else
		{
			line1 = PlotMe_Core.caption("SignId") + id;
		}
		if((PlotMe_Core.caption("SignOwner") + plot.owner).length() > 16)
		{
			line3 = (PlotMe_Core.caption("SignOwner") + plot.owner).substring(0, 16);
			if((PlotMe_Core.caption("SignOwner") + plot.owner).length() > 32)
			{
				line4 = (PlotMe_Core.caption("SignOwner") + plot.owner).substring(16, 32);
			}
			else
			{
				line4 = (PlotMe_Core.caption("SignOwner") + plot.owner).substring(16);
			}
		}else{
			line3 = PlotMe_Core.caption("SignOwner") + plot.owner;
			line4 = "";
		}
		
		getGenMan(w).setOwnerDisplay(w, plot.id, line1, line2, line3, line4);
	}
	
	public static void setSellSign(World w, Plot plot)
	{
		String line1 = "";
		String line2 = "";
		String line3 = "";
		String line4 = "";
		String id = plot.id;
		
		getGenMan(w).removeSellerDisplay(w, id);
		
		if(plot.forsale || plot.auctionned)
		{
			if(plot.forsale)
			{
				line1 = PlotMe_Core.caption("SignForSale");
				line2 = PlotMe_Core.caption("SignPrice");
				if(plot.customprice % 1 == 0)
					line3 = PlotMe_Core.caption("SignPriceColor") + Math.round(plot.customprice);
				else
					line3 = PlotMe_Core.caption("SignPriceColor") + plot.customprice;
				line4 = "/plotme " + PlotMe_Core.caption("CommandBuy");
			}
			
			getGenMan(w).setSellerDisplay(w, plot.id, line1, line2, line3, line4);
			
			line1 = "";
			line2 = "";
			line3 = "";
			line4 = "";
			
			if(plot.auctionned)
			{				
				line1 = PlotMe_Core.caption("SignOnAuction");
				if(plot.currentbidder.equals(""))
					line2 = PlotMe_Core.caption("SignMinimumBid");
				else
					line2 = PlotMe_Core.caption("SignCurrentBid");
				if(plot.currentbid % 1 == 0)
					line3 = PlotMe_Core.caption("SignCurrentBidColor") + Math.round(plot.currentbid);
				else
					line3 = PlotMe_Core.caption("SignCurrentBidColor") + plot.currentbid;
				line4 = "/plotme " + PlotMe_Core.caption("CommandBid") + " <x>";
			}
			
			getGenMan(w).setAuctionDisplay(w, plot.id, line1, line2, line3, line4);
		}
	}
	
	public static void adjustLinkedPlots(String id, World world)
	{		
		HashMap<String, Plot> plots = getPlots(world);
		
		IPlotMe_GeneratorManager gm = getGenMan(world);
		
		int x = getIdX(id);
		int z = getIdZ(id);
		
		Plot p11 = plots.get(id);
		
		if(p11 != null)
		{
			Plot p01 = plots.get((x - 1) + ";" + z);
			Plot p10 = plots.get(x + ";" + (z - 1));
			Plot p12 = plots.get(x + ";" + (z + 1));
			Plot p21 = plots.get((x + 1) + ";" + z);
			Plot p00 = plots.get((x - 1) + ";" + (z - 1));
			Plot p02 = plots.get((x - 1) + ";" + (z + 1));
			Plot p20 = plots.get((x + 1) + ";" + (z - 1));
			Plot p22 = plots.get((x + 1) + ";" + (z + 1));
			
			if(p01 != null && p01.owner.equalsIgnoreCase(p11.owner))
			{
				gm.fillroad(p01.id, p11.id, world);
			}
			
			if(p10 != null && p10.owner.equalsIgnoreCase(p11.owner))
			{
				gm.fillroad(p10.id, p11.id, world);
			}

			if(p12 != null && p12.owner.equalsIgnoreCase(p11.owner))
			{
				gm.fillroad(p12.id, p11.id, world);
			}

			if(p21 != null && p21.owner.equalsIgnoreCase(p11.owner))
			{
				gm.fillroad(p21.id, p11.id, world);
			}
			
			if(p00 != null && p10 != null && p01 != null &&
					p00.owner.equalsIgnoreCase(p11.owner) &&
					p11.owner.equalsIgnoreCase(p10.owner) &&
					p10.owner.equalsIgnoreCase(p01.owner))
			{
				gm.fillmiddleroad(p00.id, p11.id, world);
			}
			
			if(p10 != null && p20 != null && p21 != null &&
					p10.owner.equalsIgnoreCase(p11.owner) &&
					p11.owner.equalsIgnoreCase(p20.owner) &&
					p20.owner.equalsIgnoreCase(p21.owner))
			{
				gm.fillmiddleroad(p20.id, p11.id, world);
			}
			
			if(p01 != null && p02 != null && p12 != null &&
					p01.owner.equalsIgnoreCase(p11.owner) &&
					p11.owner.equalsIgnoreCase(p02.owner) &&
					p02.owner.equalsIgnoreCase(p12.owner))
			{
				gm.fillmiddleroad(p02.id, p11.id, world);
			}
			
			if(p12 != null && p21 != null && p22 != null &&
					p12.owner.equalsIgnoreCase(p11.owner) &&
					p11.owner.equalsIgnoreCase(p21.owner) &&
					p21.owner.equalsIgnoreCase(p22.owner))
			{
				gm.fillmiddleroad(p22.id, p11.id, world);
			}
			
		}
	}
	
	public static void setBiome(World w, Plot plot, Biome b)
	{
		String id = plot.id;
		
		getGenMan(w).setBiome(w, id, b);
		
		SqlManager.updatePlot(getIdX(id), getIdZ(id), plot.world, "biome", b.name());
	}
	
	public static void clear(World w, Plot plot)
	{
		String id = plot.id;
		getGenMan(w).clear(w, id);
		adjustWall(w, plot);
		
		RemoveLWC(w, plot);
	}
	
	public static boolean isPlotAvailable(String id, World world)
	{
		return isPlotAvailable(id, world.getName().toLowerCase());
	}
	
	public static boolean isPlotAvailable(String id, Player p)
	{
		return isPlotAvailable(id, p.getWorld().getName().toLowerCase());
	}
	
	public static boolean isPlotAvailable(String id, String world)
	{
		if(isPlotWorld(world))
		{
			return !getPlots(world).containsKey(id);
		}
		else
		{
			return false;
		}
	}

	public static String getPlotId(Location l) 
	{
		return getGenMan(l).getPlotId(l);
	}
	
	public static String getPlotId(Player p) 
	{
		return getPlotId(p.getLocation());
	}
	
	public static IPlotMe_GeneratorManager getGenMan(World w)
	{
		return PlotMe_Core.getGenManager(w);
	}
	
	public static IPlotMe_GeneratorManager getGenMan(Location l)
	{
		return PlotMe_Core.getGenManager(l.getWorld());
	}
	
	public static IPlotMe_GeneratorManager getGenMan(String s)
	{
		return PlotMe_Core.getGenManager(s);
	}

	public static Location getPlotBottomLoc(World w, String id) 
	{
		return getGenMan(w).getPlotBottomLoc(w, id);
	}

	public static Location getPlotTopLoc(World w, String id) 
	{
		return getGenMan(w).getPlotTopLoc(w, id);
	}

	public static void adjustWall(Location l) 
	{
		Plot plot = getPlotById(l);
		String id = getPlotId(l);
		World w = l.getWorld();
		
		if(plot == null)
		{
			getGenMan(w).adjustPlotFor(w, id, false, false, false, false);
		}
		else
		{
			getGenMan(w).adjustPlotFor(w, id, true, plot.protect, plot.auctionned, plot.forsale);
		}
	}
	
	public static void adjustWall(World w, Plot plot) 
	{
		String id = plot.id;
		getGenMan(w).adjustPlotFor(w, id, true, plot.protect, plot.auctionned, plot.forsale);
	}

	public static void removeOwnerSign(World w, String id) 
	{
		getGenMan(w).removeOwnerDisplay(w, id);
	}

	public static void removeSellSign(World w, String id) 
	{
		getGenMan(w).removeSellerDisplay(w, id);
	}

	public static void removeAuctionSign(World w, String id) 
	{
		getGenMan(w).removeAuctionDisplay(w, id);
	}

	public static boolean isValidId(World w, String id) 
	{
		return getGenMan(w).isValidId(id);
	}

	public static int bottomX(String id, World w) 
	{
		return getGenMan(w).bottomX(id, w);
	}

	public static int topX(String id, World w) 
	{
		return getGenMan(w).topX(id, w);
	}

	public static int bottomZ(String id, World w) 
	{
		return getGenMan(w).bottomZ(id, w);
	}
	
	public static int topZ(String id, World w) 
	{
		return getGenMan(w).topZ(id, w);
	}

	public static void setBiome(World w, String id, Biome biome) 
	{
		getGenMan(w).setBiome(w, id, biome);
	}

	public static Location getPlotHome(World w, String id) 
	{
		return getGenMan(w).getPlotHome(w, id);
	}

	public static List<Player> getPlayersInPlot(World w, String id) 
	{
		return getGenMan(w).getPlayersInPlot(w, id);
	}

	public static void regen(World w, Plot plot, CommandSender sender) 
	{
		getGenMan(w).regen(w, plot.id, sender);
	}
}
