package com.ristexsoftware.lolbans.Commands.Warn;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.TreeMap;
import java.util.Map;
import java.util.Optional;


public class UnWarnCommand extends RistExCommand
{
    public UnWarnCommand(Plugin owner)
    {
        super("unwarn", owner);
        this.setDescription("Remove a warning previously issued to a player");
        this.setPermission("lolbans.unwarn");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.UnWarn", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.unwarn"))
            return User.PermissionDenied(sender, "lolbans.unwarn");

        // /unwarn [-s] <PlayerName|PunishID>
        if (args.length < 2)
            return false;

        try 
        {
            boolean silent = args.length > 1 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

			Optional<Punishment> opunish = Punishment.FindPunishment(PunishmentType.PUNISH_WARN, target, false);
			if (!opunish.isPresent())
			{
				sender.sendMessage("Congratulations!! You've found a bug!! Please report it to the lolbans developers to get it fixed! :D");
                return true;
			}
			Punishment punish = opunish.get();

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("punishid", punish.GetPunishmentID());
                put("arbiter", sender.getName());
                put("silent", Boolean.toString(silent));
			}};
			
			// We don't track punishments that were removed so we just delete them.
			punish.Delete();
                
            // If they're online, require acknowledgement immediately by freezing them and sending a message.
            if (target.isOnline())
            {
                User u = Main.USERS.get(target.getUniqueId());
                u.SetWarned(false, null, null);
                u.SendMessage(Messages.Translate("Warn.RemovedWarning", Variables));
            }
			
			sender.sendMessage(Messages.Translate("Warn.RemovedSuccess", Variables));
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Warn.RemovedWarnAnnouncment", Variables));
            // TODO: DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}