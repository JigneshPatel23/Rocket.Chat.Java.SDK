package io.rocketchat.core.model;

import io.rocketchat.common.data.model.UserObject;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by sachin on 19/7/17.
 */

public class SubscriptionObject {

    String roomType;
    Date roomCreated;
    Date lastSeen;
    String roomName;
    String roomId;
    UserObject userInfo;
    Boolean open;
    Boolean alert;
    Integer unread;
    Date updatedAt;
    String subscriptionId;

    String desktopNotifications;
    String mobilePushNotifications;
    String emailNotifications;

    public SubscriptionObject(JSONObject object)  {

        try {
            roomType=object.getString("t");
            if (object.optJSONObject("ts")!=null) {
                roomCreated = new Date(object.getJSONObject("ts").getLong("$date"));
            }
            if (object.optJSONObject("ls")!=null) {
                lastSeen = new Date(object.getJSONObject("ls").getLong("$date"));
            }
            roomName = object.getString("name");
            roomId = object.getString("rid");
            if (object.optJSONObject("u")!=null){
                userInfo=new UserObject(object.optJSONObject("u"));
            }
            open = object.getBoolean("open");
            alert = object.getBoolean("alert");
            unread = object.getInt("unread");
            updatedAt = new Date(object.getJSONObject("_updatedAt").getLong("$date"));
            subscriptionId = object.getString("_id");

            desktopNotifications=object.optString("desktopNotifications");
            mobilePushNotifications=object.optString("mobilePushNotifications");
            emailNotifications=object.optString("emailNotifications");

        }catch (JSONException e) {
            e.printStackTrace();
            System.out.println("name is "+roomName);
        }
    }

    public String getRoomType() {
        return roomType;
    }

    public Date getRoomCreated() {
        return roomCreated;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomId() {
        return roomId;
    }

    public UserObject getUserInfo() {
        return userInfo;
    }

    public Boolean getOpen() {
        return open;
    }

    public Boolean getAlert() {
        return alert;
    }

    public Integer getUnread() {
        return unread;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getDesktopNotifications() {
        return desktopNotifications;
    }

    public String getMobilePushNotifications() {
        return mobilePushNotifications;
    }

    public String getEmailNotifications() {
        return emailNotifications;
    }
}