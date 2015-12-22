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
    private String hint;
    private Date lastVisited;
    @Embedded("exits")
    private Map<String, MudExit> exits;
    @Embedded("items")
    private Map<String, MudItem> items;
    @Reference
    private List<MudPlayer> players;

    public MudRoom() {
        hint = "No hint available.  Keep looking, searching and using!";
        exits = new HashMap<String, MudExit>();
        items = new HashMap<String, MudItem>();
        players = new ArrayList<MudPlayer>();
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId ID) {
        id = ID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public boolean hasExit(String name) {
        MudExit mudExit = exits.get(name);
        if (mudExit == null)
            return false;
        return true;
    }

    public MudExit getExitIfExists(String name) {
        return exits.get(name);
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

    public int addItem(MudItem mudItem) {
        return MudItemMapHelper.addItem(items, mudItem);
    }

    public MudItem removeItem(String name) {
        return MudItemMapHelper.removeItem(items, name);
    }

    public boolean hasItem(String name) {
        return MudItemMapHelper.hasItem(items, name);
    }

    public MudItem getItemIfExists(String name) {
        return MudItemMapHelper.getItemIfExists(items, name);
    }

    public List<MudItem> getItemListIfExistsByFullName(String name) {
        return MudItemMapHelper.getItemListIfExistsByFullName(items, name);
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

    public List<MudPlayer> getPlayers() {
        return players;
    }

    public Date getLastVisited() {
        return lastVisited;
    }

    public void updateLastVisited() {
        lastVisited = new Date();
    }
}
