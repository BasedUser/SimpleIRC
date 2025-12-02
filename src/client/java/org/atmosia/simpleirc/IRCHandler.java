package org.atmosia.simpleirc;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;

public class IRCHandler {
    public enum IrcVerbosity {
        QUIET,   // only messages + events that affect us directly
        NORMAL,  // QUIET + channel meta, but not other users' join/part/quit/nick
        VERBOSE, // everything for everyone in channels
        RAW      // VERBOSE + raw incoming/outgoing lines
    }

    private String server = "";
    private String nick = "";
    // Default target (channel or nick) when no prefix is used
    private String channelname = "";
    private String password = "";
    // Default TLS port
    private Integer port = 6697;

    private BufferedWriter writer;
    private BufferedReader reader;
    private SSLSocket socket = null;

    private AtomicBoolean processed = new AtomicBoolean(true);

    private IrcVerbosity verbosity = IrcVerbosity.NORMAL;

    // nick -> last channel seen (for QUIT/NICK mapping)
    private final Map<String, String> lastChannelByNick = new HashMap<String, String>();

    // channel -> (nick -> status prefix char: +, %, @, &, ~)
    private final Map<String, Map<String, Character>> channelUserModes = new HashMap<String, Map<String, Character>>();

    public IRCHandler(String server, String nick, String channelname, String password, Integer port) 
    {
        super();
        this.server = server;
        this.nick = nick;
        this.channelname = channelname;
        this.password = password;
        this.port = port;
    }

    public void setVerbosity(IrcVerbosity verbosity) {
        this.verbosity = verbosity;
    }

    public IrcVerbosity getVerbosity() {
        return verbosity;
    }

    public void startConn(){
        new Thread(() -> {
            try 
            {
                SSLSocketFactory sslFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
                socket = (SSLSocket) sslFactory.createSocket(server, port);

                socket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
                socket.startHandshake();

                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                synchronized(processed) 
                {
                    ChatUtils.message("\u00A7aConnected to " + server + " successfully");
                    login(writer);
                    ChatUtils.message("\u00A76Waiting for handshake...");

                    Listener();
                    try {
                        processed.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ChatUtils.message("\u00A76Joining channel " + channelname + "...");
                    join(channelname + " " + password);

                    try {
                        Thread.sleep(1000); // yeah. cry about it.
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(isOpen())
                    {
                        ChatUtils.message("\u00A7e[\u00A7aDONE\u00A7e]");
                    }
                }

            } catch (Exception e) 
            {
                e.printStackTrace();
                ChatUtils.message("\u00A7cCould not connect to " + server + ":" + port);
            }
        }).start();
    }

    public void Listener()
    {
        new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {

                    if (verbosity == IrcVerbosity.RAW) {
                        ChatUtils.message("\u00A78[raw <=]\u00A77 " + line);
                    }

                    handleLine(line);
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                if (isOpen()) login(writer);
            }

        }).start();
    }

    private void handleLine(String line) throws IOException {
        if (line == null || line.isEmpty()) {
            return;
        }

        // crude join failure catch
        if (line.toLowerCase().contains("cannot join channel")) {
            ChatUtils.message("\u00A7cCannot join channel (are you banned, or is the password correct?)");
            closeConnection();
            return;
        }

        // PING
        if (line.startsWith("PING")) {
            String payload = line.substring(4).trim();
            if (payload.startsWith(":")) {
                payload = payload.substring(1);
            }
            writeLine("PONG :" + payload);
            return;
        }

        // Connection registered: :server 001 nick :Welcome ...
        if (line.contains(" 001 " + nick + " ")) {
            synchronized (processed) {
                processed.notify();
            }
        }

        if (!line.startsWith(":")) {
            return;
        }

        int prefixEnd = line.indexOf(' ');
        if (prefixEnd <= 0) {
            return;
        }

        String prefix = line.substring(1, prefixEnd);   // nick!user@host
        String rest   = line.substring(prefixEnd + 1);

        int colonIndex = rest.indexOf(" :");
        String middle;
        String trailing = "";
        if (colonIndex != -1) {
            middle   = rest.substring(0, colonIndex);
            trailing = rest.substring(colonIndex + 2);
        } else {
            middle = rest;
        }

        String[] middleParts = middle.split(" ");
        if (middleParts.length < 1) {
            return;
        }

        String command = middleParts[0].toUpperCase(Locale.ROOT);
        String bareNick = extractBareNick(prefix);

        // PRIVMSG / NOTICE
        if ("PRIVMSG".equals(command) || "NOTICE".equals(command)) {
            if (middleParts.length < 2) return;

            String target  = middleParts[1];
            String displayTarget;

            if (target.equalsIgnoreCase(this.nick)) {
                // PM: either last channel or PM buffer
                String lastChan = getLastChannelForNick(bareNick);
                displayTarget = (lastChan != null) ? lastChan : bareNick;
            } else {
                displayTarget = target;
            }

            if (displayTarget.startsWith("#")) {
                rememberNickInChannel(bareNick, displayTarget);
            }

            String formattedNick = formatNick(bareNick, displayTarget);

            StringBuilder sb = new StringBuilder();
            sb.append("\u00A79").append(displayTarget).append("\u00A7r").append(" | ");
            if ("NOTICE".equals(command)) {
                sb.append("\u00A76[NOTICE]\u00A7r ");
            }
            sb.append("<").append(formattedNick).append("\u00A7r> ").append(trailing);

            ChatUtils.message(sb.toString());
            return;
        }

        // JOIN
        if ("JOIN".equals(command)) {
            String chan = trailing;
            if (chan.isEmpty() && middleParts.length >= 2) {
                chan = middleParts[1];
            }
            if (chan.isEmpty()) {
                return;
            }

            rememberNickInChannel(bareNick, chan);

            if (!shouldDisplayNickEvents(bareNick)) {
                return;
            }

            String formattedNick = formatNick(bareNick, chan);
            ChatUtils.message("\u00A79" + chan + "\u00A7r | + " + formattedNick);
            return;
        }

        // PART
        if ("PART".equals(command)) {
            if (middleParts.length < 2) return;
            String chan = middleParts[1];

            removeNickFromChannel(chan, bareNick);

            if (!shouldDisplayNickEvents(bareNick)) {
                return;
            }

            String formattedNick = formatNick(bareNick, chan);
            String reason = trailing.isEmpty() ? "" : " \u00A77(" + trailing + "\u00A77)";
            ChatUtils.message("\u00A79" + chan + "\u00A7r | - " + formattedNick + reason);
            return;
        }

        // QUIT
        if ("QUIT".equals(command)) {
            String chan = getLastChannelForNick(bareNick);
            if (chan == null) {
                chan = "*";
            }

            removeNickFromAllChannels(bareNick);

            if (!shouldDisplayNickEvents(bareNick)) {
                return;
            }

            String formattedNick = formatNick(bareNick, chan);
            String reason = trailing.isEmpty() ? "" : " \u00A77(" + trailing + "\u00A77)";
            ChatUtils.message("\u00A79" + chan + "\u00A7r | - " + formattedNick + " \u00A7cquit" + reason);
            return;
        }

        // NICK
        if ("NICK".equals(command)) {
            String newNick = trailing.isEmpty() && middleParts.length >= 2 ? middleParts[1] : trailing;
            if (newNick == null || newNick.isEmpty()) return;

            // Move channel mode roles
            for (Map<String, Character> m : channelUserModes.values()) {
                Character st = m.remove(bareNick);
                if (st != null) {
                    m.put(newNick, st);
                }
            }

            // Move last-channel association
            String lastChan = lastChannelByNick.remove(bareNick);
            if (lastChan != null) {
                lastChannelByNick.put(newNick, lastChan);
            }
            String chan = (lastChan != null) ? lastChan : "*";

            if (!shouldDisplayNickEvents(bareNick)) {
                if (bareNick.equalsIgnoreCase(this.nick)) {
                    this.nick = newNick;
                }
                return;
            }

            String oldColor = "\u00A7c" + bareNick;
            String newColor = "\u00A7c" + newNick;
            ChatUtils.message("\u00A79" + chan + "\u00A7r | \u00A7enick\u00A7r: " + oldColor + " \u00A7e->\u00A7r " + newColor);

            if (bareNick.equalsIgnoreCase(this.nick)) {
                this.nick = newNick;
            }
            return;
        }

        // KICK
        if ("KICK".equals(command)) {
            if (middleParts.length < 3) return;
            String chan = middleParts[1];
            String victim = middleParts[2];

            removeNickFromChannel(chan, victim);

            // In QUIET, only show if we are the victim
            if (verbosity == IrcVerbosity.QUIET && !victim.equalsIgnoreCase(this.nick)) {
                return;
            }

            String kicker = formatNick(bareNick, chan);
            String reason = trailing.isEmpty() ? "" : ": " + trailing;
            ChatUtils.message("\u00A79" + chan + "\u00A7r | - \u00A7c" + victim + "\u00A7r (kicked by " + kicker + reason + ")");
            return;
        }

        // INVITE
        if ("INVITE".equals(command)) {
            if (middleParts.length < 2 && trailing.isEmpty()) {
                return;
            }

            String targetNick = (middleParts.length >= 2) ? middleParts[1] : "";
            String chan = (middleParts.length >= 3) ? middleParts[2] : trailing;

            String inviter = formatNick(bareNick, chan);

            if (verbosity == IrcVerbosity.QUIET && !targetNick.equalsIgnoreCase(this.nick)) {
                return;
            }

            ChatUtils.message("\u00A79" + chan + "\u00A7r | " + inviter + " \u00A76invited\u00A7r " + targetNick + " \u00A7rto the channel");
            return;
        }

        // TOPIC
        if ("TOPIC".equals(command)) {
            if (middleParts.length < 2) return;
            String chan = middleParts[1];
            String setter = formatNick(bareNick, chan);

            ChatUtils.message("\u00A79" + chan + "\u00A7r | \u00A76Topic\u00A7r: " + trailing + " \u00A77(by " + setter + "\u00A77)");
            return;
        }

        // MODE
        if ("MODE".equals(command)) {
            if (middleParts.length < 2) return;

            String target = middleParts[1];
            String actorChan = target.startsWith("#") ? target : getLastChannelForNick(bareNick);
            String actor = formatNick(bareNick, actorChan);

            // Channel modes that affect user statuses
            if (target.startsWith("#")) {
                Map<String, Character> map = getChannelModesMap(target);

                if (middleParts.length >= 3) {
                    String modeStr = middleParts[2];
                    int argIndex = 3;
                    boolean adding = true;

                    for (int i = 0; i < modeStr.length(); i++) {
                        char m = modeStr.charAt(i);
                        if (m == '+') { adding = true; continue; }
                        if (m == '-') { adding = false; continue; }

                        if ("ovhqa".indexOf(m) >= 0 && argIndex < middleParts.length) {
                            String modeNick = middleParts[argIndex++];
                            char statusChar = modeToPrefix(m);

                            if (map != null) {
                                if (adding) {
                                    if (statusChar != 0) {
                                        map.put(modeNick, statusChar);
                                    }
                                } else {
                                    Character current = map.get(modeNick);
                                    if (current != null && current == statusChar) {
                                        map.put(modeNick, (char)0);
                                    }
                                }
                            }
                        }
                    }
                }

                boolean affectsUs = target.equalsIgnoreCase(this.nick)
                        || (trailing != null && trailing.contains(this.nick));
                if (verbosity == IrcVerbosity.QUIET && !affectsUs) {
                    return;
                }

                StringBuilder params = new StringBuilder();
                for (int i = 2; i < middleParts.length; i++) {
                    if (i > 2) params.append(" ");
                    params.append(middleParts[i]);
                }
                if (!trailing.isEmpty()) {
                    if (params.length() > 0) params.append(" ");
                    params.append(trailing);
                }

                ChatUtils.message("\u00A79" + target + "\u00A7r | " + actor + " \u00A76set modes\u00A7r " + params + " \u00A76on " + target);
                return;
            }
        }

        // Numeric handling
        if (isNumeric(command)) {
            handleNumeric(command, middleParts, trailing);
        }
    }

    private void handleNumeric(String command, String[] middleParts, String trailing) {
        // 353: RPL_NAMREPLY :server 353 you = #chan :@nick1 +nick2 nick3
        if ("353".equals(command) && middleParts.length >= 4) {
            String chan = middleParts[3];
            Map<String, Character> map = getChannelModesMap(chan);
            if (map != null) {
                String[] names = trailing.split(" ");
                for (String rawName : names) {
                    if (rawName.isEmpty()) continue;
                    char status = 0;
                    String nickOnly = rawName;
                    char c0 = rawName.charAt(0);
                    if ("+%@&~".indexOf(c0) >= 0) {
                        status = c0;
                        nickOnly = rawName.substring(1);
                    }
                    map.put(nickOnly, status);
                    rememberNickInChannel(nickOnly, chan);
                }
            }
            return;
        }

        // 332: topic
        if ("332".equals(command) && middleParts.length >= 3) {
            String chan = middleParts[2];
            ChatUtils.message("\u00A79" + chan + "\u00A7r | \u00A76Topic\u00A7r: " + trailing);
            return;
        }

        // 333: topic who/time
        if ("333".equals(command) && middleParts.length >= 5) {
            String chan = middleParts[2];
            String setter = middleParts[3];
            ChatUtils.message("\u00A79" + chan + "\u00A7r | \u00A76Topic set by\u00A7r " + setter);
            return;
        }

        // 401: no such nick/channel
        // :server 401 you target :No such nick/channel
        if ("401".equals(command) && middleParts.length >= 3) {
            String target = middleParts[2];
            String msg = trailing.isEmpty() ? "No such nick/channel" : trailing;
            ChatUtils.message("\u00A7cNo such nick/channel: \u00A7e" + target + "\u00A7c - " + msg);
            return;
        }
		
		// 404: you're not in that channel
		// :server 404 you target :No external channel messages (target)
        if ("404".equals(command) && middleParts.length >= 3) {
            String target = middleParts[2];
            String msg = trailing.isEmpty() ? "No external channel messages" : trailing;
            ChatUtils.message("\u00A7cYou're not in channel \u00A7e" + target + "\u00A7c: " + msg);
            return;
        }
    }

    private boolean isNumeric(String s) {
        if (s.length() != 3) return false;
        for (int i = 0; i < 3; i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    // QUIET + NORMAL: only show our own JOIN/PART/QUIT/NICK
    // VERBOSE + RAW: show everyone
    private boolean shouldDisplayNickEvents(String nick) {
		// VERBOSE and RAW: show all nick-related events
		if (verbosity == IrcVerbosity.VERBOSE || verbosity == IrcVerbosity.RAW) {
			return true;
		}
		// QUIET and NORMAL: only show events that affect us directly
		return nick != null && nick.equalsIgnoreCase(this.nick);
	}


    private void rememberNickInChannel(String bareNick, String chan) {
        if (bareNick == null || bareNick.isEmpty() || chan == null || chan.isEmpty()) {
            return;
        }
        lastChannelByNick.put(bareNick, chan);
    }

    private void removeNickFromChannel(String chan, String bareNick) {
        if (chan == null || bareNick == null) return;
        Map<String, Character> map = channelUserModes.get(chan);
        if (map != null) {
            map.remove(bareNick);
            if (map.isEmpty()) {
                channelUserModes.remove(chan);
            }
        }
        String lastChan = lastChannelByNick.get(bareNick);
        if (chan.equals(lastChan)) {
            lastChannelByNick.remove(bareNick);
        }
    }

    private void removeNickFromAllChannels(String bareNick) {
        if (bareNick == null) return;
        for (Map<String, Character> m : channelUserModes.values()) {
            m.remove(bareNick);
        }
        lastChannelByNick.remove(bareNick);
    }

    private String getLastChannelForNick(String bareNick) {
        if (bareNick == null) return null;
        return lastChannelByNick.get(bareNick);
    }

    private Map<String, Character> getChannelModesMap(String channel) {
        if (channel == null) return null;
        Map<String, Character> m = channelUserModes.get(channel);
        if (m == null) {
            m = new HashMap<String, Character>();
            channelUserModes.put(channel, m);
        }
        return m;
    }

    private char modeToPrefix(char mode) {
        switch (mode) {
            case 'q': return '~'; // owner
            case 'a': return '&'; // admin
            case 'o': return '@'; // op
            case 'h': return '%'; // halfop
            case 'v': return '+'; // voice
            default:  return 0;
        }
    }

    // Format nick with status prefix from stored state
    private String formatNick(String bareNick, String channel) {
        if (bareNick == null) {
            return "";
        }

        char status = 0;
        Map<String, Character> map = getChannelModesMap(channel);
        if (map != null) {
            Character s = map.get(bareNick);
            if (s != null) {
                status = s;
            }
        }

        String statusColor = null;
        if (status != 0) {
            switch (status) {
                case '+': statusColor = "\u00A7b"; break; // aqua
                case '%': statusColor = "\u00A71"; break; // dark blue
                case '@': statusColor = "\u00A7a"; break; // green
                case '&': statusColor = "\u00A76"; break; // gold
                case '~': statusColor = "\u00A7e"; break; // yellow
            }
        }

        String nickColor = "\u00A7c";

        StringBuilder sb = new StringBuilder();
        if (statusColor != null) {
            sb.append(statusColor).append(status);
        }
        sb.append(nickColor).append(bareNick);

        return sb.toString();
    }

    private String extractBareNick(String rawPrefix) {
        if (rawPrefix == null) {
            return "";
        }
        int bang = rawPrefix.indexOf('!');
        if (bang != -1) {
            return rawPrefix.substring(0, bang);
        }
        return rawPrefix;
    }

    private void writeLine(String line) throws IOException {
        if (writer == null || line == null) {
            return;
        }
        if (verbosity == IrcVerbosity.RAW) {
            // sent lines as fully yellow
            ChatUtils.message("\u00A7e[raw =>] " + line);
        }
        writer.write(line + "\r\n");
        writer.flush();
    }

    public void join(String channelname) {
        try {
            writeLine("JOIN " + channelname);
        } catch (IOException e) {
            System.err.println("Join error: " + e.getMessage());
            ChatUtils.message("\u00A7cCould not join channel "+channelname);
            login(writer);
        }
    }

    public void login(BufferedWriter writer) {
        try {
			// TODO explicit server password? might be useful with SASL PLAIN
            //if (password != null && !password.isEmpty()) {
            //    writeLine("PASS " + password);
            //}
            writeLine("NICK " + nick);
            writeLine("USER " + nick + " mc * : " + nick);
        } catch (IOException e) {
            System.err.println("login error " + e.getMessage());
            ChatUtils.message("\u00A7cCould not set identity: " + e.getMessage());
            closeConnection();
        }
    }

    public void sendGroupMsg(String groupMsg) {
        sendGroupMsg(this.channelname, groupMsg);
    }

    public void sendGroupMsg(String target, String groupMsg) {
        try 
        {
            if ("quit".equals(groupMsg)) 
            {
                writeLine("PART " + target + " Leaving ~ ~");
                closeConnection();
                return;
            }

            writeLine("PRIVMSG " + target + " :" + groupMsg);

        } catch (Exception e) 
        {
            System.err.println("sendGroupMsg error: " + e.getMessage());
            login(writer);
        }
    }

    public void sendRawLine(String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        try {
            writeLine(raw);
        } catch (IOException e) {
            System.err.println("sendRawLine error: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try 
        {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public boolean isOpen()
    {
        return socket != null && !socket.isClosed();
    }

    public String getServer() 
    {
        return server;
    }

    public String getChannelname() 
    {
        return channelname;
    }

    public String getNick() 
    {
        return nick;
    }

    public Socket getSocket()
    {
        return socket;
    }

    public synchronized void setChannelname(String channelname) {
        this.channelname = channelname;
    }

    // Public accessor for formatted nick (for ChatManager echoes)
    public String getFormattedNick(String bareNick, String channel) {
        return formatNick(bareNick, channel);
    }
}
