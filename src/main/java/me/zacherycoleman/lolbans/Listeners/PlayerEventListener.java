package me.zacherycoleman.lolbans.Listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.PlayerInventory;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.User;

public class PlayerEventListener
{
    public static void OnPlayerEvent(PlayerEvent event)
    {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;

        if (event instanceof PlayerMoveEvent)
        {
            PlayerMoveEvent E = (PlayerMoveEvent)event;
            Player p = E.getPlayer();

            // Put them in a box
            u.SpawnBox(false, null);
            
            Location from = E.getFrom();
            Location to = E.getTo();

            double x = Math.floor(from.getX());
            double z = Math.floor(from.getZ());
            double y = Math.floor(from.getY());

            Location loc = p.getLocation();
            loc.setY(loc.getY() - 2);

            // This will enforce their location server-side.
            if (Math.floor(to.getX()) != x || Math.floor(to.getZ()) != z || Math.floor(to.getY()) != y)
            {
                E.setCancelled(true);
                p.sendMessage(Configuration.Prefix + "§cYou have been warned, you may not move!\n" + Configuration.Prefix + " Please acknowledge that you've been warned by typing /accept.");
            }
            // Exit
            return;
        }

        // If they're trying to do literally anything, cancel it.
        if (event instanceof Cancellable)
        {
            // Ignore these events, they're critical to this event working.
            if (event instanceof PlayerKickEvent || event instanceof PlayerCommandPreprocessEvent || event instanceof PlayerCommandSendEvent)
                return;

            // Remind the player they're in a warned state.
            if (event instanceof AsyncPlayerChatEvent || event instanceof PlayerInventory)
                u.SendMessage(u.GetWarnMessage());

            // Cancel everything else.
            ((Cancellable)event).setCancelled(true);
        }
    }

    // Cancel most events that involve the player (they may not attack or damage people/entities)
    public static void OnEntityEvent(EntityEvent event)
    {
        Entity e = event.getEntity();

        // Ensure the player is not an entity of some kind. They're currently exempt.
        if (e instanceof Player)
        {
            User u = Main.USERS.get(((Player)e).getUniqueId());
            // Ignore players not warned
            if (u == null || !u.IsWarn())
                return;

            if (event instanceof Cancellable)
            {
                u.SendMessage(u.GetWarnMessage());
                ((Cancellable)event).setCancelled(true);
            }
        }

        // Ensure the player is not being affected in some way by a few events
        if (event instanceof EntityDamageByEntityEvent)
        {
            Entity damager = ((EntityDamageByEntityEvent)event).getDamager();

            if (damager instanceof Player)
            {
                User u = Main.USERS.get(((Player)damager).getUniqueId());
                // Ignore players not warned
                if (u == null || !u.IsWarn())
                    return;
    
                if (event instanceof Cancellable)
                {
                    u.SendMessage(u.GetWarnMessage());
                    ((EntityDamageByEntityEvent)event).setCancelled(true);
                }
            }
        }
    }
}