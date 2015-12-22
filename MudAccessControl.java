package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.HashSet;
import java.util.Date;

@Embedded
public class MudAccessControl {
    private boolean isShared;             // determines if the access control is share between all players
    private boolean isRestricted;         // if sharedlockstate by all players this contols the access control
    @Reference
    private HashSet<MudPlayer> accessList;// players in this hashset have access
    public final MudTags tags;            // functionality tags

    public MudAccessControl() {
        isShared = false;
        isRestricted = false;
        accessList = new HashSet<MudPlayer>();
        tags = new MudTags();
    }

    public boolean getIsShared() {
        return isShared;
    }

    public void setIsShared(boolean isShared) {
        this.isShared = isShared;
    }

    public boolean getIsRestricted() {
        return isRestricted;
    }

    public void setIsRestricted(boolean isRestricted) {
        this.isRestricted = isRestricted;
    }

    public boolean getIsRestrictedTo(MudPlayer player) {
        if (isShared)
            return isRestricted;
        if (accessList.contains(player))
            return false;
        return true;
    }

    public void addToAcessList(MudPlayer player) {
        accessList.add(player);
    }

    public void removeFromAcessList(MudPlayer player) {
        accessList.remove(player);
    }
}
