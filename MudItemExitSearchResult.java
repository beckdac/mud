package mud;

import java.util.List;
import java.util.ArrayList;

public class MudItemExitSearchResult {
    int found;
    List<MudItem> playerItems;
    List<MudItem> roomItems;
    List<MudExit> roomExits;

    public MudItemExitSearchResult() {
        playerItems = new ArrayList<MudItem>();
        roomItems = new ArrayList<MudItem>();
        roomExits = new ArrayList<MudExit>();
    }
}
