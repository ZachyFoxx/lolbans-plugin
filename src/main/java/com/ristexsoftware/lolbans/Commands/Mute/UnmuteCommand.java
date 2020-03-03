package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TranslationUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UnmuteCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.unmute"))
            return true;

        try 
        {
            // just incase someone, magically has a 1 char name........
            if (!(args.length < 2 || args == null))
            {
                String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length )) : args[1];
                reason = reason.replace(",", "").trim();
                // Because dumbfuck java and it's "ItS nOt FiNaL"
                OfflinePlayer target = User.FindPlayerByBanID(args[0]);
                String euuid = null;

                if (sender instanceof ConsoleCommandSender)
                    euuid = "console";

                else if (sender instanceof Player)
                    euuid = ((Player) sender).getUniqueId().toString();
                
                if (target == null)
                    return User.NoSuchPlayer(sender, args[0], true);
                
                if (!User.IsPlayerMuted(target))
                    return User.PlayerOnlyVariableMessage("Mute.PlayerIsNotMuted", sender, target.getName(), true);
                
                // Prepare our reason for unbanning
                boolean silent = args.length > 2 ? args[1].equalsIgnoreCase("-s") : false;

                final String FuckingJava = new String(reason);
                
                // Preapre a statement
                // We need to get the latest banid first.
                PreparedStatement pst3 = self.connection.prepareStatement("SELECT PunishID FROM MutedPlayers WHERE UUID = ?");
                pst3.setString(1, target.getUniqueId().toString());

                ResultSet result = pst3.executeQuery();
                result.next();
                String MuteID = result.getString("PunishID");

                Timestamp timeremoved = new Timestamp(TimeUtil.GetUnixTime() * 1000L);

                // Run the async task for the database
                Future<Boolean> UnMute = DatabaseUtil.UnMute(target.getUniqueId().toString(), target.getName(), reason, sender, euuid, timeremoved);

                // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String BanID, Timestamp BanTime)
                if (!UnMute.get())
                {
                    sender.sendMessage(Messages.ServerError);
                    return true;
                }

                // Post that to the database.
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                        continue;

                    //"&c%banner% &7has banned &c%player%&7: &c%reason%"
            
                    String UnbanAnnouncementMessage = Messages.GetMessages().Translate(silent ? "Mute.SilentUnmuteAnnouncment" : "Mute.UnmuteAnnouncment",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("prefix", Messages.Prefix);
                            put("player", target.getName());
                            put("reason", FuckingJava);
                            put("unmuter", sender.getName());
                            put("muteid", MuteID);
                        }}
                    );

                    p.sendMessage(UnbanAnnouncementMessage);
                }

                String YouWereUnMuted = Messages.GetMessages().Translate("Mute.YouWereUnMuted",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("prefix", Messages.Prefix);
                    put("player", target.getName());
                    put("reason", FuckingJava);
                    put("unmuter", sender.getName());
                    put("muteid", MuteID);
                }}
                );
                
                if (target instanceof Player && target.isOnline())
                {
                    Player target2 = (Player) target;
                    target2.sendMessage(YouWereUnMuted);
                }
                else if (target instanceof ConsoleCommandSender)
                {
                    // You cannot mute console.
                    sender.sendMessage(Messages.GetMessages().GetConfig().getString("Mute.CannotMuteConsole"));
                    return true;
                }

                /* 
                // Send to Discord.
                if (sender instanceof ConsoleCommandSender)
                    DiscordUtil.SendUnban(sender.getName().toString(), target.getName(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", target.getUniqueId().toString(), reason, BanID, silent);
                else
                    DiscordUtil.SendUnban(sender.getName().toString(), target.getName(), ((OfflinePlayer) sender).getUniqueId().toString(), target.getUniqueId().toString(), reason, BanID, silent);
                */ 

                // ":hammer: **{BANNER}** un-banned **{PLAYER}** for **{REASON}** *[SILENT] {BANID}*"
                // Send to Discord. (New method)
                //String SimplifiedMessageSilentUnban = DiscordUtil.SimplifiedMessageSilentUnban;
                //String SimplifiedMessageUnban = DiscordUtil.SimplifiedMessageUnban;

                if (DiscordUtil.UseSimplifiedMessage == true)
                {
                    String SimplifiedMessageUnban = Messages.GetMessages().Translate(silent ? "Discord.SimpMessageSilentUnmute" : "Discord.SimpMessageUnmute",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", target.getName());
                            put("reason", FuckingJava);
                            put("banner", sender.getName());
                            put("muteid", MuteID);
                        }}
                    );

                    DiscordUtil.SendFormatted(SimplifiedMessageUnban);
                    return true;
                }
                else
                {
                    DiscordUtil.SendUnmute(sender.getName().toString(), target.getName(),
                            // if they're the console, use a hard-defined UUID instead of the player's UUID.
                            (sender instanceof ConsoleCommandSender) ? "f78a4d8d-d51b-4b39-98a3-230f2de0c670" : ((OfflinePlayer) sender).getUniqueId().toString(),
                            target.getUniqueId().toString(), reason, MuteID, silent);
                    return true;
                }
            }

            else
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax.
            }
        }
        catch (SQLException | InvalidConfigurationException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }
}