package eu.fx3.plugins.totaldeathmessages;

import java.time.Instant;

/**
 * Dataclass to store kill statistics for a specific player
 */
public class PlayerKillStats {
    /**
     * Last player kill as epoch timestamp (e.g. from {@link Instant#getEpochSecond()})
     */
    private long lastKillTime = 0;

    /**
     * Kill count of the current killing spree
     */
    private int spreeKillCount = 0;

    /**
     * Total kill count of a player.
     * Currently unused.
     */
    private int totalKillCount = 0;

    public long getLastKillTime() {
        return lastKillTime;
    }

    public void setLastKillTime(long lastKillTime) {
        this.lastKillTime = lastKillTime;
    }

    public int getSpreeKillCount() {
        return spreeKillCount;
    }

    public void setSpreeKillCount(int spreeKillCount) {
        this.spreeKillCount = spreeKillCount;
    }

    public int getTotalKillCount() {
        return totalKillCount;
    }

    public void setTotalKillCount(int totalKillCount) {
        this.totalKillCount = totalKillCount;
    }
}
