package mud;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import org.bson.types.ObjectId;

import java.util.List;

public class Test {
    private static final String MONGO_DATABASE = "mud";

    private static final ObjectId MUD_ROOMID_START     = new ObjectId("000000000000000000000000");

    private static final String userId = "alexauser";

    private static Morphia morphia;
    private static Datastore datastore;

    public static void main(String[] args) {
        morphia = new Morphia();
        morphia.map(MudPlayer.class).map(MudRoom.class).map(MudItem.class).map(MudExit.class);
        datastore = morphia.createDatastore(new MongoClient(), MONGO_DATABASE);
        datastore.ensureIndexes();

        // if the starting room does not exist create the basic world
        MudRoom startRoom = datastore.get(MudRoom.class, MUD_ROOMID_START);
        if (startRoom == null) {
            MudItem mudItem;

            // the room where everything begins
            startRoom = new MudRoom();
            startRoom.setId(MUD_ROOMID_START);
            startRoom.setDescription("You are in a cold and damp stone room.  The only light is coming from a crack in the ceiling above.");
            startRoom.setHint("Try going north and getting a key from the key dispenser? and opening the east door with it.");
            // a sign to look at
            mudItem = itemNew("sign", "help sign", "The sign reads: say 'help me' for instructions or, if you are feeling lucky, say 'hint please'.", false, false, true);
            startRoom.addItem(mudItem);
            datastore.save(startRoom);

            MudRoom northRoom = new MudRoom();
            northRoom.setDescription("You are standing on a thin ledge that looks down into a great chasm with no bottom in sight.");
            startRoom.setHint("Why not get a key from the key dispenser and using it a door in the south room?");
            mudItem = itemNew("key dispenser", "key dispenser", "You see a matte black forearm length cylinder in the center of the room with a pulsing blue light eminating from the top.  It has an engraving on the top that says 'use me' or 'get key from me'.", false, false, true);
            northRoom.addItem(mudItem);
            datastore.save(northRoom);

            MudExit northExit = new MudExit();
            northExit.setDestination(northRoom);
            startRoom.getExits().put("north", northExit);
            datastore.save(startRoom);

            MudExit southExit = new MudExit();
            southExit.setDestination(startRoom);
            northRoom.getExits().put("south", southExit);
            datastore.save(northRoom);
        }

        MudPlayer player = datastore.get(MudPlayer.class, userId);
        if (player == null) {
            System.out.println("Ahh, a new player.  Welcome.");
            player = playerNew();
        }
        MudRoom currentRoom = player.getRoom();
        currentRoom.updateLastVisited();
        player.updateLastSeen();
        datastore.save(player);
        datastore.save(currentRoom);
        System.out.println(currentRoom.getDescription());

        playerMove(player, "north");
        playerGet(player, "key 2");
        playerGet(player, "key");
        playerMove(player, "south");
        playerDrop(player, "key");
        playerDrop(player, "key");
        playerGet(player, "key 2");
    }

    private static MudPlayer playerNew() {
        MudPlayer player = new MudPlayer();
        player.setId(userId);
        player.setRoom(datastore.get(MudRoom.class, MUD_ROOMID_START));
        return player;
    }

    private static boolean playerMove(MudPlayer player, String exit) {
        MudRoom currentRoom = player.getRoom();
        
        if (player.useExit(exit)) {
            datastore.save(player);
            datastore.save(currentRoom);
            currentRoom = player.getRoom();
            datastore.save(currentRoom);
            System.out.println(currentRoom.getDescription());
            return true;
        }
        return false;
    }

    private static boolean playerDrop(MudPlayer player, String item) {
        if (player.dropItem(item)) {
            datastore.save(player);
            datastore.save(player.getRoom());
            return true;
        }
        return false;
    }

    private static MudItem itemNew(String shortName, String fullName, String description, 
        boolean isGetable, boolean isContainer, boolean isVisible) {
        MudItem mudItem = new MudItem();
        mudItem.setShortName(shortName);
        mudItem.setFullName(fullName);
        mudItem.setDescription(description);
        mudItem.setIsGetable(isGetable);
        mudItem.setIsContainer(isContainer);
        mudItem.setIsVisible(isVisible);
        return mudItem;
    }

    private static MudItem playerGet(MudPlayer player, String name) {
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
