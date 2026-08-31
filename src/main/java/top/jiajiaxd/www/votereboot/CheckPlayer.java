package top.jiajiaxd.www.votereboot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class CheckPlayer implements org.bukkit.event.Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        VoteReboot.semap.put(name, 0);
        VoteReboot.isGuaji.put(name, false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void PlayerMoving(PlayerMoveEvent event) {
        String name = event.getPlayer().getName();
        if (Boolean.TRUE.equals(VoteReboot.isGuaji.get(name))) {
            if (VoteReboot.notice) VoteReboot.sendGlobalMessage(name + "回来了");
            VoteReboot.isGuaji.put(name, false);
        }
        VoteReboot.semap.put(name, 0);
    }

    @EventHandler
    public void playerchat(AsyncPlayerChatEvent event) {
        String name = event.getPlayer().getName();
        VoteReboot.semap.put(name, 0);
        if (Boolean.TRUE.equals(VoteReboot.isGuaji.get(name))) {
            VoteReboot.isGuaji.put(name, false);
            if (VoteReboot.notice) VoteReboot.sendGlobalMessage(name + "回来了");
        }
    }

    @EventHandler
    public void playerquit(PlayerQuitEvent event) {
        String name = event.getPlayer().getName();
        VoteReboot.semap.remove(name);
        VoteReboot.isGuaji.remove(name);
    }
}
