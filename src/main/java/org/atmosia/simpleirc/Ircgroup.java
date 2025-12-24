package org.atmosia.simpleirc;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;

import java.util.ArrayList;
import java.util.List;

@Modmenu(modId = "simpleirc")
@Config(name = "simple-irc", wrapperName = "SimpleIRCConfig")
public class Ircgroup {
	public String ip = "";
    public String backupnick = "";
    public Integer port = 6697;
    public boolean autoconnect;
    public IrcVerbosity verbosity = IrcVerbosity.NORMAL;
    public List<String> postConnectionCommands = new ArrayList<String>();

}
