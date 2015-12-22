package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Embedded
public class MudExit {
    @Reference
    private MudRoom destination;
    private String description;
    private boolean isLockable;
    private boolean isSharedLock;       // determines if the door lock state is share between all players
    private boolean isLocked;           // if sharedlockstate by all players this contols the lock
    @Reference
    private List<MudPlayer> unlockedTo; // if player in this list then the door is unlocked to them
    private boolean isVisible;          // if this is false, then only players in the visibleTo array can see this item
    @Reference
    private List<MudPlayer> visibleTo;  // if not visible, list of who can see it
    private List<String> tags;          // function tags
    private Date lastUsed;

    public MudExit() {
        description = "You can't see to well that way.";
        isLockable = false;
        isSharedLock = true;
        isLocked = false;
        unlockedTo = new ArrayList<MudPlayer>();
        isVisible = true;
        visibleTo = new ArrayList<MudPlayer>();
        tags = new ArrayList<String>();
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

    public boolean getIsLockable() {
        return isLockable;
    }

    public void setIsLockable(boolean isLockable) {
        this.isLockable = isLockable;
    }

    public boolean getIsSharedLock() {
        return isSharedLock;
    }

    public void setIsSharedLock(boolean isSharedLock) {
        this.isSharedLock = isSharedLock;
    }

    public boolean getIsLocked(MudPlayer player) {
        if (!isLockable)
            return false;
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

    public boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public boolean getIsVisibleTo(MudPlayer player) {
        if (isVisible || visibleTo.contains(player))
            return true;
        return false;
    }

    public void setIsVisibleTo(MudPlayer player, boolean isVisible) {
        if (isVisible) {
            if (!visibleTo.contains(player))
                visibleTo.add(player);
        } else {
            if (visibleTo.contains(player))
                visibleTo.remove(player);
        }
    }

    public boolean hasTag(String tag) {
        if (tags.contains(tag))
            return true;
        return false;
    }

    public void addTagIfNotExists(String tag) {
        if (!tags.contains(tag))
            tags.add(tag);
    }

    public void removeTagIfExists(String tag) {
        if (hasTag(tag))
            tags.remove(tag);
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        lastUsed = new Date();
    }
}
