package uni.tesis.interfazfinal;

import android.app.Application;

import java.util.HashMap;
import java.util.Map;

public class Points extends Application {
    private int totalPoints = 0;
    private static final int NUM_LEVELS = 4;

    private int[] pointsPerLevel = new int[NUM_LEVELS];
    private Map<String, MainActivity> mainActivities = new HashMap<>();
    private Map<String, Integer> pointsPerSection = new HashMap<>();

    private long totalAppUsageTime = 0;
    private long sessionStartTime = 0;
    private MainActivity mainActivity;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public void startSession() {
    }

    public void endSession() {

    }
    public int getTotalPoints() {
        return totalPoints;
    }

    public int getPointsForLevel(int level) {
        if (level > 0 && level <= NUM_LEVELS) {
            return pointsPerLevel[level - 1];
        }
        return 0;
    }
    public int getPointsForSection(String section) {
        return pointsPerSection.getOrDefault(section, 0);
    }
    public void setTotalPoints(int points) {
        totalPoints = points;
        updateMainActivityUI();

    }

    public void addPoints(String section, int points) {
        int currentPoints = pointsPerSection.getOrDefault(section, 0);
        currentPoints += points;
        pointsPerSection.put(section, currentPoints);
        totalPoints += points;
        updateMainActivityUI();
    }
    public void addMainActivity(String tag, MainActivity activity) {
        mainActivities.put(tag, activity);
    }

    public void removeMainActivity(String key) {
        mainActivities.remove(key);
    }

    public String getTotalPointsAsString() {
        return String.valueOf(totalPoints);
    }

    public void updateMainActivityUI() {
        for (MainActivity mainActivity : mainActivities.values()) {
            if (mainActivity != null) {
                mainActivity.updatePointsTextView(totalPoints);
            }
        }
    }

    public void setMainActivity(MainActivity activity) {
        this.mainActivity = activity;
    }
}
