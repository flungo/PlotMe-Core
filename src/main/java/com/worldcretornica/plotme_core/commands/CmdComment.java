package com.worldcretornica.plotme_core.commands;

import net.milkbowl.vault.economy.EconomyResponse;

import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;

public class CmdComment extends PlotCommand 
{
	public CmdComment(PlotMe_Core instance) {
		super(instance);
	}

	public boolean exec(Player p, String[] args)
	{
		if (plugin.cPerms(p, "PlotMe.use.comment"))
		{
			if(!plugin.getPlotMeCoreManager().isPlotWorld(p))
			{
				p.sendMessage(RED + C("MsgNotPlotWorld"));
			}
			else
			{
				if(args.length < 2)
				{
					p.sendMessage(C("WordUsage") + ": " + RED + "/plotme " + C("CommandComment") + " <" + C("WordText") + ">");
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
							World w = p.getWorld();
							PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(w);
							String playername = p.getName();
							
							double price = 0;
							
							if(plugin.getPlotMeCoreManager().isEconomyEnabled(w))
							{
								price = pmi.AddCommentPrice;
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
									p.sendMessage(RED + C("MsgNotEnoughComment") + " " + C("WordMissing") + " " + RESET + Util().moneyFormat(price - balance, false));
									return true;
								}
							}
							
							Plot plot = plugin.getPlotMeCoreManager().getPlotById(p, id);
							
							String text = StringUtils.join(args," ");
							text = text.substring(text.indexOf(" "));
							
							String[] comment = new String[2];
							comment[0] = playername;
							comment[1] = text;
							
							plot.comments.add(comment);
							plugin.getSqlManager().addPlotComment(comment, plot.comments.size(), plugin.getPlotMeCoreManager().getIdX(id), plugin.getPlotMeCoreManager().getIdZ(id), plot.world);
							
							p.sendMessage(C("MsgCommentAdded") + " " + Util().moneyFormat(-price));
							
							if(isAdv)
								plugin.getLogger().info(LOG + playername + " " + C("MsgCommentedPlot") + " " + id + ((price != 0) ? " " + C("WordFor") + " " + price : ""));
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
