package eu.fx3.plugins.totaldeathmessages.totaldeathmessages;

import java.time.Instant;

/**
 * Dataclass to store kill statistics for a specific player
 */
class PlayerKillStats {
    /**
     * Last player kill as epoch timestamp (e.g. from {@link Instant#getEpochSecond()})
     */
    public long lastKillTime = 0;

    /**
     * Kill count of the current killing spree
     */
    public int spreeKillCount = 0;

    /**
     * Total kill count of a player.
     * Currently unused.
     */
    public int totalKillCount = 0;
}
