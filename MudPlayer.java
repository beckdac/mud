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

    public boolean useExit(String exit) {
        MudRoom dest = room.getExitDestination(exit);
        if (dest == null)
            return false;
        setRoom(dest);
        return true;
    }

    public void setRoom(MudRoom newRoom) {
        room.getPlayers().remove(this);

        room = newRoom;
        room.updateLastVisited();
        room.getPlayers().add(this);
    }

    public MudItem removeItem(String item) {
        MudItem mudItem = inventory.get(item);
        if (mudItem == null)
            return null;
        items.remove(mudItem);
        return mudItem;
    }

    public boolean dropItem(String item) {
        MudItem mudItem = removeItem(item);
        if (mudItem == null)
            return false;
        room.getItems().add(item, mudItem);
        return true;
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
