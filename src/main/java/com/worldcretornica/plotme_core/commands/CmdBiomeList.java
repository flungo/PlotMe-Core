package com.worldcretornica.plotme_core.commands;

import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.utils.MinecraftFontWidthCalculator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdBiomeList extends PlotCommand {

    public CmdBiomeList(PlotMe_Core instance) {
        super(instance);
    }

    public boolean exec(CommandSender s, String[] args) {
        if (!(s instanceof Player) || plugin.cPerms((Player) s, "PlotMe.use.biome")) {
            s.sendMessage(C("WordBiomes") + " : ");

            //int i = 0;
            StringBuilder line = new StringBuilder();
            List<String> biomes = new ArrayList<String>();

            for (Biome b : Biome.values()) {
                biomes.add(b.name());
            }

            Collections.sort(biomes);

            List<String> column1 = new ArrayList<String>();
            List<String> column2 = new ArrayList<String>();
            List<String> column3 = new ArrayList<String>();

            for (int ctr = 0; ctr < biomes.size(); ctr++) {
                if (ctr < biomes.size() / 3) {
                    column1.add(biomes.get(ctr));
                } else if (ctr < biomes.size() * 2 / 3) {
                    column2.add(biomes.get(ctr));
                } else {
                    column3.add(biomes.get(ctr));
                }
            }

            for (int ctr = 0; ctr < column1.size(); ctr++) {
                String b;
                int nameLength;

                b = Util().FormatBiome(column1.get(ctr));
                nameLength = MinecraftFontWidthCalculator.getStringWidth(b);
                line.append(b).append(Util().whitespace(432 - nameLength));

                if (ctr < column2.size()) {
                    b = Util().FormatBiome(column2.get(ctr));
                    nameLength = MinecraftFontWidthCalculator.getStringWidth(b);
                    line.append(b).append(Util().whitespace(432 - nameLength));
                }

                if (ctr < column3.size()) {
                    b = Util().FormatBiome(column3.get(ctr));
                    line.append(b);
                }

                s.sendMessage("" + AQUA + line);
                //i = 0;
                line = new StringBuilder();

                /*int nameLength = MinecraftFontWidthCalculator.getStringWidth(b);

                 i += 1;
                 if(i == 3)
                 {
                 line.append(b);
                 s.sendMessage("" + BLUE + line);
                 i = 0;
                 line = new StringBuilder();
                 }
                 else
                 {
                 line.append(b).append(whitespace(318 - nameLength));
                 }*/
            }
        } else {
            s.sendMessage(RED + C("MsgPermissionDenied"));
        }
        return true;
    }
}
