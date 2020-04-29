package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import java.util.UUID;

public class PlayerKillStats {
    public long lastKillTime = 0;
    public String playerName;
    public UUID playerUUID;
    public int spreeKillCount = 0;
    public int totalKillCount = 0;
}
