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

package com.ristexsoftware.lolbans.Commands.Ban;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.Timing;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BanCommand extends RistExCommandAsync {
    private static Main self = Main.getPlugin(Main.class);

    public BanCommand(Plugin owner) {
        super("ban", owner);
        this.setDescription("Ban a player");
        this.setPermission("lolbans.ban");
        this.setAliases(Arrays.asList(new String[] { "eban","tempban" }));
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args) {
        try {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(
                    Messages.Translate("Syntax.Ban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args) {
        if (!PermissionUtil.Check(sender, "lolbans.ban"))
            return User.PermissionDenied(sender, "lolbans.ban");

        try {
            Timing t = new Timing();

            // /ban [-s, -o] <PlayerName> [Time|*] <Reason>
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.OptionalFlag("Overwrite", "-o");
            a.RequiredString("PlayerName", 0);
            a.OptionalString("TimePeriod", 1);
            a.RequiredSentence("Reason", 1);

            // For some reason the OptionalSentence method is broken, no clue why... So... we have to do it the shitty way...
            if (a.get("PlayerName") == null)
                return false;

            // if (!a.IsValid())
            //     return false;

            boolean silent = a.get("Silent") != null;
            boolean ow = a.get("Overwrite") != null;
            String PlayerName = a.get("PlayerName");
            Timestamp punishtime = a.get("TimePeriod") == null ? null : TimeUtil.ParseToTimestamp(a.get("TimePeriod"));
            // mmmmmm more abuse of ternary statements
            String reason = punishtime == null ? a.get("TimePeriod")+" "+ (a.get("Reason") == null ? "" : a.get("Reason")) : a.get("Reason");
            if (reason == null || reason.trim().equals("null")) {
                String configReason = Main.getPlugin(Main.class).getConfig().getString("BanSettings.DefaultReason");
                reason = configReason == null ? "Your account has suspended!" : configReason;
            }

            OfflinePlayer target = User.FindPlayerByAny(PlayerName);
            Punishment punish = new Punishment(PunishmentType.PUNISH_BAN, sender instanceof Player ? ((Player)sender).getUniqueId().toString() : null, target, reason, punishtime, silent);
            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            if (ow && !sender.hasPermission("lolbans.ban.overwrite"))
                return User.PermissionDenied(sender, "lolbans.ban.overwrite");
            else if (ow) {
                User.removePunishment(PunishmentType.PUNISH_BAN, sender, target,
                        "Overwritten by #" + punish.GetPunishmentID(), silent);
            }

            if (User.IsPlayerBanned(target) && !ow)
                return User.PlayerOnlyVariableMessage("Ban.PlayerIsBanned", sender, target.getName(), true);

            if (punishtime == null && !PermissionUtil.Check(sender, "lolbans.ban.perm"))
                return User.PermissionDenied(sender, "lolbans.ban.perm");

            if (punishtime != null && TimeUtil.ParseToTimestamp(a.get("TimePeriod")).getTime() > User.getTimeGroup(sender).getTime())
                punishtime = User.getTimeGroup(sender);
                //return User.PermissionDenied(sender, "lolbans.maxtime." + a.get("TimePeriod"));

            punish.Commit(sender);

            // Kick the player first, they're officially banned.
            if (target.isOnline())
                Bukkit.getScheduler().runTaskLater(self, () -> User.KickPlayer(punish), 1L);

            // Format our messages.
            final Timestamp whyisjavasostupid = punishtime;
            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put("player", punish.GetPlayerName());
                    put("reason", punish.GetReason());
                    put("arbiter", punish.GetExecutionerName());
                    put("punishid", punish.GetPunishmentID());
                    put("expiry", whyisjavasostupid == null ? "" : punish.GetExpiryString());
                    put("silent", Boolean.toString(silent));
                    put("appealed", Boolean.toString(punish.GetAppealed()));
                    put("expires", Boolean.toString(whyisjavasostupid != null && !punish.GetAppealed()));
                }
            };

            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Ban.BanAnnouncement", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
            t.Finish(sender);
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}