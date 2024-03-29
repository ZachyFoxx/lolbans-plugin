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

package com.ristexsoftware.lolbans;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.ristexsoftware.lolbans.Commands.Ban.BanCommand;
import com.ristexsoftware.lolbans.Commands.Ban.BanWaveCommand;
import com.ristexsoftware.lolbans.Commands.Ban.IPBanCommand;
import com.ristexsoftware.lolbans.Commands.Ban.RegexBanCommand;
import com.ristexsoftware.lolbans.Commands.Ban.RegexUnbanCommand;
import com.ristexsoftware.lolbans.Commands.Ban.UnIPBanCommand;
import com.ristexsoftware.lolbans.Commands.Ban.UnbanCommand;
import com.ristexsoftware.lolbans.Commands.History.HistoryCommand;
import com.ristexsoftware.lolbans.Commands.History.StaffHistoryCommand;
import com.ristexsoftware.lolbans.Commands.Misc.KickCommand;
import com.ristexsoftware.lolbans.Commands.Misc.LolBansCommand;
import com.ristexsoftware.lolbans.Commands.Misc.StaffRollbackCommand;
import com.ristexsoftware.lolbans.Commands.Mute.MuteChatCommand;
import com.ristexsoftware.lolbans.Commands.Mute.MuteCommand;
import com.ristexsoftware.lolbans.Commands.Mute.UnmuteCommand;
import com.ristexsoftware.lolbans.Commands.Report.ReportCommand;
import com.ristexsoftware.lolbans.Commands.Report.ReportHistoryCommand;
import com.ristexsoftware.lolbans.Commands.Warn.AcceptCommand;
import com.ristexsoftware.lolbans.Commands.Warn.UnWarnCommand;
import com.ristexsoftware.lolbans.Commands.Warn.WarnCommand;
import com.ristexsoftware.lolbans.Hacks.AsyncChatListener;
import com.ristexsoftware.lolbans.Hacks.ConnectionListeners;
import com.ristexsoftware.lolbans.Hacks.PlayerEventListener;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.ImportUtil;
import com.ristexsoftware.lolbans.Utils.InventoryUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.Metrics;
import com.ristexsoftware.lolbans.Utils.ReflectionUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import inet.ipaddr.IPAddressString;

// welcome. aaa
/**
 * <h1>LolBans Punishment Plugin</h1> The lolbans plugin. The most advanced
 * punishment management system made for Minecraft
 *
 * @author Justin Crawford &amp; Zachery Coleman
 * @version 1.0
 * @since 2019-11-13
 */
public final class Main extends JavaPlugin {
    public static HashMap<UUID, User> USERS = new HashMap<UUID, User>();
    public static HashMap<Integer, Pattern> REGEX = new HashMap<Integer, Pattern>();
    public static List<IPAddressString> BannedAddresses = new Vector<IPAddressString>();
    // For some reason using Futures with the Bukkit Async scheduler doesn't work.
    // Instead of relying on dumb bukkit APIs to get tasks done, we use a thread
    // pool of
    // our own control to get whatever we want done.
    public static ExecutorService pool = Executors.newFixedThreadPool(3);

    // Whether or not the chat has been muted globally.
    public boolean ChatMuted = false;

    // Our database connection, here for legacy reasons (it should be in
    // DatabaseUtil)
    public Connection connection;
    List<Command> CommandList = new ArrayList<Command>();

    @Override
    public void onEnable() {
        // Register our bStats ID
        int bstatsId = 8155;
        Metrics metrics = new Metrics(this, bstatsId);

        // Plugin startup logic
        new Configuration(this.getConfig());

        // This is dumb, plugman somehow breaks lolbans
        // but whatever, you shouldn't be using plugman anyway...
        if (getServer().getPluginManager().getPlugin("PlugMan") != null)
            getLogger().warning(
                    "PlugMan detected! This WILL cause issues with LolBans, please consider restarting the server to update plugins!");

        // Creating config folder, and adding config to it.
        if (!this.getDataFolder().exists()) {
            // uwubans*
            getLogger().info("Error: No folder for lolbans was found! Creating...");
            this.getDataFolder().mkdirs();
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            return;
        }

        if (!(new File(this.getDataFolder(), "config.yml").exists())) {
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            return;
        }

        // Initialize our database connections.
        if (!DatabaseUtil.InitializeDatabase())
            return;

        // Make sure our messages file exists
        Messages.GetMessages();

        // Initialize and compile the regex cache
        try {
            ResultSet res = this.connection.prepareStatement("SELECT * FROM lolbans_regexbans").executeQuery();
            while (res.next()) {
                try {
                    getLogger().info(String.format("Registered Regular Expression: \"%s\" (%x)", res.getString("Regex"),
                            res.getInt("id")));
                    Main.REGEX.put(res.getInt("id"), Pattern.compile(res.getString("Regex")));
                } catch (PatternSyntaxException ex) {
                    ex.printStackTrace();
                    getLogger().warning(String.format("Ignoring Regular Expression \"%s\"", res.getString("Regex")));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        // Used if the admin does /reload confirm
        // Do this before we register event listeners
        for (Player p : Bukkit.getOnlinePlayers())
            Main.USERS.put(p.getUniqueId(), new User(p));

        // Thanks java 11, and thanks MD fucking 5 for not allowing us to just register
        // PlayerEvent and EntityEvent
        // because god forbid someone trying to cancel everything related to a
        // player/entity....
        getServer().getPluginManager().registerEvents(new AsyncChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
        getServer().getPluginManager().registerEvents(new ConnectionListeners(), this);

        // Register command GUIs
        InventoryUtil.LoadGUIs();
        
        CommandList.add(new BanCommand(this));
        CommandList.add(new IPBanCommand(this));
        CommandList.add(new UnbanCommand(this));
        CommandList.add(new RegexUnbanCommand(this));
        CommandList.add(new UnIPBanCommand(this));
        CommandList.add(new HistoryCommand(this));
        CommandList.add(new BanWaveCommand(this));
        CommandList.add(new WarnCommand(this));
        CommandList.add(new UnWarnCommand(this));
        CommandList.add(new AcceptCommand(this));
        CommandList.add(new MuteChatCommand(this));
        CommandList.add(new MuteCommand(this));
        CommandList.add(new UnmuteCommand(this));
        CommandList.add(new KickCommand(this));
        CommandList.add(new ReportCommand(this));
        CommandList.add(new RegexBanCommand(this));
        CommandList.add(new StaffRollbackCommand(this));
        CommandList.add(new StaffHistoryCommand(this));
        CommandList.add(new ReportHistoryCommand(this));
        CommandList.add(new LolBansCommand(this));
        // CommandList.add(new FreezeCommand(this));

        // MD_5 and his knobbery continues. the CraftServer.java class has a
        // `getCommandMap()`
        // method and CommandMap is documented but there's no reasonable way to get the
        // command
        // map from within the server. Because MD_5 couldn't help but program like a 12
        // year old
        // we now have to use reflection to get a more reasonable way to register
        // commands.
        CommandMap cmap = ReflectionUtil.getProtectedValue(Bukkit.getServer(), "commandMap");
        cmap.registerAll(this.getName().toLowerCase(), CommandList);
        getLogger().info("LolBans enabled successfully!");
    }

    @Override
    public void onDisable() {
        ImportUtil.Cancel();
        // Save our config values
        reloadConfig();
        // Close out or database.
        DatabaseUtil.Terminate();
    }
}
