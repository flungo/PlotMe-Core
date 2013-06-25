package com.worldcretornica.plotme_core.commands;

import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMe_Core;

public class CmdAddTime extends PlotCommand 
{
	public CmdAddTime(PlotMe_Core instance) 
	{
		super(instance);
	}
	
	public boolean exec(Player p, String[] args)
	{
		if(plugin.cPerms(p, "PlotMe.admin.addtime"))
		{
			if(!plugin.getPlotMeCoreManager().isPlotWorld(p))
			{
				p.sendMessage(RED + C("MsgNotPlotWorld"));
				return true;
			}
			else
			{
				String id = plugin.getPlotMeCoreManager().getPlotId(p.getLocation());
				
				if(id.equals(""))
				{
					p.sendMessage(RED + C("MsgNoPlotFound"));
				}
				else
				{
					if(!plugin.getPlotMeCoreManager().isPlotAvailable(id, p))
					{
						Plot plot = plugin.getPlotMeCoreManager().getPlotById(p,id);
						
						if(plot != null)
						{
							String name = p.getName();
							
							plot.resetExpire(plugin.getPlotMeCoreManager().getMap(p).DaysToExpiration);
							p.sendMessage(C("MsgPlotExpirationReset"));
							
							if(isAdv)
								plugin.getLogger().info(LOG + name + " reset expiration on plot " + id);
						}
					}
					else
					{
						p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgHasNoOwner"));
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
