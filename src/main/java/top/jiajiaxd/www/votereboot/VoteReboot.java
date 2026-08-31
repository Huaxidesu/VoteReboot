package top.jiajiaxd.www.votereboot;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VoteReboot extends JavaPlugin {
    public static volatile boolean isVoting;
    public static Plugin me;
    public static Map<String, Integer> semap = new ConcurrentHashMap<>();
    public static Map<String, Boolean> isGuaji = new ConcurrentHashMap<>();
    public static String prefix;
    public static Map<String, Boolean> IPMap = new ConcurrentHashMap<>();
    public static Boolean ipcheck;
    public static AtomicBoolean isRebooting = new AtomicBoolean(false);
    public static AtomicInteger rs = new AtomicInteger(0);
    public static volatile int vs = 3;
    public static boolean notice;
    public static String version = "Release-2.3";
    public static boolean update = false;
    public static String updatelog;
    public static Set<String> votedPlayers = new HashSet<>();
    public static BukkitTask afkTask;
    public static BukkitTask voteTask;
    public static BukkitTask rebootTask;
    Metrics metrics = new Metrics(this, 7670);

    @Override
    public void onEnable() {
        isVoting = false;
        me = this;
        votedPlayers.clear();
        isGuaji.replaceAll((k, v) -> false);
        sendCMessage("欢迎使用VoteReboot " + version);
        getCommand("votereboot").setTabCompleter(this);
        if (!getDataFolder().exists()) {
            boolean mkdirs = getDataFolder().mkdir();
            if (!mkdirs) {
                sendCMessage("无法创建配置文件夹，可能是因为权限问题！");
                sendCMessage("因无法创建配置文件夹，插件正在关闭");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!(file.exists())) {
            sendCMessage("没有检测到配置文件！正在创建...");
            saveDefaultConfig();
            sendCMessage("创建配置文件完毕！");
        }
        reloadConfig();
        if (!getCS("EnablePlugin").equals("true")) {
            sendCMessage("插件已经关闭。如果需要启动，请在config.yml内将EnablePlugin设置为true。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        /*
        if(getCS("checkupdate").equals("true")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Boolean ok=false;
                    String nr;
                    sendCMessage("正在检查更新...");
                    nr=Internet.Get("https://open.jiajiaxd.top/vr/"+version+".html");
                    if(!nr.equals("null")){
                        if(nr.equals("bug")) {
                            ok=true;
                            sendCMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                            sendCMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                            sendCMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                            sendCMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                            sendCMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                            sendCMessage("为了防止出现问题，插件自动停用。");
                            Bukkit.getPluginManager().disablePlugin(me);
                        }
                        if(nr.equals("latest")){
                            ok=true;
                            sendCMessage("插件已经是最新版本");
                        }
                        if(nr.equals("notlatest")){
                            ok=true;
                            updatelog=Internet.Get("https://open.jiajiaxd.top/vr/uplog.html");
                            sendCMessage("插件不是最新版本！本次新版更新日志：");
                            sendCMessage(updatelog);
                            update=true;
                        }
                        if(!ok) sendCMessage("在检查更新时出现了错误！代码：121");
                    }else sendCMessage("检查更新失败！请检查是否有安全插件拦截了网络（如Yum）");
                }
            }.runTaskAsynchronously(VoteReboot.me);
        }
        */
        Bukkit.getPluginManager().registerEvents(new Notice(), this);
        prefix = getCS("prefix");
        if (getCS("IPCheck").equals("true")) ipcheck = true;
        else ipcheck = false;
        rs = new AtomicInteger(Integer.parseInt(getCS("rs")));
        if (getCS("checkplayer").equals("true")) {
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                semap.put(p.getName(), 0);
                isGuaji.put(p.getName(), false);
            }
            Bukkit.getPluginManager().registerEvents(new CheckPlayer(), this);
            sendCMessage("已经启用挂机玩家检测");
            if (getCS("notice").equals("true")) notice = true;
            else notice = false;
            startAfkTask();
        }
    }

    @Override
    public void onDisable() {
        stopAfkTask();
        stopVoteTimer();
        stopRebootTimer();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equals("votereboot")) {
            String[] subCommands = {"reload", "now", "bug"};
            if (args.length > 1) return new ArrayList<>();
            if (args.length == 0) return Arrays.asList(subCommands);
            return Arrays.stream(subCommands).filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        } else return new ArrayList<>();
    }

    public static void sendCMessage(String msg) {
        Bukkit.getLogger().info("\u001B[0;33;22m[\u001B[0;36;1mVoteReboot\u001B[0;33;22m] \u001B[0;32;1m" + msg + "\u001B[m");
    }

    public String getCS(String Path) {
        if (!getConfig().contains(Path)) {
            sendCMessage("检测到配置文件损坏，正在重置配置文件...");
            saveResource("config.yml", true);
            reloadConfig();
            sendCMessage("已经重置配置文件。");
        }
        String value = getConfig().getString(Path);
        return value == null ? "null" : value;
    }

    public static void sendWrongMessage(CommandSender sender, String Message) {
        sender.sendMessage(prefix + " §c" + Message);
    }

    public static void sendGlobalMessage(String Message) {
        final String msg = prefix + " §a" + Message;
        if (Bukkit.isPrimaryThread()) {
            Bukkit.broadcastMessage(msg);
        } else {
            Bukkit.getScheduler().runTask(me, () -> Bukkit.broadcastMessage(msg));
        }
    }

    public static void sendPlayerMessage(CommandSender sender, String Message) {
        sender.sendMessage(prefix + " §a" + Message);
    }

    public Integer getNeedPlayers() {
        int gjNum = 0;
        int NeedPlayers = 0;
        if (getCS("checkplayer").equals("true")) {
            for (String key : isGuaji.keySet()) {
                if (Boolean.TRUE.equals(isGuaji.get(key))) gjNum++;
            }
        }
        int OnlinePlayers = Bukkit.getOnlinePlayers().size() - gjNum;
        double rate = Double.parseDouble(getCS("rate"));
        NeedPlayers = (int) (OnlinePlayers * rate + 0.5);
        return NeedPlayers;
    }

    private static String getPlayerIP(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) return null;
        return String.valueOf(address.getAddress());
    }

    public void addVotedPlayer(CommandSender player) {
        String PlayerName = player.getName();
        String IP = getPlayerIP((Player) player);
        boolean havevoted = votedPlayers.contains(PlayerName);
        if (havevoted) {
            sendWrongMessage(player, "你已经投过票了！");
        } else if (ipcheck && IP != null && Boolean.TRUE.equals(IPMap.get(IP))) {
            sendWrongMessage(player, "此IP已经有玩家投过票了，请勿重复投票！");
            havevoted = true;
        }
        if (!havevoted) {
            if (IP != null) IPMap.put(IP, true);
            votedPlayers.add(PlayerName);
            int OnlinePlayers = Bukkit.getOnlinePlayers().size();
            int NeedPlayers = getNeedPlayers();
            if (!getCS("checkplayer").equals("true"))
                sendGlobalMessage("§e" + PlayerName + " §a进行了投票 本服共§e" + OnlinePlayers + "§a人在线，共需要§e" + NeedPlayers + "§a人投票 目前票数：§e" + votedPlayers.size());
            else
                sendGlobalMessage("§e" + PlayerName + " §a进行了投票 本服共§e" + OnlinePlayers + "§a人在线，共需要§e" + NeedPlayers + "§a人投票（不包含正在挂机的玩家） 目前票数：§e" + votedPlayers.size());
        }
        if (votedPlayers.size() >= getNeedPlayers()) {
            sendGlobalMessage("投票已经完成！服务器将在" + rs.get() + "秒后重启！");
            isVoting = false;
            stopVoteTimer();
            startRebootCountdown();
        }
    }

    public void startRebootCountdown() {
        stopRebootTimer();
        isRebooting.set(true);
        rebootTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRebooting.get()) {
                    this.cancel();
                    return;
                }
                if (rs.get() <= 0) {
                    this.cancel();
                    rebootTask = null;
                    // One-shot guard: reboot exactly once even if runs overlap under lag
                    if (isRebooting.compareAndSet(true, false)) {
                        sendGlobalMessage("正在重启...");
                        reboot();
                    }
                    return;
                }
                sendGlobalMessage("服务器将在" + rs.getAndDecrement() + "秒后重启！");
            }
        }.runTaskTimerAsynchronously(this, 0L, 20L);
    }

    public static void stopRebootTimer() {
        isRebooting.set(false);
        if (rebootTask != null) {
            rebootTask.cancel();
            rebootTask = null;
        }
    }

    public static void stopVoteTimer() {
        if (voteTask != null) {
            voteTask.cancel();
            voteTask = null;
        }
    }

    private void startAfkTask() {
        stopAfkTask();
        final int seconds = Integer.parseInt(getCS("s"));
        afkTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (String key : semap.keySet()) {
                    int value = semap.getOrDefault(key, 0) + 1;
                    semap.put(key, value);
                    if (value > seconds && !Boolean.TRUE.equals(isGuaji.get(key))) {
                        isGuaji.put(key, true);
                        if (notice) sendGlobalMessage(key + "暂时离开了");
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 0L, 20L);
    }

    public static void stopAfkTask() {
        if (afkTask != null) {
            afkTask.cancel();
            afkTask = null;
        }
    }

    public void reboot() {
        metrics.addCustomChart(new Metrics.SimplePie("number_of_restarts", () -> "1"));
        if (getCS("save").equals("true")) {
            final String saveCommand = getCS("savec");
            Bukkit.getScheduler().runTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), saveCommand));
        }
        if (getCS("way").equals("1")) {
            final String command = getCS("cc");
            Bukkit.getScheduler().runTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        } else {
            final String command = getCS("ccwl");
            sendCMessage("正在尝试重启...");
            sendCMessage("正在执行命令" + command);
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    sendCMessage("返回结果：" + CommandUtil.run(command));
                } catch (IOException e) {
                    sendCMessage("重启命令执行失败！");
                    e.printStackTrace();
                }
            });
        }
    }

    public void cancel() {
        isVoting = false;
        stopVoteTimer();
        stopRebootTimer();
        votedPlayers.clear();
        IPMap.clear();
    }

    public void reload() {
        cancel();
        semap.clear();
        isGuaji.clear();
        reloadConfig();
        if (!getCS("EnablePlugin").equals("true")) {
            sendCMessage("插件已经关闭。如果需要启动，请在config.yml内将EnablePlugin设置为true。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        prefix = getCS("prefix");
        if (getCS("IPCheck").equals("true")) ipcheck = true;
        else ipcheck = false;
        rs = new AtomicInteger(Integer.parseInt(getCS("rs")));
        if (getCS("notice").equals("true")) notice = true;
        else notice = false;
        if (getCS("checkplayer").equals("true")) {
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                semap.put(p.getName(), 0);
                isGuaji.put(p.getName(), false);
            }
            startAfkTask();
        } else {
            stopAfkTask();
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean a = false;
        if (label.equalsIgnoreCase("vote")) {
            a = true;
            if (!(sender instanceof Player)) {
                sendCMessage("控制台无法使用此命令");
            } else {
                if (!isVoting) {
                    int OnlinePlayers = Bukkit.getOnlinePlayers().size();
                    if (getNeedPlayers() < 2) {
                        sendCMessage("need" + getNeedPlayers());
                        if (getCS("checkplayer").equals("true")) sendWrongMessage(sender, "需要至少三名不在挂机的在线玩家才可以发起重启投票");
                        else sendWrongMessage(sender, "需要至少三名在线玩家才可以发起重启投票");
                    } else {
                        isVoting = true;
                        vs = 3;
                        votedPlayers.clear();
                        votedPlayers.add(sender.getName());
                        IPMap.clear();
                        String IP = getPlayerIP((Player) sender);
                        if (IP != null) IPMap.put(IP, true);
                        sendGlobalMessage("§e" + sender.getName() + " §a发起了投票重启 本服共§e" + OnlinePlayers + "§a人在线，需要§e" + getNeedPlayers() + "§a人投票 目前票数：§e1");
                        sendGlobalMessage("请同意重启的玩家输入/voteaccept");
                        sendGlobalMessage("本次投票在3分钟内有效");
                        stopVoteTimer();
                        voteTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!isVoting) {
                                    this.cancel();
                                    return;
                                }
                                vs--;
                                if (vs <= 0) {
                                    sendGlobalMessage("本次重启投票已经过期");
                                    isVoting = false;
                                    VoteReboot.this.cancel();
                                    this.cancel();
                                } else {
                                    sendGlobalMessage("投票将在" + vs + "分钟后结束！");
                                }
                            }
                        }.runTaskTimerAsynchronously(this, 1200L, 1200L);
                    }
                } else {
                    sendWrongMessage(sender, "已经有一个投票正在进行中，请勿重复发起投票！");
                    sendWrongMessage(sender, "若需要同意重启，请输入/voteaccept");
                }
            }
        }
        if (label.equalsIgnoreCase("voteaccept")) {
            a = true;
            if (!(sender instanceof Player)) sendCMessage("控制台无法使用此命令");
            else {
                if (isVoting) addVotedPlayer(sender);
                else sendWrongMessage(sender, "当前没有正在进行的投票！请输入/vote发起一个重启投票");
            }
        }
        if (label.equalsIgnoreCase("votecancel")) {
            a = true;
            boolean wasVoting = isVoting;
            boolean wasRebooting = isRebooting.get();
            cancel();
            sendPlayerMessage(sender, "已经取消当前所有操作");
            if (wasVoting) sendGlobalMessage("本次投票被管理员结束！");
            if (wasRebooting) sendGlobalMessage("管理员取消了本次重启！");
        }
        if (label.equalsIgnoreCase("votereboot")) {
            if (args.length > 0 && sender.isOp()) {
                if (args[0].equals("bug")) {
                    a = true;
                    sender.sendMessage("§b若插件出现BUG或你想给插件提出建议，请前往https://github.com/jiajiaxd/VRissues/issues进行反馈");
                    sender.sendMessage("§b或在MCBBS回复插件所在帖子");
                }
                if (args[0].equals("reload")) {
                    a = true;
                    sendPlayerMessage(sender, "正在重载，请稍后...");
                    reload();
                    sendPlayerMessage(sender, "重载完毕");
                }
                if (args[0].equals("now")) {
                    a = true;
                    stopRebootTimer();
                    reboot();
                }
            }
            if (args.length > 0 && !sender.isOp()) {
                sendWrongMessage(sender, "仅服务器管理员可以使用此命令！");
                a = true;
            }
            if (!a) {
                a = true;
                sender.sendMessage("§b----------§eVoteReboot 菜单§b----------");
                sender.sendMessage("§3/vote 发起一次重启投票");
                sender.sendMessage("§3/voteaccept 投票");
                sender.sendMessage("§3/votereboot reload 重载插件");
                sender.sendMessage("§3/votereboot now 立刻重启");
                sender.sendMessage("§3/votereboot bug 反馈BUG");
                sender.sendMessage("§3插件作者：甲甲");
                sender.sendMessage("§3若出现任何BUG");
                sender.sendMessage("§3请输入/votereboot bug");
                sender.sendMessage("§b----------§eVoteReboot 菜单§b----------");
            }
        }
        return a;
    }


}
