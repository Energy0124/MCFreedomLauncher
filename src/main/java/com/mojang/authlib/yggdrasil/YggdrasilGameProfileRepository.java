package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

public class YggdrasilGameProfileRepository
        implements GameProfileRepository {
    private static final Logger LOGGER;
    private static final String BASE_URL = "https://api.mojang.com/";
    private static final String SEARCH_PAGE_URL = "https://api.mojang.com/profiles/page/";
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    static {
        LOGGER = LogManager.getLogger();
    }

    private final YggdrasilAuthenticationService authenticationService;

    public YggdrasilGameProfileRepository(YggdrasilAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void findProfilesByNames(String[] names, Agent agent, ProfileLookupCallback callback) {
        Set<ProfileCriteria> criteria = Sets.newHashSet();
        for (String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(new ProfileCriteria(name, agent));
            }
        }
        Exception exception = null;
        Set<ProfileCriteria> request = Sets.newHashSet(criteria);
        int page = 1;
        int failCount = 0;
        while (!criteria.isEmpty()) {
            try {
                ProfileSearchResultsResponse response = (ProfileSearchResultsResponse) this.authenticationService.makeRequest(HttpAuthenticationService.constantURL("https://api.mojang.com/profiles/page/" + page), request, ProfileSearchResultsResponse.class, "");
                failCount = 0;
                exception = null;
                if ((response.getSize() == 0) || (response.getProfiles().length == 0)) {
                    LOGGER.debug("Page {} returned empty, aborting search", Integer.valueOf(page));
                } else {
                    LOGGER.debug("Page {} returned {} results of {}, parsing", Integer.valueOf(page), Integer.valueOf(response.getProfiles().length), Integer.valueOf(response.getSize()));
                    for (GameProfile profile : response.getProfiles()) {
                        LOGGER.debug("Successfully looked up profile {}", profile);
                        criteria.remove(new ProfileCriteria(profile.getName(), agent));
                        callback.onProfileLookupSucceeded(profile);
                    }
                    LOGGER.debug("Page {} successfully parsed", Integer.valueOf(page));
                    page++;
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (AuthenticationException e) {
                exception = e;
                failCount++;
                if (failCount != 3) {
                    try {
                        Thread.sleep(750L);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        if (criteria.isEmpty()) {
            LOGGER.debug("Successfully found every profile requested");
        } else {
            LOGGER.debug("{} profiles were missing from search results", Integer.valueOf(criteria.size()));
            if (exception == null) {
                exception = new ProfileNotFoundException("Server did not find the requested profile");
            }
            for (ProfileCriteria profileCriteria : criteria) {
                callback.onProfileLookupFailed(new GameProfile(null, profileCriteria.getName()), exception);
            }
        }
    }

    private class ProfileCriteria {
        private final String name;
        private final String agent;

        private ProfileCriteria(String name, Agent agent) {
            this.name = name;
            this.agent = agent.getName();
        }

        public String getName() {
            return this.name;
        }

        public String getAgent() {
            return this.agent;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }
            ProfileCriteria that = (ProfileCriteria) o;
            return (this.agent.equals(that.agent)) && (this.name.toLowerCase().equals(that.name.toLowerCase()));
        }

        public int hashCode() {
            return 31 * this.name.toLowerCase().hashCode() + this.agent.hashCode();
        }

        public String toString() {
            return new ToStringBuilder(this).append("agent", this.agent).append("name", this.name).toString();
        }
    }
}