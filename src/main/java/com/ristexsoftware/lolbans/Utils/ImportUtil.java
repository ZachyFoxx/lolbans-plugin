package com.ristexsoftware.lolbans.Utils;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.MojangUtil.MojangUser;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ImportUtil {

    private static Main self = Main.getPlugin(Main.class);
    static boolean isRunning = false;

    // We send the sender a message if anything happens..
    /**
     * Import function for importing Essentials user data and punishments NOTE: This
     * should only be called from an asynchronous command (RistexCommandAsync)
     * 
     * @param sender The command sender, this is used to send messages when
     *               something happens
     */
    public static void importEssentials(CommandSender sender) {
        if (isRunning) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Messages.Prefix
                    + "&cAn import task is already running! Type \"/lolbans import cancel\" to cancel it"));
            return;
        }

        File banJson = new File(Bukkit.getWorldContainer().getAbsolutePath() + "/banned-players.json");
        File userYaml = new File(Bukkit.getWorldContainer().getAbsolutePath() + "/plugins/Essentials/userdata/");
        File[] userYamls = userYaml.listFiles();

        if (banJson.exists() && userYaml.exists()) {
            try {
                Reader reader = Files.newBufferedReader(
                        Paths.get(Bukkit.getWorldContainer().getAbsolutePath() + "/banned-players.json"));
                JsonParser parser = new JsonParser();
                JsonElement tree = parser.parse(reader);
                JsonArray array = tree.getAsJsonArray();
                int seconds = userYamls.length + array.size();
                Timestamp ts = new Timestamp((TimeUtil.GetUnixTime() * 1000L) + Math.round((seconds * 1000L) * 0.25));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        Messages.Prefix + "&bImporting &f" + array.size() + " &bpunishments and &f" + userYamls.length
                                + " &busers!\n" + Messages.Prefix + "&bThis will take approximately &f"
                                + TimeUtil.Expires(ts)));

                isRunning = true;
                for (JsonElement element : array) {
                    if (isRunning) {
                        JsonObject obj = element.getAsJsonObject();
                        Timestamp timeStamp = null;
                        if (obj.get("expires").toString().replace("\"", "").equals("forever")) {
                            timeStamp = TimeUtil.ParseToTimestamp("0");
                        } else {
                            Date date = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss Z")
                                    .parse(obj.get("expires").toString().replace("\"", ""));
                            timeStamp = new Timestamp(date.getTime());
                        }
                        MojangUtil mojangAPI = new MojangUtil();
                        MojangUser mojangUser = mojangAPI.resolveUser(obj.get("uuid").toString().replace("\"", ""));
                        if (mojangUser == null) {
                            self.getLogger().warning(obj.get("name").toString() + " does not exist! Skipping...");
                            continue; // Skip this, this user doesn't even exist.
                        }
                        OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
                        if (User.IsPlayerBanned(op)) {
                            self.getLogger().warning(mojangUser.getName() + " is already banned, skipping...");
                            continue;
                        }
                        Punishment punishment = new Punishment(PunishmentType.PUNISH_BAN, sender, op,
                                obj.get("reason").toString().replace("\"", ""), timeStamp);
                        punishment.Commit(sender);
                        Thread.sleep(250); // Lets wait a bit here too...
                    }

                }

                for (final File file : userYamls) {
                    if (isRunning) {
                        FileConfiguration ymlFile = YamlConfiguration.loadConfiguration(file);

                        String name = ymlFile.getString("lastAccountName");
                        Timestamp login = ymlFile.get("timestamps.login") == null ? null
                                : new Timestamp(ymlFile.getLong("timestamps.login"));
                        Timestamp logout = ymlFile.get("timestamps.logout") == null ? null
                                : new Timestamp(ymlFile.getLong("timestamps.logout"));
                        String ipaddress = ymlFile.getString("ipAddress");
                        String uuid = file.getName().replace(".yml", "");

                        // Essentials logs NPCs for some reason, so lets just skip those!
                        if (ymlFile.getBoolean("npc"))
                            continue;

                        // If the userdata is null, don't continue there's no point
                        if (name == null || ipaddress == null || uuid == null) {
                            self.getLogger().warning("User data is null, skipping file " + file.getName());
                            continue;
                        }
                        // If either of these are null, just set the timestamp to now
                        if (login == null || logout == null) {
                            login = login == null ? TimeUtil.TimestampNow() : login;
                            logout = logout == null ? TimeUtil.TimestampNow() : logout;
                        }

                        // Finally, import the user, but also ensure InsertUser doesn't fail.
                        final Future<Boolean> insertUser = DatabaseUtil.InsertUser(uuid, name, ipaddress, login,
                                logout);
                        if (!insertUser.get()) {
                            self.getLogger().severe("There was an error importing " + file.getName());
                            continue;
                        }

                        Thread.sleep(250); // Lets wait a bit before the next import...
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isRunning = false;
        }
    }

    /**
     * Import function for importing LiteBans user data and punishments NOTE: This
     * should only be called from an asynchronous command (RistexCommandAsync)
     * 
     * @param sender The command sender, this is used to send messages when
     *               something happens
     */
    public static void importLitebans(CommandSender sender) {
        try {
            sender.sendMessage(Messages.Prefix + ChatColor.RED + "LiteBans importing is not supported yet!");
            return;
            // if (isRunning) {
            //     sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Messages.Prefix
            //             + "&cAn import task is already running! Type \"/lolbans import cancel\" to cancel it"));
            //     return;
            // }
            // PreparedStatement bans = self.connection.prepareStatement("SELECT * FROM litebans_bans");
            // PreparedStatement mutes = self.connection.prepareStatement("SELECT * FROM mutes");
            // PreparedStatement kicks = self.connection.prepareStatement("SELECT * FROM litebans_kicks");
            // PreparedStatement warns = self.connection.prepareStatement("SELECT * FROM warnings");
            // PreparedStatement users = self.connection.prepareStatement("SELECT * FROM litebans_history");
            // ResultSet banrs = bans.executeQuery();
            // ResultSet muters = mutes.executeQuery();
            // ResultSet kickrs = kicks.executeQuery();
            // ResultSet warnrs = warns.executeQuery();
            // ResultSet userrs = users.executeQuery();
            // while (banrs.next()) {
            //     MojangUtil mojangAPI = new MojangUtil();
            //     MojangUser mojangUser = mojangAPI.resolveUser(banrs.getString("uuid"));
            //     if (mojangUser == null) {
            //         self.getLogger().warning(banrs.getString("uuid") + " does not exist! Skipping...");
            //         continue; // Skip this, this user doesn't even exist.
            //     }
            //     OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
            //     if (User.IsPlayerBanned(op)) {
            //         self.getLogger().warning(mojangUser.getName() + " is already banned, skipping...");
            //         continue;
            //     }
            //     Punishment ban = new Punishment(PunishmentType.PUNISH_BAN, sender, op, banrs.getString("reason"), new Timestamp(banrs.getInt("time")));
            //     ban.Commit(sender);
            //     Thread.sleep(250);
            // }
            // while (muters.next()) {
            //     MojangUtil mojangAPI = new MojangUtil();
            //     MojangUser mojangUser = mojangAPI.resolveUser(muters.getString("uuid"));
            //     if (mojangUser == null) {
            //         self.getLogger().warning(muters.getString("uuid") + " does not exist! Skipping...");
            //         continue; // Skip this, this user doesn't even exist.
            //     }
            //     OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
            //     if (User.IsPlayerMuted(op)) {
            //         self.getLogger().warning(mojangUser.getName() + " is already muted, skipping...");
            //         continue;
            //     }
            //     Punishment ban = new Punishment(PunishmentType.PUNISH_MUTE, sender, op, muters.getString("reason"), new Timestamp(muters.getInt("time")));
            //     ban.Commit(sender);
            //     Thread.sleep(250);
            // }
            // while (kickrs.next()) {
            //     MojangUtil mojangAPI = new MojangUtil();
            //     MojangUser mojangUser = mojangAPI.resolveUser(kickrs.getString("uuid"));
            //     if (mojangUser == null) {
            //         self.getLogger().warning(kickrs.getString("uuid") + " does not exist! Skipping...");
            //         continue; // Skip this, this user doesn't even exist.
            //     }
            //     OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
            //     Punishment ban = new Punishment(PunishmentType.PUNISH_KICK, sender, op, kickrs.getString("reason"), new Timestamp(kickrs.getInt("time")));
            //     ban.Commit(sender);
            //     Thread.sleep(250);
            // }
            // while (warnrs.next()) {
            //     MojangUtil mojangAPI = new MojangUtil();
            //     MojangUser mojangUser = mojangAPI.resolveUser(warnrs.getString("uuid"));
            //     if (mojangUser == null) {
            //         self.getLogger().warning(warnrs.getString("uuid") + " does not exist! Skipping...");
            //         continue; // Skip this, this user doesn't even exist.
            //     }
            //     OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
            //     Punishment ban = new Punishment(PunishmentType.PUNISH_WARN, sender, op, warnrs.getString("reason"), new Timestamp(warnrs.getInt("time")));
            //     ban.Commit(sender);
            //     Thread.sleep(250);
            // }
            // while (userrs.next()) {
            //     MojangUtil mojangAPI = new MojangUtil();
            //     MojangUser mojangUser = mojangAPI.resolveUser(userrs.getString("uuid"));
            //     if (mojangUser == null) {
            //         self.getLogger().warning(userrs.getString("uuid") + " does not exist! Skipping...");
            //         continue; // Skip this, this user doesn't even exist.
            //     }
            //     OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
            //     //DatabaseUtil.InsertUser(userrs.getString("uuid"), PlayerName, userrs.getString("ip"), FirstLogin, LastLogin)
            //     Thread.sleep(250);
            // }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cancel any active import tasks
     */
    public static void Cancel() {
        isRunning = false;
    }

    /**
     * Check if there is an active import task running
     * 
     * @return true if a task is running
     */
    public static boolean ActiveTask() {
        return isRunning;
    }
}