package com.worldcretornica.plotme_core.commands;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.event.PlotAddAllowedEvent;
import com.worldcretornica.plotme_core.event.PlotMeEventFactory;

public class CmdAdd extends PlotCommand
{
	public CmdAdd(PlotMe_Core instance) 
	{
		super(instance);
	}

	public boolean exec(Player p, String[] args)
	{
		if (plugin.cPerms(p, "PlotMe.admin.add") || plugin.cPerms(p, "PlotMe.use.add"))
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
							p.sendMessage(C("WordUsage") + " " + RED + "/plotme " + C("CommandAdd") + " <" + C("WordPlayer") + ">");
						}
						else
						{
						
							Plot plot = plugin.getPlotMeCoreManager().getPlotById(p,id);
							String playername = p.getName();
							String allowed = args[1];
							
							if(plot.owner.equalsIgnoreCase(playername) || plugin.cPerms(p, "PlotMe.admin.add"))
							{
								if(plot.isAllowed(allowed))
								{
									p.sendMessage(C("WordPlayer") + " " + RED + args[1] + RESET + " " + C("MsgAlreadyAllowed"));
								}
								else
								{
									World w = p.getWorld();
									
									PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(w);
									
									double price = 0;
									
									PlotAddAllowedEvent event = PlotMeEventFactory.callPlotAddAllowedEvent(plugin, w, plot, p, allowed);
									
									if(!event.isCancelled())
									{
										if(plugin.getPlotMeCoreManager().isEconomyEnabled(w))
										{
											price = pmi.AddPlayerPrice;
											double balance = plugin.getEconomy().getBalance(playername);
											
											if(balance >= price)
											{
												EconomyResponse er = plugin.getEconomy().withdrawPlayer(playername, price);
												
												if(!er.transactionSuccess())
												{
													p.sendMessage(RED + er.errorMessage);
													plugin.getUtil().warn(er.errorMessage);
													return true;
												}
											}
											else
											{
												p.sendMessage(RED + C("MsgNotEnoughAdd") + " " + C("WordMissing") + " " + RESET + Util().moneyFormat(price - balance, false));
												return true;
											}
										}
										
										plot.addAllowed(args[1]);
										
										p.sendMessage(C("WordPlayer") + " " + RED + allowed + RESET + " " + C("MsgNowAllowed") + " " + Util().moneyFormat(-price));
										
										if(isAdv)
										{
											plugin.getLogger().info(LOG + playername + " " + C("MsgAddedPlayer") + " " + allowed + " " + C("MsgToPlot") + " " + 
													id + ((price != 0) ? " " + C("WordFor") + " " + price : ""));
										}
									}
								}
							}
							else
							{
								p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgNotYoursNotAllowedAdd"));
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
