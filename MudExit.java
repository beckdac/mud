package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.Date;
import java.util.HashSet;

@Embedded
public class MudExit {
    @Reference
    private MudRoom destination;
    private String description;
    private MudAccessControl visibility;        
    @Reference
    private MudLock lock;
    public final MudTags tags;
    private Date lastUsed;

    public MudExit() {
        description = "You can't see too well that way.";
        visibility = new MudAccessControl();
        tags = new MudTags();
    }

    public MudRoom getDestination() {
        return destination;
    }

    public void setDestination(MudRoom room) {
        destination = room;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getIsVisibleTo(MudPlayer player) {
        return !visibility.getIsRestrictedTo(player);
    }

    public boolean getIsLockedTo(MudPlayer player) {
        if (lock == null)
            return false;
        return lock.getIsLockedTo(player);
    }

    public String getLockedMessage() {
        return lock.getLockedMessage();
    }

    public boolean hasLock() {
        if (lock != null)
            return true;
        return false;
    }

    public void setLock(MudLock lock) {
        this.lock = lock;
    }

    public void removeLock() {
        lock = null;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        lastUsed = new Date();
    }
}
