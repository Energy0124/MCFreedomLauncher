package com.mojang.launcher.versions;

import com.mojang.launcher.OperatingSystem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompatibilityRule {
    private Action action = Action.ALLOW;
    private OSRestriction os;

    public CompatibilityRule() {
    }

    public CompatibilityRule(CompatibilityRule compatibilityRule) {
        this.action = compatibilityRule.action;
        if (compatibilityRule.os != null) {
            this.os = new OSRestriction(compatibilityRule.os);
        }
    }

    public Action getAppliedAction() {
        if ((this.os != null) && (!this.os.isCurrentOperatingSystem())) {
            return null;
        }
        return this.action;
    }

    public Action getAction() {
        return this.action;
    }

    public OSRestriction getOs() {
        return this.os;
    }

    public String toString() {
        return "Rule{action=" + this.action + ", os=" + this.os + '}';
    }

    public static enum Action {
        ALLOW, DISALLOW;

        private Action() {
        }
    }

    public class OSRestriction {
        private OperatingSystem name;
        private String version;
        private String arch;

        public OSRestriction() {
        }

        public OSRestriction(OSRestriction osRestriction) {
            this.name = osRestriction.name;
            this.version = osRestriction.version;
            this.arch = osRestriction.arch;
        }

        public OperatingSystem getName() {
            return this.name;
        }

        public String getVersion() {
            return this.version;
        }

        public String getArch() {
            return this.arch;
        }

        public boolean isCurrentOperatingSystem() {
            if ((this.name != null) && (this.name != OperatingSystem.getCurrentPlatform())) {
                return false;
            }
            if (this.version != null) {
                try {
                    Pattern pattern = Pattern.compile(this.version);
                    Matcher matcher = pattern.matcher(System.getProperty("os.version"));
                    if (!matcher.matches()) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (this.arch != null) {
                try {
                    Pattern pattern = Pattern.compile(this.arch);
                    Matcher matcher = pattern.matcher(System.getProperty("os.arch"));
                    if (!matcher.matches()) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
            }
            return true;
        }

        public String toString() {
            return "OSRestriction{name=" + this.name + ", version='" + this.version + '\'' + ", arch='" + this.arch + '\'' + '}';
        }
    }
}