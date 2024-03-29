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


package com.ristexsoftware.lolbans.Utils;

import com.ristexsoftware.lolbans.Main;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastUtil
{
    private static Main self = Main.getPlugin(Main.class);

    /**
     * Send a formatted message to all users on the server
     * @param Message The message to send
     * @param args Format arguments similar to printf-style functions.
     * @see java.util.Formatter
     */
    public static void Broadcast(String Message, Object... args)
    {
        Bukkit.broadcastMessage(String.format(Message, args));
    }

    /**
     * Send a formatted message to all administrators with  the alerts permission on the server
     * @param Message The message to send
     * @param args Format arguments similar to printf-style functions.
     * @see java.util.Formatter
     */
    public static void BroadcastOps(String Message, Object... args)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (p.hasPermission("lolbans.alerts"))
                p.sendMessage(String.format(Message, args));
        }
    }

    /**
     * Send a message to all users on the server depending on whether it is
     * silent and they have the alerts permission present.
     * @param silent Whether the announcement should be sent to everyone in the server or just people with the alerts permission
     * @param Announcement The message to send to all users on the server
     */
    public static void BroadcastEvent(boolean silent, String Announcement)
    {
        BroadcastUtil.BroadcastEvent(silent, Announcement, "lolbans.alerts");
    }

    /**
     * Send a message to all users on the server with the permission specified.
     * @param silent Whether the message is sent to all users on the server or just those with the permission
     * @param Announcement The message to send
     * @param Permission The permission node required if the message is silent.
     */
    public static void BroadcastEvent(boolean silent, String Announcement, String Permission)
    {
        self.getLogger().info(Announcement);
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (silent && !p.hasPermission(Permission))
                continue;
            
            p.sendMessage(Announcement);
        }
    }
}