package org.atmosia.simpleirc;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "simpleirc")
public class Ircgroup implements ConfigData {
	private static String ip = "";
    private static String backupnick = "";
    private static String channel = "";
    private static String password = "";
    private static Integer port = 6697;
    @ConfigEntry.Gui.RequiresRestart(value = true)
    private static boolean autoconnect;

	public static String getIp() {
		return ip;
	}
	public static String getBackupnick() {
		return backupnick;
	}
	public static String getChannel() {
		return channel;
	}
	public static String getPassword() {
		return password;
	}
	public static Integer getPort() {
		return port;
	}
	public static boolean isAutoconnect() {
		return autoconnect;
	}    
}
