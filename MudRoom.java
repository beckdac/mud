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

@Entity("rooms")
public class MudRoom {
    @Id private ObjectId id;
    private String description;
    private Date lastVisited;
    @Embedded
    private List<MudExit> exits = new ArrayList<MudExit>();
    @Embedded
    private List<MudItem> items = new ArrayList<MudItem>();
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

    public List<MudExit> getExits() {
        return exits;
    }

    public List<MudItem> getItems() {
        return items;
    }

    public Date getLastVisited() {
        return lastVisited;
    }

    public void updateLastVisited() {
        lastVisited = new Date();
    }
}
