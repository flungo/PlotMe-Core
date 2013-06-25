package com.worldcretornica.plotme_core.commands;

import java.util.List;

import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.utils.MinecraftFontWidthCalculator;

public class CmdDoneList extends PlotCommand 
{
	public CmdDoneList(PlotMe_Core instance) {
		super(instance);
	}

	public boolean exec(Player p, String[] args) 
	{
		if(plugin.cPerms(p, "PlotMe.admin.done"))
		{
			PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(p);
			
			if(pmi == null)
			{
				p.sendMessage(RED + C("MsgNotPlotWorld"));
				return true;
			}
			else
			{
				int maxpage = 0;
				int pagesize = 8;
				int page = 1;
				
				if(args.length == 2)
				{
					try
					{
						page = Integer.parseInt(args[1]);
					}catch(NumberFormatException ex){}
				}
				
				maxpage = (int) Math.ceil((double) plugin.getSqlManager().getFinishedPlotCount(p.getWorld().getName()) / (double)pagesize);
				
				if(page < 0)
				{
					page = 1;
				}else if(page > maxpage)
				{
					page = maxpage;
				}
				
				List<Plot> finishedplots = plugin.getSqlManager().getDonePlots(p.getWorld().getName(), page, pagesize);
				
				if(finishedplots.size() == 0)
				{
					p.sendMessage(C("MsgNoPlotsFinished"));
				}
				else
				{
					p.sendMessage(C("MsgFinishedPlotsPage") + " " + page + "/" + maxpage);
					
					for(int i = (page-1) * pagesize; i < finishedplots.size() && i < (page * pagesize); i++)
					{	
						Plot plot = finishedplots.get(i);
						
						String starttext = "  " + AQUA + plot.id + RESET + " -> " + plot.owner;
						
						int textLength = MinecraftFontWidthCalculator.getStringWidth(starttext);						
						
						String line = starttext + Util().whitespace(550 - textLength) + "@" + plot.finisheddate;

						p.sendMessage(line);
					}
				}
			}
		}
		else
		{
			p.sendMessage(RED + C("MsgPermissionDenied"));
		}
		return true;
	}
}
