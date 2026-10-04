scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import java.util.ArrayList;
import java.util.HashMap;

u = util();
d = data();

numbersStr = tasker.getVariable("par1");
if (!u.isValidString(numbersStr)) {
    return;
}

/* par2 = "true" to fetch block reasons for all numbers */
par2 = tasker.getVariable("par2");
fetchBlockReason = "true".equals(par2);

/* par3 = "false" to strictly use cached data, otherwise fetch fresh data for old/missing entries */
par3 = tasker.getVariable("par3");
fetchIfOld = !"false".equals(par3);

/* Split the comma-separated numbers into an ArrayList */
numArr = numbersStr.split(",");
list = new ArrayList();
for (int i = 0; i < numArr.length; i++) {
    n = numArr[i].trim();
    if (!n.isEmpty()) {
        list.add(n);
    }
}

if (list.isEmpty()) {
    return;
}

numbersToFetch = new ArrayList();
dbResults = new HashMap();

/* 1. Check the local DB first to separate cached vs. outdated numbers */
for (int i = 0; i < list.size(); i++) {
    number = (String) list.get(i);
    details = d.loadNumberDetails(number);
    shouldFetch = d.needsUpdate(details);
    
    if (shouldFetch && !fetchIfOld) shouldFetch = false;
    
    if (shouldFetch) {
        numbersToFetch.add(number);
    } else {
        dbResults.put(number, details);
    }
}

/* 2. Fetch only the outdated/missing numbers from Truecaller */
if (!numbersToFetch.isEmpty()) {
    try {
        fetchedResults = d.fetchBulkFromTruecaller(numbersToFetch);
        if (fetchedResults != null) {
            keys = fetchedResults.keySet().toArray();
            for (int i = 0; i < keys.length; i++) {
                key = (String) keys[i];
                dbResults.put(key, fetchedResults.get(key));
            }
        }
    } catch (Exception e) {
        tasker.showToast("Bulk fetch error: " + e.getMessage());
    }
}

/* 3. Format the final output */
formattedResults = new HashMap();
requestedKeys = list.toArray(); 

for (int i = 0; i < requestedKeys.length; i++) {
    key = (String) requestedKeys[i];
    details = (Object[]) dbResults.get(key); 
    
    itemMap = new HashMap();
    if (details != null) {
        itemMap.put("name", details[0]);
        itemMap.put("json", details[1]);
        itemMap.put("spam_score", details[2]);
        itemMap.put("is_verified", details[3]);
        itemMap.put("image", details[4]);
        itemMap.put("note", details[5]);
    }
    
    e164Number = u.convertToE164(key);
    itemMap.put("e164Number", e164Number);
    
    csName = d.getPhoneBookName(key);
    if (csName != null) itemMap.put("cs_name", csName);

    /* 4. Evaluate Block Reason if requested via %par2 */
    if (fetchBlockReason) {
        blockReason = d.getBlockReason(key, csName, details);
        if (blockReason != null) {
            itemMap.put("block_reason", blockReason);
        }
    }
    
    formattedResults.put(key, itemMap);
}

return tasker.toJson(formattedResults, true);