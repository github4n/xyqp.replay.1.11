package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;

import java.io.Serializable;
import java.util.UUID;

/**
 * The type Messages.
 */
public class Messages implements Serializable {
    private static final long serialVersionUID = 7021328701898559635L;
    /**
     * The Session id.
     */
    private UUID sessionId;
    /**
     * The Data object.
     */
    private Object dataObject;
    /**
     * The Gid.游戏id
     */
    private int gid;
    /**
     * The Sorts.事件顺序
     */
    private int sorts;

    /**
     * Instantiates a new Messages.
     *
     * @param client     the client
     * @param dataObject the data object
     * @param gid        the gid
     * @param sorts      the sorts
     */
    public Messages(SocketIOClient client, Object dataObject, int gid, int sorts) {
        super();
        this.sessionId = client.getSessionId();
        this.dataObject = dataObject;
        this.gid = gid;
        this.sorts = sorts;
    }

    /**
     * Gets session id.
     *
     * @return the session id
     */
    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * Sets session id.
     *
     * @param sessionId the session id
     */
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets data object.
     *
     * @return the data object
     */
    public Object getDataObject() {
        return dataObject;
    }

    /**
     * Sets data object.
     *
     * @param dataObject the data object
     */
    public void setDataObject(Object dataObject) {
        this.dataObject = dataObject;
    }

    /**
     * Gets gid.
     *
     * @return the gid
     */
    public int getGid() {
        return gid;
    }

    /**
     * Sets gid.
     *
     * @param gid the gid
     */
    public void setGid(int gid) {
        this.gid = gid;
    }

    /**
     * Gets sorts.
     *
     * @return the sorts
     */
    public int getSorts() {
        return sorts;
    }

    /**
     * Sets sorts.
     *
     * @param sorts the sorts
     */
    public void setSorts(int sorts) {
        this.sorts = sorts;
    }


}
