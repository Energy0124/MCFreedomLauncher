package com.mojang.authlib;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseUserAuthentication implements UserAuthentication {
    private static final String STORAGE_KEY_PROFILE_NAME = "displayName";
    private static final String STORAGE_KEY_PROFILE_ID = "uuid";
    private static final String STORAGE_KEY_PROFILE_PROPERTIES = "profileProperties";
    private static final String STORAGE_KEY_USER_NAME = "username";
    private static final String STORAGE_KEY_USER_ID = "userid";
    private static final String STORAGE_KEY_USER_PROPERTIES = "userProperties";
    private static final Logger LOGGER = LogManager.getLogger();

    private final AuthenticationService authenticationService;
    private final PropertyMap userProperties;
    private String userid;
    private String username;
    private String password;
    private GameProfile selectedProfile;
    private UserType userType;

    BaseUserAuthentication(final AuthenticationService authenticationService) {
        this.userProperties = new PropertyMap();
        Validate.notNull(authenticationService);
        this.authenticationService = authenticationService;
    }

    @Override
    public boolean canLogIn() {
        return !this.canPlayOnline() && StringUtils.isNotBlank(this.getUsername()) && StringUtils.isNotBlank(this.getPassword());
    }

    @Override
    public void logOut() {
        this.password = null;
        this.userid = null;
        this.setSelectedProfile(null);
        this.getModifiableUserProperties().clear();
        this.setUserType(null);
    }

    @Override
    public boolean isLoggedIn() {
        return this.getSelectedProfile() != null;
    }

    protected String getUsername() {
        return this.username;
    }

    @Override
    public void setUsername(final String username) {
        if (this.isLoggedIn() && this.canPlayOnline()) {
            throw new IllegalStateException("Cannot change username whilst logged in & online");
        }
        this.username = username;
    }

    protected String getPassword() {
        return this.password;
    }

    @Override
    public void setPassword(final String password) {
        if (this.isLoggedIn() && this.canPlayOnline() && StringUtils.isNotBlank(password)) {
            throw new IllegalStateException("Cannot set password whilst logged in & online");
        }
        this.password = password;
    }

    @Override
    public void loadFromStorage(final Map<String, Object> credentials) {
        this.logOut();
        this.setUsername(String.valueOf(credentials.get(STORAGE_KEY_USER_NAME)));
        if (credentials.containsKey(STORAGE_KEY_USER_ID)) {
            this.userid = String.valueOf(credentials.get(STORAGE_KEY_USER_ID));
        } else {
            this.userid = this.username;
        }
        if (credentials.containsKey(STORAGE_KEY_USER_PROPERTIES)) {
            try {
                final List<Map<String, String>> list = (List) credentials.get(STORAGE_KEY_USER_PROPERTIES);
                for (final Map<String, String> propertyMap : list) {
                    final String name = propertyMap.get("name");
                    final String value = propertyMap.get("value");
                    final String signature = propertyMap.get("signature");
                    if (signature == null) {
                        this.getModifiableUserProperties().put(name, new Property(name, value));
                    } else {
                        this.getModifiableUserProperties().put(name, new Property(name, value, signature));
                    }
                }
            } catch (Throwable t) {
                BaseUserAuthentication.LOGGER.warn("Couldn't deserialize user properties", t);
            }
        }
        if (credentials.containsKey(STORAGE_KEY_PROFILE_NAME) && credentials.containsKey(STORAGE_KEY_PROFILE_ID)) {
            final GameProfile profile = new GameProfile(UUIDTypeAdapter.fromString(String.valueOf(credentials.get(STORAGE_KEY_PROFILE_ID))), String.valueOf(credentials.get(STORAGE_KEY_PROFILE_NAME)));
            if (credentials.containsKey(STORAGE_KEY_PROFILE_PROPERTIES)) {
                try {
                    final List<Map<String, String>> list2 = (List) credentials.get(STORAGE_KEY_PROFILE_PROPERTIES);
                    for (final Map<String, String> propertyMap2 : list2) {
                        final String name2 = propertyMap2.get("name");
                        final String value2 = propertyMap2.get("value");
                        final String signature2 = propertyMap2.get("signature");
                        if (signature2 == null) {
                            profile.getProperties().put(name2, new Property(name2, value2));
                        } else {
                            profile.getProperties().put(name2, new Property(name2, value2, signature2));
                        }
                    }
                } catch (Throwable t2) {
                    BaseUserAuthentication.LOGGER.warn("Couldn't deserialize profile properties", t2);
                }
            }
            this.setSelectedProfile(profile);
        }
    }

    @Override
    public Map<String, Object> saveForStorage() {
        final Map<String, Object> result = new HashMap<>();
        if (this.getUsername() != null) {
            result.put(STORAGE_KEY_USER_NAME, this.getUsername());
        }
        if (this.getUserID() != null) {
            result.put(STORAGE_KEY_USER_ID, this.getUserID());
        } else if (this.getUsername() != null) {
            result.put(STORAGE_KEY_USER_NAME, this.getUsername());
        }
        if (!this.getUserProperties().isEmpty()) {
            final List<Map<String, String>> properties = new ArrayList<>();
            for (final Property userProperty : (this.getUserProperties()).values()) {
                final Map<String, String> property = new HashMap<>();
                property.put("name", userProperty.getName());
                property.put("value", userProperty.getValue());
                property.put("signature", userProperty.getSignature());
                properties.add(property);
            }
            result.put(STORAGE_KEY_USER_PROPERTIES, properties);
        }
        final GameProfile selectedProfile = this.getSelectedProfile();
        if (selectedProfile != null) {
            result.put(STORAGE_KEY_PROFILE_NAME, selectedProfile.getName());
            result.put(STORAGE_KEY_PROFILE_ID, selectedProfile.getId());
            final List<Map<String, String>> properties2 = new ArrayList<>();
            for (final Property profileProperty : (selectedProfile.getProperties()).values()) {
                final Map<String, String> property2 = new HashMap<>();
                property2.put("name", profileProperty.getName());
                property2.put("value", profileProperty.getValue());
                property2.put("signature", profileProperty.getSignature());
                properties2.add(property2);
            }
            if (!properties2.isEmpty()) {
                result.put(STORAGE_KEY_PROFILE_PROPERTIES, properties2);
            }
        }
        return result;
    }

    @Override
    public GameProfile getSelectedProfile() {
        return this.selectedProfile;
    }

    protected void setSelectedProfile(final GameProfile selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(this.getClass().getSimpleName());
        result.append("{");
        if (this.isLoggedIn()) {
            result.append("Logged in as ");
            result.append(this.getUsername());
            if (this.getSelectedProfile() != null) {
                result.append(" / ");
                result.append(this.getSelectedProfile());
                result.append(" - ");
                if (this.canPlayOnline()) {
                    result.append("Online");
                } else {
                    result.append("Offline");
                }
            }
        } else {
            result.append("Not logged in");
        }
        result.append("}");
        return result.toString();
    }

    AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }

    @Override
    public String getUserID() {
        return this.userid;
    }

    @Override
    public PropertyMap getUserProperties() {
        if (this.isLoggedIn()) {
            final PropertyMap result = new PropertyMap();
            result.putAll(this.getModifiableUserProperties());
            return result;
        }
        return new PropertyMap();
    }

    protected PropertyMap getModifiableUserProperties() {
        return this.userProperties;
    }

    @Override
    public UserType getUserType() {
        if (this.isLoggedIn()) {
            return (this.userType == null) ? UserType.LEGACY : this.userType;
        }
        return null;
    }

    protected void setUserType(final UserType userType) {
        this.userType = userType;
    }

    protected void setUserid(final String userid) {
        this.userid = userid;
    }
}
