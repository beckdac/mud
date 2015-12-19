package mud;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

import java.util.Date;

@Embedded
public class MudExit {
    @Reference
    private MudRoom destination;
    private Date lastUsed;

    public MudExit() {
    }

    public MudRoom getDestination() {
        return destination;
    }

    public void setDestination(MudRoom room) {
        destination = room;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        lastUsed = new Date();
    }
}
