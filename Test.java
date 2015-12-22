package mud;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import org.bson.types.ObjectId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.FileNotFoundException;

public class Test {
    private static final Logger log = LoggerFactory.getLogger(Test.class);

    private static final String MONGO_DATABASE = "mud";

    private static final ObjectId MUD_ROOMID_START     = new ObjectId("000000000000000000000000");

    private static final String userId = "alexauser";

    private static Morphia morphia;
    private static Datastore datastore;

    // for dumping the custom slot values
    static HashSet<String> ObjectSpec = new HashSet<String>();
    static HashSet<String> ContainerSpec = new HashSet<String>();
    static HashSet<String> IngestableSpec = new HashSet<String>();
    static HashSet<String> ExitSpec = new HashSet<String>();
    static HashSet<String> LockableSpec = new HashSet<String>();

    public static void main(String[] args) {
        morphia = new Morphia();
        morphia.map(MudPlayer.class).map(MudRoom.class).map(MudItem.class)
                .map(MudExit.class).map(MudLock.class).map(MudAccessControl.class);
        datastore = morphia.createDatastore(new MongoClient(), MONGO_DATABASE);
        datastore.ensureIndexes();

        // if the starting room does not exist create the basic world
        MudRoom startRoom = datastore.get(MudRoom.class, MUD_ROOMID_START);
        if (startRoom == null) {
            createWorld();
            startRoom = datastore.get(MudRoom.class, MUD_ROOMID_START);
        }

        MudPlayer player = MudManagerHelper.getPlayer(datastore, userId);

        MudManagerHelper.playerMove(datastore, player, "north");
        MudManagerHelper.playerGet(datastore, player, "key 2");
        MudManagerHelper.playerGet(datastore, player, "key");
        MudManagerHelper.playerMove(datastore, player, "south");
        MudManagerHelper.playerDrop(datastore, player, "key");
        MudManagerHelper.playerDrop(datastore, player, "key");
        MudManagerHelper.playerGet(datastore, player, "key 2");

        dumpAllSlots();
    }

    private static void processItemMap(Map<String, MudItem>itemMap) {
        Iterator it = itemMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            MudItem mudItem = (MudItem)pair.getValue();
            String key = (String)pair.getKey();
            ObjectSpec.add(key);
            ObjectSpec.add(mudItem.getShortName());
            if (mudItem.getIsIngestable()) {
                IngestableSpec.add(key);
                IngestableSpec.add(mudItem.getShortName());
            }
            if (mudItem.hasLock()) {
                LockableSpec.add(key);
                LockableSpec.add(mudItem.getShortName());
            }
            if (mudItem.getIsContainer()) {
                ContainerSpec.add(key);
                ContainerSpec.add(mudItem.getShortName());
                processItemMap(mudItem.getContents());
            }
        }
    }

    private static void dumpHashSetToFile(HashSet<String> hashSet, String filename) {
        try {
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            for (String s : hashSet)
                writer.println(s);
            writer.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private static void dumpAllSlots() {
        // iterate over rooms
        for (MudRoom mudRoom : datastore.find(MudRoom.class)) {
            processItemMap(mudRoom.getItems());
            Iterator it = mudRoom.getExits().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String exitName = (String)pair.getKey();
                MudExit exit = (MudExit)pair.getValue();
                ExitSpec.add(exitName);
                if (exit.hasLock())
                    LockableSpec.add(exitName);
            }
        }
        // iterate over players
        for (MudPlayer mudPlayer : datastore.find(MudPlayer.class)) {
            processItemMap(mudPlayer.getItems());
        }

        // dump
        dumpHashSetToFile(ObjectSpec, "speechAssets/customSlotTypes/LIST_OF_OBJECTS.txt");
        dumpHashSetToFile(ContainerSpec, "speechAssets/customSlotTypes/LIST_OF_CONTAINERS.txt");
        dumpHashSetToFile(IngestableSpec, "speechAssets/customSlotTypes/LIST_OF_INGESTABLES.txt");
        dumpHashSetToFile(ExitSpec, "speechAssets/customSlotTypes/LIST_OF_EXITS.txt");
        dumpHashSetToFile(LockableSpec, "speechAssets/customSlotTypes/LIST_OF_LOCKABLES.txt");
    }

    private static void createWorld() {
        MudItem mudItem, container;
        MudRoom startRoom, northRoom;

        // the room where everything begins
        startRoom = new MudRoom();
        startRoom.setId(MUD_ROOMID_START);
        startRoom.setDescription("You are in a cold stone room.");
        startRoom.setHint("Try getting a key from the key dispenser? and opening the north door with it.");
        // a sign to look at
        mudItem = MudManagerHelper.itemNew("sign", "help sign", "The sign reads: say 'help me' for instructions or say 'hint please'.");
        mudItem.setIsGetable(false);
        startRoom.addItem(mudItem);
        // a key dispenser
        mudItem = MudManagerHelper.itemNew("key dispenser", "key dispenser", "You see a matte black forearm length cylinder in the center of the room with a pulsing blue light eminating from the top.  It has instructions that read: Say 'get key from key dispenser'.");
        mudItem.setIsGetable(false);
        mudItem.setIsContainer(true);
        mudItem.setHint("To use the key dispenser, say the phrase: 'get key from key dispenser'.");
        mudItem.tags.addTag("dispenser");
        startRoom.addItem(mudItem);
        // save the room
        datastore.save(startRoom);

        northRoom = new MudRoom();
        northRoom.setDescription("You are standing on a thin ledge that looks down into a great chasm with no bottom in sight.");
        northRoom.setHint("Congratulations.  You won the game.");
        // winner's trophy
        mudItem = MudManagerHelper.itemNew("trophy", "winner's trophy", "The trophy is made of cheap tin and is poorly mounted to a plate that reads: 'Congratulations. You won the game.");
        mudItem.setIsGetable(false);
        northRoom.addItem(mudItem);
        // chest
        container = MudManagerHelper.itemNew("chest", "wooden chest", "A simple wooden chest.  I wonder what's inside.");
        container.setIsGetable(false);
        container.setIsContainer(true);
        container.setHint("Try the phrases 'look in chest', 'put something in chest', or 'get something from chest'.");
        northRoom.addItem(container);
        // cake
        mudItem = MudManagerHelper.itemNew("cake", "chocolate cake", "A rich chocolate cake with dark chocolate frosting. Mmmm... Tasty.");
        mudItem.setIsIngestable(true);
        mudItem.setHint("Try 'eat cake' or look at it to see how many portions are left.");
        container.addContent(mudItem);
        // trashcan
        mudItem = MudManagerHelper.itemNew("trashcan", "bottomless trashcan", "This trashcan has no bottom!  Anything you put in it will disapear.");
        mudItem.setIsGetable(false);
        mudItem.setIsContainer(true);
        mudItem.setHint("To use the trashcan, say the phrase: 'put key in trashcan'.");
        mudItem.tags.addTag("trashcan");
        northRoom.addItem(mudItem);
        datastore.save(northRoom);

        MudLock lock = new MudLock();
        lock.setDescription("A simple lock.");
        lock.access.setIsRestricted(true);
        lock.access.setIsShared(false);
        datastore.save(lock);
        
        MudExit northExit = new MudExit();
        northExit.setDestination(northRoom);
        northExit.setLock(lock);
        startRoom.getExits().put("north", northExit);
        datastore.save(startRoom);

        MudExit southExit = new MudExit();
        southExit.setDestination(startRoom);
        southExit.setLock(lock);
        northRoom.getExits().put("south", southExit);
        datastore.save(northRoom);
    }
}
