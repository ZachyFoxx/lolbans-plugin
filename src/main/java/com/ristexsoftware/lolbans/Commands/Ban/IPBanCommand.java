package com.ristexsoftware.lolbans.Commands.Ban;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Optional;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.IPBanUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.Timing;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;

// FIXME: Hostname-based bans?

public class IPBanCommand extends RistExCommandAsync
{
	private Main self = (Main)this.getPlugin();

	public IPBanCommand(Plugin owner)
	{
		super("ipban", owner);
		this.setDescription("Ban an ip address or CIDR range");
		this.setPermission("lolbans.ipban");
		// Stupid ass minecraft has to have it's special way.
		this.setAliases(Arrays.asList(new String[] { "ban-ip" }));
	}

	/**
	 * Check to make sure that the address is sane.
	 * This prevents you from doing insane bans (like 0.0.0.0/0)
	 * which is configurable in the config file.
	 * 
	 * Returns true if it is an insane (over-reaching) ban.
	 */
	private boolean SanityCheck(IPAddress bnyeh, CommandSender sender)
	{
		// Checking IP masks for sanity has been disabled by the config.
		if (self.getConfig().getBoolean("BanSettings.insane.ipmasks"))
			return false;

		double sanepercent = self.getConfig().getDouble("BanSettings.insane.trigger");

		// Get the total number of affected players.
		int affected = 0;
		int TotalOnline = Bukkit.getOnlinePlayers().size();
		for (Player player : Bukkit.getOnlinePlayers())
		{
			HostName hn = new HostName(player.getAddress());
			if (bnyeh.contains(hn.asAddress()))
				affected++;
		}

		// calculate percentage
		double percentage = affected == 0 ? 0 : (affected / TotalOnline) * 100.0;

		// TODO: Add confirmation command for people with override.
		// They have an override permission, let them execute
		if (percentage >= sanepercent)
		{
			// if the sender can override.
			if (sender.hasPermission("lolbans.insanityoverride"))
				return false;

			// Format our messages.
			try 
			{
				final int fuckingfinal = affected;
				String Insanity = Messages.Translate("IPBan.Insanity",
					new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
					{{
						put("ipaddress", String.valueOf(bnyeh));
						put("arbiter", sender.getName());
						put("AFFECTEDPLAYERS", String.valueOf(fuckingfinal));
						put("TOTALPLAYERS", String.valueOf(TotalOnline));
						put("INSANEPERCENT", String.valueOf(percentage));
						put("INSANETHRESHOLD", String.valueOf(sanepercent));
					}}
				);
				sender.sendMessage(Insanity);
			}
			catch (InvalidConfigurationException e)
			{
				e.printStackTrace();
				sender.sendMessage(Messages.ServerError);
			}

			// They breach the insanity limit and are not allowed to continue.
			return true;
		}

		// They're not insane (according to above logic... lol)
		return false;
	}

	@Override
	public void onSyntaxError(CommandSender sender, String label, String[] args)
	{
		try 
		{
			sender.sendMessage(Messages.InvalidSyntax);
			sender.sendMessage(Messages.Translate("Syntax.IPBan", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
		if (!PermissionUtil.Check(sender, "lolbans.ipban"))
			return User.PermissionDenied(sender, "lolbans.ipban");

		// /ipban [-s] <ip address>[/<cidr>] <time> <Reason here unlimited length>
		try
		{
			Timing t = new Timing();
			
			ArgumentUtil a = new ArgumentUtil(args);
			a.OptionalFlag("Silent", "-s");
			a.RequiredString("CIDR", 0);
			a.RequiredString("Time", 1);
			a.RequiredSentence("Reason", 2);

			if (!a.IsValid())
				return false;

			boolean silent = a.get("Silent") != null;
			String reason = a.get("Reason");
			Timestamp bantime = TimeUtil.ParseToTimestamp(a.get("Time"));

			if (bantime == null && !PermissionUtil.Check(sender, "lolbans.ipban.perm"))
				return User.PermissionDenied(sender, "lolbans.ipban.perm"); 
			
			// Is a future, needed != null for some reason.
			// IPAddress thingy = new IPAddressString(a.get("CIDR")).toAddress();
			IPAddress thingy = User.FindAddressByAny(a.get("CIDR"));
			if (thingy == null) return false;
			// TODO: handle this better? Send the banned subnet string instead of the address they tried to ban?
			Optional<ResultSet> res = IPBanUtil.IsBanned(thingy.toInetAddress()).get();
			if (res.isPresent() && res.get().next())
				return User.PlayerOnlyVariableMessage("IPBan.IPIsBanned", sender, thingy.toString(), true);

			if (SanityCheck(thingy, sender))
				return true;

			String banid = PunishID.GenerateID(DatabaseUtil.GenID("IPBans"));

			int i = 1;
			PreparedStatement pst = self.connection.prepareStatement("INSERT INTO lolbans_ipbans (IPAddress, Reason, ArbiterName, ArbiterUUID, PunishID, Expiry) VALUES (?, ?, ?, ?, ?, ?)");
			pst.setString(i++, thingy.toString());
			pst.setString(i++, reason);
			pst.setString(i++, sender.getName());
			pst.setString(i++, sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "CONSOLE");
			pst.setString(i++, banid);
			pst.setTimestamp(i++, bantime);
			DatabaseUtil.ExecuteUpdate(pst);

			// Format our messages.
			String IPBanAnnouncement = Messages.Translate("IPBan.BanAnnouncement",
				new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
				{{
					put("ipaddress", thingy.toString());
					put("reason", reason);
					put("arbiter", sender.getName());
					put("punishid", banid);
					put("silent", Boolean.toString(silent));
					put("appealed", Boolean.toString(false));
					if (bantime != null)
						put("expiry", bantime.toString());
				}}
			);

			self.getLogger().info(IPBanAnnouncement);

			// Send messages to all players (if not silent) or only to admins (if silent)
			// and also kick players who match the ban.
			for (Player p : Bukkit.getOnlinePlayers())
			{
				HostName hn = new HostName(p.getAddress());
				
				// FIXME: Do we use a custom message? what's this func even doing?
				// "KickPlayer" sends the inputed strings into the function in the User class
				// there are multiple "KickPlayer" funcs but this one is for lolbans_ipbans (hence why the IP is on the end)
				// Once the func gets the inputs, it'll kick the player with a message specified in the config
				if (thingy.contains(hn.asAddress()))
				{
					Bukkit.getScheduler().runTaskLater(self, () -> User.KickPlayerIP(sender.getName(), p, banid, reason, TimeUtil.TimestampNow(), bantime, thingy.toString()), 1L);
					continue;
				}

				if (silent && !p.hasPermission("lolbans.alerts"))
					continue;

				p.sendMessage(IPBanAnnouncement);
			}
			
			// SendIP
			DiscordUtil.GetDiscord().SendBanObject(sender, thingy.toString(), reason, banid, bantime);
			t.Finish(sender);

			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			sender.sendMessage(Messages.ServerError);
			return true;
		}
	}
}