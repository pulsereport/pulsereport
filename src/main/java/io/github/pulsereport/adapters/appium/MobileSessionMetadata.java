package io.github.pulsereport.adapters.appium;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Structured mobile session metadata, recorded once per run and surfaced in
 * the final {@code TestRun.environment} block.
 *
 * <p>Use {@link AppiumAdapter#recordSessionMetadata(MobileSessionMetadata)}
 * to attach this to the report. All fields are optional; only non-null values
 * are written into the environment map.</p>
 *
 * @author Pulse Report Team
 * @since 1.1.0
 */
public final class MobileSessionMetadata {

    private final String platformName;
    private final String platformVersion;
    private final String deviceName;
    private final String deviceModel;
    private final String udid;
    private final String appPackage;
    private final String appActivity;
    private final String appVersion;
    private final String appBuild;
    private final String automationName;
    private final String appiumServerVersion;

    private MobileSessionMetadata(Builder builder) {
        this.platformName = builder.platformName;
        this.platformVersion = builder.platformVersion;
        this.deviceName = builder.deviceName;
        this.deviceModel = builder.deviceModel;
        this.udid = builder.udid;
        this.appPackage = builder.appPackage;
        this.appActivity = builder.appActivity;
        this.appVersion = builder.appVersion;
        this.appBuild = builder.appBuild;
        this.automationName = builder.automationName;
        this.appiumServerVersion = builder.appiumServerVersion;
    }

    /**
     * Converts this metadata to an environment map containing only the
     * non-null entries.
     *
     * @return an ordered map of environment keys to values
     */
    public Map<String, String> toEnvironmentMap() {
        Map<String, String> env = new LinkedHashMap<>();
        putIfPresent(env, "mobile.platform", platformName);
        putIfPresent(env, "mobile.platformVersion", platformVersion);
        putIfPresent(env, "mobile.deviceName", deviceName);
        putIfPresent(env, "mobile.deviceModel", deviceModel);
        putIfPresent(env, "mobile.udid", udid);
        putIfPresent(env, "mobile.appPackage", appPackage);
        putIfPresent(env, "mobile.appActivity", appActivity);
        putIfPresent(env, "mobile.appVersion", appVersion);
        putIfPresent(env, "mobile.appBuild", appBuild);
        putIfPresent(env, "mobile.automationName", automationName);
        putIfPresent(env, "mobile.appiumServerVersion", appiumServerVersion);
        return env;
    }

    private static void putIfPresent(Map<String, String> env, String key, String value) {
        if (value != null && !value.isBlank()) {
            env.put(key, value);
        }
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public String getUdid() {
        return udid;
    }

    public String getAppPackage() {
        return appPackage;
    }

    public String getAppActivity() {
        return appActivity;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getAppBuild() {
        return appBuild;
    }

    public String getAutomationName() {
        return automationName;
    }

    public String getAppiumServerVersion() {
        return appiumServerVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MobileSessionMetadata that = (MobileSessionMetadata) o;
        return Objects.equals(platformName, that.platformName)
                && Objects.equals(platformVersion, that.platformVersion)
                && Objects.equals(deviceName, that.deviceName)
                && Objects.equals(deviceModel, that.deviceModel)
                && Objects.equals(udid, that.udid)
                && Objects.equals(appPackage, that.appPackage)
                && Objects.equals(appActivity, that.appActivity)
                && Objects.equals(appVersion, that.appVersion)
                && Objects.equals(appBuild, that.appBuild)
                && Objects.equals(automationName, that.automationName)
                && Objects.equals(appiumServerVersion, that.appiumServerVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformName, platformVersion, deviceName, deviceModel, udid,
                appPackage, appActivity, appVersion, appBuild, automationName, appiumServerVersion);
    }

    @Override
    public String toString() {
        return "MobileSessionMetadata{" + toEnvironmentMap() + '}';
    }

    /**
     * Builder for {@link MobileSessionMetadata}.
     */
    public static final class Builder {
        private String platformName;
        private String platformVersion;
        private String deviceName;
        private String deviceModel;
        private String udid;
        private String appPackage;
        private String appActivity;
        private String appVersion;
        private String appBuild;
        private String automationName;
        private String appiumServerVersion;

        private Builder() {
        }

        public Builder platformName(String platformName) {
            this.platformName = platformName;
            return this;
        }

        public Builder platformVersion(String platformVersion) {
            this.platformVersion = platformVersion;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder deviceModel(String deviceModel) {
            this.deviceModel = deviceModel;
            return this;
        }

        public Builder udid(String udid) {
            this.udid = udid;
            return this;
        }

        public Builder appPackage(String appPackage) {
            this.appPackage = appPackage;
            return this;
        }

        public Builder appActivity(String appActivity) {
            this.appActivity = appActivity;
            return this;
        }

        public Builder appVersion(String appVersion) {
            this.appVersion = appVersion;
            return this;
        }

        public Builder appBuild(String appBuild) {
            this.appBuild = appBuild;
            return this;
        }

        public Builder automationName(String automationName) {
            this.automationName = automationName;
            return this;
        }

        public Builder appiumServerVersion(String appiumServerVersion) {
            this.appiumServerVersion = appiumServerVersion;
            return this;
        }

        public MobileSessionMetadata build() {
            return new MobileSessionMetadata(this);
        }
    }
}
