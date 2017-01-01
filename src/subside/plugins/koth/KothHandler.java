package subside.plugins.koth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.collect.Lists;

import lombok.Getter;
import subside.plugins.koth.areas.Area;
import subside.plugins.koth.areas.Koth;
import subside.plugins.koth.events.KothInitializeEvent;
import subside.plugins.koth.events.KothPostUpdateEvent;
import subside.plugins.koth.events.KothPreUpdateEvent;
import subside.plugins.koth.events.KothStartEvent;
import subside.plugins.koth.exceptions.AnotherKothAlreadyRunningException;
import subside.plugins.koth.exceptions.KothAlreadyExistException;
import subside.plugins.koth.exceptions.KothAlreadyRunningException;
import subside.plugins.koth.exceptions.KothNotExistException;
import subside.plugins.koth.gamemodes.RunningKoth;
import subside.plugins.koth.gamemodes.RunningKoth.EndReason;
import subside.plugins.koth.gamemodes.StartParams;
import subside.plugins.koth.hooks.HookManager;
import subside.plugins.koth.loaders.JSONLoader;
import subside.plugins.koth.loaders.KothLoader;
import subside.plugins.koth.loot.Loot;
import subside.plugins.koth.scheduler.Schedule;
import subside.plugins.koth.scheduler.ScheduleHandler;
import subside.plugins.koth.utils.MessageBuilder;

/**
 * @author Thomas "SubSide" van den Bulk
 *
 */
public class KothHandler extends AbstractModule implements Runnable {
    private @Getter List<RunningKoth> runningKoths;
    private @Getter List<Koth> availableKoths;
    
    private @Getter int taskId;
    
    public KothHandler(KothPlugin plugin){
        super(plugin);
        
        runningKoths = new ArrayList<>();
        availableKoths = new ArrayList<>();
    }
    
    @Override
    public void onLoad(){
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void onEnable(){
        loadKoths(); // Load all KoTH's
        
        // Add a repeating ASYNC scheduler for the KothHandler
        this.taskId = Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, this, 20, 20);
    }
    
    @Override
    public void onDisable(){
        // Remove all previous schedulings
        Bukkit.getScheduler().cancelTask(this.taskId);
        
        saveKoths(); // Save all KoTH's
    }

    @Override
    public void run() {
        synchronized (runningKoths) {
            Iterator<RunningKoth> it = runningKoths.iterator();
            while (it.hasNext()) {
                // Call an PreUpdateEvent, this can be cancelled (For whichever reason)
                KothPreUpdateEvent preEvent = new KothPreUpdateEvent(it.next());
                plugin.getServer().getPluginManager().callEvent(preEvent);
                if(!preEvent.isCancelled()){
                    preEvent.getRunningKoth().update();
                    
                    // If the preEvent is not cancelled call postUpdateEvent, this cannot be cancelled as there is nothing to cancel.
                    plugin.getServer().getPluginManager().callEvent(new KothPostUpdateEvent(preEvent.getRunningKoth()));
                }
            }
            ScheduleHandler.getInstance().tick();
            HookManager.getHookManager().tick();
        }
    }

    /** Gets a the currently running KoTH
     * 
     * @return the currently running KoTH, null if none is running.
     */
    public RunningKoth getRunningKoth() {
        synchronized (runningKoths) {
            if (runningKoths.size() > 0) {
                return runningKoths.get(0);
            } else {
                return null;
            }
        }
    }
    
    /** Remove a RunningKoth from runningKoths list
     * 
     * @param koth the runningKoth object
     */
    public void removeRunningKoth(RunningKoth runningKoth){
        synchronized (runningKoths) {
            runningKoths.remove(runningKoth);
        }
    }
    /** Add a runningKoth to the runningKoths list
     * 
     * @param rKoth
     */
    public void addRunningKoth(RunningKoth runningKoth){
        KothInitializeEvent event = new KothInitializeEvent(runningKoth);
        Bukkit.getServer().getPluginManager().callEvent(event);
        
        runningKoths.add(runningKoth);
    }
    
    public void startKoth(Schedule schedule){
        StartParams params = new StartParams(schedule.getKoth());
        params.setCaptureTime(schedule.getCaptureTime()*60);
        params.setMaxRunTime(schedule.getMaxRunTime());
        params.setLootAmount(schedule.getLootAmount());
        params.setLootChest(schedule.getLootChest());
        params.setEntityType(schedule.getEntityType());
        params.setScheduled(true);
        
        startKoth(params);
    }
    

    /** Start a certain KoTH
     * 
     * @param koth              The KoTH to run
     * @param captureTime       The captureTime
     * @param maxRunTime        The maximum time this KoTH can run (-1 for unlimited time)
     * @param lootAmount        The amount of loot that should spawn (-1 for default config settings)
     * @param lootChest         The lootchest it should use (null for default config settings)
     * @param entityType        The entity type that should be able to cap the KoTH (Players, Factions etc.)
     * @param isScheduled       This is used to see if it should obey stuff like minimumPlayers
     */
    public void startKoth(StartParams params) {
        synchronized (runningKoths) {
            for (RunningKoth rKoth : runningKoths) {
                if (rKoth.getKoth() == params.getKoth()) {
                    throw new KothAlreadyRunningException(params.getKoth().getName());
                }
            }
            KothStartEvent event = new KothStartEvent(params.getKoth(), params.getCaptureTime(), params.getMaxRunTime(), params.isScheduled(), params.getEntityType());
            
            boolean anotherAlreadyRunning = false;
            if(this.getRunningKoth() != null && !ConfigHandler.getInstance().getGlobal().isMultipleKothsAtOnce()){
                event.setCancelled(true);
                anotherAlreadyRunning = true;
            }
            
            boolean minimumNotMet = false;
            if (params.isScheduled() && Lists.newArrayList(Bukkit.getOnlinePlayers()).size() < ConfigHandler.getInstance().getKoth().getMinimumPlayersNeeded()) {
                event.setCancelled(true);
                minimumNotMet = true;
            }

            Bukkit.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                RunningKoth rKoth = this.getGamemodeRegistry().createGame(params.getGamemode());
                rKoth.init(params);
                addRunningKoth(rKoth);
            } else if(anotherAlreadyRunning) {
                throw new AnotherKothAlreadyRunningException();
            }else if(minimumNotMet){
                new MessageBuilder(Lang.KOTH_PLAYING_MINIMUM_PLAYERS_NOT_MET).buildAndBroadcast();
            }
        }

    }


    /** Create a new KoTH
     * 
     * @param name              The KoTH name
     * @param min               The first location
     * @param max               The max position
     * @throws                  KothAlreadyExistException 
     */
    public void createKoth(String name, Location min, Location max) throws KothAlreadyExistException {
        if (getKoth(name) == null && !name.equalsIgnoreCase("random")) {
            Koth koth = new Koth(this, name);
            koth.getAreas().add(new Area(name, min, max));
            availableKoths.add(koth);
            KothLoader.save();
        } else {
            throw new KothAlreadyExistException(name);
        }
    }


    /** Remove a certain KoTH
     * 
     * @param koth              The KoTH to remove
     * @throws                  KothNotExistException 
     */
    public void removeKoth(String name) throws KothNotExistException {
        Koth koth = getKoth(name);
        if (koth == null) {
            throw new KothNotExistException(name);
        }

        availableKoths.remove(koth);
        KothLoader.save();
    }


    /** Get a loot by name
     * 
     * @param name      The name of the loot chest
     * @return          The loot object
     */
    public Loot getLoot(String name){
        if(name == null) return null;
        for(Loot loot : loots){
            if(loot.getName().equalsIgnoreCase(name)){
                return loot;
            }
        }
        return null;
    }

    /** Get a KoTH by name
     * 
     * @param name      The name of the KoTH
     * @return          The KoTH object
     */
    public Koth getKoth(String name) {
        for (Koth koth : availableKoths) {
            if (koth.getName().equalsIgnoreCase(name)) {
                return koth;
            }
        }
        return null;
    }


    /** Gracefully ends all running KoTHs
     * 
     */
    public void endAllKoths() {
        synchronized (runningKoths) {
            Iterator<RunningKoth> it = runningKoths.iterator();
            while (it.hasNext()) {
                it.next().endKoth(EndReason.GRACEFUL);
            }
        }
    }

    /** Gracefully ends a certain KoTH
     * 
     * @param name      The name of the KoTH to end
     * @throws          KothNotExistException 
     */
    public void endKoth(String name) throws KothNotExistException {
        synchronized (runningKoths) {
            Iterator<RunningKoth> it = runningKoths.iterator();
            while (it.hasNext()) {
                RunningKoth koth = it.next();
                if (koth.getKoth().getName().equalsIgnoreCase(name)) {
                    koth.endKoth(EndReason.GRACEFUL);
                    return;
                }
            }
            throw new KothNotExistException(name);
        }
    }
    
    /** Stop a specific koth
     * 
     * @param name      Stop a KoTH by a certain name
     */
    public void stopKoth(String name) {
        Iterator<RunningKoth> it = runningKoths.iterator();
        while (it.hasNext()) {
            RunningKoth koth = it.next();
            if (koth.getKoth().getName().equalsIgnoreCase(name)) {
                koth.endKoth(EndReason.FORCED);
            }
        }
    }

    /** Stop all running koths
     * 
     */
    public void stopAllKoths() {
        synchronized (runningKoths) {
            Iterator<RunningKoth> it = runningKoths.iterator();
            while (it.hasNext()) {
                it.next().endKoth(EndReason.FORCED);
            }
        }
    }
    
    /* Save/Load time */
    public void loadKoths() {
        availableKoths = new ArrayList<>();
        
        Object obj = new JSONLoader(plugin, "koths.json").load();
        if(obj == null)
            return;
        
        if(obj instanceof JSONArray){
            JSONArray koths = (JSONArray) obj;
            
            Iterator<?> it = koths.iterator();
            while(it.hasNext()){
                try {
                    Koth koth = new Koth(this, null);
                    koth.load((JSONObject)it.next());
                    availableKoths.add(koth);
                } catch(Exception e){
                    plugin.getLogger().log(Level.SEVERE, "////////////////\nError loading koth!\n////////////////", e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void saveKoths() {
        JSONArray obj = new JSONArray();
        for (Koth koth : availableKoths) {
            obj.add(koth.save());
        }
        new JSONLoader(plugin, "koths.json").save(obj);
    }

}
