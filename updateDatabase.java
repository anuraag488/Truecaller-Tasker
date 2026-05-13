scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;
import com.joaomgcd.taskerm.action.java.JavaCodeException;

u = util();
d = data();

installId = tasker.getVariable("TC_installation_id");
if (!u.isValidString(installId) || installId.equals("%TC_installation_id")) {
    tasker.showToast("Installation ID not set! Cannot update database.");
    return;
}

userAgent = tasker.getVariable("TC_user_agent");
if (!u.isValidString(userAgent)) userAgent = "Truecaller/11.75.5 (Android;10)";

/* 
 * If the task is triggered automatically by a time profile, %caller1 starts with "profile=".
 * If you run it manually from the UI or via a button, it will be "ui" or "task=".
 * We force the update if it's a manual run.
 */
caller1 = tasker.getVariable("caller1");
forceUpdate = false;
if (caller1 == null || !caller1.startsWith("profile=")) {
    forceUpdate = true;
}

currentTime = System.currentTimeMillis() / 1000;
sevenDays = 7 * 86400;

/* Load the JSON variable tracking last updates */
lastUpdateJsonStr = tasker.getVariable("TC_last_update_check");
lastUpdateObj = null;
try {
    if (u.isValidString(lastUpdateJsonStr)) {
        lastUpdateObj = new JSONObject(lastUpdateJsonStr);
    } else {
        lastUpdateObj = new JSONObject();
    }
} catch (Exception e) {
    // Failsafe if the JSON string gets corrupted
    lastUpdateObj = new JSONObject();
}

jsonModified = false;

boolean shouldUpdate(String key) {
    if (forceUpdate) return true;
    
    long lastCheck = lastUpdateObj.optLong(key, 0L);
    if (lastCheck == 0L) return true; // Never updated
    
    return (currentTime - lastCheck) > sevenDays;
}

httpClient = new OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build();

db = null;
try {
    db = d.getWritableDb();
} catch (Exception e) {
    tasker.showToast("Could not open database: " + e.getMessage());
    return;
}

try {
    /* --- 1. SPAM CATEGORIES --- */
    if (shouldUpdate("spam_categories")) {
        u.logToFile("Updating Spam Categories...");
        try {
            request = new Request.Builder()
                .url("https://filter-store4-noneu.truecaller.com/v1/spamCategories?encoding=json")
                .header("Authorization", "Bearer " + installId)
                .header("user-agent", userAgent)
                .build();
                
            response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                jsonStr = response.body().string();
                rootObj = new JSONObject(jsonStr);
                arr = rootObj.getJSONArray("categories");
                
                db.beginTransaction();
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS spam_categories (id TEXT PRIMARY KEY, name TEXT, icon TEXT)");
                    db.execSQL("DELETE FROM spam_categories"); 
                    
                    for (int i = 0; i < arr.length(); i++) {
                        obj = arr.getJSONObject(i);
                        cv = new ContentValues();
                        cv.put("id", obj.optString("id"));
                        cv.put("name", obj.optString("name"));
                        cv.put("icon", obj.optString("icon", null));
                        db.insert("spam_categories", null, cv);
                    }
                    db.setTransactionSuccessful();
                    
                    // Update JSON Object
                    lastUpdateObj.put("spam_categories", currentTime);
                    jsonModified = true;
                    u.logToFile("Spam Categories updated successfully.");
                } finally {
                    db.endTransaction();
                }
            } else {
                u.logToFile("Failed to fetch Spam Categories: HTTP " + response.code());
            }
            response.close();
        } catch (Exception e) {
            u.logToFile("Error updating Spam Categories: " + e.getMessage());
        }
    }

    /* --- 2. TAGS --- */
    if (shouldUpdate("tags")) {
        u.logToFile("Updating Tags...");
        try {
            request = new Request.Builder()
                .url("https://tagging5-noneu.truecaller.com/v1/tags?encoding=json")
                .header("Authorization", "Bearer " + installId)
                .header("user-agent", userAgent)
                .build();
                
            response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                jsonStr = response.body().string();
                rootObj = new JSONObject(jsonStr);
                arr = rootObj.getJSONArray("data");
                
                db.beginTransaction();
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS tags (id TEXT PRIMARY KEY, name TEXT, iconUrl TEXT, parentId TEXT)");
                    db.execSQL("DELETE FROM tags"); 
                    
                    for (int i = 0; i < arr.length(); i++) {
                        obj = arr.getJSONObject(i);
                        cv = new ContentValues();
                        cv.put("id", obj.optString("id"));
                        cv.put("name", obj.optString("name"));
                        if (!obj.isNull("parentId")) cv.put("parentId", obj.optString("parentId"));
                        if (!obj.isNull("iconUrl")) cv.put("iconUrl", obj.optString("iconUrl"));
                        db.insert("tags", null, cv);
                    }
                    db.setTransactionSuccessful();
                    
                    // Update JSON Object
                    lastUpdateObj.put("tags", currentTime);
                    jsonModified = true;
                    u.logToFile("Tags updated successfully.");
                } finally {
                    db.endTransaction();
                }
            } else {
                u.logToFile("Failed to fetch Tags: HTTP " + response.code());
            }
            response.close();
        } catch (Exception e) {
            u.logToFile("Error updating Tags: " + e.getMessage());
        }
    }

    /* --- 3. TOP SPAMMERS --- */
    /*if (shouldUpdate("topspammers")) {
        u.logToFile("Updating Top Spammers...");
        try {
            request = new Request.Builder()
                .url("https://topspammers-noneu.truecaller.com/v4/spammers?maxSize=40000&type=caller&encoding=json")
                .header("Authorization", "Bearer " + installId)
                .header("user-agent", userAgent)
                .build();
                
            response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                jsonStr = response.body().string();
                arr = new JSONArray(jsonStr);
                
                db.beginTransaction();
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS topspammers (number TEXT PRIMARY KEY, name TEXT, reports INTEGER DEFAULT 0)");
                    db.execSQL("DELETE FROM topspammers");
                    
                    for (int i = 0; i < arr.length(); i++) {
                        obj = arr.getJSONObject(i);
                        cv = new ContentValues();
                        cv.put("name", obj.optString("label"));
                        cv.put("number", obj.optString("value"));
                        cv.put("reports", obj.optInt("reports", 0));
                        db.insert("topspammers", null, cv);
                    }
                    db.setTransactionSuccessful();
                    
                    // Update JSON Object
                    lastUpdateObj.put("topspammers", currentTime);
                    jsonModified = true;
                    u.logToFile("Top Spammers updated successfully.");
                } finally {
                    db.endTransaction();
                }
            } else {
                u.logToFile("Failed to fetch Top Spammers: HTTP " + response.code());
            }
            response.close();
        } catch (Exception e) {
            u.logToFile("Error updating Top Spammers: " + e.getMessage());
        }
    }*/
} finally {
    if (db != null) {
        db.close();
    }
}

/* Save the updated JSON back to Tasker if there were any successful updates */
if (jsonModified) {
    tasker.setVariable("TC_last_update_check", lastUpdateObj.toString());
}

if (forceUpdate) {
    tasker.showToast("Database Update Complete");
}