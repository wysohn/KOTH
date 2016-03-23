package subside.plugins.koth.adapter;

import java.util.Random;

import lombok.Getter;
import lombok.Setter;
import subside.plugins.koth.ConfigHandler;
import subside.plugins.koth.exceptions.KothNotExistException;

public class StartParams {
        private @Getter @Setter Koth koth;
        private @Getter @Setter String gamemode = "classic";
        private @Getter @Setter int captureTime = 15*60;
        private @Getter @Setter int maxRunTime = -1;
        private @Getter @Setter int lootAmount = ConfigHandler.getCfgHandler().getLoot().getLootAmount();
        private @Getter @Setter String lootChest = null;
        private @Getter @Setter boolean isScheduled = false;
        
        public StartParams(String kth){
            if (kth.equalsIgnoreCase("random")) {
                if (KothHandler.getInstance().getAvailableKoths().size() > 0) {
                    kth = KothHandler.getInstance().getAvailableKoths().get(new Random().nextInt(KothHandler.getInstance().getAvailableKoths().size())).getName();
                }
            }

            for (Koth koth : KothHandler.getInstance().getAvailableKoths()) {
                if (koth.getName().equalsIgnoreCase(kth)) {
                    this.koth = koth;
                    return;
                }
            }
            throw new KothNotExistException(kth);
        }
    }