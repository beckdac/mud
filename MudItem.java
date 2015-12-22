package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Embedded
public class MudItem {
    private String shortName;           // simple name, e.g. key, torch
    private String fullName;            // instead of just key, this could be "key for the red door"
    private String description;         // what is seen then the player looks at the item
    private String hint;                // hint, if any
    private boolean isGetable;          // can the item be taken or gotten from wherever it is
    private boolean isContainer;        // can the item contain stuff
    @Embedded("contents")
    private Map<String, MudItem> contents;// contents of the container if above is true
    private boolean isVisible;          // if this is is false, then only players in the visibleTo array can see this item
    @Reference
    private List<MudPlayer> visibleTo;  // if not visible, list of who can see it
    private boolean isUsable;           // can be invoked in a use context
    private int usesLeft;               // how many more times can this be used, -1 = infinite, also how many ingests left, etc.
    private boolean isIngestable;       // can be ingested
    private List<String> tags;          // function tags
    private Date lastUsed;              // the last time the item was manipulated

    public MudItem() {
        shortName = "thing";
        fullName = "shapeless thing";
        description = "a shapeless fob without color";
        isGetable = true;
        isContainer = false;
        contents = new HashMap<String, MudItem>();
        isVisible = true;
        visibleTo = new ArrayList<MudPlayer>();
        isUsable = false;
        isIngestable = false;
        usesLeft = -1;
        tags = new ArrayList<String>();
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

    public Map<String, MudItem> getContents() {
        return contents;
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

    public void setIsVisibleTo(MudPlayer player) {
        if (!getIsVisibleTo(player))
            visibleTo.add(player);
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
