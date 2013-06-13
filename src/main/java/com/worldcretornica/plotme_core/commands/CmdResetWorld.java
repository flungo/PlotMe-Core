package com.worldcretornica.plotme_core.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.utils.Util;

public class CmdResetWorld extends PlotCommand 
{
	public boolean exec(CommandSender s, String[] args)
	{
		if (!(s instanceof Player) || PlotMe_Core.cPerms((Player) s, "PlotMe.admin.resetworld"))
		{
			//TODO
			
			if(isAdv)
				PlotMe_Core.self.getLogger().info(LOG + s.getName() + " " + Util.C("MsgReloadedConfigurations"));
		}
		else
		{
			Util.Send(s, RED + Util.C("MsgPermissionDenied"));
		}
		return true;
	}
}
