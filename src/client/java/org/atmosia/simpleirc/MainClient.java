package org.atmosia.simpleirc;

import org.lwjgl.glfw.GLFW;

import me.lortseam.completeconfig.gui.ConfigScreenBuilder;
import me.lortseam.completeconfig.gui.cloth.ClothConfigScreenBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.atmosia.simpleirc.ModMenuIntegration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class MainClient implements ClientModInitializer {	
	public static IRCHandler irc = null;

	private static KeyBinding JBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
    	    "key.simpleirc.config", // The translation key of the keybinding's name
    	    InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
    	    GLFW.GLFW_KEY_J, // The keycode of the key
    	    "category.simpleirc.main" // The translation key of the keybinding's category.
    	));
	
	private static KeyBinding KBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
    	    "key.simpleirc.toggleconnect", // translation key
    	    InputUtil.Type.KEYSYM, // type
    	    GLFW.GLFW_KEY_K, // keycode
    	    "category.simpleirc.main" // category key
    	));
	
	
	@Override
	public void onInitializeClient() {
		/*if (FabricLoader.getInstance().isModLoaded("cloth-config2")) 
        {
           ConfigScreenBuilder.setMain(Main.MOD_ID, new ClothConfigScreenBuilder());
        }*/
        
    	
              
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (JBind.wasPressed()) 
            {
        		MinecraftClient instance = MinecraftClient.getInstance();
				ModMenuIntegration integration = new ModMenuIntegration();
    			Screen configScreen = integration.getModConfigScreenFactory().create(instance.currentScreen);
        		
        		instance.setScreen(configScreen);
            }
            
            while(KBind.wasPressed())
            {
            	if(irc==null || !irc.isOpen())
            	{
            		ChatUtils.sudomessage("@connect");
            	}
            	else
            	{
            		ChatUtils.sudomessage("@disconnect");
            	}
            }
        });
		
	}

}
