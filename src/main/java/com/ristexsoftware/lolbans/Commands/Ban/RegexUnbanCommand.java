package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexUnbanCommand extends RistExCommandAsync
{
    private Main self = (Main) this.getPlugin();
    
    public RegexUnbanCommand(Plugin owner)
    {
        super("regexunban", owner);
        this.setDescription("Remove a regular expression ban");
        this.setPermission("lolbans.regexunban");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.RegexUnban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.regexunban"))
            return User.PermissionDenied(sender, "lolbans.regexunban");

        if (args.length < 2)
            return false;
        
        // Syntax: /regexunban [-s] <Regular Expression|PunishID> <Reason>
        try 
        {
            boolean silent = args.length > 2 ? args[0].equalsIgnoreCase("-s") : false;
            String Regex = args[silent ? 1 : 0];
            String reason = Messages.ConcatenateRest(args, silent ? 2 : 1).trim();
            PreparedStatement ps = null;
            Pattern rx = null;

            try 
            {
                rx = Pattern.compile(Regex);
                ps = self.connection.prepareStatement("SELECT * FROM RegexBans WHERE Regex = ? LIMIT 1");
                ps.setString(1, rx.pattern());
            }
            catch (PatternSyntaxException ex)
            {
                if (PunishID.ValidateID(Regex.replaceAll("#", "")))
                {
                    ps = self.connection.prepareStatement("SELECT * FROM RegexBans WHERE PunishID = ?");
                    ps.setString(1, Regex);
                }
                else
                    return false; // Syntax error
            }

            Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(ps).get();
            
            if (!ores.isPresent())
                return User.PlayerOnlyVariableMessage("RegexBan.RegexIsNotBanned", sender, Regex, true);

            ResultSet res = ores.get();
            res.next();

            int i = 1;
            ps = self.connection.prepareStatement("UPDATE RegexBans SET AppealReason = ?, AppelleeName = ?, AppelleeUUID = ?, AppealTime = CURRENT_TIMESTAMP, Appealed = TRUE WHERE id = ?");
            ps.setString(i++, reason);
            ps.setString(i++, sender.getName());
            ps.setString(i++, sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "CONSOLE");
            ps.setInt(i++, res.getInt("id"));

            DatabaseUtil.ExecuteUpdate(ps);

            // Prepare our announce message
            final Pattern GetJollied = rx;
            final String PunishID = res.getString("PunishID");
            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("regex", GetJollied.pattern());
                put("arbiter", sender.getName());
				put("Reason", reason);
                put("punishid", PunishID);
                put("silent", Boolean.toString(silent));
            }};
            
            sender.sendMessage(Messages.Translate("RegexBan.UnbanSuccess", Variables));
            
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("RegexBan.UnbanAnnouncment", Variables));
            // TODO: DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (InvalidConfigurationException | SQLException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}