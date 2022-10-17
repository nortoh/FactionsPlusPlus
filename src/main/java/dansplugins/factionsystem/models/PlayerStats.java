package dansplugins.factionsystem.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

import java.time.ZonedDateTime;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import dansplugins.factionsystem.jsonadapters.ZonedDateTimeAdapter;

public class PlayerStats {
    @Expose
    private int logins = 0;
    @Expose
    private int powerLost = 0;
    @Expose
    @JsonAdapter(ZonedDateTimeAdapter.class)
    private ZonedDateTime lastLogout = ZonedDateTime.now();

    public PlayerStats(int initialLogins) {
        this.logins = initialLogins;
    }

    public int getPowerLost() {
        return this.powerLost;
    }

    public void setPowerLost(int power) {
        powerLost = power;
    }

    // TODO: reimplement in Player Service
    public void incrementPowerLost() {
        // powerLost += configService.getInt("powerDecreaseAmount");
    }

    public ZonedDateTime getLastLogout() {
        return this.lastLogout;
    }

    public void setLastLogout(ZonedDateTime date) {
        this.lastLogout = date;
    }

    public void incrementLogins() {
        this.logins++;
    }

    public int getLogins() {
        return this.logins;
    }

    public int getMinutesSinceLastLogout() {
        if (this.lastLogout == null) {
            return 0;
        }
        ZonedDateTime now = ZonedDateTime.now();
        Duration duration = Duration.between(this.lastLogout, now);
        double totalSeconds = duration.getSeconds();
        return (int) totalSeconds / 60;
    }

    /**
     * Method to obtain the current session length in dd:hh:mm:ss
     * <p>
     * If days are not found, hh:mm:ss are returned.
     * </p>
     *
     * @return formatted String dd:hh:mm:ss
     * @author Callum
     */
    public String getActiveSessionLength() {
        if (this.lastLogout == null) {
            return "00:00:00";
        }
        final ZonedDateTime now = ZonedDateTime.now();
        final Duration duration = Duration.between(this.lastLogout, now);
        long totalSeconds = duration.getSeconds();
        final long days = TimeUnit.SECONDS.toDays(totalSeconds);
        totalSeconds -= TimeUnit.DAYS.toSeconds(days); // Remove Days from Total.
        final long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        totalSeconds -= TimeUnit.HOURS.toSeconds(hours); // Remove Hours from Total.
        final long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds);
        totalSeconds -= TimeUnit.MINUTES.toSeconds(minutes); // Remove Minutes from Total.
        final long seconds = totalSeconds; // Last one is just the remainder.
        final String d = pad(days), h = pad(hours), m = pad(minutes), s = pad(seconds);
        return (d.equalsIgnoreCase("00") ? "" : d + ":") + h + ":" + m + ":" + s;
    }

    public String getTimeSinceLastLogout() {
        if (this.lastLogout != null) {
            ZonedDateTime now = ZonedDateTime.now();
            Duration duration = Duration.between(this.lastLogout, now);
            double totalSeconds = duration.getSeconds();
            int minutes = (int) totalSeconds / 60;
            int hours = minutes / 60;
            int days = hours / 24;
            int hoursSince = hours - (days * 24);
            return days + " days and " + hoursSince + " hours";
        } else {
            return null;
        }
    }

    /**
     * Method to pad a value with a zero to its left.
     *
     * @param value to pad
     * @return 00 or 0(0-9) or 10-(very big numbers)
     * @author Callum
     */
    private String pad(Number value) {
        String tmp = String.valueOf(value);
        return tmp.length() == 0 ? ("00") : (tmp.length() == 1 ? ("0" + value) : (tmp));
    }
}