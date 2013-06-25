package com.worldcretornica.plotme_core.commands;

import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;

public class CmdDeny extends PlotCommand 
{
	public CmdDeny(PlotMe_Core instance) {
		super(instance);
	}

	public boolean exec(Player p, String[] args)
	{
		if (plugin.cPerms(p, "PlotMe.admin.deny") || plugin.cPerms(p, "PlotMe.use.deny"))
		{
			if(!plugin.getPlotMeCoreManager().isPlotWorld(p))
			{
				p.sendMessage(RED + C("MsgNotPlotWorld"));
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
						if(args.length < 2 || args[1].equals(""))
						{
							p.sendMessage(C("WordUsage") + " " + RED + "/plotme " + C("CommandDeny") + " <" + C("WordPlayer") + ">");
						}
						else
						{
						
							Plot plot = plugin.getPlotMeCoreManager().getPlotById(p,id);
							String playername = p.getName();
							String denied = args[1];
							
							if(plot.owner.equalsIgnoreCase(playername) || plugin.cPerms(p, "PlotMe.admin.deny"))
							{
								if(plot.isDenied(denied))
								{
									p.sendMessage(C("WordPlayer") + " " + RED + args[1] + RESET + " " + C("MsgAlreadyDenied"));
								}
								else
								{
									World w = p.getWorld();
									
									PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(w);
									
									double price = 0;
									
									if(plugin.getPlotMeCoreManager().isEconomyEnabled(w))
									{
										price = pmi.DenyPlayerPrice;
										double balance = plugin.getEconomy().getBalance(playername);
										
										if(balance >= price)
										{
											EconomyResponse er = plugin.getEconomy().withdrawPlayer(playername, price);
											
											if(!er.transactionSuccess())
											{
												p.sendMessage(RED + er.errorMessage);
												Util().warn(er.errorMessage);
												return true;
											}
										}
										else
										{
											p.sendMessage(RED + C("MsgNotEnoughDeny") + " " + C("WordMissing") + " " + RESET + Util().moneyFormat(price - balance, false));
											return true;
										}
									}
									
									plot.addDenied(args[1]);
									
									if(denied.equals("*"))
									{
										List<Player> deniedplayers = plugin.getPlotMeCoreManager().getPlayersInPlot(w, id);
										
										for(Player deniedplayer : deniedplayers)
										{
											if(!plot.isAllowed(deniedplayer.getName()))
												deniedplayer.teleport(plugin.getPlotMeCoreManager().getPlotHome(w, plot.id));
										}
									}
									else
									{
										Player deniedplayer = Bukkit.getServer().getPlayer(denied);
										
										if(deniedplayer != null)
										{
											if(deniedplayer.getWorld().equals(w))
											{
												String deniedid = plugin.getPlotMeCoreManager().getPlotId(deniedplayer.getLocation());
												
												if(deniedid.equalsIgnoreCase(id))
												{
													deniedplayer.teleport(plugin.getPlotMeCoreManager().getPlotHome(w, plot.id));
												}
											}
										}
									}
									
									p.sendMessage(C("WordPlayer") + " " + RED + denied + RESET + " " + C("MsgNowDenied") + " " + Util().moneyFormat(-price));
									
									if(isAdv)
										plugin.getLogger().info(LOG + playername + " " + C("MsgDeniedPlayer") + " " + denied + " " + C("MsgToPlot") + " " + id + ((price != 0) ? " " + C("WordFor") + " " + price : ""));
								}
							}
							else
							{
								p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgNotYoursNotAllowedDeny"));
							}
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
