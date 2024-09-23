package nl.aurorion.blockregen.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@UtilityClass
public class ThreadUtil {

    public void synchronize(JavaPlugin plugin, Runnable runnable) {
        // Already running on the primary thread, nothing to do.
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            // Synchronize to the main thread.
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }
}
