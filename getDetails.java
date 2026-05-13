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
    return;
    //throw new JavaCodeException("par1 (phone number) is required");
}

par2 = tasker.getVariable("par2");
fetchBlockReason = "true".equals(par2); // Default to true

par3 = tasker.getVariable("par3");
fetchIfOld = !"false".equals(par3); // Default to true

details = d.loadNumberDetails(number);
shouldFetch = d.needsUpdate(details);

if (shouldFetch && !fetchIfOld) shouldFetch = false;

if (shouldFetch) {
    try {
        details = d.fetchFromTruecaller(number);
    } catch (Exception e) {
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

return tasker.toJson(responseMap, true);