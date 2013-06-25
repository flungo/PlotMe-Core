package com.worldcretornica.plotme_core.commands;

import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMe_Core;

public class CmdComments extends PlotCommand 
{
	public CmdComments(PlotMe_Core instance) {
		super(instance);
	}

	public boolean exec(Player p, String[] args)
	{
		if (plugin.cPerms(p, "PlotMe.use.comments"))
		{
			if(!plugin.getPlotMeCoreManager().isPlotWorld(p))
			{
				p.sendMessage(RED + C("MsgNotPlotWorld"));
			}
			else
			{
				if(args.length < 2)
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
							
							if(plot.owner.equalsIgnoreCase(p.getName()) || plot.isAllowed(p.getName()) || plugin.cPerms(p, "PlotMe.admin"))
							{
								if(plot.comments.size() == 0)
								{
									p.sendMessage(C("MsgNoComments"));
								}
								else
								{
									p.sendMessage(C("MsgYouHave") + " " + 
											AQUA + plot.comments.size() + RESET + " " + C("MsgComments"));
									
									for(String[] comment : plot.comments)
									{
										p.sendMessage(AQUA + C("WordFrom") + " : " + RED + comment[0]);
										p.sendMessage(ITALIC + comment[1]);
									}
									
								}
							}
							else
							{
								p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgNotYoursNotAllowedViewComments"));
							}
						}
						else
						{
							p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgHasNoOwner"));
						}
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
