package mud;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public final class MudItemExitSearchHelper {

    private MudItemExitSearchHelper() {
    }

    public static boolean isItemMatch(MudPlayer player, MudItem mudItem,
            Boolean isGetable, Boolean isContainer, Boolean isVisible, Boolean isUseable,
            Boolean hasUsesLeft, Boolean isIngestable, String hasTag) {

        if (mudItem != null) {
            boolean addItem = true;
            if (isGetable != null && (isGetable == mudItem.getIsGetable()))
                addItem = false;
            else if (isContainer != null && (isContainer == mudItem.getIsContainer()))
                addItem = false;
            else if (isVisible != null && (isVisible == (mudItem.getIsVisible() || mudItem.getIsVisibleTo(player))))
                addItem = false;
            else if (isUseable != null && (isUseable == mudItem.getIsUsable()))
                addItem = false;
            else if (hasUsesLeft != null && ((hasUsesLeft && mudItem.getUsesLeft() > 0) 
                    || (!hasUsesLeft && mudItem.getUsesLeft() == 0)))
                addItem = false;
            else if (isIngestable != null && (isIngestable == mudItem.getIsIngestable()))
                addItem = false;
            else if (hasTag != null && mudItem.hasTag(hasTag))
                addItem = false;
        }
        return false;
    }

    public static MudItemExitSearchResult MudItemExitSearch(MudPlayer player, String name,
            Boolean isGetable, Boolean isContainer, Boolean isVisible, Boolean isUseable,
            Boolean hasUsesLeft, Boolean isIngestable, String hasTag, Boolean includePlayer,
            Boolean includeRoom, Boolean includeExits, Boolean includeFullName) {

        MudItemExitSearchResult result = new MudItemExitSearchResult();
        MudRoom room = player.getRoom();
        MudItem mudItem;

        if (includePlayer != null && includePlayer) {
            mudItem = player.getItemIfExists(name);
            if (isItemMatch(player, mudItem, isGetable, isContainer, isVisible, isUseable,
                    hasUsesLeft, isIngestable, hasTag))
                result.playerItems.add(mudItem);
            if (includeFullName) {
                List<MudItem> mudItemList = player.getItemListIfExistsByFullName(name);
                Iterator<MudItem> it = mudItemList.iterator();
                while (it.hasNext()) {
                    mudItem = it.next();
                    if (isItemMatch(player, mudItem, isGetable, isContainer, isVisible, isUseable,
                            hasUsesLeft, isIngestable, hasTag))
                        result.playerItems.add(mudItem);
                }
            }
        }

        if (includeRoom != null && includeRoom) {
            mudItem = room.getItemIfExists(name);
            if (isItemMatch(player, mudItem, isGetable, isContainer, isVisible, isUseable,
                    hasUsesLeft, isIngestable, hasTag))
                result.roomItems.add(mudItem);
            if (includeFullName) {
                List<MudItem> mudItemList = room.getItemListIfExistsByFullName(name);
                Iterator<MudItem> it = mudItemList.iterator();
                while (it.hasNext()) {
                    mudItem = it.next();
                    if (isItemMatch(player, mudItem, isGetable, isContainer, isVisible, isUseable,
                            hasUsesLeft, isIngestable, hasTag))
                        result.playerItems.add(mudItem);
                }
            }
        }

        if (includeExits != null && includeExits) {
            MudExit mudExit = room.getExitIfExists(name);
            boolean addExit = true;
            if (isGetable != null && isGetable == true)
                addExit = false;
            else if (isContainer != null && isContainer == true)
                addExit = false;
            else if (isVisible != null && (isVisible == (mudExit.getIsVisible() || mudExit.getIsVisibleTo(player))))
                addExit = false;
            else if (isUseable != null && isUseable == true)
                addExit = false;
            else if (hasUsesLeft != null)
                addExit = false;
            else if (isIngestable != null)
                addExit = false;
            else if (hasTag != null && !mudExit.hasTag(hasTag))
                addExit = false;
            if (addExit)
                result.roomExits.add(mudExit);
        }
        return result;
    }
}
