package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.BanID;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.TranslationUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.Map;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;


public class ReportCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.report"))
            return true;

        try 
        {
            // /report <player> <reason>
            if (args.length >= 2)
            {
                String username = args[0];
                String reason = Messages.ConcatenateRest(args, 1).trim();

                // They must have *something* in their report message.
                if (reason.isEmpty())
                {
                    sender.sendMessage(Messages.GetMessages().Translate("Report.ReasonRequired",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("prefix", Messages.Prefix);
                            put("player", username);
                        }}
                    ));
                    // SHow the syntax.
                    return false;
                }

                OfflinePlayer u = User.FindPlayerByBanID(username);
                if (u == null)
                    return User.NoSuchPlayer(sender, username, true);

                try
                {
                    // Make sure they're not already reported by this user.
                    if (!(sender instanceof ConsoleCommandSender))
                    {
                        PreparedStatement ps = self.connection.prepareStatement("SELECT 1 FROM Reports WHERE DefendantUUID = ? AND PlaintiffUUID = ? AND NOT Closed = True");
                        ps.setString(1, u.getUniqueId().toString());
                        ps.setString(2, ((Player)sender).getUniqueId().toString());

                        Future<Optional<ResultSet>> frs = DatabaseUtil.ExecuteLater(ps);
                        Optional<ResultSet> bnyeh = frs.get();

                        if (bnyeh.isPresent())
                        {
                            ResultSet res = bnyeh.get();
                            if (res.next())
                            {
                                sender.sendMessage(Messages.GetMessages().Translate("Report.TooManyTries",
                                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                                    {{
                                        put("prefix", Messages.Prefix);
                                        put("player", u.getName());
                                    }}
                                ));
                                return true;
                            }
                        }
                    }


                    int i = 1;
                    PreparedStatement ps = self.connection.prepareStatement("INSERT INTO Reports (PlaintiffUUID, PlaintiffName, DefendantUUID, DefendantName, Reason) VALUES (?, ?, ?, ?, ?)");
                    ps.setString(i++, sender instanceof ConsoleCommandSender ? "console" : ((Player)sender).getUniqueId().toString());
                    ps.setString(i++, sender instanceof ConsoleCommandSender ? "console" : ((Player)sender).getName());
                    ps.setString(i++, u.getUniqueId().toString());
                    ps.setString(i++, u.getName());
                    ps.setString(i++, reason);

                    DatabaseUtil.ExecuteUpdate(ps);

                    sender.sendMessage(Messages.GetMessages().Translate("Report.ReportSuccess",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("prefix", Messages.Prefix);
                            put("player", u.getName());
                            put("reason", reason);
                        }}
                    ));

                    String AnnounceMessage = Messages.GetMessages().Translate("Report.ReportAnnouncement",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("prefix", Messages.Prefix);
                            put("player", u.getName());
                            put("reporter", sender.getName());
                            put("reason", reason);
                        }}
                    );

                    self.getLogger().warning(AnnounceMessage);

                    for (Player p : Bukkit.getOnlinePlayers())
                        p.sendMessage(AnnounceMessage);

                    // TODO: Discord notifs, email notifs, whatever else notifs?

                    return true;
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                    sender.sendMessage(Messages.ServerError);
                    return true;
                }
            }
            else
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax.
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        
        return true;
    }
}