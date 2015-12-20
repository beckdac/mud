package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Embedded
public class MudItem {
    private String shortName;           // simple name, e.g. key, torch
    private String fullName;            // instead of just key, this could be "key for the red door"
    private String description;         // what is seen then the player looks at the item
    private boolean isGetable;          // can the item be taken or gotten from wherever it is
    private boolean isContainer;        // can the item contain stuff
    @Reference
    private List<MudItem> contents;     // contents of the container if above is true
    private boolean isVisible;          // if this is is false, then only players in the visibleTo array can see this item
    @Reference
    private List<MudPlayer> visibleTo;  // if not visible, list of who can see it
    private List<String> tags;          // function tags
    private Date lastUsed;              // the last time the item was manipulated

    public MudItem() {
        fullName = "thing";
        description = "a shapeless fob without color";
        isGetable = true;
        isContainer = false;
        contents = new ArrayList<MudItem>();
        isVisible = true;
        visibleTo = new ArrayList<MudPlayer>();
        tags = new ArrayList<String>();
    }

    public String getShortName() {
        return fullName;
    }

    public void setShortName(String name) {
        fullName = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String name) {
        fullName = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        description = desc;
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
        if (contents.contains(item))
            return true;
    }

    public boolean addContent(String name, MudItem item) {
        if (!isContainer)
            return false;
        // go through 
        MudItem previousItem = contents.put(name, item);
        int suffix = 2;
        String nextName = name + " " + Integer.toString(suffix);
        while (previousItem != null) {
        }
        for (int i = 2; 
        updateLastUsed();
    }

    public boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public boolean isVisibleTo(MudPlayer player) {
        if (isVisible || visibleTo.contains(player))
            return true;
        return false;
    }

    public void setIsVisibleTo(MudPlayer player) {
        if (!isVisibleTo(player))
            visibleTo.add(player);
    }

    public boolean hasTag(String tag) {
        if (tags.contains(tag))
            return true;
        return false;
    }

    public void addTagIfNotExists(String tag) {
        if (!tags.contain(tag))
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
