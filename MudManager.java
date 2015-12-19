package mud;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;
import com.mongodb.Block;
import org.bson.Document;
import org.mongodb.morphia.Morphia;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import static java.util.Arrays.asList;

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

    private final Morphia morphia;
    private final Datastore datastore;

    public MudManager(final MongoClient mongoClient) {
        morphia = new Morphia();
        morphia.map(MudPlayer.class).map(MudRoom.class).map(MudItem.class).map(MudExit.class);
        datastore = morphia.createDatastore(MongoClient(), MONGO_DATABASE);
        datastore.ensureIndexes();
    }

    private MudPlayer getPlayer(String userId) {
        MudPlayer player;

        FindIterable<Document> cursor = player_collection.find(new Document("_id", userId)).limit(1);
        Document pDoc = cursor.first();

        return player;
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
        String speechOutput = "", repromptText = "";
        MudPlayer player = getPlayer(session.getUser().getUserId());

        if (player == null) {
    	    speechOutput = "Welcome to the Mud. I see this is your first time here, for instructions, say 'help me'. ";
	    player = newPlayer(session);
        } else {
            speechOutput = "Welcome back to the Mud.";
        }

        speechOutput += "You have " + player.inventory.size() + " items in your inventory.";
        // add the room description to the output
        speechOutput += "What do you want to do now?";
        repromptText = "For instructions, please say help me.";

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
