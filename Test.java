package mud;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import org.bson.types.ObjectId;

import java.util.List;

public class Test {
    private static final String MONGO_DATABASE = "mud";

    private static final ObjectId MUD_ROOMID_START     = new ObjectId("000000000000000000000000");
    private static final ObjectId MUD_ROOMID_GRAVEYARD = new ObjectId("000000000000000000000001");

    private static final String userId = "alexauser";

    private static Morphia morphia;
    private static Datastore datastore;

    public static void main(String[] args) {
        morphia = new Morphia();
        morphia.map(MudPlayer.class).map(MudRoom.class).map(MudItem.class).map(MudExit.class);
        datastore = morphia.createDatastore(new MongoClient(), MONGO_DATABASE);
        datastore.ensureIndexes();

        MudRoom startRoom = datastore.get(MudRoom.class, MUD_ROOMID_START);
        if (startRoom == null) {
            startRoom = new MudRoom();
            startRoom.setId(MUD_ROOMID_START);
            startRoom.setDescription("You are in a cold and damp stone room.  The only light is coming from a crack in the ceiling above.");
            datastore.save(startRoom);

            MudRoom northRoom = new MudRoom();
            northRoom.setDescription("You are standing on a thin ledge that looks down into a great chasm with no bottom in sight.");
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
            player = newPlayer();
        }
        player.updateLastSeen();
        datastore.save(player);
        System.out.println(player.getRoom().getDescription());
    }

    private static MudPlayer newPlayer() {
            MudPlayer player = new MudPlayer();
            player.setId(userId);
            player.setRoom(datastore.get(MudRoom.class, MUD_ROOMID_START));
            return player;
    }
}
