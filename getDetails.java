scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import android.util.Patterns;
import com.joaomgcd.taskerm.action.java.JavaCodeException;
import java.util.HashMap;

u = util();
d = data();

number = tasker.getVariable("par1");
if (!u.isValidString(number) || !Patterns.PHONE.matcher(number).matches()) {
    u.logDebug("getDetails: ignoring invalid or empty number: " + number);
    return;
    //throw new JavaCodeException("par1 (phone number) is required");
}

par2 = tasker.getVariable("par2");
fetchBlockReason = "true".equals(par2); // Default to true

par3 = tasker.getVariable("par3");
fetchIfOld = !"false".equals(par3); // Default to true
u.logDebug("getDetails: number=" + number + " fetchBlockReason=" + fetchBlockReason + " fetchIfOld=" + fetchIfOld);

details = d.loadNumberDetails(number);
shouldFetch = d.needsUpdate(details);

if (shouldFetch && !fetchIfOld) shouldFetch = false;
u.logDebug("getDetails: cached=" + (details != null) + " shouldFetch=" + shouldFetch);

if (shouldFetch) {
    try {
        details = d.fetchFromTruecaller(number);
    } catch (Exception e) {
        /* The fetch error itself is already logged by data.fetchFromTruecaller. */
        u.logDebug("getDetails: fetch failed, using cached details: " + (details != null));
        tasker.showToast(e.getMessage());
        if (details == null) {
            return tasker.toJson(new HashMap());
        }
    }
}

responseMap = new HashMap();

if (details != null) {
    responseMap.put("name", details[0]);
    responseMap.put("json", details[1]);
    responseMap.put("spam_score", details[2]);
    responseMap.put("is_verified", details[3]);
    responseMap.put("image", details[4]);
    responseMap.put("note", details[5]);
}

e164Number = u.convertToE164(number);
responseMap.put("e164Number", e164Number);

csName = d.getPhoneBookName(number);
if (csName != null) responseMap.put("cs_name", csName);

if (fetchBlockReason) {
    blockReason = d.getBlockReason(number, csName, details);
    if (blockReason != null) {
        responseMap.put("block_reason", blockReason);
    }
}

u.logDebug("getDetails: done number=" + number + " found=" + (details != null) + " block_reason=" + responseMap.get("block_reason"));
return tasker.toJson(responseMap, true);