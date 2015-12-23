package mud;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.Datastore;

import org.bson.types.ObjectId;

import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

// the MudManager essentially is the glue between the data access objects / mongodb
// and the alexa skill handler
public class MudManager {
    private static final Logger log = LoggerFactory.getLogger(MudManager.class);

    private static final String SLOT_CONTAINERANDEXITSPEC = "ContainerAndExitSpec";
    private static final String SLOT_CONTAINERSPEC = "ContainerSpec";
    private static final String SLOT_EXITSPEC = "ExitSpec";
    private static final String SLOT_INGESTABLESPEC = "IngestableSpec";
    private static final String SLOT_LOCKABLESPEC = "LockableSpec";
    private static final String SLOT_OBJECTSPEC = "ObjectSpec";
    private static final String SLOT_ONOBJECTSPEC = "OnObjectSpec";

    private static final String MONGO_DATABASE = "mud";

    private static <T> T randomFrom(T... items) { return items[new Random().nextInt(items.length)]; }
    private static final String[] WHAT_NEXT_Q_LIST = {
            "<p>What do you want to do now?</p>",
            "<p>Tell me what you want to do next?</p>",
            "<p>What do you want to do next?</p>",
            "<p>OK, what next?</p>",
            "<p>What now?</p>"
        };
    private static final String[] REPROMPT_Q_LIST = {
            "<p>Still there? Why not try saying 'inventory' for something to 'use'.</p>",
            "<p>For instructions, please say 'help me'.</p>",
            "<p>You can look again, take some other action, ask for help or a hint.</p>",
            "<p>Why not try taking another look around or doing a search?</p>"
        };
    private static final String[] SUCCESS_LIST = {
            "<p>Done.</p>",
            "<p>No problem.</p>",
            "<p>Success.</p>",
            "<p>Roger, roger.</p>",
            "<p>Ten four.</p>"
        };
    private static final String[] OBJECT_NOT_FOUND_LIST = {
            "<p>Sorry, I cannot find %s.</p>",
            "<p>Uhhh, I can't find %s.</p>",
            "<p>There doesn't seem to be a %s nearby.</p>",
            "<p>Nope.  Sorry, no %s around.</p>"
        };
    private static final String[] GOODBYE_LIST = {
            "<p>Goodbye.</p>",
            "<p>Over and out.</p>",
            "<p>Back to my day job.</p>",
            "<p>Peace out.</p>",
            "<p>Kay Kay Buh Bye.</p>"
        };

    private final Morphia morphia;
    private final Datastore datastore;

    private MudPlayer player;

    private String speechOutput;
    private String repromptSpeech;

    public MudManager(final MongoClient mongoClient, Session session) {
        morphia = new Morphia();
        morphia.map(MudPlayer.class).map(MudRoom.class).map(MudItem.class).map(MudExit.class)
                .map(MudLock.class).map(MudAccessControl.class);
        datastore = morphia.createDatastore(new MongoClient(), MONGO_DATABASE);
        datastore.ensureIndexes();

        // ready to go
    }

    public void joinSession(Session session) {
        String userId = session.getUser().getUserId();
        String sessionId = session.getSessionId();
        speechOutput = "";
        repromptSpeech = "";

        log.info("joinSession called for userId = {}, sessionId = {}", userId, sessionId);
        // we are up to date with the current session information
        if (player != null && player.getId().equals(userId) && player.getSessionId().equals(sessionId)) {
            // instrumentation that might be expensive
            player.incrementInteractions();
            datastore.save(player);
            return;
        }
        log.info("no existing session found, creating a new one for userId = {}, sessionId = {}", userId, sessionId);
        // load the player and populate the interaction
        player = MudManagerHelper.getPlayer(datastore, userId);
        if (player.getIsNew()  == true) {
    	    speechOutput += "<p><s>Ahhh <break strength='strong'/> I always love a new player.</s>  <s>Welcome.</s>  <s>For instructions, say <break strength='strong'/>'help me'.</s></p>";
            player.setIsNew(false);
            player.setSessionId(sessionId);
            player.incrementSessions();
            datastore.save(player);
        }
        if (!player.getSessionId().equals(sessionId)) {
            speechOutput += "<p>Welcome back to the Mud.</p>";
            player.setSessionId(sessionId);
            player.incrementSessions();
            datastore.save(player);
        }
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
        speechOutput += getRoomFullDescriptionSSML();
        speechOutput += "<p>You have " + player.getInventorySize() + " items in your inventory</p>";
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    private String getRoomFullDescriptionSSML() {
        String ssml = "";
        MudRoom room = player.getRoom();
        ssml += "<p>" + room.getDescription() + "</p>";
        String itemList = "";
        Iterator it = room.getItems().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            itemList += (String)pair.getKey() + ",";
        }
        if (itemList.length() > 1) {
            ssml += "<p>Nearby you see:" + itemList + "</p>";
        }
        int playersNearby = room.getPlayers().size();
        if (playersNearby > 1)
            ssml += String.format("<p>There are %d other players here.</p>", playersNearby - 1);
        String exitList = "";
        it = room.getExits().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            exitList += (String)pair.getKey() + ",";
        } 
        if (exitList.length() > 0) {
            if (exitList.length() > 1) {
                ssml += String.format("<p>You can go %d places: ", room.getExits().size());
                ssml += exitList + "</p>";
            }
        }
        return ssml;
    }

    public SpeechletResponse getLookIntentResponse(Intent intent, Session session) {
        Slot objectSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);
        if (objectSpecSlot != null && objectSpecSlot.getValue() != null) {
            String objectSpec = objectSpecSlot.getValue();
            // search items and exits on player and room
            MudItemExitSearchResult searchResult = 
                    MudManagerHelper.playerItemExitSearch(player, objectSpec,
                            null, null, true, null,
                            null, null, null, true,
                            true, true, false);
            // report search results
            if (searchResult.found == 0) {
                speechOutput += String.format(randomFrom(OBJECT_NOT_FOUND_LIST), objectSpec);
            } else if (searchResult.found == 1) {
                if (searchResult.playerItems.size() > 0)
                    speechOutput += "<p>" + searchResult.playerItems.get(0).getDescription() + "</p>";
                else if (searchResult.roomItems.size() > 0)
                    speechOutput += "<p>" + searchResult.roomItems.get(0).getDescription() + "</p>";
                else
                    speechOutput += "<p>" + searchResult.roomExits.get(0).getDescription() + "</p>";
            } else {
                // report how many found in each set, not too helpful right now :(
                speechOutput += String.format("<p><s>OK, I found %d things called '%s'.</s>", searchResult.found, objectSpec);
                if (searchResult.playerItems.size() > 0) {
                    speechOutput += "<s>" + Integer.toString(searchResult.playerItems.size()) + " in your inventory.</s>";
                } else if (searchResult.playerItems.size() > 0) {
                    speechOutput += "<s>" + Integer.toString(searchResult.roomItems.size()) + " nearby.</s>";
                } else {
                    speechOutput += "<s>And " + Integer.toString(searchResult.roomExits.size()) + "exits.<s/><p>";
                }
            }
        } else {
            speechOutput += getRoomFullDescriptionSSML();
        }
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getPutIntentResponse(Intent intent, Session session) {
        Slot objectSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);       // any object
        String objectSpec = null;
        Slot containerSpecSlot = intent.getSlot(SLOT_CONTAINERSPEC); // preceeded by 'into' so basically anything with isContainer set
        String containerSpec;

        MudItem mudItem = null;
        if (objectSpecSlot != null && objectSpecSlot.getValue() != null) {
            // check local inventory
            objectSpec = objectSpecSlot.getValue();
            mudItem = player.getItem(objectSpec);
        }
        if (mudItem != null) {
            MudItem mudItemInto; 
            if (containerSpecSlot != null && containerSpecSlot.getValue() != null) {
                containerSpec = containerSpecSlot.getValue();
                mudItemInto = player.getItem(containerSpec);
                
                // we need to find an object in the local environment that is a container
            }
        } else {
            if (objectSpec != null)
                speechOutput += "Sorry, I couldn't find an item named " + objectSpec + ".";
            else
                speechOutput += "Sorry, I don't understand what you want to put where.";
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        }
        return getAskSpeechletResponse();
    }

    public SpeechletResponse getGetIntentResponse(Intent intent, Session session) {
        Slot objectSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);          // any object
        Slot containerSpecSlot = intent.getSlot(SLOT_CONTAINERSPEC); // preceeded by 'from' so basically anything with isContainer set
        if (objectSpecSlot != null && objectSpecSlot.getValue() != null) {
            String objectSpec = objectSpecSlot.getValue();

            // this is a take or get item from a container
            if (containerSpecSlot != null && containerSpecSlot.getValue() != null) {
                String containerSpec = containerSpecSlot.getValue();
                MudItem fromContainer = null;
                // find in environment
                        MudItemExitSearchResult searchResult = 
                                MudManagerHelper.playerItemExitSearch(player, containerSpec,
                                null, true, true, null,
                                null, null, null, true,
                                true, false, false);
                        // for simplicity, use the first match
                        if (searchResult.playerItems.size() > 0) {
                            fromContainer = searchResult.playerItems.get(0);
                        } else if (searchResult.roomItems.size() > 0) {
                            fromContainer = searchResult.roomItems.get(0);
                        }
                        if (fromContainer != null) {
log.info("transfering");
                            // remove item from container, save, put in player, save
                            MudItem mudItem = null;
                            // dispenser tag doesn't remove the original from the source container
                            if (!fromContainer.tags.hasTag("dispenser")) {
                                mudItem = fromContainer.removeContent(objectSpec);
                            } else {
                                mudItem = fromContainer.getContent(objectSpec);
                            }
                            if (mudItem == null) {
log.info("failed find in container");
                                speechOutput += String.format("<p>Huh. I can't find a %s in the %s.</p>", objectSpec, containerSpec);
                            } else {
                                player.addItem(mudItem);
                                datastore.save(fromContainer);
                                datastore.save(player);
                                speechOutput += randomFrom(SUCCESS_LIST);
                            }
                        } else {
                            speechOutput += String.format("<p>I can't find a suitable container named %s</p>", containerSpec);
                        }
            } else {
            // take from the room
                MudItem mudItem = MudManagerHelper.playerFindItemInRoom(datastore, player, objectSpec);
                if (mudItem == null || !mudItem.getIsVisibleTo(player))
                    speechOutput += String.format(randomFrom(OBJECT_NOT_FOUND_LIST), objectSpec);
                else {
                    if (!mudItem.getIsGetable())
                        speechOutput += "<p>" + mudItem.getNotGetableMessage() + "</p>";
                    else {
                        if (containerSpecSlot != null && containerSpecSlot.getValue() != null) {
                            String containerSpec = containerSpecSlot.getValue();
                        } else if (MudManagerHelper.playerGetFromRoom(datastore, player, mudItem, objectSpec))
                            speechOutput += randomFrom(SUCCESS_LIST);
                        else
                            speechOutput += "<p>Uh oh. Looks like that is no longer here.</p>";
                    }
                }
            }
        } else {
            // what do you want to get?
            speechOutput += "Sorry, I don't know what you want to get.";
        }
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getDropIntentResponse(Intent intent, Session session) {
        Slot objectSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);          // any object in inventory
        if (objectSpecSlot != null && objectSpecSlot.getValue() != null) {
            String objectSpec = objectSpecSlot.getValue();
            if (MudManagerHelper.playerDrop(datastore, player, objectSpec))
                speechOutput += randomFrom(SUCCESS_LIST);
            else
                speechOutput += String.format(randomFrom(OBJECT_NOT_FOUND_LIST), objectSpec);
        } else {
            // what do you want to get?
            speechOutput += "Sorry, I don't know what you want to drop.";
        }
        // transferItem
        speechOutput += 
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getOpenIntentResponse(Intent intent, Session session) {
        // find items or exits with is closed
        speechOutput += "unimplemented";
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getUseIntentResponse(Intent intent, Session session) {
        speechOutput += "unimplemented";
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    // reveals hidden items
    public SpeechletResponse getSearchIntentResponse(Intent intent, Session session) {
        speechOutput += "unimplemented";
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getHintIntentResponse(Intent intent, Session session) {
        Slot objectSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);          // any object in inventory
        if (objectSpecSlot != null && objectSpecSlot.getValue() != null) {
            String objectSpec = objectSpecSlot.getValue();
            speechOutput += "unimplemented";
        } else {
            speechOutput += player.getRoom().getHint();
        }
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    // eat, drink, quaff
    public SpeechletResponse getIngestIntentResponse(Intent intent, Session session) {
        speechOutput += "unimplemented";
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    public SpeechletResponse getUnlockIntentResponse(Intent intent, Session session) {
        speechOutput += "unimplemented";
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

        return getAskSpeechletResponse();
    }

    // go through an exit
    public SpeechletResponse getGoIntentResponse(Intent intent, Session session) {
        Slot exitSpecSlot = intent.getSlot(SLOT_OBJECTSPEC);          // any exit in the room
        if (exitSpecSlot != null && exitSpecSlot.getValue() != null) {
            String exitSpec = exitSpecSlot.getValue();

            MudExit mudExit = MudManagerHelper.playerGetExit(datastore, player, exitSpec);
            if (mudExit != null) {
                if (mudExit.getIsLockedTo(player))
                    speechOutput += "<p>" + mudExit.getLockedMessage() + "</p>";
                else if (MudManagerHelper.playerMove(datastore, player, mudExit))
                    speechOutput += randomFrom(SUCCESS_LIST);
                else
                    speechOutput += "<p>There was a problem trying to move " + exitSpec + "</p>";
            } else {
                speechOutput += "<p>Sorry, I couldn't find an exit named " + exitSpec + "</p>";
            }
        } else {
            speechOutput += player.getRoom().getHint();
        }
        speechOutput += randomFrom(WHAT_NEXT_Q_LIST);
        repromptSpeech += randomFrom(REPROMPT_Q_LIST);

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
        repromptSpeech += "Try saying 'look around' or ask me for help again to hear the instructions.";

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
        return getTellSpeechletResponse(randomFrom(GOODBYE_LIST));
    }

    /**
     * Returns an ask Speechlet response for a speech and reprompt text.
     *
     * @param speechOutput
     *            Text for speech output
     * @param repromptSpeech
     *            Text for reprompt output
     * @return ask Speechlet response for a speech and reprompt text
     */
    private SpeechletResponse getAskSpeechletResponse() {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Mud");
        card.setContent(speechOutput);

        // create the SSML output
        SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml("<speak>" + speechOutput + "</speak>");

        // Create reprompt
        SsmlOutputSpeech reprompt = new SsmlOutputSpeech();
        ((SsmlOutputSpeech) reprompt).setSsml("<speak>" + repromptSpeech + "</speak>");
        Reprompt reprompter = new Reprompt();
        reprompter.setOutputSpeech(reprompt);

        // reset these for the next interaction
        speechOutput = "";
        repromptSpeech = "";

        return SpeechletResponse.newAskResponse(speech, reprompter, card);
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
