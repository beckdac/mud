package mud;

import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import org.bson.types.ObjectId;

import java.util.Date;
import java.util.HashSet;

@Entity("locks")
public class MudLock {
    @Id private ObjectId id;
    private String description;
    public final MudAccessControl access;     // lock / unlock
    private MudAccessControl visibility;      // can the lock be seen
    public final MudTags tags;                // functionality tags
    private Date lastUsed;
    private int timesUsed;

    public MudLock() {
        access = new MudAccessControl();
        visibility = new MudAccessControl();
        tags = new MudTags();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getIsLockedTo(MudPlayer player) {
        return access.getIsRestrictedTo(player);
    }

    public boolean getIsVisibleTo(MudPlayer player) {
        return !visibility.getIsRestrictedTo(player);
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        lastUsed = new Date();
    }

    public void incrementTimesUsed() {
        timesUsed++;
    }

    public int getTimesUsed() {
        return timesUsed;
    }
}
