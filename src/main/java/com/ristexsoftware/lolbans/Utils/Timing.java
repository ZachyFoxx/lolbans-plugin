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
import org.bukkit.configuration.InvalidConfigurationException;

import java.util.TreeMap;

/**
 * <h1>Timing Functions</h1>
 * The timing class will measure how long it takes between
 * the class being created to when the Finish() function is
 * called. This allows us to time how long certain operations
 * have taken.
 */
public class Timing 
{
	private Long start = System.currentTimeMillis();
	private Long later = 0L;
	public Timing()
	{
	}

	public Long Finish()
	{
		if (later == 0L)
			later = System.currentTimeMillis();
		return later - start;
	}

	public void Finish(CommandSender sender)
	{
		Timing self = this;
		try
		{
			sender.sendMessage(Messages.Translate("CommandComplete",
				new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
				{{
					put("Milliseconds", Long.toString(self.Finish()));
				}}
			));
		}
		catch (InvalidConfigurationException ex)
		{
			ex.printStackTrace();
		}
	}
}