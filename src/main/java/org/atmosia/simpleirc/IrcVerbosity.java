package org.atmosia.simpleirc;

public enum IrcVerbosity {
    QUIET,   // only messages + events that affect us directly
    NORMAL,  // QUIET + channel meta, but not other users' join/part/quit/nick
    VERBOSE, // everything for everyone in channels
    RAW,     // VERBOSE + raw incoming/outgoing lines
    DEBUG    // And some debug stuff, EXTREMELY detailed and clutters chat.
}
