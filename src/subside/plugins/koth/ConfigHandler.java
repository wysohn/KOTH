package subside.plugins.koth;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;
import subside.plugins.koth.adapter.MapRotation;

public class ConfigHandler {
    private @Getter static ConfigHandler instance;
    
	private @Getter Global global;
	private @Getter Loot loot;
	private @Getter Koth koth;
	private @Getter Hooks hooks;
	
	public ConfigHandler(FileConfiguration cfg){
		instance = this;
        
		global = new Global(cfg.getConfigurationSection("global"));
		loot = new Loot(cfg.getConfigurationSection("loot"));
		koth = new Koth(cfg.getConfigurationSection("koth"));
		hooks = new Hooks(cfg.getConfigurationSection("hooks"));
	}
	
	public class Global {
	    private @Getter boolean useCache = false;
	    private @Getter boolean currentDayOnly = false;
	    private @Getter String timeZone = "Europe/Amsterdam";
        private @Getter int minuteOffset = 0;
        private @Getter int startWeekMinuteOffset = 0;
	    private @Getter boolean usePlayerMoveEvent = false;
	    private @Getter int preBroadcast = 0;
	    private @Getter int noCapBroadcastInterval = 30;
	    private @Getter List<String> helpCommand = null;
        private @Getter boolean useFancyPlayerName = false;
        private @Getter boolean multipleKothsAtOnce = true;
	    private @Getter boolean debug = false;
	    
	    public Global(ConfigurationSection section){
	        useCache = section.getBoolean("use-cache");
	        currentDayOnly = section.getBoolean("schedule-show-current-day-only");
	        timeZone = section.getString("schedule-timezone");
            minuteOffset = section.getInt("minuteoffset");
            startWeekMinuteOffset = section.getInt("startweekminuteoffset");
	        usePlayerMoveEvent = section.getBoolean("use-playermoveevent");
	        preBroadcast = section.getInt("pre-broadcast");
	        noCapBroadcastInterval = section.getInt("nocap-broadcast-interval");
	        helpCommand = section.getStringList("helpcommand");
            useFancyPlayerName = section.getBoolean("fancyplayername");
            multipleKothsAtOnce = section.getBoolean("multiplekothsatonce");
	        debug = section.getBoolean("debug");
	    }
	}
	
	public class Hooks {
	    private @Getter boolean vanishNoPacket = true;
	    private @Getter boolean factions = true;
	    private @Getter boolean kingdoms = true;
	    private @Getter Featherboard featherboard;
	    
	    public Hooks(ConfigurationSection section){
            vanishNoPacket = section.getBoolean("vanishnopacket");
            factions = section.getBoolean("factions");
            kingdoms = section.getBoolean("kingdoms");
            featherboard = new Featherboard(section.getConfigurationSection("featherboard"));
	    }
	    
	    public class Featherboard {
	        private @Getter boolean enabled = false;
	        private @Getter int range = 100;
	        private @Getter int rangeMargin = 5;
	        private @Getter String board = "KoTH";
	        
	        public Featherboard(ConfigurationSection section){
	            enabled = section.getBoolean("enabled");
	            range = section.getInt("range");
	            rangeMargin = section.getInt("rangemargin");
	            board = section.getString("board");
	        }
	        
	    }
	}
	
	public class Loot {

	    private @Getter String defaultLoot = "";
	    private @Getter boolean randomizeLoot = true;
	    private @Getter int lootAmount = 5;
	    private @Getter boolean randomizeStackSize = false;
	    private @Getter boolean useItemsMultipleTimes = true;
	    private @Getter long removeLootAfterSeconds = 0;
	    private @Getter boolean dropLootOnRemoval = false;
	    private @Getter boolean instantLoot = false;

        private @Getter boolean cmdEnabled = false;
        private @Getter boolean cmdIngame = false;
        private @Getter boolean cmdNeedOp = true;
	    
	    
	    public Loot(ConfigurationSection section){
	        defaultLoot = section.getString("default");
	        randomizeLoot = section.getBoolean("randomize");
	        lootAmount = section.getInt("default-amount");
	        randomizeStackSize = section.getBoolean("randomize-stacksize");
	        useItemsMultipleTimes = section.getBoolean("can-use-same-items");
	        removeLootAfterSeconds = section.getInt("remove-after");
	        dropLootOnRemoval = section.getBoolean("drop-on-removal");
	        instantLoot = section.getBoolean("give-instantly");

            cmdEnabled = section.getBoolean("commands.enabled");
            cmdNeedOp = section.getBoolean("commands.needop");
            cmdIngame = section.getBoolean("commands.changeingame");
	    }
	}
	
	public class Koth {
	    private @Getter int knockTime = 0;
	    private @Getter boolean removeChestAtStart = true;
	    private @Getter int minimumPlayersNeeded = 0;
        private @Getter String defaultCaptureType = "Player";
	    
	    public Koth(ConfigurationSection section){
	        removeChestAtStart = section.getBoolean("remove-chest-at-start");
	        knockTime = section.getInt("knockTime");
	        minimumPlayersNeeded = section.getInt("minimum-players");
	        defaultCaptureType = section.getString("default-capturetype");
	        new MapRotation(section.getStringList("map-rotation"));
	    }
	}
	
}
