package me.StevenLawson.TotalFreedomMod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;

public class TFM_Util
{
    private static final Logger log = Logger.getLogger("Minecraft");
    
    public TFM_Util()
    {
    }
    
    public static void tfm_broadcastMessage(String message, ChatColor color)
    {
        log.info(message);

        for (Player p : Bukkit.getOnlinePlayers())
        {
            p.sendMessage(color + message);
        }
    }

    public static void tfm_broadcastMessage(String message)
    {
        log.info(ChatColor.stripColor(message));

        for (Player p : Bukkit.getOnlinePlayers())
        {
            p.sendMessage(message);
        }
    }

    public static String implodeStringList(String glue, List<String> pieces)
    {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++)
        {
            if (i != 0)
            {
                output.append(glue);
            }
            output.append(pieces.get(i));
        }
        return output.toString();
    }

    public static String formatLocation(Location in_loc)
    {
        return String.format("%s: (%d, %d, %d)",
                in_loc.getWorld().getName(),
                Math.round(in_loc.getX()),
                Math.round(in_loc.getY()),
                Math.round(in_loc.getZ()));
    }

    public static void gotoWorld(CommandSender sender, String targetworld)
    {
        if (sender instanceof Player)
        {
            Player sender_p = (Player) sender;

            if (sender_p.getWorld().getName().equalsIgnoreCase(targetworld))
            {
                sender.sendMessage(ChatColor.GRAY + "Going to main world.");
                Bukkit.getServer().dispatchCommand(sender, "world 0");
                return;
            }

            for (World world : Bukkit.getWorlds())
            {
                if (world.getName().equalsIgnoreCase(targetworld))
                {
                    sender.sendMessage(ChatColor.GRAY + "Going to world: " + targetworld);
                    Bukkit.getServer().dispatchCommand(sender, "mv tp " + targetworld);
                    return;
                }
            }
        }
        else
        {
            sender.sendMessage("This command may not be used from the console.");
        }
    }

    public static void buildHistory(Location location, int length, TFM_UserInfo playerdata)
    {
        Block center_block = location.getBlock();
        for (int x_offset = -length; x_offset <= length; x_offset++)
        {
            for (int y_offset = -length; y_offset <= length; y_offset++)
            {
                for (int z_offset = -length; z_offset <= length; z_offset++)
                {
                    Block block = center_block.getRelative(x_offset, y_offset, z_offset);
                    playerdata.insertHistoryBlock(block.getLocation(), block.getType());
                }
            }
        }
    }

    public static void generateCube(Location location, int length, Material material)
    {
        Block center_block = location.getBlock();
        for (int x_offset = -length; x_offset <= length; x_offset++)
        {
            for (int y_offset = -length; y_offset <= length; y_offset++)
            {
                for (int z_offset = -length; z_offset <= length; z_offset++)
                {
                    center_block.getRelative(x_offset, y_offset, z_offset).setType(material);
                }
            }
        }
    }
    
    public static void setWorldTime(World world, long ticks)
    {
        long time = world.getTime();
        time -= time % 24000;
        world.setTime(time + 24000 + ticks);
    }
    
    public static void createDefaultConfiguration(String name, TotalFreedomMod tfm, File plugin_file)
    {
        File actual = new File(tfm.getDataFolder(), name);
        if (!actual.exists())
        {
            log.info("[" + tfm.getDescription().getName() + "]: Installing default configuration file template: " + actual.getPath());
            InputStream input = null;
            try
            {
                JarFile file = new JarFile(plugin_file);
                ZipEntry copy = file.getEntry(name);
                if (copy == null)
                {
                    log.severe("[" + tfm.getDescription().getName() + "]: Unable to read default configuration: " + actual.getPath());
                    return;
                }
                input = file.getInputStream(copy);
            }
            catch (IOException ioex)
            {
                log.severe("[" + tfm.getDescription().getName() + "]: Unable to read default configuration: " + actual.getPath());
            }
            if (input != null)
            {
                FileOutputStream output = null;

                try
                {
                    tfm.getDataFolder().mkdirs();
                    output = new FileOutputStream(actual);
                    byte[] buf = new byte[8192];
                    int length = 0;
                    while ((length = input.read(buf)) > 0)
                    {
                        output.write(buf, 0, length);
                    }

                    log.info("[" + tfm.getDescription().getName() + "]: Default configuration file written: " + actual.getPath());
                }
                catch (IOException ioex)
                {
                    log.log(Level.SEVERE, "[" + tfm.getDescription().getName() + "]: Unable to write default configuration: " + actual.getPath(), ioex);
                }
                finally
                {
                    try
                    {
                        if (input != null)
                        {
                            input.close();
                        }
                    }
                    catch (IOException ioex)
                    {
                    }

                    try
                    {
                        if (output != null)
                        {
                            output.close();
                        }
                    }
                    catch (IOException ioex)
                    {
                    }
                }
            }
        }
    }
    
    public static boolean isUserSuperadmin(CommandSender user, TotalFreedomMod tfm)
    {
        try
        {
            if (!(user instanceof Player))
            {
                return true;
            }

            if (Bukkit.getOnlineMode())
            {
                if (tfm.superadmins.contains(user.getName()))
                {
                    return true;
                }
            }

            Player p = (Player) user;
            if (p != null)
            {
                InetSocketAddress ip_address_obj = p.getAddress();
                if (ip_address_obj != null)
                {
                    String user_ip = ip_address_obj.getAddress().toString().replaceAll("/", "").trim();
                    if (user_ip != null && !user_ip.isEmpty())
                    {
                        if (tfm.superadmin_ips.contains(user_ip))
                        {
                            return true;
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            log.severe("Exception in TFM_Util.isUserSuperadmin: " + ex.getMessage());
        }

        return false;
    }

    public static int wipeDropEntities(TotalFreedomMod tfm)
    {
        int removed = 0;
        for (World world : Bukkit.getWorlds())
        {
            for (Entity ent : world.getEntities())
            {
                if (ent instanceof Arrow || (ent instanceof TNTPrimed && !tfm.allowExplosions) || ent instanceof Item || ent instanceof ExperienceOrb)
                {
                    ent.remove();
                    removed++;
                }
            }
        }
        return removed;
    }
}