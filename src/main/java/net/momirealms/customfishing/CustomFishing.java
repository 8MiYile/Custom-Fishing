/*
 *  Copyright (C) <2022> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.momirealms.customfishing.command.Execute;
import net.momirealms.customfishing.command.TabComplete;
import net.momirealms.customfishing.competition.CompetitionSchedule;
import net.momirealms.customfishing.competition.bossbar.BossBarManager;
import net.momirealms.customfishing.helper.LibraryLoader;
import net.momirealms.customfishing.hook.Placeholders;
import net.momirealms.customfishing.listener.*;
import net.momirealms.customfishing.utils.AdventureUtil;
import net.momirealms.customfishing.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CustomFishing extends JavaPlugin {

    public static JavaPlugin instance;
    public static BukkitAudiences adventure;
    public static MiniMessage miniMessage;
    private CompetitionSchedule competitionSchedule;
    public static Placeholders placeholders;

    @Override
    public void onLoad(){
        instance = this;
        LibraryLoader.load("redis.clients","jedis","4.2.3","https://repo.maven.apache.org/maven2/");
        LibraryLoader.load("org.apache.commons","commons-pool2","2.11.1","https://repo.maven.apache.org/maven2/");
        LibraryLoader.load("dev.dejvokep","boosted-yaml","1.3","https://repo.maven.apache.org/maven2/");
    }

    @Override
    public void onEnable() {
        adventure = BukkitAudiences.create(this);
        miniMessage = MiniMessage.miniMessage();
        Objects.requireNonNull(Bukkit.getPluginCommand("customfishing")).setExecutor(new Execute());
        Objects.requireNonNull(Bukkit.getPluginCommand("customfishing")).setTabCompleter(new TabComplete());
        Bukkit.getPluginManager().registerEvents(new FishListener(),this);

        AdventureUtil.consoleMessage("[CustomFishing] Running on <white>" + Bukkit.getVersion());

        Bukkit.getScheduler().runTaskAsynchronously(this, ()-> {

            ConfigReader.Reload();
            if (ConfigReader.Config.competition){
                competitionSchedule = new CompetitionSchedule();
                competitionSchedule.checkTime();
                Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.getPluginManager().registerEvents(new BossBarManager(), this);
                });
            }
            if (ConfigReader.Config.convertMMOItems){
                Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.getPluginManager().registerEvents(new MMOItemsConverter(), this);
                });
            }
            if (ConfigReader.Config.papi){
                placeholders = new Placeholders();
                Bukkit.getScheduler().runTask(this, () -> {
                    placeholders.register();
                    Bukkit.getPluginManager().registerEvents(new PapiUnregister(), this);
                });
            }
            if (ConfigReader.Config.preventPick){
                Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.getPluginManager().registerEvents(new PickUpListener(),this);
                });
            }
            if (!Objects.equals(ConfigReader.Config.version, "7")){
                ConfigUtil.update();
            }
            ConfigReader.tryEnableJedis();
            AdventureUtil.consoleMessage("<gradient:#0070B3:#A0EACF>[CustomFishing] </gradient><color:#E1FFFF>Plugin Enabled!");
        });
    }

    @Override
    public void onDisable() {
        if (competitionSchedule != null){
            competitionSchedule.stopCheck();
            competitionSchedule = null;
        }
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        if (instance != null){
            instance = null;
        }
    }
}
