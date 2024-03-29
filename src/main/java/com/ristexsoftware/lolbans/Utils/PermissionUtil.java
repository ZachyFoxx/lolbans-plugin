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

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;


public class PermissionUtil
{
    private static Main self = Main.getPlugin(Main.class);

    /**
     * Check if the command sender has been grated the permission
     * @param sender Command Sender to check a permission against
     * @param Perm the permission node to check
     * @return True if the command sender has the permission
     */
    public static boolean Check(CommandSender sender, String Perm)
    {
        // Console ALWAYS has full perms
        if (sender instanceof ConsoleCommandSender)
            return true;

        if (sender instanceof Player)
        {
            Player p = (Player)sender;
            
            // If configured to allow ops to bypass all permission checks
            if (self.getConfig().getBoolean("General.OpsBypassPermissions") && p.isOp())
                return true;

            // Otherwise check if they actually have the permission
            return p.hasPermission(Perm);
        }
        else
            return false; // Something that isnt a player or a command sender
    }
}