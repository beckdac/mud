package mud;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.Datastore;

import org.bson.types.ObjectId;

import java.util.List;
import java.util.Random;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

// the MudManager essentially is the glue between the data access objects / mongodb
// and the alexa skill handler

public class MudManager {
    private static final Logger log = LoggerFactory.getLogger(MudManager.class);

    private static final String SLOT_ACTION = "Action";
    private static final String SLOT_ITEM = "Item";
    private static final String SLOT_EXIT = "Exit";
    private static final String SLOT_WHERE = "Where";

    private static final String MONGO_DATABASE = "mud";

    private static final ObjectId MUD_ROOMID_START     = new ObjectId("000000000000000000000000");

    private static <T> T randomFrom(T... items) { return items[new Random().nextInt(items.length)]; }
    private static final String[] WHAT_NEXT_Q_LIST = {
            "What do you want to do now?",
            "Tell me what you want to do next?",
            "What do you want to do next?",
            "OK, what next?",
            "What now?"
        };
    private static final String[] REPROMPT_Q_LIST = {
            "Still there? Why not try saying 'inventory' for something to 'use'.",
            "For instructions, please say 'help me'.",
            "You can look again, take some other action, ask for help or a hint.",
            "Why not try taking another look around or doing a search?"
        };

    private final Morphia morphia;
    private final Datastore datastore;

    private final MudPlayer player;

    private String speechOutput;
    private String repromptText;

    public MudManager(final MongoClient mongoClient, Session session) {
        morphia = new Morphia();
        morphia.map(MudPlayer.class).map(MudRoom.class).map(MudItem.class).map(MudExit.class);
        datastore = morphia.createDatastore(new MongoClient(), MONGO_DATABASE);
        datastore.ensureIndexes();

        player = datastore.get(MudPlayer.class, session.getUser().getUserId());
        if (player == null) {
            log.info("new player, Id = {}", session.getUser().getUserId());
            MudPlayer player = new MudPlayer();
            player.setId(session.getUser().getUserId());
            player.setRoom(datastore.get(MudRoom.class, MUD_ROOMID_START));
    	    speechOutput += "Ah, a new player.  Welcome.  For instructions, say 'help me'.";
        } else {
            speechOutput += "Welcome back to the Mud.";
        }

        // login ritual
        MudRoom currentRoom = player.getRoom();
        currentRoom.updateLastVisited();
        player.updateLastSeen();
        datastore.save(player);
        datastore.save(currentRoom);
        log.info("player {} in {}", player.getId(), currentRoom.getId());

        speechOutput = "";
        repromptText = "";
        // ready to go
    }

    private boolean playerMove(String exit) {
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

    public MudItem playerItemSearch(String item) {
        return player.getItemIfExists(item);
    }

    public MudItem roomItemSearch(String item) {
        return player.getRoom().getItemIfExists(item);
    }

/*
    private boolean transferItem(Map<String, MudItem> from, Map<String, MudItem> to, String item) {
        fromItem = from.getItems().get(item);
        if (fromItem == null) {
            speechOutput += "Sorry.  I was unable to find a " + item + ".";
            return false;
        }
        from.getItems().remove(item);
        to.getItems().put(item, fromItem);
        return true;
    }

    private boolean playerDrop(String item) {
        currentRoom = player.getRoom();
        if (transferItem(player.getItems(), currentRoom.items, item)) {
            datastore.save(player);
            datastore.save(currentRoom);
            return true;
        }
        return false;
    }
*/

    /**
     * Creates and returns response for Launch request.
     *
     * @param request
     *            {@link LaunchRequest} for this request
     * @param session
     *            Speechlet {@link Session} for this request
     * @return response for launch request
     */
    public SpeechletResponse getLaunchResponse(LaunchRequest request, Session session) {
        speechOutput += "You have " + player.getInventorySize() + " items in your inventory.";
        speechOutput += player.getRoom().getDescription();
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getLookIntentResponse(Intent intent, Session session) {
        Slot whereSlot = intent.getSlot(SLOT_WHERE);
        if (whereSlot != null && whereSlot.getValue() != null) {
            String whereValue = whereSlot.getValue();
            String playerSpeechOutput = "", roomSpeechOutput = "", exitSpeechOutput = "";
            int found = 0;
            // go through precedence chain looking for something matching the where slot
            // player
            MudItem item = playerItemSearch(whereValue);
            if (item != null && item.isVisibleTo(player)) {
                ++found;
                playerSpeechOutput = item.getDescription();
            }
            // room
            item = roomItemSearch(whereValue);
            if (item != null && item.isVisibleTo(player)) {
                ++found;
                roomSpeechOutput = item.getDescription();
            }
            // exits
            MudExit exit = player.getRoom().getExitIfExists(whereValue);
            if (exit != null && exit.isVisibleTo(player)) {
                ++found;
                exitSpeechOutput += exit.getDestination().getDescription();
            }
            if (found == 0) {
                // nothing matched, show the room description
                speechOutput += player.getRoom().getDescription();
            } else if (found == 1) {
                if (playerSpeechOutput != null)
                    speechOutput += playerSpeechOutput;
                else if (roomSpeechOutput != null)
                    speechOutput += roomSpeechOutput;
                else
                    speechOutput += exitSpeechOutput;
            } else {
                speechOutput += String.format("I found %d items matching '%s'.", found, whereValue);
                if (playerSpeechOutput != null) {
                    speechOutput += "In your inventory: " + playerSpeechOutput;
                }
                if (roomSpeechOutput != null) {
                    speechOutput += "In the room: " + roomSpeechOutput;
                }
                if (exitSpeechOutput != null) {
                    speechOutput += "There is also an exit: " + exitSpeechOutput;
                }
            }
        } else 
            speechOutput += player.getRoom().getDescription();
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getPutIntentResponse(Intent intent, Session session) {
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getGetIntentResponse(Intent intent, Session session) {
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getDropIntentResponse(Intent intent, Session session) {
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getOpenIntentResponse(Intent intent, Session session) {
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getUseIntentResponse(Intent intent, Session session) {
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    // reveals hidden items
    public SpeechletResponse getSearchIntentResponse(Intent intent, Session session) {
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getHintIntentResponse(Intent intent, Session session) {
        speechOutput += player.getRoom().getHint();
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    // eat, drink, quaff
    public SpeechletResponse getIngestIntentResponse(Intent intent, Session session) {
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }


    /**
     * Creates and returns response for the help intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the help intent
     */
    public SpeechletResponse getHelpIntentReponse(Intent intent, Session session) {
        speechOutput += "You are in a multiuser dungeon or mud. "
            + "Your goal is to find treasure, gain experience, and explore! "
            + "You can take actions like 'look around', 'open chest', "
            + "'light torch', 'get key', and 'search room'. "
            + "You can move around the dungeon by going through exits, "
            + "for example if I tell you a room has a north exit, you "
            + "can say 'go north'.  You can also ask me for a hint. "
            + "Now, what can I help you with?";
        repromptText += "Try saying 'look around' or ask me for help again to hear the instructions.";

        return getAskSpeechletResponse();
    }

    /**
     * Creates and returns response for the exit intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the exit intent
     */
    public SpeechletResponse getExitIntentResponse(Intent intent, Session session) {
	    return getTellSpeechletResponse("Goodbye!");
    }

    /**
     * Returns an ask Speechlet response for a speech and reprompt text.
     *
     * @param speechOutput
     *            Text for speech output
     * @param repromptText
     *            Text for reprompt output
     * @return ask Speechlet response for a speech and reprompt text
     */
    private SpeechletResponse getAskSpeechletResponse() {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Mud");
        card.setContent(speechOutput);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechOutput);

        // Create reprompt
        PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
        repromptSpeech.setText(repromptText);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptSpeech);

        // reset these for the next interaction
        speechOutput = "";
        repromptText = "";

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    /**
     * Returns a tell Speechlet response for a speech and reprompt text.
     *
     * @param speechOutput
     *            Text for speech output
     * @return a tell Speechlet response for a speech and reprompt text
     */
    private SpeechletResponse getTellSpeechletResponse(String speechOutput) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Mud");
        card.setContent(speechOutput);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechOutput);

        return SpeechletResponse.newTellResponse(speech, card);
    }
}
