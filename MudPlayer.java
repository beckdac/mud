package mud;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

@Entity("players")
public class MudPlayer {
    @Id private String id;
    private Date lastSeen;
    @Reference
    private MudRoom room;
    @Embedded("inventory")
    private Map<String, MudItem> inventory = new HashMap<String, MudItem>();

    public MudPlayer() {
    }

    public void setId(String ID) {
        id = ID;
    }

    public MudRoom getRoom() {
        return room;
    }

    public void setRoom(MudRoom newRoom) {
        room = newRoom;
    }

    public Map<String, MudItem> getInventory() {
        return inventory;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        lastSeen = new Date();
    }
}
