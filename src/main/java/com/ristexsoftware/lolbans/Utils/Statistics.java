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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.ristexsoftware.lolbans.Main;

public class Statistics {

    public static Integer getBansCount() {
        try {
            return DatabaseUtil.getPunishmentCount(PunishmentType.PUNISH_BAN).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getMutesCount() {
        try {
            return DatabaseUtil.getPunishmentCount(PunishmentType.PUNISH_MUTE).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getKicksCount() {
        try {
            return DatabaseUtil.getPunishmentCount(PunishmentType.PUNISH_KICK).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getWarnsCount() {
        try {
            return DatabaseUtil.getPunishmentCount(PunishmentType.PUNISH_WARN).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getRegexCount() {
        try {
            return DatabaseUtil.getPunishmentCount(PunishmentType.PUNISH_REGEX).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getIPCount() {
        try {
            return DatabaseUtil.getPunishmentCount(PunishmentType.PUNISH_IP).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getTotalPunishments() {
        return getBansCount() + getMutesCount() + getKicksCount() + getWarnsCount() + getRegexCount() + getIPCount();
    }

    public static Integer getUsersCount() {
        try {
            return DatabaseUtil.getUsersCount().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getUniquePunishments() {
        try {
            PreparedStatement ps = Main.getPlugin(Main.class).connection.prepareStatement("SELECT DISTINCT UUID FROM lolbans_punishments");
            Future<Optional<ResultSet>> result = DatabaseUtil.ExecuteLater(ps);
            if (result.isDone()) {
                Optional<ResultSet> ores = result.get();
                ResultSet results = ores.get();
                if (results.next()) {
                    results.last();
                    return results.getRow();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getUniquePunishmentsCount() {
        try {
            PreparedStatement ps = Main.getPlugin(Main.class).connection.prepareStatement("SELECT Count(Distinct UUID) AS UUID FROM lolbans_punishments");
            ResultSet results = ps.executeQuery();
            if (results.next() &&  !results.wasNull()) {
                return results.getInt("UUID");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}