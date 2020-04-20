package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.Optional;
import java.util.TreeMap;

public class UnbanCommand extends RistExCommand
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Unban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.unban"))
            return true;

        if (args.length < 2)
            return false;
        
        // Syntax: /unban [-s] <PlayerName|PunishID> <Reason>
        try 
        {
            boolean silent = args.length > 3 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
            String reason = Messages.ConcatenateRest(args, silent ? 2 : 1).trim();
            OfflinePlayer target = User.FindPlayerByAny(args[0]);
            
            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);
            
            if (!User.IsPlayerBanned(target))
                return User.PlayerOnlyVariableMessage("Ban.PlayerIsNotBanned", sender, target.getName(), true);

            // Preapre a statement
            // We need to get the latest banid first.
            Optional<Punishment> op = Punishment.FindPunishment(PunishmentType.PUNISH_BAN, target, false);

            if (!op.isPresent())
            {
                sender.sendMessage("Congratulations!! You've found a bug!! Please report it to the lolbans developers to get it fixed! :D");
                return true;
            }

            Punishment punish = op.get();
            punish.SetAppealReason(reason);
            punish.SetAppealed(true);
            punish.SetAppealStaff(sender instanceof OfflinePlayer ? (OfflinePlayer)sender : null);
            punish.Commit(sender);

            // Prepare our announce message
            String AnnounceMessage = Messages.Translate(silent ? "Ban.SilentUnbanAnnouncment" : "Ban.UnbanAnnouncment",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("arbiter", sender.getName());
                    put("punishid", punish.GetPunishmentID());
                }}
            );

            // Log to console.
            self.getLogger().info(AnnounceMessage);

            // Post that to the database.
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (!silent && (p.hasPermission("lolbans.alerts") || p.isOp()))
                    p.sendMessage(AnnounceMessage);
            }

            if (DiscordUtil.UseSimplifiedMessage == true)
            {
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentUnban" : "Discord.SimpMessageUnban",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", reason);
                        put("arbiter", sender.getName());
                        put("punishid", punish.GetPunishmentID());
                    }}
                ));
            }
            else
                DiscordUtil.SendDiscord(punish, silent);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}