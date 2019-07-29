package org.fogbowcloud.app.core.auth.models;

import java.time.Instant;
import java.util.Objects;
import org.fogbowcloud.app.core.constants.JsonKey;
import org.json.JSONException;
import org.json.JSONObject;

public class DefaultUser implements User {

    private String identifier;
    private String iguassuToken;
    private SessionState sessionState;
    private long sessionTime;

    public DefaultUser(String identifier, String iguassuToken) {
        this.identifier = identifier;
        this.iguassuToken = iguassuToken;
        this.resetSession();
        this.sessionState = SessionState.ACTIVE;
    }

    public static JSONObject toJSON(User user) throws JSONException {
        JSONObject userJson = new JSONObject();
        userJson.put(JsonKey.USER_ID.getKey(), user.getIdentifier());
        userJson.put(JsonKey.IGUASSU_TOKEN.getKey(), user.getIguassuToken());
        userJson.put(
                JsonKey.SESSION_STATE.getKey(),
                user.isActive() ? SessionState.ACTIVE.getState() : SessionState.EXPIRED.getState());
        userJson.put(JsonKey.SESSION_TIME.getKey(), user.getSessionTime());
        return userJson;
    }

    public static User fromJSON(JSONObject userJSON) {
        final DefaultUser newUser =
                new DefaultUser(
                        userJSON.optString(JsonKey.USER_ID.getKey()),
                        userJSON.optString(JsonKey.IGUASSU_TOKEN.getKey()));

        final String newSessionState = userJSON.optString(JsonKey.SESSION_STATE.getKey());

        newUser.changeSessionState(SessionState.valueOf(newSessionState.toUpperCase()));
        newUser.setSessionTime(userJSON.optLong(JsonKey.SESSION_TIME.getKey()));
        return newUser;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public boolean isActive() {
        return this.sessionState == SessionState.ACTIVE;
    }

    @Override
    public void changeSessionState(SessionState state) {
        this.sessionState = state;
    }

    @Override
    public String getIguassuToken() {
        return this.iguassuToken;
    }

    @Override
    public long getSessionTime() {
        return this.sessionTime;
    }

    private void setSessionTime(long sessionTime) {
        this.sessionTime = sessionTime;
    }

    @Override
    public void resetSession() {
        this.sessionTime = Instant.now().getEpochSecond();
        this.sessionState = SessionState.ACTIVE;
    }

    @Override
    public void updateToken(String token) {
        this.iguassuToken = token;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject user = new JSONObject();
        user.put(JsonKey.USER_ID.getKey(), this.identifier);
        user.put(JsonKey.IGUASSU_TOKEN.getKey(), this.iguassuToken);
        user.put(JsonKey.SESSION_STATE.getKey(), this.sessionState.getState());
        user.put(JsonKey.SESSION_TIME.getKey(), this.sessionTime);
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultUser user = (DefaultUser) o;
        return sessionState == user.sessionState
                && sessionTime == user.sessionTime
                && identifier.equals(user.identifier)
                && iguassuToken.equals(user.iguassuToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, iguassuToken, sessionState, sessionTime);
    }
}