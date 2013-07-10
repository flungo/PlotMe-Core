package com.worldcretornica.plotme_core.commands;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.ClearReason;
import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.event.PlotClearEvent;
import com.worldcretornica.plotme_core.event.PlotMeEventFactory;

public class CmdClear extends PlotCommand 
{
	public CmdClear(PlotMe_Core instance) {
		super(instance);
	}

	public boolean exec(Player p, String[] args)
	{
		if (plugin.cPerms(p, "PlotMe.admin.clear") || plugin.cPerms(p, "PlotMe.use.clear"))
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
						Plot plot = plugin.getPlotMeCoreManager().getPlotById(p,id);
						
						if(plot.protect)
						{
							p.sendMessage(RED + C("MsgPlotProtectedCannotClear"));
						}
						else
						{
							String playername = p.getName();
							
							if(plot.owner.equalsIgnoreCase(playername) || plugin.cPerms(p, "PlotMe.admin.clear"))
							{
								World w = p.getWorld();
								
								PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(w);
								
								double price = 0;
								
								PlotClearEvent event;
								
								if(plugin.getPlotMeCoreManager().isEconomyEnabled(w))
								{
									price = pmi.ClearPrice;
									double balance = plugin.getEconomy().getBalance(playername);
									
									if(balance >= price)
									{
										event = PlotMeEventFactory.callPlotClearEvent(plugin, w, plot, p);
										
										if(event.isCancelled())
										{
											return true;
										}
										else
										{
											EconomyResponse er = plugin.getEconomy().withdrawPlayer(playername, price);
											
											if(!er.transactionSuccess())
											{
												p.sendMessage(RED + er.errorMessage);
												Util().warn(er.errorMessage);
												return true;
											}
										}
									}
									else
									{
										p.sendMessage(RED + C("MsgNotEnoughClear") + " " + C("WordMissing") + " " + RESET + (price - balance) + RED + " " + plugin.getEconomy().currencyNamePlural());
										return true;
									}
								}
								else
								{
									event = PlotMeEventFactory.callPlotClearEvent(plugin, w, plot, p);
								}
								
								if(!event.isCancelled())
								{
									plugin.getPlotMeCoreManager().clear(w, plot, p, ClearReason.Clear);
									//RemoveLWC(w, plot, p);
									//plugin.getPlotMeCoreManager().regen(w, plot);
									
									//p.sendMessage(C("MsgPlotCleared") + " " + Util.moneyFormat(-price));
									
									if(isAdv)
										plugin.getLogger().info(LOG + playername + " " + C("MsgClearedPlot") + " " + id + ((price != 0) ? " " + C("WordFor") + " " + price : ""));
								}
							}
							else
							{
								p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgNotYoursNotAllowedClear"));
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
