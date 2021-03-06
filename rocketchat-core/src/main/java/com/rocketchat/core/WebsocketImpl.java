package com.rocketchat.core;

import com.rocketchat.common.RocketChatException;
import com.rocketchat.common.SocketListener;
import com.rocketchat.common.data.lightstream.GlobalStreamCollectionManager;
import com.rocketchat.common.data.model.MessageType;
import com.rocketchat.common.data.model.User;
import com.rocketchat.common.data.model.internal.ConnectedMessage;
import com.rocketchat.common.data.rpc.RPC;
import com.rocketchat.common.listener.ConnectListener;
import com.rocketchat.common.listener.SimpleCallback;
import com.rocketchat.common.listener.SimpleListCallback;
import com.rocketchat.common.listener.SubscribeCallback;
import com.rocketchat.common.listener.TypingListener;
import com.rocketchat.common.network.ConnectivityManager;
import com.rocketchat.common.network.ReconnectionStrategy;
import com.rocketchat.common.network.Socket;
import com.rocketchat.common.network.SocketFactory;
import com.rocketchat.common.utils.Logger;
import com.rocketchat.common.utils.Utils;
import com.rocketchat.core.callback.HistoryCallback;
import com.rocketchat.core.callback.LoginCallback;
import com.rocketchat.core.callback.MessageCallback;
import com.rocketchat.core.callback.RoomCallback;
import com.rocketchat.core.factory.ChatRoomFactory;
import com.rocketchat.core.internal.middleware.CoreMiddleware;
import com.rocketchat.core.internal.middleware.CoreStreamMiddleware;
import com.rocketchat.core.internal.rpc.AccountRPC;
import com.rocketchat.core.internal.rpc.BasicRPC;
import com.rocketchat.core.internal.rpc.ChatHistoryRPC;
import com.rocketchat.core.internal.rpc.CoreSubRPC;
import com.rocketchat.core.internal.rpc.FileUploadRPC;
import com.rocketchat.core.internal.rpc.MessageRPC;
import com.rocketchat.core.internal.rpc.PresenceRPC;
import com.rocketchat.core.internal.rpc.RoomRPC;
import com.rocketchat.core.internal.rpc.TypingRPC;
import com.rocketchat.core.model.Emoji;
import com.rocketchat.core.model.Message;
import com.rocketchat.core.model.Permission;
import com.rocketchat.core.model.PublicSetting;
import com.rocketchat.core.model.Room;
import com.rocketchat.core.model.RoomRole;
import com.rocketchat.core.model.Subscription;
import com.rocketchat.core.model.Token;
import com.rocketchat.core.roomstream.LocalStreamCollectionManager;
import com.rocketchat.core.uploader.IFileUpload;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;

public class WebsocketImpl implements SocketListener {
    private final OkHttpClient client;
    private final SocketFactory factory;
    private final Moshi moshi;
    private final String baseUrl;
    private final Logger logger;
    private final Socket socket;

    private final CoreMiddleware coreMiddleware;
    private final CoreStreamMiddleware coreStreamMiddleware;

    private AtomicInteger integer;
    private String sessionId;
    private String userId;

    private final ConnectivityManager connectivityManager;
    private GlobalStreamCollectionManager globalStreamCollectionManager;
    private ChatRoomFactory chatRoomFactory;

    WebsocketImpl(OkHttpClient client, SocketFactory factory, Moshi moshi, String baseUrl, Logger logger, ChatRoomFactory chatRoomFactory, GlobalStreamCollectionManager globalStreamCollectionManager, ConnectivityManager connectivityManager) {
        this.client = client;
        this.factory = factory;
        this.baseUrl = baseUrl;
        this.moshi = moshi;
        this.logger = logger;
        this.socket = factory.create(client, baseUrl, logger, this);


        coreMiddleware = new CoreMiddleware(moshi);
        coreStreamMiddleware = new CoreStreamMiddleware(moshi);

        integer = new AtomicInteger(1);

        this.connectivityManager = connectivityManager;
        this.globalStreamCollectionManager = globalStreamCollectionManager;
        this.chatRoomFactory = chatRoomFactory;
    }

    void connect(ConnectListener connectListener) {
        connectivityManager.register(connectListener);
        socket.connect();
    }

    void disconnect() {
        socket.disconnect();
    }

    public Socket getSocket() {
        return socket;
    }


    public String getMyUserId() {
        return userId;
    }

    //Tested
    void login(String username, String password, final LoginCallback delegate) {
        LoginCallback callback = new LoginCallback() {
            @Override
            public void onLoginSuccess(Token token) {
                userId = token.userId();
                delegate.onLoginSuccess(token);
            }

            @Override
            public void onError(RocketChatException error) {
                delegate.onError(error);
            }
        };
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.LOGIN);
        socket.sendData(BasicRPC.login(uniqueID, username, password));
    }

    //Tested
    void loginUsingToken(String token, final LoginCallback delegate) {
        LoginCallback callback = new LoginCallback() {
            @Override
            public void onLoginSuccess(Token token) {
                userId = token.userId();
                delegate.onLoginSuccess(token);
            }

            @Override
            public void onError(RocketChatException error) {
                delegate.onError(error);
            }
        };
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.LOGIN);
        socket.sendData(BasicRPC.loginUsingToken(uniqueID, token));
    }

    //Tested
    void getPermissions(SimpleListCallback<Permission> callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.GET_PERMISSIONS);
        socket.sendData(AccountRPC.getPermissions(uniqueID, null));
    }

    //Tested
    void getPublicSettings(SimpleListCallback<PublicSetting> callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.GET_PUBLIC_SETTINGS);
        socket.sendData(AccountRPC.getPublicSettings(uniqueID, null));
    }

    //Tested
    void getUserRoles(SimpleListCallback<User> callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.GET_USER_ROLES);
        socket.sendData(BasicRPC.getUserRoles(uniqueID));
    }

    //Tested
    void listCustomEmoji(SimpleListCallback<Emoji> callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.LIST_CUSTOM_EMOJI);
        socket.sendData(BasicRPC.listCustomEmoji(uniqueID));
    }

    //Tested
    void logout(SimpleCallback listener) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, listener, CoreMiddleware.CallbackType.LOGOUT);
        socket.sendData(BasicRPC.logout(uniqueID));
    }

    //Tested
    void getSubscriptions(SimpleListCallback<Subscription> callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.GET_SUBSCRIPTIONS);
        socket.sendData(BasicRPC.getSubscriptions(uniqueID));
    }

    //Tested
    void getRooms(SimpleListCallback<Room> callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.GET_ROOMS);
        socket.sendData(BasicRPC.getRooms(uniqueID));
    }

    //Tested
    void getRoomRoles(String roomId, SimpleListCallback<RoomRole> callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.GET_ROOM_ROLES);
        socket.sendData(BasicRPC.getRoomRoles(uniqueID, roomId));
    }

    //Tested
    void getChatHistory(String roomID, int limit, Date oldestMessageTimestamp,
                        Date lasttimestamp, HistoryCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.LOAD_HISTORY);
        socket.sendData(ChatHistoryRPC.loadHistory(uniqueID, roomID, oldestMessageTimestamp, limit, lasttimestamp));
    }

    //Tested
    void sendIsTyping(String roomId, String username, Boolean istyping) {
        int uniqueID = integer.getAndIncrement();
        socket.sendData(TypingRPC.sendTyping(uniqueID, roomId, username, istyping));
    }

    //Tested
    void sendMessage(String msgId, String roomID, String message, MessageCallback.MessageAckCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.SEND_MESSAGE);
        socket.sendData(MessageRPC.sendMessage(uniqueID, msgId, roomID, message));
    }

    //Tested
    void deleteMessage(String msgId, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.MESSAGE_OP);
        socket.sendData(MessageRPC.deleteMessage(uniqueID, msgId));
    }

    //Tested
    void updateMessage(String msgId, String roomId, String message, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.MESSAGE_OP);
        socket.sendData(MessageRPC.updateMessage(uniqueID, msgId, roomId, message));
    }

    //Tested
    void pinMessage(JSONObject message, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.MESSAGE_OP);
        socket.sendData(MessageRPC.pinMessage(uniqueID, message));
    }

    //Tested
    void unpinMessage(JSONObject message, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.MESSAGE_OP);
        socket.sendData(MessageRPC.unpinMessage(uniqueID, message));
    }

    //Tested
    void starMessage(String msgId, String roomId, Boolean starred, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.MESSAGE_OP);
        socket.sendData(MessageRPC.starMessage(uniqueID, msgId, roomId, starred));
    }

    //Tested
    void setReaction(String emojiId, String msgId, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.MESSAGE_OP);
        socket.sendData(MessageRPC.setReaction(uniqueID, emojiId, msgId));
    }

    void searchMessage(String message, String roomId, int limit,
                       SimpleListCallback<Message> callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.SEARCH_MESSAGE);
        socket.sendData(MessageRPC.searchMessage(uniqueID, message, roomId, limit));
    }

    //Tested
    void createPublicGroup(String groupName, String[] users, Boolean readOnly,
                           RoomCallback.GroupCreateCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.CREATE_GROUP);
        socket.sendData(RoomRPC.createPublicGroup(uniqueID, groupName, users, readOnly));
    }

    //Tested
    void createPrivateGroup(String groupName, String[] users,
                            RoomCallback.GroupCreateCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.CREATE_GROUP);
        socket.sendData(RoomRPC.createPrivateGroup(uniqueID, groupName, users));
    }

    //Tested
    void deleteGroup(String roomId, SimpleCallback callback) {
        //Apply simpleListener
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.DELETE_GROUP);
        socket.sendData(RoomRPC.deleteGroup(uniqueID, roomId));
    }

    //Tested
    void archiveRoom(String roomId, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.ARCHIVE);
        socket.sendData(RoomRPC.archieveRoom(uniqueID, roomId));
    }

    //Tested
    void unarchiveRoom(String roomId, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.UNARCHIVE);
        socket.sendData(RoomRPC.unarchiveRoom(uniqueID, roomId));
    }

    //Tested
    void joinPublicGroup(String roomId, String joinCode, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.JOIN_PUBLIC_GROUP);
        socket.sendData(RoomRPC.joinPublicGroup(uniqueID, roomId, joinCode));
    }

    //Tested
    void leaveGroup(String roomId, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.LEAVE_GROUP);
        socket.sendData(RoomRPC.leaveGroup(uniqueID, roomId));
    }

    //Tested
    void hideRoom(String roomId, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.HIDE_ROOM);
        socket.sendData(RoomRPC.hideRoom(uniqueID, roomId));
    }

    //Tested
    void openRoom(String roomId, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.OPEN_ROOM);
        socket.sendData(RoomRPC.openRoom(uniqueID, roomId));
    }

    //Tested
    void setFavouriteRoom(String roomId, Boolean isFavouriteRoom, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.SET_FAVOURITE_ROOM);
        socket.sendData(RoomRPC.setFavouriteRoom(uniqueID, roomId, isFavouriteRoom));
    }

    void sendFileMessage(String roomId, String store, String fileId, String fileType,
                         int size, String fileName, String desc, String url,
                         MessageCallback.MessageAckCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.SEND_MESSAGE);
        socket.sendData(MessageRPC.sendFileMessage(uniqueID, roomId, store, fileId, fileType, size, fileName, desc, url));
    }

    //Tested
    void setStatus(User.Status s, SimpleCallback callback) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, callback, CoreMiddleware.CallbackType.SET_STATUS);
        socket.sendData(PresenceRPC.setDefaultStatus(uniqueID, s));
    }

    void subscribeActiveUsers(SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeActiveUsers(uniqueID));
    }

    void subscribeUserData(SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeUserData(uniqueID));
    }

    void subscribeUserRoles(SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeUserRoles(uniqueID));
    }

    void subscribeLoginConf(SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeLoginServiceConfiguration(uniqueID));
    }

    void subscribeClientVersions(SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeClientVersions(uniqueID));
    }

    String subscribeRoomFiles(String roomId, int limit, SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeRoomFiles(uniqueID, roomId, limit));
        return uniqueID;
    }

    String subscribeMentionedMessages(String roomId, int limit, SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeMentionedMessages(uniqueID, roomId, limit));
        return uniqueID;
    }

    String subscribeStarredMessages(String roomId, int limit, SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeStarredMessages(uniqueID, roomId, limit));
        return uniqueID;
    }

    String subscribePinnedMessages(String roomId, int limit, SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribePinnedMessages(uniqueID, roomId, limit));
        return uniqueID;
    }

    String subscribeSnipettedMessages(String roomId, int limit, SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeSnipettedMessages(uniqueID, roomId, limit));
        return uniqueID;
    }

    //Tested
    String subscribeRoomMessageEvent(String roomId, Boolean enable, SubscribeCallback subscribeCallback, MessageCallback.MessageListener listener) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        coreStreamMiddleware.createSubscription(roomId, listener, CoreStreamMiddleware.SubscriptionType.SUBSCRIBE_ROOM_MESSAGE);
        socket.sendData(CoreSubRPC.subscribeRoomMessageEvent(uniqueID, roomId, enable));
        return uniqueID;
    }

    String subscribeRoomTypingEvent(String roomId, Boolean enable, SubscribeCallback subscribeCallback, TypingListener listener) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        coreStreamMiddleware.createSubscription(roomId, listener, CoreStreamMiddleware.SubscriptionType.SUBSCRIBE_ROOM_TYPING);
        socket.sendData(CoreSubRPC.subscribeRoomTypingEvent(uniqueID, roomId, enable));
        return uniqueID;
    }

    String subscribeRoomDeleteEvent(String roomId, Boolean enable, SubscribeCallback subscribeCallback) {
        String uniqueID = Utils.shortUUID();
        coreStreamMiddleware.createSubscriptionListener(uniqueID, subscribeCallback);
        socket.sendData(CoreSubRPC.subscribeRoomMessageDeleteEvent(uniqueID, roomId, enable));
        return uniqueID;
    }

    void unsubscribeRoom(String subId, SubscribeCallback subscribeCallback) {
        socket.sendData(CoreSubRPC.unsubscribeRoom(subId));
        coreStreamMiddleware.createSubscriptionListener(subId, subscribeCallback);
    }

    void setReconnectionStrategy(ReconnectionStrategy strategy) {
        socket.setReconnectionStrategy(strategy);
    }

    void createUFS(String fileName, int fileSize, String fileType, String roomId, String description, String store, IFileUpload.UfsCreateCallback listener) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, listener, CoreMiddleware.CallbackType.UFS_CREATE);
        socket.sendData(FileUploadRPC.ufsCreate(uniqueID, fileName, fileSize, fileType, roomId, description, store));
    }

    void completeUFS(String fileId, String store, String token, IFileUpload.UfsCompleteListener listener) {
        int uniqueID = integer.getAndIncrement();
        coreMiddleware.createCallback(uniqueID, listener, CoreMiddleware.CallbackType.UFS_COMPLETE);
        socket.sendData(FileUploadRPC.ufsComplete(uniqueID, fileId, store, token));
    }

    void setPingInterval(long interval) {
        socket.setPingInterval(interval);
    }

    void disablePing() {
        socket.disablePing();
    }

    void enablePing() {
        socket.enablePing();
    }

    private void processOnConnected(String message) {
        JsonAdapter<ConnectedMessage> adapter = moshi.adapter(ConnectedMessage.class);
        try {
            ConnectedMessage connectedMessage = adapter.fromJson(message);
            sessionId = connectedMessage.session();
            connectivityManager.publishConnect(sessionId);
        } catch (IOException e) {
            e.printStackTrace();
            coreMiddleware.notifyDisconnection(e.getMessage());
            coreStreamMiddleware.cleanup();
            connectivityManager.publishConnectError(e);
        }
    }

    private void processCollectionsAdded(JSONObject object) {
        if (userId == null) {
            userId = object.optString("id");
        }

        switch (GlobalStreamCollectionManager.getCollectionType(object)) {
            case OTHER_COLLECTION:
                ChatRoom room = chatRoomFactory.getChatRoomById(getRoomIdFromCollection(object));
                if (room != null) {
//                    System.out.println("Got into room " + room.getRoomData().getRoomName());
                    room.getLocalStreamCollectionManager().update(object, RPC.MsgType.ADDED);
                } else {
                    System.out.println("Room not found for subscribed room");
                }
                break;
            case GLOBAL_COLLECTION:
                globalStreamCollectionManager.update(object, RPC.MsgType.ADDED);
                break;
        }
    }

    private void processCollectionsChanged(JSONObject object) {
        switch (GlobalStreamCollectionManager.getCollectionType(object)) {
            case OTHER_COLLECTION:
                switch (LocalStreamCollectionManager.getCollectionType(object)) {
                    case STREAM_COLLECTION:
                        coreStreamMiddleware.processListeners(object);
                        break;
                    case LOCAL_COLLECTION:
                        System.out.println("Local collection " + object.toString());
                        ChatRoom room = chatRoomFactory.getChatRoomById(getRoomIdFromCollection(object));
                        if (room != null) {
                            System.out.println("Got into room " + room.getRoomData().name());
                            room.getLocalStreamCollectionManager().update(object, RPC.MsgType.CHANGED);
                        } else {
                            System.out.println("Room not found for subscribed room");
                        }
                        break;
                }
                break;
            case GLOBAL_COLLECTION:
                globalStreamCollectionManager.update(object, RPC.MsgType.CHANGED);
                break;
        }
    }

    private void processCollectionsRemoved(JSONObject object) {
        switch (GlobalStreamCollectionManager.getCollectionType(object)) {
            case OTHER_COLLECTION:
                System.out.println("Local collection " + object.toString());
                ChatRoom room = chatRoomFactory.getChatRoomById(getRoomIdFromCollection(object));
                if (room != null) {
                    System.out.println("Got into room " + room.getRoomData().name());
                    room.getLocalStreamCollectionManager().update(object, RPC.MsgType.REMOVED);
                } else {
                    System.out.println("Room not found for subscribed room");
                }
                break;
            case GLOBAL_COLLECTION:
                globalStreamCollectionManager.update(object, RPC.MsgType.REMOVED);
                break;
        }
    }

    private String getRoomIdFromCollection(JSONObject object) {
        String roomId = null;
        try {
            roomId = object.getJSONObject("fields").getString("rid");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return roomId;
    }

    @Override
    public void onConnected() {
        logger.info("RocketChatAPI Connected");
        integer.set(1);
        socket.sendData(BasicRPC.ConnectObject());
    }

    @Override
    public void onMessageReceived(MessageType type, /* nullable */ String id, String message) {
        /* FIXME - temporary JSONObject while we don't convert everything to Moshi and AutoValue */
        JSONObject object = null;
        try {
            object = new JSONObject(message);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        switch (type) {
            case CONNECTED:
                processOnConnected(message);
                break;
            case PING:
                socket.sendData(RPC.PONG_MESSAGE);
                break;
            case RESULT:
                coreMiddleware.processCallback(Long.valueOf(id), object, message);
                break;
            case READY:
                coreStreamMiddleware.processSubscriptionSuccess(object);
                break;
            case ADDED:
                processCollectionsAdded(object);
                break;
            case CHANGED:
                processCollectionsChanged(object);
                break;
            case REMOVED:
                processCollectionsRemoved(object);
                break;
            case UNSUBSCRIBED:
                coreStreamMiddleware.processUnsubscriptionSuccess(object);
                break;
        }
    }

    @Override
    public void onClosing() {
        logger.info("onClosing");
    }

    @Override
    public void onClosed() {
        logger.info("onClosed");
        coreMiddleware.cleanup();
        coreStreamMiddleware.cleanup();
        connectivityManager.publishDisconnect(true);
    }

    @Override
    public void onFailure(Throwable throwable) {
        throwable.printStackTrace();
        logger.info("onFailure: " + throwable);
        coreMiddleware.notifyDisconnection(throwable.getMessage());
        coreStreamMiddleware.cleanup();
        connectivityManager.publishConnectError(throwable);
    }

    void removeSubscription(String roomId, CoreStreamMiddleware.SubscriptionType type) {
        coreStreamMiddleware.removeSubscription(roomId, type);
    }

    void removeAllSubscriptions(String roomId) {
        coreStreamMiddleware.removeAllSubscriptions(roomId);
    }
}
