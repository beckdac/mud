package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.HashSet;

@Embedded
public class MudTags {
    private HashSet<String> tags;

    public MudTags() {
    }

    public boolean hasTag(String tag) {
        if (tags == null)
            return false;
        if (tags.contains(tag))
            return true;
        return false;
    }

    public void addTag(String tag) {
        if (tags == null)
            tags = new HashSet<String>();
        tags.add(tag);
    }

    public void removeTagIfExists(String tag) {
        if (tags != null)
            tags.remove(tag);
    }
}
