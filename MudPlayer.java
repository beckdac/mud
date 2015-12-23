package mud;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

@Entity("players")
public class MudPlayer {
    @Id private String id;
    private Date lastSeen;
    private String sessionId;
    int sessions;
    int interactions;
    boolean isNew;
    @Reference
    private MudRoom room;
    @Embedded("inventory")
    private Map<String, MudItem> inventory = new HashMap<String, MudItem>();
    public MudTags tags;

    public MudPlayer() {
        sessionId = "";
        sessions = 0;
        interactions = 0;
        isNew = true;
        tags = new MudTags();
    }

    public String getId() {
        return id;
    }

    public void setId(String ID) {
        id = ID;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isSessionId(String sessionId) {
        return this.sessionId.equals(sessionId);
    }

    public MudRoom getRoom() {
        return room;
    }

    public void setRoom(MudRoom newRoom) {
        room = newRoom;
    }

    public int addItem(MudItem mudItem) {
        return MudItemMapHelper.addItem(inventory, mudItem);
    }

    public MudItem removeItem(String name) {
        return MudItemMapHelper.removeItem(inventory, name);
    }

    public boolean hasItem(String name) {
        return MudItemMapHelper.hasItem(inventory, name);
    }

    public MudItem getItem(String name) {
        return MudItemMapHelper.getItem(inventory, name);
    }

    public HashSet<MudItem> getItemListByFullName(String name) {
        return MudItemMapHelper.getItemListByFullName(inventory, name);
    }

    public Map<String, MudItem> getItems() {
        return inventory;
    }

    public int getInventorySize() {
        return inventory.size();
    }

    public boolean getIsNew() {
        return isNew;
    }
    
    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public int getSessions() {
        return interactions;
    }

    public void incrementSessions() {
        interactions++;
    }

    public int getInteractions() {
        return interactions;
    }

    public void incrementInteractions() {
        interactions++;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        lastSeen = new Date();
    }
}
