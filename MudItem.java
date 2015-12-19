package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Embedded
public class MudItem {
    private String description;
    private boolean visible;
    private List<String> tags = new ArrayList<String>();
    private Date lastUsed;

    public MudItem() {
    }

    public String getDescription() {
        return description;
    }

    public boolean getVisible() {
        return visible;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        lastUsed = new Date();
    }
}
