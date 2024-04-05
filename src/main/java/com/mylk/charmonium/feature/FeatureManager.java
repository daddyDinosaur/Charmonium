package com.mylk.charmonium.feature;

import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.feature.impl.*;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.util.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeatureManager {
    private static FeatureManager instance;
    private final ArrayList<IFeature> features = new ArrayList<>();

    public static FeatureManager getInstance() {
        if (instance == null) {
            instance = new FeatureManager();
        }
        return instance;
    }

    public List<IFeature> fillFeatures() {
        List<IFeature> featuresList = Arrays.asList(
            AutoBazaar.getInstance(),
            AntiStuck.getInstance(),
            AutoCookie.getInstance(),
            AutoGodPot.getInstance(),
            AutoReconnect.getInstance(),
            AutoSell.getInstance(),
            DesyncChecker.getInstance(),
            Freelook.getInstance(),
            LagDetector.getInstance(),
            LeaveTimer.getInstance(),
            Scheduler.getInstance(),
            BanInfo.getInstance(),
            UngrabMouse.getInstance()
        );
        features.addAll(featuresList);
        return features;
    }

    public boolean shouldPauseMacroExecution() {
        return features.stream().anyMatch(feature -> {
            if (feature.isRunning()) {
                return feature.shouldPauseMacroExecution();
            }
            return false;
        });
    }

    public void disableAll() {
        features.forEach(feature -> {
            if (feature.isToggled() && feature.isRunning()) {
                feature.stop();
                LogUtils.sendDebug("Disabled feature: " + feature.getName());
            }
        });
    }

    public void disableAllExcept(IFeature... sender) {
        features.forEach(feature -> {
            if (feature.isToggled() && feature.isRunning() && !Arrays.asList(sender).contains(feature)) {
                feature.stop();
                LogUtils.sendDebug("Disabled feature: " + feature.getName());
            }
        });
    }

    public void resetAllStates() {
        features.forEach(IFeature::resetStatesAfterMacroDisabled);
    }

    public boolean isAnyOtherFeatureEnabled(IFeature... sender) {
        return features.stream().anyMatch(feature -> feature.shouldPauseMacroExecution() && feature.isRunning() && !Arrays.asList(sender).contains(feature));
    }

    public boolean shouldIgnoreFalseCheck() {
        if (AutoCookie.getInstance().isRunning() && !AutoCookie.getInstance().shouldCheckForFailsafes()) {
            return true;
        }
        if (AutoGodPot.getInstance().isRunning() && !AutoGodPot.getInstance().shouldCheckForFailsafes()) {
            return true;
        }
        if (AutoReconnect.getInstance().isRunning() && !AutoReconnect.getInstance().shouldCheckForFailsafes()) {
            return true;
        }
        if (LeaveTimer.getInstance().isRunning() && !LeaveTimer.getInstance().shouldCheckForFailsafes()) {
            return true;
        }
        return false;
    }

    public void enableAll() {
        features.forEach(feature -> {
            if (feature.shouldStartAtMacroStart() && feature.isToggled()) {
                feature.start();
                LogUtils.sendDebug("Enabled feature: " + feature.getName());
            }
        });
    }

    public void disableCurrentlyRunning(IFeature sender) {
        features.forEach(feature -> {
            if (feature.isRunning() && feature != sender) {
                feature.stop();
                LogUtils.sendDebug("Disabled feature: " + feature.getName());
            }
        });
    }
}
