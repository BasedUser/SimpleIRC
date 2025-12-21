package org.atmosia.simpleirc;

import org.atmosia.simpleirc.commands.SimpleIrcCommand;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.Objects;

public class MainClient implements ClientModInitializer {	
	public static IRCNetwork irc = null;
    public static Boolean DefaultToMinecraftChat = false;
    public static Character MinecraftChatPrefix = '!';

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
            	if(irc==null || !irc.isConnected())
            	{
            		Connect();
            	}
            	else
            	{
            		Disconnect("Disconnected via command");
            	}
            }
        });
        SimpleIrcCommand.Register();
	}
    public static void Connect() {
        MainClient.irc = new IRCNetwork(Main.getSettings().ip, Main.getSettings().port, ChatUtils.getUsername(),
                Main.getSettings().backupnick, Main.getSettings().verbosity);
        try {
            MainClient.irc.Connect();
        } catch (IllegalStateException e) {
            ChatUtils.Error("Failed to connect to IRC:");
            ChatUtils.Error(e);
        }
    }
    public static void Disconnect(String reason) {
        if (Objects.equals(reason, "")) {
            reason = "Leaving";
        }
        MainClient.irc.Disconnect(reason);
    }
}
