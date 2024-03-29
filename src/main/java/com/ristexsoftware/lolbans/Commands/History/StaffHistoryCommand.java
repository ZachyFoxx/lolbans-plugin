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

package com.ristexsoftware.lolbans.Commands.History;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.NumberUtil;
import com.ristexsoftware.lolbans.Utils.Paginator;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.sql.*;

public class StaffHistoryCommand extends RistExCommand
{
    private Main self = (Main)this.getPlugin();

    public StaffHistoryCommand(Plugin owner)
    {
        super("staffhistory", owner);
        this.setDescription("Show history staff member's punishment actions");
        this.setPermission("lolbans.staffhistory");
    }

    private String GodForbidJava8HasUsableLambdaExpressionsSoICanAvoidDefiningSuperflouosFunctionsLikeThisOne(PunishmentType Type, Timestamp ts)
    {
        if (Type == PunishmentType.PUNISH_BAN)
        {
            // Check if ts is null.
            if (ts == null)
                return "Permanent Ban";
            else 
                return "Temporary Ban";
        }
        else 
            return Type.DisplayName();
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.StaffHistory", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args) 
    {
        if (!PermissionUtil.Check(sender, "lolbans.staffhistory"))
            return true;

        // Command runs as /staffhistory <staffmember>

        if (args.length < 1)
        {
            sender.sendMessage(Messages.InvalidSyntax);
            return false;
        }

        try 
        {
            // FIXME: What if args[0] == "Console"
            OfflinePlayer target = User.FindPlayerByAny(args[0]);
            if (target == null)
                return User.NoSuchPlayer(sender, args[0], true);

            // Preapre a statement
            // TODO: What about IP bans?
            PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE ArbiterUUID = ?");
            pst.setString(1, target.getUniqueId().toString());

            ResultSet result = pst.executeQuery();
            if (!result.next() || result.wasNull())
                return User.PlayerOnlyVariableMessage("History.NoHistory", sender, args[0], true);


            // The page to use.
            int pageno = args.length > 0 ? (args.length > 1 ? (NumberUtil.isInteger(args[1]) ? Integer.valueOf(args[1]) : 0) : NumberUtil.isInteger(args[0]) ? Integer.valueOf(args[0]) : 1 ) : 1;
            if (pageno == 0)
                return false;

            // We use a do-while loop because we already checked if there was a result above.
            // NYE!... nye..... bye....go to bed bum or what I'll disconnect your inet bye
            List<String> pageditems = new ArrayList<String>();
            do 
            {
                // First, we have to calculate our punishment type.
                PunishmentType Type = PunishmentType.FromOrdinal(result.getInt("Type"));
                Timestamp ts = result.getTimestamp("TimePunished");

                pageditems.add(Messages.Translate(ts == null ? "History.StaffHistoryMessagePerm" : "History.StaffHistoryMessageTemp", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("playername", result.getString("PlayerName"));
                        put("punishid", result.getString("PunishID"));
                        put("reason", result.getString("Reason"));
                        put("arbiter", result.getString("ArbiterName"));
                        put("type", GodForbidJava8HasUsableLambdaExpressionsSoICanAvoidDefiningSuperflouosFunctionsLikeThisOne(Type, ts));
                        put("expiry", ts.toString());
                        // TODO: Add more variables for people who want more info?
                    }}
                ));
            }
            while(result.next());

            // This is several rendered things in one string
            // Minecraft's short window (when chat is closed) can hold 10 lines
            // their extended window can hold 20 lines
            Paginator<String> page = new Paginator<String>(pageditems, Messages.GetMessages().GetConfig().getInt("History.StaffPageSize", 2));

            for (Object str : page.GetPage(pageno))
                sender.sendMessage((String)str);

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

            return true;
        }
        catch (SQLException | InvalidConfigurationException ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }

}