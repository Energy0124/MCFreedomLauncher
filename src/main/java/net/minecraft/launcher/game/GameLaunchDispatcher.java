package net.minecraft.launcher.game;

import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.runner.GameRunner;
import com.mojang.launcher.game.runner.GameRunnerListener;
import com.mojang.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

public class GameLaunchDispatcher
        implements GameRunnerListener {
    private final net.minecraft.launcher.Launcher launcher;
    private final String[] additionalLaunchArgs;
    private final ReentrantLock lock = new ReentrantLock();
    private final BiMap<UserAuthentication, MinecraftGameRunner> instances = HashBiMap.create();
    private boolean downloadInProgress = false;

    public GameLaunchDispatcher(net.minecraft.launcher.Launcher launcher, String[] additionalLaunchArgs) {
        this.launcher = launcher;
        this.additionalLaunchArgs = additionalLaunchArgs;
    }

    public PlayStatus getStatus() {
        ProfileManager profileManager = this.launcher.getProfileManager();
        Profile profile = profileManager.getProfiles().isEmpty() ? null : profileManager.getSelectedProfile();
        UserAuthentication user = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        if ((user == null) || (!user.isLoggedIn()) || (profile == null) || (this.launcher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).isEmpty())) {
            return PlayStatus.LOADING;
        }
        this.lock.lock();
        try {
            PlayStatus localPlayStatus;
            if (this.downloadInProgress) {
                return PlayStatus.DOWNLOADING;
            }
            if (this.instances.containsKey(user)) {
                return PlayStatus.ALREADY_PLAYING;
            }
        } finally {
            this.lock.unlock();
        }
        if (user.getSelectedProfile() == null) {
            return PlayStatus.CAN_PLAY_DEMO;
        }
        if (user.canPlayOnline()) {
            return PlayStatus.CAN_PLAY_ONLINE;
        }
        return PlayStatus.CAN_PLAY_OFFLINE;
    }

    public GameInstanceStatus getInstanceStatus() {
        ProfileManager profileManager = this.launcher.getProfileManager();
        UserAuthentication user = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());

        this.lock.lock();
        try {
            GameRunner gameRunner = (GameRunner) this.instances.get(user);
            if (gameRunner != null) {
                return gameRunner.getStatus();
            }
        } finally {
            this.lock.unlock();
        }
        return GameInstanceStatus.IDLE;
    }

    public void play() {
        ProfileManager profileManager = this.launcher.getProfileManager();
        final Profile profile = profileManager.getSelectedProfile();
        UserAuthentication user = profileManager.getSelectedUser() == null ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        final String lastVersionId = profile.getLastVersionId();
        final MinecraftGameRunner gameRunner = new MinecraftGameRunner(this.launcher, this.additionalLaunchArgs);
        gameRunner.setStatus(GameInstanceStatus.PREPARING);

        this.lock.lock();
        try {
            if ((this.instances.containsKey(user)) || (this.downloadInProgress)) {
                return;
            }
            this.instances.put(user, gameRunner);
            this.downloadInProgress = true;
        } finally {
            this.lock.unlock();
        }
        this.launcher.getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {
            public void run() {
                gameRunner.setVisibility((LauncherVisibilityRule) Objects.firstNonNull(profile.getLauncherVisibilityOnGameClose(), Profile.DEFAULT_LAUNCHER_VISIBILITY));

                VersionSyncInfo syncInfo = null;
                if (lastVersionId != null) {
                    syncInfo = GameLaunchDispatcher.this.launcher.getLauncher().getVersionManager().getVersionSyncInfo(lastVersionId);
                }
                if ((syncInfo == null) || (syncInfo.getLatestVersion() == null)) {
                    syncInfo = (VersionSyncInfo) GameLaunchDispatcher.this.launcher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).get(0);
                }
                gameRunner.setStatus(GameInstanceStatus.IDLE);
                gameRunner.addListener(GameLaunchDispatcher.this);
                gameRunner.playGame(syncInfo);
            }
        });
    }

    public void onGameInstanceChangedState(GameRunner runner, GameInstanceStatus status) {
        this.lock.lock();
        try {
            if (status == GameInstanceStatus.IDLE) {
                this.instances.inverse().remove(runner);
            }
            this.downloadInProgress = false;
            for (GameRunner instance : this.instances.values()) {
                if (instance.getStatus() != GameInstanceStatus.PLAYING) {
                    this.downloadInProgress = true;
                    break;
                }
            }
            this.launcher.getUserInterface().updatePlayState();
        } finally {
            this.lock.unlock();
        }
    }

    public boolean isRunningInSameFolder() {
        this.lock.lock();
        try {
            File currentGameDir = (File) Objects.firstNonNull(this.launcher.getProfileManager().getSelectedProfile().getGameDir(), this.launcher.getLauncher().getWorkingDirectory());
            for (MinecraftGameRunner runner : this.instances.values()) {
                Profile profile = runner.getSelectedProfile();
                if (profile != null) {
                    File otherGameDir = (File) Objects.firstNonNull(profile.getGameDir(), this.launcher.getLauncher().getWorkingDirectory());
                    if (currentGameDir.equals(otherGameDir)) {
                        return true;
                    }
                }
            }
        } finally {
            File currentGameDir;
            this.lock.unlock();
        }
        return false;
    }

    public static enum PlayStatus {
        LOADING("Loading...", false), CAN_PLAY_DEMO("Play Demo", true), CAN_PLAY_ONLINE("Play", true), CAN_PLAY_OFFLINE("Play Offline", true), ALREADY_PLAYING("Already Playing...", false), DOWNLOADING("Installing...", false);

        private final String name;
        private final boolean canPlay;

        private PlayStatus(String name, boolean canPlay) {
            this.name = name;
            this.canPlay = canPlay;
        }

        public String getName() {
            return this.name;
        }

        public boolean canPlay() {
            return this.canPlay;
        }
    }
}