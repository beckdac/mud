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
    private static final String SLOT_OBJECTSPEC = "ObjectSpec";

    private static final String MONGO_DATABASE = "mud";

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

        // load the player and populate the interaction
        player = MudManagerHelper.getPlayer(datastore, session.getUser().getUserId());
        if (player.getIsNew()  == true) {
    	    speechOutput += "Ah, a new player.  Welcome.  For instructions, say 'help me'.";
            player.setIsNew(false);
            datastore.save(player);
        } else {
            speechOutput += "Welcome back to the Mud.";
        }

        speechOutput = "";
        repromptText = "";
        // ready to go
    }

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
        Slot objectSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);
        if (objectSpecSlot != null && objectSpecSlot.getValue() != null) {
            String objectSpec = objectSpecSlot.getValue();
            String playerSpeechOutput = "", roomSpeechOutput = "", exitSpeechOutput = "";
            int found = 0;
            // go through precedence chain looking for something matching the where slot
            // player
            MudItem item = playerItemSearch(objectSpec);
            if (item != null && item.isVisibleTo(player)) {
                ++found;
                playerSpeechOutput = item.getDescription();
            }
            // room
            item = roomItemSearch(objectSpec);
            if (item != null && item.isVisibleTo(player)) {
                ++found;
                roomSpeechOutput = item.getDescription();
            }
            // exits
            MudExit exit = player.getRoom().getExitIfExists(objectSpec);
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
                speechOutput += String.format("I found %d items matching '%s'.", found, objectSpec);
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
        Slot objectSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);       // any object
        String objectSpec;
        Slot containerSpecSlot = intent.getSlot(SLOT_CONTAIERSPEC); // preceeded by 'into' so basically anything with isContainer set
        String containerSpec;

        MudItem mudItem;
        if (objectSpecSlot != null && objectSpecSlot.getValue() != null) {
            // check local inventory
            objectSpec = objectSpecSlot.getValue();
            mudItem = player.getItemIfExists(objectSpec);
        }
        if (mudItem != null) {
            MudItem mudItemInto; 
            if (containerSpecSlot != null && containerSpecSlot.getValue() != null) {
                containerSpec = containerSpecSlot.getValue();
                mudItemInto = player.getItemIfExists(containerSpec);
                
                // we need to find an object in the local environment that is a container
            }
        } else {
            if (objectSpec != null)
                speechOutput += "Sorry, I couldn't find an item named " + objectSpec + ".";
            else
                speechOutput += "Sorry, I don't understand what you want to put where.";
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getGetIntentResponse(Intent intent, Session session) {
        Slot objectSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);          // any object
        Slot fromObjectSpecSlot = intent.getSlot(SLOT_FROM_OBJECTSPEC); // preceeded by 'from' so basically anything with isContainer set
        if (objectSpecSlot != null && objectSpecSlot.getValue() != null) {
            String objectSpec = objectSpecSlot.getValue();
            
            speechOutput += 
        } else {
            // what do you want to get?
            speechOutput += "
        }
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getDropIntentResponse(Intent intent, Session session) {
        // transferItem
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptText += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getOpenIntentResponse(Intent intent, Session session) {
        // find items or exits with is closed
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
