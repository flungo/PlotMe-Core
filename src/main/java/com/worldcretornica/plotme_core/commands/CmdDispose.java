package com.worldcretornica.plotme_core.commands;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.event.PlotDisposeEvent;
import com.worldcretornica.plotme_core.event.PlotMeEventFactory;

public class CmdDispose extends PlotCommand 
{
	public CmdDispose(PlotMe_Core instance) {
		super(instance);
	}

	public boolean exec(Player p, String[] args) 
	{
		if (plugin.cPerms(p, "PlotMe.admin.dispose") || plugin.cPerms(p, "PlotMe.use.dispose"))
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
							p.sendMessage(RED + C("MsgPlotProtectedNotDisposed"));
						}
						else
						{
							String name = p.getName();
							
							if(plot.owner.equalsIgnoreCase(name) || plugin.cPerms(p, "PlotMe.admin.dispose"))
							{
								PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(p);
								
								double cost = pmi.DisposePrice;
								
								World w = p.getWorld();
								
								PlotDisposeEvent event;
								
								if(plugin.getPlotMeCoreManager().isEconomyEnabled(p))
								{
									if(cost != 0 && plugin.getEconomy().getBalance(name) < cost)
									{
										p.sendMessage(RED + C("MsgNotEnoughDispose"));
										return true;
									}
									
									event = PlotMeEventFactory.callPlotDisposeEvent(plugin, w, plot, p);
									
									if(event.isCancelled())
									{
										return true;
									}
									else
									{
										EconomyResponse er = plugin.getEconomy().withdrawPlayer(name, cost);
										
										if(!er.transactionSuccess())
										{	
											p.sendMessage(RED + er.errorMessage);
											Util().warn(er.errorMessage);
											return true;
										}
									
										if(plot.auctionned)
										{
											String currentbidder = plot.currentbidder;
											
											if(!currentbidder.equals(""))
											{
												EconomyResponse er2 = plugin.getEconomy().depositPlayer(currentbidder, plot.currentbid);
												
												if(!er2.transactionSuccess())
												{
													p.sendMessage(RED + er2.errorMessage);
													Util().warn(er2.errorMessage);
												}
												else
												{
												    for(Player player : Bukkit.getServer().getOnlinePlayers())
												    {
												        if(player.getName().equalsIgnoreCase(currentbidder))
												        {
												            player.sendMessage(C("WordPlot") + 
												            		" " + id + " " + C("MsgOwnedBy") + " " + plot.owner + " " + C("MsgWasDisposed") + " " + Util().moneyFormat(cost));
												            break;
												        }
												    }
												}
											}
										}
									}
								}
								else
								{
									event = PlotMeEventFactory.callPlotDisposeEvent(plugin, w, plot, p);
								}
								
								if(!event.isCancelled())
								{
									if(!plugin.getPlotMeCoreManager().isPlotAvailable(id, p))
									{
										plugin.getPlotMeCoreManager().removePlot(w, id);
									}
									
									plugin.getPlotMeCoreManager().removeOwnerSign(w, id);
									plugin.getPlotMeCoreManager().removeSellSign(w, id);
									plugin.getPlotMeCoreManager().removeAuctionSign(w, id);
									
									plugin.getSqlManager().deletePlot(plugin.getPlotMeCoreManager().getIdX(id), plugin.getPlotMeCoreManager().getIdZ(id), w.getName().toLowerCase());
									
									p.sendMessage(C("MsgPlotDisposedAnyoneClaim"));
									
									if(isAdv)
										plugin.getLogger().info(LOG + name + " " + C("MsgDisposedPlot") + " " + id);
								}
							}
							else
							{
								p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgNotYoursCannotDispose"));
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
