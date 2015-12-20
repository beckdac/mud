package mud;

import java.util.Map;

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
    public static int addItem(Map<String, MudItem> items, MudItem item) {
        // put in the new item

        MudItem previousItem = items.put(item.getShortName(), item);
        // if we had a non-null return value there was a pre-existing item
        // with the same name; rename it with a space and a number, starting
        // at 2, rename any previous item with that name with incremental
        // numbers
        int suffix = 2;
        while (previousItem != null) {
            String nextName = previousItem.getShortName() + " " + Integer.toString(suffix++);
            previousItem = items.put(nextName, previousItem);
        }
        return suffix - 2;
    }

    // remove an item from the list and pop names off the stack
    public static MudItem removeItem(Map<String, MudItem> items, String name) {
        MudItem item = items.get(name);
        if (item == null)
            return null;

        int suffix = 0;
        String shortName = item.getShortName();
        items.remove(name);
        if (shortName.equals(name))
            suffix = 2;
        else {
            String words[] = name.split(" ");
            String lastWord = words[words.length - 1];
            if (lastWord.matches("^\\d+$")) {
                suffix = Integer.parseInt(lastWord) + 1;
            } 
        }
        if (suffix > 0) {
            String previousName = name;
            String nextName = shortName + " " + Integer.toString(suffix);
            MudItem nextItem = items.get(nextName);
            while (nextItem != null) {
                items.put(previousName, nextItem);
                items.remove(nextName);
                previousName = nextName;
                nextName = shortName + " " + Integer.toString(suffix++);
                nextItem = items.get(nextName);
            }
        } // else case can only happen on database inconsistency, transactions? or weird names
        return item;
    }

    public static boolean hasItem(Map<String, MudItem> itemMap, String name) {
        if (itemMap.containsKey(name))
            return true;
        return false;
    }

    public static MudItem getItemIfExists(Map<String, MudItem> itemMap, String name) {
        if (hasItem(itemMap, name))
            return itemMap.get(name);
        return null;
    }

    public static boolean transferItem(String name, Map<String, MudItem> from, Map<String, MudItem> to) {
        MudItem item = from.get(name);
        if (item == null) {
            return false;
        }
        MudItemMapHelper.addItem(to, item);
        MudItemMapHelper.removeItem(from, name);

        return true;
    }
}
