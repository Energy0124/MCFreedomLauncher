package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;

import static net.minecraft.launcher.LauncherConstants.URL_PROFILES_API;

public class YggdrasilGameProfileRepository implements GameProfileRepository {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SEARCH_PAGE_URL = URL_PROFILES_API;
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final long DELAY_BETWEEN_PAGES = 100L;
    private static final long DELAY_BETWEEN_FAILURES = 750L;

    private final YggdrasilAuthenticationService authenticationService;

    public YggdrasilGameProfileRepository(final YggdrasilAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public void findProfilesByNames(final String[] names, final Agent agent, final ProfileLookupCallback callback) {
        final Set<String> criteria = Sets.newHashSet();
        for (final String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(name.toLowerCase());
            }
        }
        final int page = 0;
        for (final List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            int failCount = 0;
            boolean failed;
            do {
                failed = false;
                try {
                    final ProfileSearchResultsResponse response = this.authenticationService.makeRequest(HttpAuthenticationService.constantURL(SEARCH_PAGE_URL + agent.getName().toLowerCase()), request, ProfileSearchResultsResponse.class, "");
                    failCount = 0;
                    YggdrasilGameProfileRepository.LOGGER.debug("Page {} returned {} results, parsing", page, response.getProfiles().length);
                    final Set<String> missing = Sets.newHashSet(request);
                    for (final GameProfile profile : response.getProfiles()) {
                        YggdrasilGameProfileRepository.LOGGER.debug("Successfully looked up profile {}", profile);
                        missing.remove(profile.getName().toLowerCase());
                        callback.onProfileLookupSucceeded(profile);
                    }
                    for (final String name2 : missing) {
                        YggdrasilGameProfileRepository.LOGGER.debug("Couldn't find profile {}", name2);
                        callback.onProfileLookupFailed(new GameProfile(null, name2), new ProfileNotFoundException("Server did not find the requested profile"));
                    }
                    try {
                        Thread.sleep(DELAY_BETWEEN_PAGES);
                    } catch (InterruptedException ex) {
                        LOGGER.debug("An InterruptedException is caught!");
                    }
                } catch (AuthenticationException e) {
                    if (++failCount == MAX_FAIL_COUNT) {
                        for (final String name3 : request) {
                            YggdrasilGameProfileRepository.LOGGER.debug("Couldn't find profile {} because of a server error", name3);
                            callback.onProfileLookupFailed(new GameProfile(null, name3), e);
                        }
                    } else {
                        try {
                            Thread.sleep(DELAY_BETWEEN_FAILURES);
                        } catch (InterruptedException ex2) {
                            LOGGER.debug("An InterruptedException is caught!");
                        }
                        failed = true;
                    }
                }
            } while (failed);
        }
    }

}
