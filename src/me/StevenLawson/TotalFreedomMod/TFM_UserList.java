package me.StevenLawson.TotalFreedomMod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class TFM_UserList
{
    private static final String USERLIST_FILENAME = "userlist.yml";
    private static TFM_UserList instance = null;
    private Map<String, TFM_UserListEntry> userlist = new HashMap<String, TFM_UserListEntry>();
    private final TotalFreedomMod plugin;

    protected TFM_UserList(TotalFreedomMod plugin)
    {
        this.plugin = plugin;

        primeList();
    }

    private void primeList()
    {
        try
        {
            userlist.clear();

            FileConfiguration saved_userlist = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), USERLIST_FILENAME));

            for (String username : saved_userlist.getKeys(false))
            {
                TFM_UserListEntry entry = new TFM_UserListEntry(username, saved_userlist.getStringList(username));
                userlist.put(username, entry);
            }

            for (Player player: plugin.getServer().getOnlinePlayers())
            {
                addUser(player);
            }

            exportList();
        }
        catch (Exception e)
        {
            TFM_Log.severe("Error loading Userlist, resetting list: " + e.getMessage());
            purge();
        }
    }

    private void exportList()
    {
        FileConfiguration new_userlist = new YamlConfiguration();

        for (TFM_UserListEntry entry : userlist.values())
        {
            new_userlist.set(entry.getUsername(), entry.getIpAddresses());
        }

        try
        {
            new_userlist.save(new File(plugin.getDataFolder(), USERLIST_FILENAME));
        }
        catch (IOException e)
        {
            TFM_Log.severe(e);
        }
    }

    public static TFM_UserList getInstance(TotalFreedomMod plugin)
    {
        if (instance == null)
        {
            instance = new TFM_UserList(plugin);
        }
        return instance;
    }

    public void addUser(Player player)
    {
        addUser(player.getName(), player.getAddress().getAddress().getHostAddress());
    }

    public void addUser(String username, String ip_address)
    {
        username = username.toLowerCase();

        TFM_UserListEntry entry = userlist.get(username);
        if (entry == null)
        {
            entry = new TFM_UserListEntry(username);
        }

        userlist.put(username, entry);

        if (entry.addIpAddress(ip_address))
        {
            exportList();
        }
    }

    public TFM_UserListEntry getEntry(Player player)
    {
        return getEntry(player.getName());
    }

    public TFM_UserListEntry getEntry(String username)
    {
        return userlist.get(username.toLowerCase());
    }

    public void purge()
    {
        userlist.clear();

        for (Player player: plugin.getServer().getOnlinePlayers())
        {
            addUser(player);
        }

        exportList();
    }

    public String searchByPartialName(String needle)
    {
        needle = needle.toLowerCase().trim();
        Integer minEditDistance = null;
        String minEditMatch = null;
        Iterator<String> it = userlist.keySet().iterator();
        while (it.hasNext())
        {
            String haystack = it.next();
            int editDistance = StringUtils.getLevenshteinDistance(needle, haystack.toLowerCase());
            if (minEditDistance == null || minEditDistance.intValue() > editDistance)
            {
                minEditDistance = editDistance;
                minEditMatch = haystack;
            }
        }
        return minEditMatch;
    }

    public class TFM_UserListEntry
    {
        private String username;
        private List<String> ip_addresses = new ArrayList<String>();

        public TFM_UserListEntry(String username, List<String> ip_addresses)
        {
            this.username = username;
            this.ip_addresses = ip_addresses;
        }

        public TFM_UserListEntry(String username)
        {
            this.username = username;
        }

        public List<String> getIpAddresses()
        {
            return ip_addresses;
        }

        public String getUsername()
        {
            return username;
        }

        public boolean addIpAddress(String ip_address)
        {
            if (!ip_addresses.contains(ip_address))
            {
                ip_addresses.add(ip_address);
                return true;
            }
            return false;
        }
    }
}
