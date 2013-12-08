// Example script for farming carrots.
// Harvests and replants all carrots (in 4th grow stage) in a 30 tile radius.
// Ground needs to be plowed beforehand or script will fail.
// Also there should be any obstacles in the way.
// Will fail when runs out of stamina.
//
// 1. Harvest all carrots found in a given area till inventory is full.
// 2. Replants all the highest q carrots.
// 3. Drop any leftovers.
// 4. Goto step 1.
//
// !!NOTE!! Not all food has been described yet, so you might want to check/edit itemtypes.conf file.

import java.util.*;
import haven.*;

// search for carrots in 30 tile radius
farmRadius = 30;

// array to hold locations of carrots we have harvested. 
// we will use it to plant the carrots back at same locations.
List<Coord> emptyLocs = new ArrayList<Coord>();

Inventory inv = maid.getInventory();

// find carrots in 4th grow stage
while((carrot = maid.doAreaFindCrops(farmRadius, "carrot", 4)) != null) {

    // save carrot location
    emptyLocs.add(carrot.c);   

    // harvest it
    maid.doRightClick(carrot);
    menu = maid.waitForFlowerMenu();
    maid.doSelect(menu, "Harvest");
    maid.waitForItemCreate(3); // if we are on full NATURE we should wait for 3 carrots to be created in the inventory

    // get number of free slots in the inventory
    inv = maid.getInventory();
    freeSlots = maid.getInventoryFreeSlots(inv);
    
    // if there is no more space for carrots in the inventory start planting them
    if (freeSlots < 3) {
        plant(inv, emptyLocs);

        // drop any leftovers
        seeds = maid.getItems(inv, "carrot");
        for (Item seed : seeds) {
            maid.doDrop(seed);
            maid.waitForItemDestroy();
        }
    }
}

def plant(Inventory inv, List<Coord> emptyLocs) {
    // get all carrots and sort them by their quality in descending order 
    Item[] seeds = maid.getItems(inv, "carrot");
    
	Arrays.sort(seeds, new Comparator<Item>(){
                            public int compare(Item o1, Item o2) {
                                if(o1.q == o2.q)
                                     return 0;
                                return o1.q > o2.q ? -1 : 1;
                            } });      
	// plant them
	int i = 0;
    for (Coord c : emptyLocs) {
        Item seedToPlant = seeds[i++];  
        // take the carrot from inventory    
        maid.doTake(seedToPlant);
        maid.waitForGrab();
        // plant it
        maid.doInteract(c);
        maid.waitForRelease();
    }
    
    // we have planted in all the harvested spots, hence clear the locations list.
    emptyLocs.clear();
}
