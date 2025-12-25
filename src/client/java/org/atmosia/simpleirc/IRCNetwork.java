package org.atmosia.simpleirc;

import net.kyori.adventure.text.event.ClickEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class IRCNetwork {
    private String Ip;
    private Integer Port;
    private String Nickname;
    private String BackupNickname;
    private Boolean UseBackupNickname = false;
    public IrcVerbosity Verbosity;
    private List<IRCChannel> Channels;
    private String PrimaryChannel;
    private boolean IsConnected;
    private SSLSocket Socket;
    private BufferedReader Reader;
    private BufferedWriter Writer;
    private final AtomicBoolean initialConnectionFinished;
    private final AtomicBoolean registeredSasl;
    public Boolean IsAway;
    public IRCNetwork(String ip, Integer port, String nickname, String backupNickname, IrcVerbosity verbosity) {
        Ip = ip;
        Port = port;
        Nickname = nickname;
        BackupNickname = backupNickname;
        Verbosity = verbosity;
        Channels = new ArrayList<>();
        IsConnected = false;
        initialConnectionFinished = new AtomicBoolean(false);
        registeredSasl = new AtomicBoolean(false);
    }
    public void Connect() {
        if (isConnected()) {
            ChatUtils.Info("Already connected, doing nothing");
            return;
        }
        CallbackInfo callbackInfo = new CallbackInfo("IRC connection", true);
        try {
            new Thread(() ->
            {

                VerifyFields(callbackInfo);
                if (callbackInfo.isCancelled()) return;
                ChatUtils.Debug("Verified fields for connection");
                OpenSocket(callbackInfo);
                if (callbackInfo.isCancelled()) return;
                ChatUtils.Debug("Opened socket for connection");
                Listener();
                ChatUtils.Debug("Started listener");
                Register();
                ChatUtils.Debug("Registered user");
                synchronized (initialConnectionFinished) {
                    try {
                        ChatUtils.Info("Waiting for a welcome message...");
                        initialConnectionFinished.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ChatUtils.Success("Successfully connected to IRC network " + Ip + ".");
                    ChatUtils.Notify("Done. You should be now connected to IRC.");
                    IsConnected = true;
                }
                for (var command : Main.settings.postConnectionCommands()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        ChatUtils.Error("Error sending command: Sleep interrupted");
                    }
                    try {
                        var time = Integer.parseInt(command.split(" ")[1]);
                        if (command.startsWith("wait")) {
                            Thread.sleep(time);
                        }
                    } catch (NumberFormatException e) {
                        SendLine(command);
                    } catch (InterruptedException e) {
                        ChatUtils.Error("Error sending command: Sleep interrupted");
                    }
                }
            }).start();


        } catch (IllegalStateException e) {
            ChatUtils.Error("Error connecting to server: ");
            ChatUtils.Error(e);
            callbackInfo.cancel();
            return;
        }


    }
    private void Listener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = Reader.readLine()) != null) {
                    System.out.println(line);
                    if (Verbosity == IrcVerbosity.RAW) {
                        ChatUtils.RawIn(line);
                    }
                    HandleInput(line);
                }
            } catch (Exception e) {
                ChatUtils.Error("An error has occured: " + e.getMessage());
                e.printStackTrace();
            }

        }).start();
    }
    private void Register() {
        // still TODO explicit server password? might be useful with SASL PLAIN
        SendLine("NICK " + Nickname);
        SendLine("USER " + Nickname + " mc * : " + Nickname);
    }

    public void SendLine(String line) {
        if (Writer == null)
            throw new IllegalStateException("Writer is not connected to a server. Are you connected?");
        if (Verbosity == IrcVerbosity.RAW) {
            ChatUtils.RawOut(line);
        }
        try {
            if (line.contains("PART")) {
                Channels.remove(GetChannel(line.split(" ")[1]));
            }
            Writer.write(line + "\r\n");
            Writer.flush();
        } catch (IOException e) {
            ChatUtils.Warn("Writer is not connected to a server. Are you connected?");
        }


    }
    private void OpenSocket(CallbackInfo callbackInfo) {
        try {
            ChatUtils.Debug("Creating socket factory...");
            SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
            ChatUtils.Debug("Creating socket...");
            Socket = (SSLSocket) factory.createSocket(Ip, Port);
            ChatUtils.Debug("Setting protocols...");
            Socket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
            // Socket.setSoTimeout(5000); breaks everything...
            ChatUtils.Debug("Starting handshake... <dark_green>// Usually this is the point it gets stuck</dark_green>");
            Socket.startHandshake();
            ChatUtils.Debug("Getting writer...");
            Writer = new BufferedWriter(new OutputStreamWriter(Socket.getOutputStream()));
            ChatUtils.Debug("And reader...");
            Reader = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
            ChatUtils.Debug("Done, lets proceed");
            ChatUtils.Info("Connected to server <blue>" + Ip + "</blue>");

        }
        catch (IOException e) {
            callbackInfo.cancel();
//          ChatUtils.Exception(new Exception("An exception happened while connecting to the server:\n" + e.getMessage()));
            ChatUtils.Error("An exception happened while connecting to the server: " + e.getMessage() + "\n Usually this happens if ip or port are incorrect.");
        }
    }

    private void VerifyFields(CallbackInfo callbackInfo) throws IllegalStateException {
        if (Ip.isEmpty() || Port < 1) {
            throw new IllegalStateException("Ip or Port stated are certainly invalid. Please check them before connecting again.");
        }
        if (Nickname.isEmpty()) {
            if (BackupNickname.isEmpty())
                throw new IllegalStateException(
                            "Both nickname and backup nickname are empty. Idk how would you get" +
                            "your nickname empty, but consider changing it.");
            Nickname = BackupNickname;
        }
        if (Character.isDigit(Nickname.charAt(0))) {
            if (Character.isDigit(BackupNickname.charAt(0))) {
                throw new IllegalStateException(
                            "Both nickname and backup nickname start with digits. " +
                            "These are reserved for server purposes and cannot be used." +
                            "Please change either of them and try again");
            }
            Nickname = BackupNickname;
        }

    }

    private void HandleInput(String input) {
        if (input == null || input.isEmpty()) return;
        if (input.toLowerCase().contains("cannot join channel")) {
            ChatUtils.Warn("Cannot join channel. Message from server: " + input);
        }
        if (input.startsWith("PING")) {
            Pong(input);
            return;
        }
        if (input.contains("001")) {
            synchronized (initialConnectionFinished){
                initialConnectionFinished.notify();
            }
        }
        MessageHandler.HandleMessage(input);
    }

    private void Pong(String input) {
        String payload = input.substring(4).trim();
        if (payload.startsWith(":")) {
            payload = payload.substring(1);
        }
        SendLine("PONG :" + payload);

    }
    public void close() {
        try {Socket.close();} catch (IOException e) {
            e.printStackTrace();
        }

    }
    public boolean isConnected() {
        return IsConnected;
    }
    public String ip() {
        return Ip;
    }
    public void SetIp(String ip) {
        Ip = ip;
    }
    public Integer port() {
        return Port;
    }
    public void SetPort(Integer port) {
        Port = port;
    }
    public String nickname() {
        if (UseBackupNickname) return BackupNickname;
        return Nickname;
    }
    public void SetDisplayNickname(String nickname) {
        Nickname = nickname;
    }
    public void SetBackupNickname(String backupNickname) {
        BackupNickname = backupNickname;
    }
    public List<IRCChannel> channels() {
        return Channels;
    }
    public void SendMessage(String target, String message) {
        if (target.isEmpty())
            target = PrimaryChannel;
        SendLine("PRIVMSG " + target + " :" + message);
    }
    public void setPrimaryChannel(String channel) {
        if (Channels.stream().noneMatch(x -> Objects.equals(x.Name, channel))) throw new IllegalArgumentException("You are not connected to this channel.");
        PrimaryChannel = channel;
    }
    public void SetPrimaryChannel(String channel) {
        if (Channels.stream().noneMatch(x -> Objects.equals(x.Name, channel))) {
            throw new RuntimeException("You aren't connected to channel " + channel);
        }
        PrimaryChannel = channel;
    }
    public String GetPrimaryChannel() {
        if (MainClient.DefaultToMinecraftChat) return "#minecraft_chat";

        return PrimaryChannel;
    }
    public void Disconnect(String reason) {
        SendLine("QUIT " + reason);
        try {
            ChatUtils.Verbose("Closing socket");
            Socket.close();
            IsConnected = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ChatUtils.Notify("Disconnected from server");
    }
    public void SetVerbosity(IrcVerbosity verbosity) {
        Verbosity = verbosity;
    }
    public void AddChannel(IRCChannel channel, Boolean connect) {
        Channels.add(channel);
        ChatUtils.Info("Added channel " + channel.Name);
        if (connect) {
            ChatUtils.Info("Joining channel " + channel.Name + "...");
            SendLine(channel.Join());
        }
        if (Channels.size() == 1) {
            SetPrimaryChannel(channel.Name);
        }
    }
    public IRCChannel GetChannel(String channelName) {
        return Channels.stream().filter(x -> Objects.equals(x.Name, channelName) || Objects.equals(x.Name.substring(1), channelName)).findFirst().orElse(null);
    }
    public IRCChannel GetChannelByPrefix(Character prefix) {
        return Channels.stream().filter(x -> Objects.equals(x.Prefix, prefix)).findFirst().orElse(null);
    }
    public void AddChannelByPrefix(Character prefix, IRCChannel channel) {
        channel.Prefix = prefix;
        if (Channels.stream().noneMatch(x -> Objects.equals(x.Name, channel.Name))) {
            AddChannel(channel, true);
        }

    }
    public void UseBackupNickname() {
        SendLine("NICK " + BackupNickname);
        UseBackupNickname = true;
    }
}
