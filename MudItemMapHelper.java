package mud;

import java.util.List;

public final class MudItemMapHelper {

    private MudItemMapHelper() {
    }

    // next two functions manage name collisions in item lists
    // these work by appending numbers, starting with two to any
    // additional items with the same name in the same item list, e.g.
    // key, key 2, key 3, key 4

    // this effectively acts as a stack with the most recent item
    // in a collision being the unnumbered on, e.g.
    // with a stack like: key, key 2
    // and an addItem of key the old key would become key 2 and key 2
    // would become key key 3

    // add an item to the list and resolve any name conflicts
    // returns how many name collisions it resolved
    public static int addItem(List<MudItem>, MudItem item) {
        // put in the new item
        MudItem previousItem = contents.put(item.getShortName(), item);
        // if we had a non-null return value there was a pre-existing item
        // with the same name; rename it with a space and a number, starting
        // at 2, rename any previous item with that name with incremental
        // numbers
        int suffix = 2;
        while (previousItem != null) {
            String nextName = previousItem.getShortName() + " " + Integer.toString(suffix++);
            previousItem = contents.put(nextName, item);
        }
        return suffix - 2;
    }

    // remove an item from the list and pop names off the stack

    public static boolean transferItem(Map<String, MudItem> from, Map<String, MudItem> to, String name) {
        MudItem item = from.getItems().get(name);
        if (item == null) {
            return false;
        }
        addItem(to, item);
        from.getItems().remove(itemKey);
        to.getItems().put(item, fromItem);

        return true;
    }
}
