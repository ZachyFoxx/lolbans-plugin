/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */

package com.ristexsoftware.lolbans.Commands.Report;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.NumberUtil;
import com.ristexsoftware.lolbans.Utils.Paginator;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.List;
import java.sql.*;

public class ReportHistoryCommand extends RistExCommand
{
    private Main self = (Main)this.getPlugin();

    public ReportHistoryCommand(Plugin owner)
    {
        super("reports", owner);
        this.setDescription("Get latest reports for a specific player or everyone");
        this.setPermission("lolbans.report.history");
        this.setAliases(Arrays.asList(new String[]{"rps"}));
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.ReportHistory", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }
    
    // reports [PlayerName] [<page>]
    @Override
    public boolean Execute(CommandSender sender, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.report.history"))
            return User.PermissionDenied(sender, "lolbans.report.history");

        try
        {
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalString("PlayerOrPage", 0);
            a.OptionalString("Page", 1);
            
            // There are two ways this command can work, it can either specify a player, or show all reports.
            PreparedStatement pst = null;
            if (args.length < 1 || args.length > 0 && NumberUtil.isInteger(a.get("PlayerOrPage")))
                pst = self.connection.prepareStatement("SELECT * FROM lolbans_reports ORDER BY Closed, TimeAdded");
            if (args.length > 0 && a.get("PlayerOrPage").length() > 2 && !NumberUtil.isInteger(a.get("PlayerOrPage")))
            {
                OfflinePlayer target = User.FindPlayerByAny(a.get("PlayerOrPage"));
                pst = self.connection.prepareStatement("SELECT * FROM lolbans_reports WHERE DefendantUUID = ?");
                pst.setString(1, target.getUniqueId().toString());
            }

            ResultSet result = pst.executeQuery();
            if (!result.next() || result.wasNull())
                return User.PlayerOnlyVariableMessage("History.NoHistory2", sender, sender.getName(), true);

            // The page to use.
            // I spent a while figuring out the logic to this, and now it Just Works:tm:
            // This is dumb and I don't know if i want to keep this around.
            int pageno = args.length > 0 ? (args.length > 1 ? (NumberUtil.isInteger(a.get("Page")) ? Integer.valueOf(a.get("Page")) : 0) : NumberUtil.isInteger(a.get("PlayerOrPage")) ? Integer.valueOf(a.get("PlayerOrPage")) : 1 ) : 1;
            if (pageno == 0)
                return false;

            // Integer.valueOf(a.get("Page")) : 1
            
            // We use a do-while loop because we already checked if there was a result above.
            List<String> pageditems = new ArrayList<String>();
            do 
            {
                // First, we have to calculate our punishment type.
                //PunishmentType Type = PunishmentType.FromOrdinal(result.getInt("Type"));
                //Timestamp ts = result.getTimestamp("Expiry");

                pageditems.add(Messages.Translate("History.HistoryMessageReport", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("playername", result.getString("DefendantName"));
                        put("reason", result.getString("Reason"));
                        put("arbiter", result.getString("PlaintiffName"));
                        put("date", result.getTimestamp("TimeAdded").toString());
                        // TODO: Add more variables for people who want more info?
                    }}
                ));
            }
            while(result.next());
            
            
            // This is several rendered things in one string
            // Minecraft's short window (when chat is closed) can hold 10 lines
            // their extended window can hold 20 lines
            Paginator<String> page = new Paginator<String>(pageditems, Messages.GetMessages().GetConfig().getInt("History.PageSize", 2));

            // Minecraft trims whitespace which can cause formatting issues
            // To avoid this, we have to send everything as one big message.
            String Message = "";
            for (Object str : page.GetPage(pageno))
                Message += (String)str;

            sender.sendMessage(Message);

            // Check if the paginator needs the page text or not.
            if (page.GetTotalPages() > 1)
            {
                sender.sendMessage(Messages.Translate("History.Paginator",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("current", String.valueOf(page.GetCurrent()));
                        put("total", String.valueOf(page.GetTotalPages()));
                    }}
                ));
            }
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}
