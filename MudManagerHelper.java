package mud;

import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bson.types.ObjectId;

import org.mongodb.morphia.Datastore;

public final class MudManagerHelper {
    private static final Logger log = LoggerFactory.getLogger(MudManagerHelper.class);

    private static final ObjectId MUD_ROOMID_START = new ObjectId("000000000000000000000000");

    private MudManagerHelper() {
    }

    public static MudPlayer getPlayer(Datastore datastore, String userId) {
        MudPlayer player = datastore.get(MudPlayer.class, userId);
        if (player == null) {
            player = playerNew(datastore, userId);
        }
        player.updateLastSeen();
        player.incrementInteractions();
        datastore.save(player);

        MudRoom room = player.getRoom();
        room.updateLastVisited();
        datastore.save(room);

        log.info("player activated - userId = {} roomId = {}", userId, room.getId());

        return player;
    }

    public static MudPlayer playerNew(Datastore datastore, String userId) {
        MudPlayer player = new MudPlayer();
        MudRoom startRoom = datastore.get(MudRoom.class, MUD_ROOMID_START);
        log.info("new player with userId = {} in roomId = {}", userId, startRoom.getId());
        player.setId(userId);
        player.setRoom(startRoom);
        player.setIsNew(true);
        datastore.save(player);

        startRoom.addPlayer(player);
        startRoom.updateLastVisited();
        datastore.save(startRoom);

        return player;
    }

    public static MudExit playerGetExit(Datastore datastore, MudPlayer player, String exit) {
        MudRoom oldRoom = player.getRoom();

        MudExit mudExit = oldRoom.getExit(exit);

        return mudExit;
    }

    // this doesn't check for locks
    public static boolean playerMove(Datastore datastore, MudPlayer player, MudExit mudExit) {
        MudRoom oldRoom = player.getRoom();

        if (mudExit == null)
            return false;

        MudRoom newRoom = mudExit.getDestination();
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

        log.info("player {} moved from room {} to room {}", player.getId(), oldRoom.getId(), newRoom.getId());
        return true;
    }

    public static boolean playerDrop(Datastore datastore, MudPlayer player, String name) {
        MudItem mudItem = player.removeItem(name);
        MudRoom mudRoom = player.getRoom();
        if (mudItem == null)
            return false;
        mudRoom.addItem(mudItem);

        datastore.save(player);
        datastore.save(mudRoom);
        
        return true;
    }

    public static MudItem playerFindItemInRoom(Datastore datastore, MudPlayer player, String name) {
        MudRoom currentRoom = player.getRoom();
        return currentRoom.getItem(name);
    }

    public static boolean playerGetFromRoom(Datastore datastore, MudPlayer player, MudItem mudItem, String name) {
        MudRoom currentRoom = player.getRoom();
        if (mudItem != null) {
            currentRoom.removeItem(name);
            player.addItem(mudItem);
            datastore.save(player);
            datastore.save(currentRoom);
            return true;
        }
        return false;
    }

    public static MudItem itemNew(String shortName, String fullName, String description) {
        MudItem mudItem = new MudItem();
        mudItem.setShortName(shortName);
        mudItem.setFullName(fullName);
        mudItem.setDescription(description);
        return mudItem;
    }

    public static boolean isItemMatch(MudPlayer player, MudItem mudItem,
            Boolean isGetable, Boolean isContainer, Boolean isVisible, Boolean isUseable,
            Boolean hasUsesLeft, Boolean isIngestable, String hasTag) {
        boolean addItem = true;

        if (mudItem != null) {
            log.info("isItemMatch: mudItem.shortName = {}", mudItem.getShortName());
            if (isGetable != null && (isGetable != mudItem.getIsGetable()))
                addItem = false;
            else if (isContainer != null && (isContainer != mudItem.getIsContainer()))
                addItem = false;
            else if (isVisible != null && (isVisible != mudItem.getIsVisibleTo(player))) 
                addItem = false;
            else if (isUseable != null && (isUseable =! mudItem.getIsUsable()))
                addItem = false;
            else if (hasUsesLeft != null && ((hasUsesLeft && mudItem.getUsesLeft() > 0) 
                    || (!hasUsesLeft && mudItem.getUsesLeft() == 0)))
                addItem = false;
            else if (isIngestable != null && (isIngestable != mudItem.getIsIngestable()))
                addItem = false;
            else if (hasTag != null && !mudItem.tags.hasTag(hasTag))
                addItem = false;
            return addItem;
        }
        return false;
    }

    public static MudItemExitSearchResult playerItemExitSearch(MudPlayer player, String name,
            Boolean isGetable, Boolean isContainer, Boolean isVisible, Boolean isUseable,
            Boolean hasUsesLeft, Boolean isIngestable, String hasTag, boolean includePlayer,
            boolean includeRoom, boolean includeExits, boolean includeFullName) {

        MudItemExitSearchResult result = new MudItemExitSearchResult();
        MudRoom room = player.getRoom();
        MudItem mudItem;

        log.info("playerItemExitSearch({}, {}, isGetable = {}, isContainer = {}, isVisible = {}, isUseable = {}, "
                + "hasUsesLeft = {}, isIngestable = {}, hasTag = {}, includePlayer = {}, includeRoom = {}, "
                + "includeExits = {}, includeFullName = {})", player.getId(), name, isGetable, isContainer, isVisible,
                    isUseable, hasUsesLeft, isIngestable, hasTag, includePlayer, includeRoom,
                    includeExits, includeFullName);

        if (includePlayer) {
            mudItem = player.getItem(name);
            if (isItemMatch(player, mudItem, isGetable, isContainer, isVisible, isUseable,
                    hasUsesLeft, isIngestable, hasTag))
                result.playerItems.add(mudItem);
            if (includeFullName) {
                HashSet<MudItem> mudItemList = player.getItemListByFullName(name);
                Iterator<MudItem> it = mudItemList.iterator();
                while (it.hasNext()) {
                    mudItem = it.next();
                    if (isItemMatch(player, mudItem, isGetable, isContainer, isVisible, isUseable,
                            hasUsesLeft, isIngestable, hasTag))
                        result.playerItems.add(mudItem);
                }
            }
        }

        if (includeRoom) {
            mudItem = room.getItem(name);
            if (isItemMatch(player, mudItem, isGetable, isContainer, isVisible, isUseable,
                    hasUsesLeft, isIngestable, hasTag))
                result.roomItems.add(mudItem);
            if (includeFullName) {
                HashSet<MudItem> mudItemList = room.getItemListByFullName(name);
                Iterator<MudItem> it = mudItemList.iterator();
                while (it.hasNext()) {
                    mudItem = it.next();
                    if (isItemMatch(player, mudItem, isGetable, isContainer, isVisible, isUseable,
                            hasUsesLeft, isIngestable, hasTag))
                        result.playerItems.add(mudItem);
                }
            }
        }

        if (includeExits) {
            MudExit mudExit = room.getExit(name);
            if (mudExit != null) {
                boolean addExit = true;
                if (isGetable != null && isGetable == true)
                    addExit = false;
                else if (isContainer != null && isContainer == true)
                    addExit = false;
                else if (isVisible != null && mudExit.getIsVisibleTo(player))
                    addExit = false;
                else if (isUseable != null && isUseable == true)
                    addExit = false;
                else if (hasUsesLeft != null)
                    addExit = false;
                else if (isIngestable != null)
                    addExit = false;
                else if (hasTag != null && !mudExit.tags.hasTag(hasTag))
                    addExit = false;
                if (addExit)
                    result.roomExits.add(mudExit);
            }
        }
        result.found = result.playerItems.size() + result.roomItems.size() + result.roomExits.size();
        return result;
    }
}
