package org.atmosia.simpleirc;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
	public static boolean autoconnect = true;
	
	public static final String MOD_ID = "simpleirc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static org.atmosia.simpleirc.SimpleIRCConfig settings = org.atmosia.simpleirc.SimpleIRCConfig.createAndLoad();
	
    @Override
    public void onInitialize() {    	

    }	
}
