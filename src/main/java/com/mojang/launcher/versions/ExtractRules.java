package com.mojang.launcher.versions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtractRules {
    private final List<String> exclude;

    public ExtractRules() {
        this.exclude = new ArrayList<>();
    }

    public ExtractRules(final String... exclude) {
        this.exclude = new ArrayList<>();
        if (exclude != null) {
            Collections.addAll(this.exclude, exclude);
        }
    }

    public ExtractRules(final ExtractRules rules) {
        this.exclude = new ArrayList<>();
        this.exclude.addAll(rules.exclude);
    }

    public List<String> getExcludes() {
        return this.exclude;
    }

    public boolean shouldExtract(final String path) {
        if (this.exclude != null) {
            for (final String rule : this.exclude) {
                if (path.startsWith(rule)) {
                    return false;
                }
            }
        }
        return true;
    }
}
