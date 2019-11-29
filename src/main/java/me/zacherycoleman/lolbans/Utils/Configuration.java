package me.zacherycoleman.lolbans.Utils; // Zachery's package owo

import me.zacherycoleman.lolbans.Runnables.QueryRunnable;

import java.io.File;
import java.sql.Connection;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import me.zacherycoleman.lolbans.Main;

public class Configuration
{
    static Main self = Main.getPlugin(Main.class);
    private static Configuration me;

    public static String dbhost = "";
    public static String dbname = "";
    public static String dbusername = "";
    public static String dbpassword = "";
    public static Integer dbport = 3306;
    public static Integer MaxReconnects = 5;

    public static String DiscordWebhook;
    public static String Prefix;
    public static String TempBanMessage;
    public static String PermBanMessage;
    public static Long QueryUpdateLong;
    public static String UnbanAnnouncment;
    public static String SilentUnbanAnnouncment;
    public static String CannotBanSelf;
    public static String InvalidSyntax;
    public static String CannotAddSelf;
    public static String PlayerDoesntExist;
    public static String PlayerIsBanned;
    public static String PlayerIsInBanWave;
    public static String BannedPlayersInBanWave;
    public static String SilentWarnAnnouncment;
    public static String WarnAnnouncment;
    public static String WarnedMessage;
    public static String WarnKickMessage;

    public static Connection connection;
    public static YamlConfiguration LANG;
    public static File LANG_FILE;

    public Configuration(FileConfiguration config)
    {
        this.Reload(config);
        Configuration.me = this;
    }

    public static Configuration GetConfig()
    {
        return me;
    }

    public void Reload(FileConfiguration config)
    {
        // Database
        Configuration.dbhost = config.getString("database.host");
        Configuration.dbport = config.getInt("database.port");
        Configuration.dbname = config.getString("database.name");
        Configuration.dbusername = config.getString("database.username");
        Configuration.dbpassword = config.getString("database.password");
        Configuration.MaxReconnects = config.getInt("database.MaxReconnects");
        Configuration.QueryUpdateLong = config.getLong("database.QueryUpdate");

        // Discord
        DiscordUtil.Webhook = config.getString("Discord.Webhook");
        
        // Messages
        Configuration.Prefix = config.getString("Prefix").replace("&", "§");
        Configuration.TempBanMessage = config.getString("TempBanMessage");
        Configuration.PermBanMessage = config.getString("PermMessage");
        Configuration.CannotBanSelf = config.getString("CannotBanSelf");
        Configuration.BanAnnouncment = config.getString("BanAnnouncment");
        Configuration.SilentUnbanAnnouncment = config.getString("BanAnnouncment");
        Configuration.UnbanAnnouncment = config.getString("UnbanAnnouncment");
        Configuration.SilentUnbanAnnouncment = config.getString("SilentUnbanAnnouncment");
        Configuration.InvalidSyntax = config.getString("InvalidSyntax");
        Configuration.CannotAddSelf = config.getString("CannotAddSelf");
        Configuration.PlayerDoesntExist = config.getString("PlayerDoesntExist");
        Configuration.PlayerIsBanned = config.getString("PlayerIsBanned"); 
        Configuration.PlayerIsInBanWave = config.getString("PlayerIsInBanWave"); 
        Configuration.BannedPlayersInBanWave = config.getString("BannedPlayersInBanWave");
        Configuration.SilentWarnAnnouncment = config.getString("SilentWarnAnnouncment");
        Configuration.WarnAnnouncment = config.getString("WarnAnnouncment");
        Configuration.WarnedMessage = config.getString("WarnedMessage");
        Configuration.WarnKickMessage = config.getString("WarnKickMessage");
    }
}