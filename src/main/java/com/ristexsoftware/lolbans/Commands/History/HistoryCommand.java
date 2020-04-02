package com.ristexsoftware.lolbans.Commands.History;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.Paginator;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.sql.*;

public class HistoryCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

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

    private boolean HandleHistory(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.history"))
            return User.PermissionDenied(sender, "lolbans.history");

        if (args.length < 1)
            return false; // Show syntax.

        try
        {
            OfflinePlayer target = User.FindPlayerByAny(args[0]);

            if (target == null)
                return User.NoSuchPlayer(sender, args[0], true);

            // Preapre a statement
            PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM Punishments WHERE UUID = ?");
            pst.setString(1, target.getUniqueId().toString());

            ResultSet result = pst.executeQuery();
            if (!result.next() || result.wasNull())
                return User.PlayerOnlyVariableMessage("History.NoHistory", sender, args[0], true);


            // The page to use.
            // TODO: WHat if args[1] is a string not an int?
            int pageno = args.length > 1 ? Integer.valueOf(args[1]) : 1;
            
            // We use a do-while loop because we already checked if there was a result above.
            List<String> pageditems = new ArrayList<String>();
            do 
            {
                // First, we have to calculate our punishment type.
                PunishmentType Type = PunishmentType.FromOrdinal(result.getInt("Type"));
                Timestamp ts = result.getTimestamp("Expiry");

                pageditems.add(Messages.Translate(ts == null ? "History.HistoryMessagePerm" : "History.HistoryMessageTemp", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("playername", result.getString("PlayerName"));
                        put("punishid", result.getString("PunishID"));
                        put("reason", result.getString("Reason"));
                        put("moderator", result.getString("ExecutionerName"));
                        put("type", GodForbidJava8HasUsableLambdaExpressionsSoICanAvoidDefiningSuperflouosFunctionsLikeThisOne(Type, ts));
                        put("punishdate", TimeUtil.TimeString(result.getTimestamp("TimePunished")));
                        put("expirydate", TimeUtil.TimeString(ts));
                        put("expiryduration", TimeUtil.Expires(ts));
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

            //if (sender instanceof Player)
            //    ((Player)sender).sendRawMessage("{\"text\":\"" + Message + "\"}");
            //else
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
    
    private boolean HandleClearHistory(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.clearhistory"))
            return User.PermissionDenied(sender, "lolbans.ban");

        if (args.length < 1)
            return false;
            
        try 
        {
            OfflinePlayer target = User.FindPlayerByAny(args[0]);

            if (target == null)
                return User.NoSuchPlayer(sender, args[0], true);

            // Also unban the user (as they no longer have any history)
            PreparedStatement pst2 = self.connection.prepareStatement("DELETE FROM Punishments WHERE UUID = ? AND AppealStaff != NULL AND WarningAck != NULL");
            pst2.setString(1, target.getUniqueId().toString());
            DatabaseUtil.ExecuteUpdate(pst2);

            // Send response.
            User.PlayerOnlyVariableMessage("History.ClearedHistory", sender, target.getName(), false);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // Handle the History command
        if (Messages.CompareMany(command.getName(), new String[]{"history", "h"}))
            return this.HandleHistory(sender, command, label, args);

        // Handle the clear history
        if (Messages.CompareMany(command.getName(), new String[]{"clearhistory", "ch"}))
            return this.HandleClearHistory(sender, command, label, args);
            
        // Invalid command.
        return false;
    }
}