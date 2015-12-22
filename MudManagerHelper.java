package mud;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mongodb.morphia.Datastore;

public final class MudManagerHelper {
    private static final Logger log = LoggerFactory.getLogger(MudManagerHelper.class);

    private MudManagerHelper() {
    }

    private boolean playerMove(Datastore datastore, MudPlayer player, String exit) {
        MudRoom oldRoom = player.getRoom()
        MudRoom newRoom = oldRoom.getExitDestination(exit);

        if (newRoom == null ) {
            return false;
        }

        oldRoom.removePlayer(player);
        datastore.save(oldRoom);

        newRoom.addPlayer(player);
        newRoom.updateLastVisited();
        datastore.save(newRoom);

        player.setRoom(newRoom);
        datastore.save(player);

        log.debug("player {} moved from room {} to room {}", player.getId(), oldRoom.getId(), newRoom.getId());
        return true;
    }

    private static boolean playerDrop(Datastore datastore, MudPlayer player, String name) {
        MudItem mudItem = player.removeItem(name);
        MudRoom mudRoom = player.getRoom();
        if (mudItem == null)
            return false;
        mudRoom = addItem(mudItem);

        datastore.save(player);
        datastore.save(mudRoom);
        
        return true;
    }

    private static MudItem playerGet(String name) {
        MudRoom currentRoom = player.getRoom();
        MudItem mudItem = currentRoom.getItemIfExists(name);
        if (mudItem != null && mudItem.getIsGetable()) {
            currentRoom.removeItem(name);
            player.addItem(mudItem);
            datastore.save(player);
            datastore.save(currentRoom);
        }
        return mudItem;
    }
}
