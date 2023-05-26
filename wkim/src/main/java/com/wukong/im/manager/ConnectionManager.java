package com.wukong.im.manager;

import android.text.TextUtils;

import com.wukong.im.WKIM;
import com.wukong.im.WKIMApplication;
import com.wukong.im.interfaces.IConnectionStatus;
import com.wukong.im.interfaces.IGetIpAndPort;
import com.wukong.im.interfaces.IGetSocketIpAndPortListener;
import com.wukong.im.message.ConnectionHandler;
import com.wukong.im.message.MessageHandler;
import com.wukong.im.utils.WKLoggerUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/21/21 10:31 AM
 * connect manager
 */
public class ConnectionManager extends BaseManager {
    private ConnectionManager() {

    }

    private static class ConnectionManagerBinder {
        static final ConnectionManager connectManager = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return ConnectionManagerBinder.connectManager;
    }


    private IGetIpAndPort iGetIpAndPort;
    private ConcurrentHashMap<String, IConnectionStatus> concurrentHashMap;

    // 连接
    public void connection() {
        if (TextUtils.isEmpty(WKIMApplication.getInstance().getToken()) || TextUtils.isEmpty(WKIMApplication.getInstance().getUid())) {
            throw new NullPointerException("连接UID和Token不能为空");
        }
        WKIMApplication.getInstance().isCanConnect = true;
        if (ConnectionHandler.getInstance().connectionIsNull()) {
            ConnectionHandler.getInstance().reconnection();
        }
    }


    public void disconnect(boolean isLogout) {
        if (TextUtils.isEmpty(WKIMApplication.getInstance().getToken())) return;
        WKLoggerUtils.getInstance().e("断开连接，是否退出IM:" + isLogout);
        if (isLogout) {
            logoutChat();
        } else {
            stopConnect();
        }
    }

    /**
     * 断开连接
     */
    private void stopConnect() {
        WKIMApplication.getInstance().isCanConnect = false;
        ConnectionHandler.getInstance().stopAll();
    }

    /**
     * 退出登录
     */
    private void logoutChat() {
        WKLoggerUtils.getInstance().e("退出登录设置不能连接");
        WKIMApplication.getInstance().isCanConnect = false;
        MessageHandler.getInstance().saveReceiveMsg();

        WKIMApplication.getInstance().setToken("");
        MessageHandler.getInstance().updateLastSendingMsgFail();
        ConnectionHandler.getInstance().stopAll();
        WKIM.getInstance().getChannelManager().clearARMCache();
        WKIMApplication.getInstance().closeDbHelper();
    }

    public void getIpAndPort(IGetSocketIpAndPortListener iGetIpAndPortListener) {
        if (iGetIpAndPort != null) {
            runOnMainThread(() -> iGetIpAndPort.getIP(iGetIpAndPortListener));
        }
    }

    // 监听获取IP和port
    public void addOnGetIpAndPortListener(IGetIpAndPort iGetIpAndPort) {
        this.iGetIpAndPort = iGetIpAndPort;
    }

    public void setConnectionStatus(int status, String reason) {
        if (concurrentHashMap != null && concurrentHashMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IConnectionStatus> entry : concurrentHashMap.entrySet()) {
                    entry.getValue().onStatus(status, reason);
                }
            });
        }
    }

    // 监听连接状态
    public void addOnConnectionStatusListener(String key, IConnectionStatus iConnectionStatus) {
        if (iConnectionStatus == null || TextUtils.isEmpty(key)) return;
        if (concurrentHashMap == null) concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put(key, iConnectionStatus);
    }

    // 移除监听
    public void removeOnConnectionStatusListener(String key) {
        if (!TextUtils.isEmpty(key) && concurrentHashMap != null) {
            concurrentHashMap.remove(key);
        }
    }
}