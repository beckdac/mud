package mud;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.Datastore;

import org.bson.types.ObjectId;

import java.util.List;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

public class MudManager {
    private static final String SLOT_ACTION = "Action";
    private static final String SLOT_ITEM = "Item";
    private static final String SLOT_EXIT = "Exit";

    private static final String MONGO_DATABASE = "mud";

    private static final ObjectId MUD_ROOMID_START     = new ObjectId("000000000000000000000000");

    private final Morphia morphia;
    private final Datastore datastore;

    private final MudPlayer player;

    private String speechOutput = "", repromptText = "";

    public MudManager(final MongoClient mongoClient, Session session) {
        morphia = new Morphia();
        morphia.map(MudPlayer.class).map(MudRoom.class).map(MudItem.class).map(MudExit.class);
        datastore = morphia.createDatastore(new MongoClient(), MONGO_DATABASE);
        datastore.ensureIndexes();

        player = datastore.get(MudPlayer.class, userId);
        if (player == null) {
            log.info("new player, Id = {}", session.userId);
            MudPlayer player = new MudPlayer();
            player.setId(userId);
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

        speechOutput += "You have " + player.inventory.size() + " items in your inventory.";
        speechOutput += player.getRoom().getDescription();
        // This next line should come from a randomized list of questions with the same 
        // intent for flavor
        speechOutput += "What do you want to do now?";
        repromptText = "For instructions, please say 'help me'.";

        return getAskSpeechletResponse(speechOutput, repromptText);
    }

    public SpeechletResponse getLookIntentResponse(Intent intent, Session session) {
        MudPlayer player = getPlayer(session.getUser().getUserId());

        String speechOutput, repromptText;
        speechOutput = player.room().description();
        repromptText = "You can look again, take some other action, ask for help or a hint.";

        return getAskSpeechletResponse(speechOutput, repromptText);
    }

    public SpeechletResponse getHintIntentResponse(Intent intent, Session session) {
        MudPlayer player = getPlayer(session.getUser().getUserId());

	String speechOutput, repromptText;
        speechOutput = player.room().hint();
        repromptText = "Why not try taking another look around or doing a search?";

        return getAskSpeechletResponse(speechOutput, repromptText);
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
        String speechOutput = "You are in a multiplayer dungeon or mud. "
        + "Your goal is to find treasure, gain experience, and escape! "
        + "You can take actions like 'look around', 'open chest', "
        + "'light torch', 'get key', and 'search room'. "
        + "You can move around the dungeon by going through exits, "
        + "for example if I tell you a room has a north exit, you "
        + "can say 'go north'.  You can also ask me for a hint. "
        + "Now, what can I help you with?";
        String repromptText = "Try saying 'look around' or ask me for help again to hear the instructions.";

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

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
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
    private SpeechletResponse getAskSpeechletResponse(String speechOutput, String repromptText) {
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
