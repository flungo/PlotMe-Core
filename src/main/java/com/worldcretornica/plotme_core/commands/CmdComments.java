package com.worldcretornica.plotme_core.commands;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMe_Core;
import org.bukkit.entity.Player;

public class CmdComments extends PlotCommand {

    public CmdComments(PlotMe_Core instance) {
        super(instance);
    }

    public boolean exec(Player p, String[] args) {
        if (plugin.cPerms(p, "PlotMe.use.comments")) {
            if (!plugin.getPlotMeCoreManager().isPlotWorld(p)) {
                p.sendMessage(RED + C("MsgNotPlotWorld"));
            } else {
                if (args.length < 2) {
                    String id = plugin.getPlotMeCoreManager().getPlotId(p.getLocation());

                    if (id.equals("")) {
                        p.sendMessage(RED + C("MsgNoPlotFound"));
                    } else {
                        if (!plugin.getPlotMeCoreManager().isPlotAvailable(id, p)) {
                            Plot plot = plugin.getPlotMeCoreManager().getPlotById(p, id);

                            if (plot.getOwner().equalsIgnoreCase(p.getName()) || plot.isAllowed(p.getName()) || plugin.cPerms(p, "PlotMe.admin")) {
                                if (plot.getCommentsCount() == 0) {
                                    p.sendMessage(C("MsgNoComments"));
                                } else {
                                    p.sendMessage(C("MsgYouHave") + " "
                                            + AQUA + plot.getCommentsCount() + RESET + " " + C("MsgComments"));

                                    for (String[] comment : plot.getComments()) {
                                        p.sendMessage(AQUA + C("WordFrom") + " : " + RED + comment[0]);
                                        p.sendMessage(ITALIC + comment[1]);
                                    }

                                }
                            } else {
                                p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgNotYoursNotAllowedViewComments"));
                            }
                        } else {
                            p.sendMessage(RED + C("MsgThisPlot") + "(" + id + ") " + C("MsgHasNoOwner"));
                        }
                    }
                }
            }
        } else {
            p.sendMessage(RED + C("MsgPermissionDenied"));
        }
        return true;
    }
}
