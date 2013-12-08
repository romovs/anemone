// Example script to starve/eat (No table).
//
// Place you char between LC and a chair. Then run the script. 
// It will plow current location three times then sit on chair till stamina recovers. 
// It will eat any food found in the inventory or in the LC.
//
// 1. Plow current location 3 times.
// 2. Sit in chair for one minute.
// 3. Eat any FEP food found in the inventory.
// 4. If no food in inventory get it from a container.
// 5. Goto step 1.
//
// NOTE: Player should be standing next to container and a chair before running the script. 
//       Something like [chair - player - container].

import java.util.*;
import java.lang.*;
import haven.event.*;
import haven.*;

searchRadius = 10;

chair = maid.doAreaFind(searchRadius, "chair");
container = maid.doAreaFind(searchRadius, "lchest");
plowCoord = maid.getCoord();

while(true) {
    // plow 3 times
    maid.doAction("act", "plow");
    maid.doLeftClick(plowCoord);
    maid.waitForTask();
       
    maid.doAction("act", "plow");
    maid.doLeftClick(plowCoord);
    maid.waitForTask();
    
    maid.doAction("act", "plow");
    maid.doLeftClick(plowCoord);
    maid.waitForTask();
    
    maid.doRightClick(chair); // get rid of plow action
    maid.doRightClick(chair); // get on chair
    sit();

    // check if have any FEP food in the inventory
    Inventory inv = maid.getInventory();
    Item[] items = maid.getItems(inv);
    boolean noFepFood = true;
    for (Item i : items) {
        ItemType itemType = i.getItemType(i);
        if (itemType == ItemType.FOOD_FEP) {
            noFepFood = false;
            break;
        }
    }
    
    // if we don't have any food in the inventory - get some from the container
    if (noFepFood) {
        maid.doRightClick(container);
        maid.waitForInventory();        // wait for container inventory to get created

        // get LC inventory
        Inventory contInv = maid.getInventory("Chest");
        Item[] contItems = maid.getItems(contInv);

        // transfer items
        for (Item item : contItems) {
            maid.doTransfer(item);
            maid.waitForItemCreate();
            if (maid.getInventoryFreeSlots(inv) == 0) // stop if our inventory is full
                break;
        }
    }
    
    items = maid.getItems(inv);
    if (items.length == 0)          // no more food left. stop.
        break;
        
    // if below Full 50% - eat till we reach Full 50% on the hunger bar or run out of the food in inventory
    if (maid.meterHunger != null && 
        (maid.meterHunger.type == MeterEventObjectHunger.HungerType.STARVING ||
        maid.meterHunger.type == MeterEventObjectHunger.HungerType.HUNGRY ||
        maid.meterHunger.type == MeterEventObjectHunger.HungerType.VERY_HUNGRY || 
        (maid.meterHunger.type == MeterEventObjectHunger.HungerType.FULL && maid.meterHunger.value < 50))) {
    
        for (Item i : items) {
            ItemType itemType = i.getItemType(i);

            // if items is FEP food - eat it
            if (itemType == ItemType.FOOD_FEP) {
                maid.doInteract(i);

                flowerMenu = maid.waitForFlowerMenu();
                maid.doSelect(flowerMenu, "Eat");
                maid.waitForItemDestroy();
                // stop if we are above 50% Full.
	            if (maid.meterHunger != null && 
	                maid.meterHunger.type == MeterEventObjectHunger.HungerType.FULL &&
	                maid.meterHunger.value >= 50) {
		            break;
                }
            } 
        }
    }
}

def sit() {
    Thread.sleep(60*1000); // sit for 1 minute
}
