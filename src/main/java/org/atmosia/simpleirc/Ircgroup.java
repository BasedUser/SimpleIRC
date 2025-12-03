package org.atmosia.simpleirc;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "simpleirc")
public class Ircgroup implements ConfigData {
	public String ip = "";
    public String backupnick = "";
    public String channel = "";
    public String password = "";
    public Integer port = 6697;
    @ConfigEntry.Gui.RequiresRestart()
    public boolean autoconnect;
}
