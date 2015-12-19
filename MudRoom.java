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
        return exits.get(name).getDestination();
    }

    public Map<String, MudExit> getExits() {
        return exits;
    }

    public MudItem getItem(String name) {
        return items.get(name);
    }

    public Map<String, MudItem> getItems() {
        return items;
    }

    public Date getLastVisited() {
        return lastVisited;
    }

    public void updateLastVisited() {
        lastVisited = new Date();
    }
}
