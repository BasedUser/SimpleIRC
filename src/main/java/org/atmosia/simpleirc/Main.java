package org.atmosia.simpleirc;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

public class Main implements ModInitializer {
	
	
	public static boolean autoconnect = true;
	
	public static final String MOD_ID = "simpleirc";
		
	private static Ircgroup settings;
	
	public static Ircgroup getSettings() 
	{
        return settings;
    }
	
    @Override
    public void onInitialize() {    	
		AutoConfig.register(Ircgroup.class, JanksonConfigSerializer::new);
    	System.out.println("Simple IRC has started.");

    	settings = AutoConfig.getConfigHolder(Ircgroup.class).getConfig();
        
    }	
}
