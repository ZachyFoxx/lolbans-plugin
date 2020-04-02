package com.ristexsoftware.lolbans.Objects;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Punishment
{
    private static Main self = Main.getPlugin(Main.class);
    // Our class variables
    private OfflinePlayer player = null;
    private String DatabaseID = null;
    private String PID = null;
    private UUID uuid = null;
    private String PlayerName = null;
    private String IPAddress = null;
    private String Reason = null;
    private PunishmentType Type = null;
    private Timestamp TimePunished = null;
    private Timestamp Expiry = null;
    // If Executioner is null but IsConsole is true, it's a console punishment.
    private OfflinePlayer Executioner = null;
    private boolean IsConsoleExectioner = false;

    // Appealed stuff
    private String AppealReason = null;
    private Timestamp AppealedTime = null;
    private OfflinePlayer AppealStaff = null;
    private boolean IsConsoleAppealer = false;
    private boolean Appealed = false;
    private boolean WarningAcknowledged = false;

    // Used in FindPunishment
    private Punishment() {}

    // Create a new punishment from scratch.
    public Punishment(PunishmentType Type, CommandSender sender, OfflinePlayer target, String Reason, Timestamp Expiry) throws SQLException
    {
        this.Type = Type;
        this.player = target;
        this.uuid = target.getUniqueId();
        this.PlayerName = target.getName();
        this.TimePunished = TimeUtil.TimestampNow();
        this.IPAddress = target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN";
        this.Reason = Reason;
        this.Expiry = Expiry;
        
        if (sender instanceof Player)
            this.Executioner = (OfflinePlayer)sender;
        else
            this.IsConsoleExectioner = true;

        switch (Type)
        {
            case PUNISH_BAN: this.PID = PunishID.GenerateID(DatabaseUtil.GenID("Punishments"));
            case PUNISH_KICK: this.PID = PunishID.GenerateID(DatabaseUtil.GenID("Punishments"));
            case PUNISH_MUTE: this.PID = PunishID.GenerateID(DatabaseUtil.GenID("Punishments"));
            case PUNISH_WARN: this.PID = PunishID.GenerateID(DatabaseUtil.GenID("Punishments"));
            default:
                throw new UnknownError("Unknown Punishment Type for " + target.getName() + " " + Reason);
        }
    }

    public static Optional<Punishment> FindPunishment(String PunishmentID)
    {
        try 
        {
            PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM Punishments WHERE PunishID = ?");
            ps.setString(1, PunishmentID);

            Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(ps).get();
            if (ores.isPresent())
            {
                ResultSet res = ores.get();
                if (res.next())
                {
                    // Fill in our structure.
                    Punishment p = new Punishment();
                    p.PID = res.getString("PunishID");
                    p.DatabaseID = res.getString("id");
                    p.uuid = UUID.fromString(res.getString("UUID"));
                    p.PlayerName = res.getString("PlayerName");
                    p.IPAddress = res.getString("IPAddress");
                    p.Reason = res.getString("Reason");
                    p.Type = PunishmentType.FromOrdinal(res.getInt("Type"));
                    p.TimePunished = res.getTimestamp("TimePunished");
                    p.Expiry = res.getTimestamp("Expiry");
                    p.AppealReason = res.getString("AppealReason");
                    p.Appealed = res.getBoolean("Appealed");
                    p.WarningAcknowledged = res.getBoolean("WarningAck");

                    // Find players now.
                    p.player = Bukkit.getOfflinePlayer(p.uuid);

                    if (res.getString("ExecutionerUUID").equalsIgnoreCase("CONSOLE"))
                        p.IsConsoleExectioner = true;
                    else
                        p.Executioner = Bukkit.getOfflinePlayer(UUID.fromString(res.getString("ExecutionerUUID")));

                    if (res.getString("AppealUUID").equalsIgnoreCase("CONSOLE"))
                        p.IsConsoleAppealer = true;
                    else
                        p.AppealStaff = Bukkit.getOfflinePlayer(UUID.fromString(res.getString("AppealUUID")));

                    return Optional.of(p);
                }
            }
        }
        catch (SQLException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<Punishment> FindPunishment(PunishmentType Type, OfflinePlayer Player, boolean Appealed)
    {
        try
        {
            PreparedStatement pst3 = self.connection.prepareStatement("SELECT PunishID FROM Punishments WHERE UUID = ? AND Type = ? AND Appealed = ?");
            pst3.setInt(1, Type.ordinal());
            pst3.setString(2, Player.getUniqueId().toString());
            pst3.setBoolean(3, Appealed);

            Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(pst3).get();
            if (!ores.isPresent())
                return Optional.empty();

            ResultSet result = ores.get();
            if (!result.next())
                return Optional.empty();

            return Punishment.FindPunishment(result.getString("PunishID"));
        }
        catch (SQLException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Commit the punishment to the database.
     */
    public void Commit(CommandSender sender)
    {
        Punishment me = this;
        FutureTask<Void> t = new FutureTask<>(new Callable<Void>()
        {
            @Override
            public Void call()
            {

                //This is where you should do your database interaction
                try 
                {
                    int i = 1;
                    PreparedStatement InsertBan = null;
                    if (me.DatabaseID != null)
                    {
                        if (me.Appealed && me.AppealedTime == null)
                            me.AppealedTime = TimeUtil.TimestampNow();

                        InsertBan = self.connection.prepareStatement("UPDATE Punishments SET UUID = ?, PlayerName = ?, IPAddress = ?, Reason = ?, ExecutionerName = ?, ExecutionerUUID = ?, PunishID = ?, Expiry = ?, Type = ?, TimePunished = ?, AppealReason = ?, AppealStaff = ?, AppealUUID = ?, AppealTime = ?, Appealed = ?, WarningAck = ? WHERE id = ?");
                        InsertBan.setString(i++, me.uuid.toString());
                        InsertBan.setString(i++, me.PlayerName);
                        InsertBan.setString(i++, me.IPAddress);
                        InsertBan.setString(i++, me.Reason);
                        InsertBan.setString(i++, Executioner.getName().toString());
                        InsertBan.setString(i++, me.IsConsoleExectioner ? "CONSOLE" : ((Player)me.Executioner).getUniqueId().toString());
                        InsertBan.setString(i++, me.IsConsoleExectioner ? "CONSOLE" : ((Player)me.Executioner).getUniqueId().toString());
                        InsertBan.setString(i++, me.PID);
                        InsertBan.setTimestamp(i++, Expiry);
                        InsertBan.setInt(i++, Type.ordinal());
                        // TimePunished = ?, AppealReason = ?, AppealStaff = ?, AppealUUID = ?, AppealTime = ?, Appealed = ?, WarningAck = ? WHERE id = ?
                        InsertBan.setTimestamp(i++, me.TimePunished);
                        InsertBan.setString(i++, me.AppealReason);
                        InsertBan.setString(i++, me.IsConsoleAppealer ? "CONSOLE" : ((Player)me.AppealStaff).getName());
                        InsertBan.setString(i++, me.IsConsoleAppealer ? "CONSOLE" : ((Player)me.AppealStaff).getUniqueId().toString());
                        InsertBan.setTimestamp(i++, me.AppealedTime);
                        InsertBan.setBoolean(i++, me.Appealed);
                        InsertBan.setBoolean(i++, me.WarningAcknowledged);
                        InsertBan.setString(i++, me.DatabaseID);
                    }
                    else
                    {                    
                        // Preapre a statement
                        InsertBan = self.connection.prepareStatement(String.format("INSERT INTO Punishments (UUID, PlayerName, IPAddress, Reason, ExecutionerName, ExecutionerUUID, PunishID, Expiry, Type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"));
                        InsertBan.setString(i++, me.uuid.toString());
                        InsertBan.setString(i++, me.PlayerName);
                        InsertBan.setString(i++, me.IPAddress);
                        InsertBan.setString(i++, me.Reason);
                        InsertBan.setString(i++, Executioner.getName().toString());
                        InsertBan.setString(i++, me.IsConsoleExectioner ? "CONSOLE" : ((Player)me.Executioner).getUniqueId().toString());
                        InsertBan.setString(i++, me.IsConsoleExectioner ? "CONSOLE" : ((Player)me.Executioner).getUniqueId().toString());
                        InsertBan.setString(i++, me.PID);
                        InsertBan.setTimestamp(i++, Expiry);
                        InsertBan.setInt(i++, Type.ordinal());
                    }
                    InsertBan.executeUpdate();
                } 
                catch (SQLException e)
                {
                    e.printStackTrace();
                    sender.sendMessage(Messages.ServerError);
                }
                return null;
            }
        });

        Main.pool.execute(t);
    }

    public void Delete()
    {
        Punishment me = this;
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                //This is where you should do your database interaction
                try 
                {
                    // Preapre a statement
                    PreparedStatement pst2 = self.connection.prepareStatement("DELETE FROM Punishments WHERE id = ?");
                    pst2.setString(1, me.DatabaseID);
                    pst2.executeUpdate();

                    // Nullify everything!
                    me.player = null;
                    me.DatabaseID = null;
                    me.PID = null;
                    me.uuid = null;
                    me.PlayerName = null;
                    me.IPAddress = null;
                    me.Reason = null;
                    me.Type = null;
                    me.TimePunished = null;
                    me.Expiry = null;
                    // If Executioner is null but IsConsole is true, it's a console punishment.
                    me.Executioner = null;
                    me.IsConsoleExectioner = false;
                
                    // Appealed stuff
                    me.AppealReason = null;
                    me.AppealedTime = null;
                    me.AppealStaff = null;
                    me.IsConsoleAppealer = false;
                    me.Appealed = false;
                    me.WarningAcknowledged = false;
                } 
                catch (SQLException e) 
                {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        Main.pool.execute(t);
    }


    /************************************
     * Getters/Setters
     */
    public OfflinePlayer GetPlayer() { return this.player; }
    public String GetPunishmentID() { return this.PID; }
    public UUID GetUUID() { return this.uuid; }
    public String GetPlayerName() { return this.PlayerName; }
    public String GetIPAddress() { return this.IPAddress; }
    public String GetReason() { return this.Reason; }
    public PunishmentType GetPunishmentType() { return this.Type; }
    public Timestamp GetTimePunished() { return this.TimePunished; }
    public Timestamp GetExpiry() { return this.Expiry; }
    public OfflinePlayer GetExecutioner() { return this.Executioner; }
    public boolean IsConsoleExectioner() { return this.IsConsoleExectioner; }
    public String GetAppealReason() { return this.AppealReason; }
    public Timestamp GetAppealTime() { return this.AppealedTime; }
    public OfflinePlayer GetAppealStaff() { return this.AppealStaff; }
    public boolean IsConsoleAppealer() { return this.IsConsoleAppealer; }
    public boolean GetAppealed() { return this.Appealed; }
    public boolean AcknowledgedWarning() { return this.WarningAcknowledged; }

    public String GetExpiryDateAndDuration() { return this.Expiry != null ? String.format("%s (%s)", TimeUtil.TimeString(this.Expiry), TimeUtil.Expires(this.Expiry)) : "Never"; }
    public String GetExpiryDuration() { return this.Expiry != null ? TimeUtil.Expires(this.Expiry) : "Never"; }
    public String GetExpiryDate() { return this.Expiry != null ? TimeUtil.TimeString(this.Expiry) : "Never"; }


    /// Setters
    public void SetAppealReason(String Reason) { this.AppealReason = Reason; }
    public void SetAppealTime(Timestamp time) { this.AppealedTime = time; }
    public void SetAppealed(Boolean value) { this.Appealed = value; }
    public void SetWarningAcknowledged(Boolean value) { this.WarningAcknowledged = value; }
    public void SetAppealStaff(OfflinePlayer player)
    {
        if (player == null)
        {
            this.AppealStaff = null;
            this.IsConsoleAppealer = true;
        }
        else
        {
            this.AppealStaff = player;
            this.IsConsoleAppealer = false;
        }
    }
}