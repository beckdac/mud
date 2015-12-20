package mud;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Entity("rooms")
public class MudRoom {
    @Id private ObjectId id;
    private String description;
    private Date lastVisited;
    @Embedded("exits")
    private Map<String, MudExit> exits = new HashMap<String, MudExit>();
    @Embedded("items")
    private Map<String, MudItem> items = new HashMap<String, MudItem>();
    @Reference
    private List<MudPlayer> players = new ArrayList<MudPlayer>();

    public MudRoom() {
    }

    public void setId(ObjectId ID) {
        id = ID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public MudRoom getExitDestination(String name) {
        MudExit exit = exits.get(name);
        if (exit != null)
            return exits.get(name).getDestination();
        return null;
    }

    public Map<String, MudExit> getExits() {
        return exits;
    }

    public MudItem getItem(String name) {
        return items.get(name);
    }

    public int addItem(MudItem mudItem) {
        return MudItemMapHelper.addItem(items, mudItem);
    }

    public MudItem removeItem(String item) {
        return MudItemMapHelper.removeItem(items, item);
    }

    public Map<String, MudItem> getItems() {
        return items;
    }

    public boolean hasPlayer(MudPlayer player) {
        if (players.contains(player))
            return true;
        return false;
    }

    public void removePlayer(MudPlayer player) {
        if (hasPlayer(player))
            players.remove(player);
    }

    public void addPlayer(MudPlayer player) {
        if (!hasPlayer(player))
            players.add(player);
    }

//    public List<MudPlayer> getPlayers() {
//        return players;
//    }

    public Date getLastVisited() {
        return lastVisited;
    }

    public void updateLastVisited() {
        lastVisited = new Date();
    }
}
