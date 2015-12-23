package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

@Embedded
public class MudItem {
    private String shortName;           // simple name, e.g. key, torch
    private String fullName;            // instead of just key, this could be "key for the red door"
    private String description;         // what is seen then the player looks at the item
    private String hint;                // hint, if any
    private boolean isGetable;          // can the item be taken or gotten from wherever it is
    private String notGetableMessage;   // what to display when someone tries to get it when isGetable = false
    private boolean isContainer;        // can the item contain stuff
    @Embedded("contents")
    private Map<String, MudItem> contents;// contents of the container if above is true
    @Reference
    private MudLock lock;               // if not null, then this item is locked
    public final MudAccessControl visibility; // can this be seen
    private boolean isUsable;           // can be invoked in a use context
    private int usesLeft;               // how many more times can this be used, -1 = infinite, also how many ingests left, etc.
    private boolean isIngestable;       // can be ingested
    public final MudTags tags;          // functionality tags
    private Date lastUsed;              // the last time the item was manipulated

    public MudItem() {
        shortName = "thing";
        fullName = "shapeless thing";
        description = "a shapeless fob without color";
        isGetable = true;
        notGetableMessage = "strangely, that is immovable";
        isContainer = false;
        contents = new HashMap<String, MudItem>();
        visibility = new MudAccessControl();
        isUsable = false;
        usesLeft = -1;
        isIngestable = false;
        tags = new MudTags();
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public boolean getIsGetable() {
        return isGetable;
    }

    public void setIsGetable(boolean isGetable) {
        this.isGetable = isGetable;
    }

    public String getNotGetableMessage() {
        return notGetableMessage;
    }

    public void setNotGetableMessage(String notGetableMessage) {
        this.notGetableMessage = notGetableMessage;
    }

    public boolean getIsContainer() {
        return isContainer;
    }

    public void setIsContainer(boolean isContainer) {
        this.isContainer = isContainer;
    }

    public boolean hasContent(String item) {
        if (!isContainer)
            return false;
        return MudItemMapHelper.hasItem(contents, item);
    }

    public boolean addContent(MudItem item) {
        if (!isContainer)
            return false;
        MudItemMapHelper.addItem(contents, item);
        updateLastUsed();
        return true;
    }

    public MudItem removeContent(String name) {
        if (!isContainer)
            return null;
        return MudItemMapHelper.removeItem(contents, name);
    }

    public MudItem getContent(String name) {
        if (!isContainer)
            return null;
        return MudItemMapHelper.getItem(contents, name);
    }

    public Map<String, MudItem> getContents() {
        return contents;
    }

    public boolean getIsVisibleTo(MudPlayer player) {
        return !visibility.getIsRestrictedTo(player);
    }

    public boolean getIsUsable() {
        return isUsable;
    }

    public void setIsUsable(boolean isUsable) {
        this.isUsable = isUsable;
    }

    public int getUsesLeft() {
        return usesLeft;
    }

    public void setUsesLeft(int usesLeft) {
        this.usesLeft = usesLeft;
    }

    public int incrementUsesLeft() {
        usesLeft++;
        return usesLeft;
    }

    public int decrementUsesLeft() {
        usesLeft--;
        return usesLeft;
    }

    public boolean getIsIngestable() {
        return isIngestable;
    }

    public void setIsIngestable(boolean isIngestable) {
        this.isIngestable = isIngestable;
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
        this.lock = null;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        lastUsed = new Date();
    }
}
