scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.util.HashMap;

u = util();
d = data();

callLogUri = Uri.parse("content://call_log/calls");

// 1. Gather all unique numbers that have a blank name
projection = new String[]{"number"};
selection = "name IS NULL OR name = ''";
cursor = null;
uniqueMap = new HashMap();

try {
    cursor = context.getContentResolver().query(callLogUri, projection, selection, null, null);
    if (cursor != null) {
        while (cursor.moveToNext()) {
            number = cursor.getString(0);
            if (u.isValidString(number)) {
                // HashMap automatically deduplicates numbers
                uniqueMap.put(number, true);
            }
        }
    }
} catch (Exception e) {
    u.logToFile("Error reading call log: " + e.getMessage());
} finally {
    if (cursor != null) {
        cursor.close();
    }
}

// 2. Iterate through unique numbers and update all their matching rows at once
updatedRowsCount = 0;
keys = uniqueMap.keySet().toArray();

for (int i = 0; i < keys.length; i++) {
    number = (String) keys[i];
    
    // Look up local database only once per unique number
    details = d.loadNumberDetails(number);
    
    if (details != null && details[0] != null) {
        tcName = (String) details[0];
        spamScore = ((Integer) details[2]).intValue();
        isVerified = ((Boolean) details[3]).booleanValue();

        // Format the name
        formattedName = "🆔 " + tcName;
        if (isVerified) {
            formattedName += " ✅";
        } else if (spamScore > 0) {
            formattedName += " 🔴";
        }

        cv = new ContentValues();
        cv.put("name", formattedName);

        // Update all rows simultaneously for this specific number
        updateSelection = "(name IS NULL OR name = '') AND number = ?";
        
        String[] selectionArgs = new String[1];
        selectionArgs[0] = number;

        try {
            rows = context.getContentResolver().update(callLogUri, cv, updateSelection, selectionArgs);
            if (rows > 0) {
                updatedRowsCount += rows;
            }
        } catch (Exception e) {
            u.logToFile("Error updating number " + number + ": " + e.getMessage());
        }
    }
}

u.logToFile("Updated " + updatedRowsCount + " call log entries.");