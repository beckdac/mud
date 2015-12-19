package mud;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

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

    private final MongoDatabase db;

    public MudManager(final MongoClient mongoClient) {
	db = mongoClient.getDatabase(MONGO_DATABASE);
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
        MudUser user = MudDB.getUser(session);

        if (user == null) {
            return getNewPlayerResponse(session);
        }

	    String speechOutput, repromptText;
        speechOutput = "You have " + user.inventory().size() + " items in your inventory.";
        // add the room description to the output
        speechOutput += "What do you want to do now?";
        repromptText = "For instructions, please say help me.";

        return getAskSpeechletResponse(speechOutput, repromptText);
    }

    private SpeechletResponse getNewPlayerResponse(Session session) {
	    String speechOutput, repromptText;
    	    speechOutput = "Welcome to the Mud. I see this is your first time here. ";
	    repromptText = "For instructions, please say help me.";
	    speechOutput += repromptText;
	    MudDB.newUser(session);
    }

    public SpeechletResponse getLookIntentResponse(Intent intent, Session session) {
        MudUser user = MudDB.getUser(session);

        if (user == null) {
            return getNewPlayerResponse(session);
        }

	    String speechOutput, repromptText;
        speechOutput = user.room().description();
        repromptText = "You can look again, ask for help, get a hint, or do something.";

        return getAskSpeechletResponse(speechOutput, repromptText);
    }

    public SpeechletResponse getHintIntentResponse(Intent intent, Session session) {
        MudUser user = MudDB.getUser(session);

        if (user == null) {
            return getNewPlayerResponse(session);
        }

	    String speechOutput, repromptText;
        speechOutput = user.room().hint();
        repromptText = "That is what is available for a hint.  Try looking again or searching, maybe?";

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
    public SpeechletResponse getHelpIntentResponse(Intent intent, Session session) {
        String speechOutput = "You are in a multiuser dungeon or mud. "
        + "Your goal is to find treasure, gain experience, and escape! "
        + "You can take actions like 'look around', 'open chest', "
        + "'light torch', 'get key', and 'search room'. "
        + "You can move around the dungeon by going through exits, "
        + "for example if I tell you a room has a north exit, you "
        + "can say 'go north'.  You can also ask me for a hint. "
        + "Now, what can I help you with?";
        String repromptText = "Try saying 'look around' or ask me for help again to hear the instructions.";
        return newAskResponse(speechOutput, repromptText);
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
