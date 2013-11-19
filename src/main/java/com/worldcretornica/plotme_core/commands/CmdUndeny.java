package com.worldcretornica.plotme_core.commands;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CmdUndeny extends PlotCommand {

    public CmdUndeny(PlotMe_Core instance) {
        super(instance);
    }

    public boolean exec(Player p, String[] args) {
        if (plugin.cPerms(p, "PlotMe.admin.undeny") || plugin.cPerms(p, "PlotMe.use.undeny")) {
            if (!plugin.getPlotMeCoreManager().isPlotWorld(p)) {
                p.sendMessage(RED + C("MsgNotPlotWorld"));
            } else {
                String id = plugin.getPlotMeCoreManager().getPlotId(p.getLocation());
                if (id.equals("")) {
                    p.sendMessage(RED + C("MsgNoPlotFound"));
                } else {
                    if (!plugin.getPlotMeCoreManager().isPlotAvailable(id, p)) {
                        if (args.length < 2 || args[1].equals("")) {
                            p.sendMessage(C("WordUsage") + ": " + RED + "/plotme " + C("CommandUndeny") + " <" + C("WordPlayer") + ">");
                        } else {
                            Plot plot = plugin.getPlotMeCoreManager().getPlotById(p, id);
                            String playername = p.getName();
                            String denied = args[1];

                            if (plot.getOwner().equalsIgnoreCase(playername) || plugin.cPerms(p, "PlotMe.admin.undeny")) {
                                if (plot.isDenied(denied)) {

                                    World w = p.getWorld();

                                    PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(w);

                                    double price = 0;
                                    //TODO event
                                    if (plugin.getPlotMeCoreManager().isEconomyEnabled(w)) {
                                        price = pmi.getUndenyPlayerPrice();
                                        double balance = plugin.getEconomy().getBalance(playername);

                                        if (balance >= price) {
                                            EconomyResponse er = plugin.getEconomy().withdrawPlayer(playername, price);

                                            if (!er.transactionSuccess()) {
                                                p.sendMessage(RED + er.errorMessage);
                                                Util().warn(er.errorMessage);
                                                return true;
                                            }
                                        } else {
                                            p.sendMessage(RED + C("MsgNotEnoughUndeny") + " " + C("WordMissing") + " " + RESET + Util().moneyFormat(price - balance, false));
                                            return true;
                                        }
                                    }

                                    plot.removeDenied(denied);

                                    p.sendMessage(C("WordPlayer") + " " + RED + denied + RESET + " " + C("MsgNowUndenied") + " " + Util().moneyFormat(-price));

                                    if (isAdv) {
                                        plugin.getLogger().info(LOG + playername + " " + C("MsgUndeniedPlayer") + " " + denied + " " + C("MsgFromPlot") + " " + id + ((price != 0) ? " " + C("WordFor") + " " + price : ""));
                                    }
                                } else {
                                    p.sendMessage(C("WordPlayer") + " " + RED + args[1] + RESET + " " + C("MsgWasNotDenied"));
                                }
                            } else {
                                p.sendMessage(RED + C("MsgThisPlot") + " (" + id + ") " + C("MsgNotYoursNotAllowedUndeny"));
                            }
                        }
                    } else {
                        p.sendMessage(RED + C("MsgThisPlot") + " (" + id + ") " + C("MsgHasNoOwner"));
                    }
                }
            }
        } else {
            p.sendMessage(RED + C("MsgPermissionDenied"));
        }
        return true;
    }
}
