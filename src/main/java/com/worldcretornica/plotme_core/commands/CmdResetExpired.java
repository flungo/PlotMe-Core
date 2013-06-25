package com.worldcretornica.plotme_core.commands;

import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.PlotRunnableDeleteExpire;

public class CmdResetExpired extends PlotCommand 
{
	public CmdResetExpired(PlotMe_Core instance) {
		super(instance);
	}

	public boolean exec(CommandSender s, String[] args)
	{
		if(plugin.cPerms(s, "PlotMe.admin.resetexpired"))
		{
			if(args.length <= 1)
			{
				s.sendMessage(C("WordUsage") + ": " + RED + "/plotme " + C("CommandResetExpired") + " <" + C("WordWorld") + "> " + RESET + "Example: " + RED + "/plotme " + C("CommandResetExpired") + " plotworld ");
			}
			else
			{
				if(plugin.getWorldCurrentlyProcessingExpired() != null)
				{
					s.sendMessage(plugin.getCommandSenderCurrentlyProcessingExpired().getName() + " " + C("MsgAlreadyProcessingPlots"));
				}
				else
				{
					World w = s.getServer().getWorld(args[1]);
					
					if(w == null)
					{
						s.sendMessage(RED + C("WordWorld") + " '" + args[1] + "' " + C("MsgDoesNotExistOrNotLoaded"));
						return true;
					}
					else
					{					
						if(!plugin.getPlotMeCoreManager().isPlotWorld(w))
						{
							s.sendMessage(RED + C("MsgNotPlotWorld"));
							return true;
						}
						else
						{
							plugin.setWorldCurrentlyProcessingExpired(w);
							plugin.setCommandSenderCurrentlyProcessingExpired(s);
							plugin.setCounterExpired(50);
							plugin.setNbPerDeletionProcessingExpired(5);
							
							plugin.scheduleTask(new PlotRunnableDeleteExpire(plugin), 5, 50);
						}
					}
				}
			}
		}
		else
		{
			s.sendMessage(RED + C("MsgPermissionDenied"));
		}
		return true;
	}
}
