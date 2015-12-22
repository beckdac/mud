package mud;

import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.Date;
import java.util.HashSet;

@Entity("locks")
public class MudLock {
    private String description;
    private boolean isSharedLock;       // determines if the door lock state is share between all players
    private boolean isLocked;           // if sharedlockstate by all players this contols the lock
    @Reference
    private HashSet<MudPlayer> unlockedTo; // if player in this list then the door is unlocked to them
    private HashSet<String> tags;       // functionality tags
    private Date lastUsed;
    private int timesUsed;

    public MudLock() {
        isSharedLock = false;
        isLocked = true;
        unlockedTo = new HashSet<MudPlayer>();
        tags = new HashSet<String>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getIsSharedLock() {
        return isSharedLock;
    }

    public void setIsSharedLock(boolean isSharedLock) {
        this.isSharedLock = isSharedLock;
    }

    public boolean getIsLocked(MudPlayer player) {
        if (isSharedLock)
            return isLocked;
        if (isLocked && unlockedTo.contains(player))
            return false;
        return true;
    }

    public void setIsLocked(MudPlayer player, boolean isLocked) {
        if (isSharedLock)
            this.isLocked = isLocked;
        else {
            if (isLocked) {
                if (unlockedTo.contains(player))
                    unlockedTo.remove(player);
            } else {
                if (!unlockedTo.contains(player))
                    unlockedTo.add(player);
            }
        }
    }

    public boolean hasTag(String tag) {
        if (tags.contains(tag))
            return true;
        return false;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTagIfExists(String tag) {
        tags.remove(tag);
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        lastUsed = new Date();
    }
}
