package net.minecraft.launcher.game;

import com.google.common.base.MoreObjects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.runner.GameRunner;
import com.mojang.launcher.game.runner.GameRunnerListener;
import com.mojang.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

public class GameLaunchDispatcher implements GameRunnerListener {
    private final Launcher launcher;
    private final String[] additionalLaunchArgs;
    private final ReentrantLock lock;
    private final BiMap<UserAuthentication, MinecraftGameRunner> instances;
    private boolean downloadInProgress;

    public GameLaunchDispatcher(final Launcher launcher, final String[] additionalLaunchArgs) {
        this.lock = new ReentrantLock();
        this.instances = HashBiMap.create();
        this.downloadInProgress = false;
        this.launcher = launcher;
        this.additionalLaunchArgs = additionalLaunchArgs;
    }

    public PlayStatus getStatus() {
        final ProfileManager profileManager = this.launcher.getProfileManager();
        final Profile profile = profileManager.getProfiles().isEmpty() ? null : profileManager.getSelectedProfile();
        final UserAuthentication user = (profileManager.getSelectedUser() == null) ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        if (user == null || !user.isLoggedIn() || profile == null || this.launcher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).isEmpty()) {
            return PlayStatus.LOADING;
        }
        this.lock.lock();
        try {
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
        final ProfileManager profileManager = this.launcher.getProfileManager();
        final UserAuthentication user = (profileManager.getSelectedUser() == null) ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        this.lock.lock();
        try {
            final GameRunner gameRunner = this.instances.get(user);
            if (gameRunner != null) {
                return gameRunner.getStatus();
            }
        } finally {
            this.lock.unlock();
        }
        return GameInstanceStatus.IDLE;
    }

    public void play() {
        final ProfileManager profileManager = this.launcher.getProfileManager();
        final Profile profile = profileManager.getSelectedProfile();
        final UserAuthentication user = (profileManager.getSelectedUser() == null) ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());
        final String lastVersionId = profile.getLastVersionId();
        final MinecraftGameRunner gameRunner = new MinecraftGameRunner(this.launcher, this.additionalLaunchArgs);
        gameRunner.setStatus(GameInstanceStatus.PREPARING);
        this.lock.lock();
        try {
            if (this.instances.containsKey(user) || this.downloadInProgress) {
                return;
            }
            this.instances.put(user, gameRunner);
            this.downloadInProgress = true;
        } finally {
            this.lock.unlock();
        }
        this.launcher.getLauncher().getVersionManager().getExecutorService().execute(() -> {
            gameRunner.setVisibility(MoreObjects.firstNonNull(profile.getLauncherVisibilityOnGameClose(), Profile.DEFAULT_LAUNCHER_VISIBILITY));
            VersionSyncInfo syncInfo = null;
            if (lastVersionId != null) {
                syncInfo = GameLaunchDispatcher.this.launcher.getLauncher().getVersionManager().getVersionSyncInfo(lastVersionId);
            }
            if (syncInfo == null || syncInfo.getLatestVersion() == null) {
                syncInfo = GameLaunchDispatcher.this.launcher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter()).get(0);
            }
            gameRunner.setStatus(GameInstanceStatus.IDLE);
            gameRunner.addListener(GameLaunchDispatcher.this);
            gameRunner.playGame(syncInfo);
        });
    }

    @Override
    public void onGameInstanceChangedState(final GameRunner runner, final GameInstanceStatus status) {
        this.lock.lock();
        try {
            if (status == GameInstanceStatus.IDLE) {
                this.instances.inverse().remove(runner);
            }
            this.downloadInProgress = false;
            for (final GameRunner instance : this.instances.values()) {
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
            final File currentGameDir = MoreObjects.firstNonNull(this.launcher.getProfileManager().getSelectedProfile().getGameDir(), this.launcher.getLauncher().getWorkingDirectory());
            for (final MinecraftGameRunner runner : this.instances.values()) {
                final Profile profile = runner.getSelectedProfile();
                if (profile != null) {
                    final File otherGameDir = MoreObjects.firstNonNull(profile.getGameDir(), this.launcher.getLauncher().getWorkingDirectory());
                    if (currentGameDir.equals(otherGameDir)) {
                        return true;
                    }
                }
            }
        } finally {
            this.lock.unlock();
        }
        return false;
    }

    public enum PlayStatus {
        LOADING("Loading...", false),
        CAN_PLAY_DEMO("Play Demo", true),
        CAN_PLAY_ONLINE("Play", true),
        CAN_PLAY_OFFLINE("Play Offline", true),
        ALREADY_PLAYING("Already Playing...", false),
        DOWNLOADING("Installing...", false);

        private final String name;
        private final boolean canPlay;

        PlayStatus(final String name, final boolean canPlay) {
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
