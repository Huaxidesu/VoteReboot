package top.jiajiaxd.www.votereboot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class Notice implements org.bukkit.event.Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().isOp() && VoteReboot.update) {
            event.getPlayer().sendMessage("§b[VoteReboot]当前版本不是最新版本，请前往https://open.jiajiaxd.top/vr/进行更新！");
        }
    }
}
