scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.joaomgcd.taskerm.action.java.ClassImplementation;
import com.joaomgcd.taskerm.action.java.JavaCodeException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

u = util();
d = data();
uiObj = ui();
vd = viewDetail(uiObj);

taskDescription = "Truecaller-List";

existingTask = uiObj.handleExistingTask(taskDescription, false);
if (existingTask != null) {
    existingTask.moveToFront();
    return;
}

currentTab = "calllog";
callLogData = new ArrayList();
truecallerLogData = new ArrayList();
searchResults = new ArrayList();
currentAppTask = null;
isSearching = false;
currentSearchId = 0;
rootContainer = null;
callLogTab = null;
truecallerTab = null;
tabIndicator = null;
mainListView = null;
mainWrapper = null;
searchBox = null;
detailOverlay = null;
dialpadView = null;
fab = null;
numberDisplay = null;
mainAdapter = null;
executorService = Executors.newCachedThreadPool();

String getT9Regex(String query) {
  sb = new StringBuffer();
  sb.append("(?i).*");
  for (int i = 0; i < query.length(); i++) {
    c = query.charAt(i);
    if (c == '2') sb.append("[2a-c]");
    else if (c == '3') sb.append("[3d-f]");
    else if (c == '4') sb.append("[4g-i]");
    else if (c == '5') sb.append("[5j-l]");
    else if (c == '6') sb.append("[6m-o]");
    else if (c == '7') sb.append("[7pqrs]");
    else if (c == '8') sb.append("[8t-v]");
    else if (c == '9') sb.append("[9wxyz]");
    else if (c == '*') sb.append("\\*");
    else if (c == '+') sb.append("\\+");
    else sb.append(c);
  }
  sb.append(".*");
  return sb.toString();
}

String getT9Glob(String query) {
  sb = new StringBuffer();
  sb.append("*");
  for (int i = 0; i < query.length(); i++) {
    c = query.charAt(i);
    if (c == '2') sb.append("[2a-cA-C]");
    else if (c == '3') sb.append("[3d-fD-F]");
    else if (c == '4') sb.append("[4g-iG-I]");
    else if (c == '5') sb.append("[5j-lJ-L]");
    else if (c == '6') sb.append("[6m-oM-O]");
    else if (c == '7') sb.append("[7pqrsPQRS]");
    else if (c == '8') sb.append("[8t-vT-V]");
    else if (c == '9') sb.append("[9wxyzWXYZ]");
    else if (c == '*') sb.append("[*]");
    else if (c == '+') sb.append("[+]");
    else sb.append(c);
  }
  sb.append("*");
  return sb.toString();
}

performLiveSearch(query) {
    uiObj.mainHandler.removeCallbacksAndMessages(null);
    cancelPendingLoads();
    currentSearchId++;
    final long thisSearchId = currentSearchId;
    
    if (!u.isValidString(query)) {
        isSearching = false;
        if (mainAdapter != null) mainAdapter.notifyDataSetChanged();
        return;
    }
    
    final String finalQuery = query;
    executorService.submit(new Runnable() {
        run() {
            callLogResults = loadCallLogData(finalQuery);
            if (thisSearchId != currentSearchId) return;
            truecallerResults = loadTruecallerLogData(finalQuery);
            if (thisSearchId != currentSearchId) return;
            contactsResults = searchContacts(finalQuery);
            if (thisSearchId != currentSearchId) return;
            
            allResults = new ArrayList();
            allResults.addAll(callLogResults);
            allResults.addAll(truecallerResults);
            allResults.addAll(contactsResults);
            final ArrayList finalResults = allResults;
            
            uiObj.mainHandler.post(new Runnable() {
                run() {
                    if (thisSearchId != currentSearchId) return;
                    searchResults = finalResults;
                    isSearching = true;
                    if (mainAdapter != null) mainAdapter.notifyDataSetChanged();
                    if (mainListView != null) mainListView.setSelection(0);
                }
            });
        }
    });
}

cancelPendingLoads() {
    if (uiObj.mainHandler != null) {
        uiObj.mainHandler.removeCallbacksAndMessages(null);
    }
}

cachedContactPhotos = null;
cachedContactsList = null;

loadAllContacts() {
    if (cachedContactsList != null) return cachedContactsList;
    list = new ArrayList();
    cursor = null;
    try {
        contactsUri = android.net.Uri.parse("content://com.android.contacts/data/phones");
        projection = new String[]{"display_name", "data1", "photo_uri"};
        cursor = context.getContentResolver().query(contactsUri, projection, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                name = cursor.getString(0);
                num = cursor.getString(1);
                photo = cursor.getString(2);
                if (name == null) name = "Unknown";
                if (num == null) num = "";
                cleanNum = num.replaceAll("[^0-9+*#]", "");
                list.add(new Object[]{name, num, photo, cleanNum});
            }
        }
    } catch(Exception e) {
        u.logToFile("Error loading contacts cache: " + e.getMessage());
    } finally {
        u.closeQuietly(cursor);
    }
    cachedContactsList = list;
    return list;
}

loadAllContactPhotos(forceRefresh) {
    if (cachedContactPhotos != null && !forceRefresh) return cachedContactPhotos;
    map = new HashMap();
    cursor = null;
    try {
        contactsUri = android.net.Uri.parse("content://com.android.contacts/data/phones");
        projection = new String[]{"data4", "photo_uri"};
        cursor = context.getContentResolver().query(contactsUri, projection, "photo_uri IS NOT NULL", null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                num = cursor.getString(0);
                photo = cursor.getString(1);
                if (u.isValidString(num) && u.isValidString(photo)) {
                    map.put(num, photo);
                }
            }
        }
    } catch(Exception e) {
        u.logToFile("Error loading contact photos: " + e.getMessage());
    } finally {
        u.closeQuietly(cursor);
    }
    cachedContactPhotos = map;
    return map;
}

cleanup() {
    u.logToFile("Cleaning up resources...");
    executorService.shutdownNow();
    uiObj.cleanup();
}

loadCallLogData(searchQuery) {
    list = new ArrayList();
    db = null;
    cursor = null;
    try {
        callLogUri = android.net.Uri.parse("content://call_log/calls");
        projection = new String[]{"date", "number", "normalized_number", "name", "type", "duration"};
        cursor = context.getContentResolver().query(callLogUri, projection, null, null, "date DESC");

        uniqueNumbers = new ArrayList();
        seenNumbers = new HashMap();
        rawCalls = new ArrayList();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                callDate = cursor.getLong(0);
                callNumber = cursor.getString(1);
                normNumber = cursor.getString(2);
                callName = cursor.getString(3);
                callType = cursor.getInt(4);
                callDuration = cursor.getInt(5);

                if (callNumber == null) callNumber = "";
                e164 = u.isValidString(normNumber) ? normNumber : u.convertToE164(callNumber);

                if (!seenNumbers.containsKey(e164)) {
                    seenNumbers.put(e164, true);
                    uniqueNumbers.add(e164);
                    rawCalls.add(new Object[]{callNumber, e164, callName, callDate, callType, new Integer(callDuration)});
                }
            }
            u.closeQuietly(cursor);
            cursor = null;
        }

        tcDataMap = new HashMap();
        db = d.getReadableDb();

        batchSize = 500;
        total = uniqueNumbers.size();
        for (int i = 0; i < total; i += batchSize) {
            end = Math.min(i + batchSize, total);
            placeholders = new StringBuilder();
            args = new String[end - i];
            for (int j = i; j < end; j++) {
                placeholders.append("?");
                if (j < end - 1) placeholders.append(",");
                args[j - i] = uniqueNumbers.get(j);
            }

            query = "SELECT d.number_e164, d.name as tc_name, d.is_verified, d.spam_score, " +
                    "d.image, d.note FROM data d WHERE d.number_e164 IN (" + placeholders.toString() + ")";

            dbCursor = db.rawQuery(query, args);
            while (dbCursor.moveToNext()) {
                tcData = new Object[]{
                    dbCursor.getString(1),
                    dbCursor.getInt(2),
                    dbCursor.getInt(3),
                    dbCursor.getString(4),
                    dbCursor.getString(5)
                };
                tcDataMap.put(dbCursor.getString(0), tcData);
            }
            u.closeQuietly(dbCursor);
        }
        
        contactPhotosMap = loadAllContactPhotos(false);

        sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        lowerQuery = u.isValidString(searchQuery) ? searchQuery.toLowerCase() : null;
        
        isT9 = false;
        t9Pattern = null;
        if (searchQuery != null && searchQuery.matches("[0-9+*#]+")) {
            isT9 = true;
            t9Pattern = Pattern.compile(getT9Regex(searchQuery));
        }

        for (int i = 0; i < rawCalls.size(); i++) {
            raw = rawCalls.get(i);
            callNumber = raw[0];
            e164 = raw[1];
            callName = raw[2];
            callDate = raw[3];
            callType = raw[4];
            callDuration = ((Integer) raw[5]).intValue();

            tcData = tcDataMap.get(e164);
            tcName = null;
            isVerified = false;
            spamScore = 0;
            image = null;
            note = null;

            if (tcData != null) {
                tcName = tcData[0];
                isVerified = tcData[1] == 1;
                spamScore = tcData[2];
                image = tcData[3];
                note = tcData[4];
            }
            
            photo = contactPhotosMap.get(e164);
            if (u.isValidString(photo)) image = photo;

            finalName = callName;
            if (!u.isValidString(finalName) || finalName.startsWith("🆔")) {
                if (u.isValidString(tcName)) {
                    finalName = "🆔 " + tcName;
                } else {
                    finalName = callNumber;
                }
            }

            formattedName = finalName;
            if (finalName.startsWith("🆔 ") && u.isValidString(tcName) && finalName.equals("🆔 " + tcName)) {
                if (isVerified) formattedName += " ✅";
                else if (spamScore > 0) formattedName += " 🔴";
            }

            matches = true;
            if (lowerQuery != null) {
                matches = false;
                if (isT9) {
                    cleanNum = callNumber != null ? callNumber.replaceAll("[^0-9+*#]", "") : "";
                    if (cleanNum.contains(searchQuery)) matches = true;
                    else if (finalName != null && t9Pattern.matcher(finalName).matches()) matches = true;
                    else if (tcName != null && t9Pattern.matcher(tcName).matches()) matches = true;
                    else if (note != null && t9Pattern.matcher(note).matches()) matches = true;
                } else {
                    if (finalName != null && finalName.toLowerCase().contains(lowerQuery)) matches = true;
                    else if (callNumber != null && callNumber.toLowerCase().contains(lowerQuery)) matches = true;
                    else if (tcName != null && tcName.toLowerCase().contains(lowerQuery)) matches = true;
                    else if (note != null && note.toLowerCase().contains(lowerQuery)) matches = true;
                }
            }

            if (matches) {
                entry = new Object[8];
                entry[0] = formattedName;
                entry[1] = e164;
                entry[2] = image;
                entry[3] = note;
                entry[4] = sdf.format(new java.util.Date(callDate)) + u.formatDuration(callDuration);
                entry[5] = isVerified;
                entry[6] = spamScore;
                entry[7] = callType;
                list.add(entry);
            }
        }
    } catch(Exception e) {
        u.logToFile("Error loading call log: " + e.getMessage());
    } finally {
        u.closeQuietly(cursor);
        u.closeQuietly(db);
    }
    return list;
}

loadTruecallerLogData(searchQuery) {
    list = new ArrayList();
    db = null;
    cursor = null;
    try {
        db = d.getReadableDb();
        sb = new StringBuffer();
        sb.append("SELECT ");
        sb.append("IFNULL(data.name,'Unknown') ||CASE WHEN is_verified = 1 THEN ' ✅'WHEN spam_score > 0 THEN ' 🔴'ELSE '' END as formatted_name, ");
        sb.append("number_e164, ");
        sb.append("image, ");
        sb.append("note, ");
        sb.append("strftime('%d-%m-%Y %H:%M:%S',datetime(round(timestamp), 'unixepoch', 'localtime')), ");
        sb.append("is_verified, ");
        sb.append("spam_score ");
        sb.append("FROM data ");
        
        queryArgs = null;
        if (u.isValidString(searchQuery)) {
            if (searchQuery.matches("[0-9+*#]+")) {
                glob = getT9Glob(searchQuery);
                sb.append("WHERE data.name GLOB ? OR number_e164 LIKE ? OR data.note GLOB ? ");
                queryArgs = new String[]{glob, "%" + searchQuery + "%", glob};
            } else {
                sb.append("WHERE data.name LIKE ? OR number_e164 LIKE ? OR data.note LIKE ? ");
                safeArg = "%" + searchQuery + "%";
                queryArgs = new String[]{safeArg, safeArg, safeArg};
            }
        }
        sb.append("ORDER BY timestamp DESC");
        
        contactPhotosMap = loadAllContactPhotos(false);
        
        cursor = db.rawQuery(sb.toString(), queryArgs);
        while (cursor.moveToNext()) {
            entry = new Object[8];
            entry[0] = cursor.getString(0);
            entry[1] = cursor.getString(1);
            
            num = cursor.getString(1);
            img = cursor.getString(2);
            photo = contactPhotosMap.get(num);
            if (u.isValidString(photo)) img = photo;
            entry[2] = img;
            
            entry[3] = cursor.getString(3);
            entry[4] = cursor.getString(4);
            entry[5] = cursor.getInt(5) == 1;
            entry[6] = cursor.getInt(6);
            entry[7] = 0;
            list.add(entry);
        }
    } catch(Exception e) {
        u.logToFile("Error loading truecaller log: " + e.getMessage());
    } finally {
        u.closeQuietly(cursor);
        u.closeQuietly(db);
    }
    return list;
}

searchContacts(searchQuery) {
    results = new ArrayList();
    if (!u.isValidString(searchQuery)) return results;
    
    allContacts = loadAllContacts();
    isT9 = searchQuery.matches("[0-9+*#]+");
    lowerQuery = searchQuery.toLowerCase();
    
    p = null;
    if (isT9) {
        try { p = Pattern.compile(getT9Regex(searchQuery)); } catch(Exception e) {}
    }

    for (int i = 0; i < allContacts.size(); i++) {
        contact = (Object[]) allContacts.get(i);
        name = (String) contact[0];
        num = (String) contact[1];
        photo = (String) contact[2];
        cleanNum = (String) contact[3];

        matches = false;
        if (isT9) {
            if (cleanNum.contains(searchQuery)) matches = true;
            else if (p != null && p.matcher(name).matches()) matches = true;
        } else {
            if (name.toLowerCase().contains(lowerQuery)) matches = true;
            else if (num.toLowerCase().contains(lowerQuery)) matches = true;
        }

        if (matches) {
            entry = new Object[8];
            entry[0] = name;
            entry[1] = num;
            entry[2] = photo;
            entry[3] = null;
            entry[4] = "";
            entry[5] = false;
            entry[6] = 0;
            entry[7] = 0;
            results.add(entry);
        }
    }
    return results;
}

createMenuItem(activity, text, iconName, index) {
    itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setPadding(u.dpToPx(16), u.dpToPx(12), u.dpToPx(16), u.dpToPx(12));
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemIcon = new ImageView(activity);
    iconDrawable = uiObj.getDrawable(activity, iconName);
    if (iconDrawable != null) {
        itemIcon.setImageDrawable(iconDrawable);
        itemIcon.setColorFilter(uiObj.getColorInt("primary"));
    }
    iconParams = new LinearLayout.LayoutParams(u.dpToPx(24), u.dpToPx(24));
    iconParams.rightMargin = u.dpToPx(16);
    itemIcon.setLayoutParams(iconParams);
    itemLayout.addView(itemIcon);
    itemText = new TextView(activity);
    itemText.setText(text);
    itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    itemText.setTextColor(uiObj.getColorInt("on-surface"));
    itemLayout.addView(itemText);
    itemLayout.setTag(index);
    itemLayout.setOnTouchListener(new View.OnTouchListener() {
        onTouch(v, event) {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.setBackgroundColor(uiObj.getColorInt("surface-variant"));
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || 
                       event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.setBackgroundColor(Color.TRANSPARENT);
            }
            return false;
        }
    });
    return itemLayout;
}

createDivider(activity) {
    divider = new View(activity);
    dividerParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        u.dpToPx(1)
    );
    divider.setLayoutParams(dividerParams);
    divider.setBackgroundColor(uiObj.getColorInt("outline-variant"));
    return divider;
}

showClearConfirmation(activity, title, message, taskParam) {
    confirmAction = new Runnable() {
        run() {
            db = null;
            try {
                db = d.getWritableDb();
                if ("clear_call_log".equals(taskParam)) {
                    tasker.showToast("Feature not added");
                } else if ("clear_truecaller_log".equals(taskParam)) {
                    db.execSQL("DELETE FROM data");
                    db.execSQL("DELETE FROM number_map");
                    tasker.showToast("Truecaller log cleared");
                }
                uiObj.mainHandler.postDelayed(new Runnable() {
                    run() {
                        refreshListView();
                    }
                }, 500);
            } catch(Exception e) {
                tasker.showToast("Failed to clear: " + e.getMessage());
            } finally {
                u.closeQuietly(db);
            }
        }
    };
    uiObj.showConfirmDialog(activity, title, message, "YES", confirmAction);
}

showStyledMenuDialog(activity) {
    dialogLayout = new LinearLayout(activity);
    dialogLayout.setOrientation(LinearLayout.VERTICAL);
    dialogLayout.setBackgroundColor(uiObj.getColorInt("surface"));
    dialogBg = uiObj.createCardBackground(uiObj.getThemeColor("surface"), 12);
    dialogLayout.setBackground(dialogBg);
    dialogLayout.setPadding(0, u.dpToPx(8), 0, u.dpToPx(8));
    dialogLayout.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ));
    menuItems = new String[]{"User Filters", "Settings", "Refresh", "Clear Call Log", "Clear Truecaller Log"};
    menuIcons = new String[]{"mw_content_filter_list", "mw_action_settings", "mw_navigation_refresh", "mw_action_delete", "mw_action_delete"};
    for (int i = 0; i < menuItems.length; i++) {
        itemLayout = createMenuItem(activity, menuItems[i], menuIcons[i], i);
        dialogLayout.addView(itemLayout);
        if (i < menuItems.length - 1) {
            divider = createDivider(activity);
            dividerParams = divider.getLayoutParams();
            dividerParams.leftMargin = u.dpToPx(16);
            dividerParams.rightMargin = u.dpToPx(16);
            divider.setLayoutParams(dividerParams);
            dialogLayout.addView(divider);
        }
    }
    dialog = new android.app.Dialog(activity);
    dialog.requestWindowFeature(1);
    dialog.setContentView(dialogLayout);
    dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    for (int i = 0; i < dialogLayout.getChildCount(); i++) {
        child = dialogLayout.getChildAt(i);
        if (child instanceof LinearLayout && child.getTag() != null) {
            child.setOnClickListener(new View.OnClickListener() {
                onClick(v) {
                    menuIndex = v.getTag();
                    dialog.dismiss();
                    taskParams = new HashMap();
                    if (menuIndex == 0) {
                        uiObj.showUserFiltersDialog(activity, null, null, null, null);
                    } else if (menuIndex == 1) {
                        onThemeChanged = new Runnable() {
                            run() {
                                d.reloadDb();
                                if (vd != null && vd.d != null) vd.d.reloadDb();
                                
                                uiObj.mainHandler.post(new Runnable() {
                                    run() {
                                        rootContainer.removeAllViews();
                                        listView = createListView(activity);
                                        rootContainer.addView(listView);
                                    }
                                });
                            }
                        };
                        uiObj.showSettingsDialog(activity, onThemeChanged);
                    } else if (menuIndex == 2) {
                        refreshListView();
                        tasker.showToast("Refreshed");
                    } else if (menuIndex == 3) {
                        showClearConfirmation(activity, "Clear Call Log", "Are you sure you want to clear the call log?", "clear_call_log");
                    } else if (menuIndex == 4) {
                        showClearConfirmation(activity, "Clear Truecaller Log", "Are you sure you want to clear the Truecaller log?", "clear_truecaller_log");
                    }
                }
            });
        }
    }
    dialog.show();
}

showContextMenu(activity, entry) {
    dialogLayout = new LinearLayout(activity);
    dialogLayout.setOrientation(LinearLayout.VERTICAL);
    dialogLayout.setBackgroundColor(uiObj.getColorInt("surface"));
    dialogBg = uiObj.createCardBackground(uiObj.getThemeColor("surface"), 12);
    dialogLayout.setBackground(dialogBg);
    dialogLayout.setPadding(0, u.dpToPx(8), 0, u.dpToPx(8));
    dialogLayout.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ));
    
    menuItems = new String[]{"Call", "Edit before call", "Send Message", "Copy Number", "Contact Via", "View Details", "Delete"};
    menuIcons = new String[]{"mw_communication_call", "mw_editor_mode_edit", "mw_communication_textsms", "mw_content_content_copy", "mw_social_person_add", "mw_action_info", "mw_action_delete"};
    
    for (int i = 0; i < menuItems.length; i++) {
        itemLayout = createMenuItem(activity, menuItems[i], menuIcons[i], i);
        dialogLayout.addView(itemLayout);
        if (i < menuItems.length - 1) {
            divider = createDivider(activity);
            dividerParams = divider.getLayoutParams();
            dividerParams.leftMargin = u.dpToPx(16);
            dividerParams.rightMargin = u.dpToPx(16);
            divider.setLayoutParams(dividerParams);
            dialogLayout.addView(divider);
        }
    }
    
    contextMenuDialog = new android.app.Dialog(activity);
    contextMenuDialog.requestWindowFeature(1);
    contextMenuDialog.setContentView(dialogLayout);
    contextMenuDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    
    for (int i = 0; i < dialogLayout.getChildCount(); i++) {
        child = dialogLayout.getChildAt(i);
        if (child instanceof LinearLayout && child.getTag() != null) {
            child.setOnClickListener(new View.OnClickListener() {
                onClick(v) {
                    menuIndex = v.getTag();
                    contextMenuDialog.dismiss();
                    number = entry[1];
                    name = entry[0];
                    
                    if (menuIndex == 0) {
                        u.makeCall(number);
                    } else if (menuIndex == 1) {
                        if (dialpadView != null && numberDisplay != null) {
                            numberDisplay.setText(number);
                            numberDisplay.setSelection(numberDisplay.length());
                            dialpadView.setVisibility(View.VISIBLE);
                            if (fab != null) fab.setVisibility(View.GONE);
                            performLiveSearch(number);
                        } else {
                            tasker.showToast("Dialpad is not available here");
                        }
                    } else if (menuIndex == 2) {
                        u.sendSms(number);
                    } else if (menuIndex == 3) {
                        uiObj.setClip(number);
                    } else if (menuIndex == 4) {
                        uiObj.showContactViaDialog(activity, number);
                    } else if (menuIndex == 5) {
                        openDetailsView(activity, number);
                    } else if (menuIndex == 6) {
                        confirmAction = new Runnable() {
                            run() {
                                try {
                                    if (currentTab.equals("calllog")) {
                                        vd.deleteFromCallLog(number);
                                    } else {
                                        vd.deleteFromDatabase(number);
                                    }
                                    removeCardFromUi(number);
                                } catch(Exception e) {
                                    tasker.showToast("Delete failed: " + e.getMessage());
                                }
                            }
                        };
                        dialogTitle = "Delete " + name + "?";
                        uiObj.showConfirmDialog(activity, dialogTitle, null, "DELETE", confirmAction);
                    }
                }
            });
        }
    }
    contextMenuDialog.show();
}

createDialpadView(activity) {
    dialpadSheet = new LinearLayout(activity);
    dialpadSheet.setOrientation(LinearLayout.VERTICAL);
    dialpadSheet.setVisibility(View.GONE);
    
    sheetBg = new GradientDrawable();
    sheetBg.setColor(uiObj.getColorInt("surface"));
    sheetBg.setCornerRadii(new float[]{u.dpToPx(16), u.dpToPx(16), u.dpToPx(16), u.dpToPx(16), 0, 0, 0, 0});
    dialpadSheet.setBackground(sheetBg);
    dialpadSheet.setElevation(u.dpToPx(16));
    
    sheetParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    dialpadSheet.setLayoutParams(sheetParams);
    dialpadSheet.setPadding(u.dpToPx(8), u.dpToPx(8), u.dpToPx(8), u.dpToPx(8));

    numberDisplay = new EditText(activity);
    numberDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
    numberDisplay.setGravity(Gravity.CENTER);
    numberDisplay.setTextColor(uiObj.getColorInt("on-surface"));
    numberDisplay.setBackgroundColor(Color.TRANSPARENT);
    numberDisplay.setShowSoftInputOnFocus(false);
    numberDisplay.setCursorVisible(true);
    
    displayParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    displayParams.bottomMargin = u.dpToPx(4);
    numberDisplay.setLayoutParams(displayParams);
    
    numberDisplay.setMinHeight(u.dpToPx(40));
    numberDisplay.setPadding(u.dpToPx(8), u.dpToPx(0), u.dpToPx(8), u.dpToPx(4));
    dialpadSheet.addView(numberDisplay);

    String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
    String[] letters = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", ""};
    int k = 0;
    
    for (int r = 0; r < 4; r++) {
        row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rowParams);
        row.setWeightSum(3);

        for (int c = 0; c < 3; c++) {
            btnKey = keys[k];
            btnLetter = letters[k];
            k++;
            
            btn = new LinearLayout(activity);
            btn.setOrientation(LinearLayout.HORIZONTAL);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(0, u.dpToPx(8), 0, u.dpToPx(8));
            
            btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            btn.setLayoutParams(btnParams);
            
            mainText = new TextView(activity);
            mainText.setText(btnKey);
            mainText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            mainText.setTextColor(uiObj.getColorInt("on-surface"));
            mainText.setGravity(Gravity.CENTER_VERTICAL);
            btn.addView(mainText);
            
            subText = new TextView(activity);
            subText.setText(btnLetter);
            subText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            subText.setTextColor(uiObj.getColorInt("on-surface-variant"));
            subText.setGravity(Gravity.CENTER_VERTICAL);
            
            subTextParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            subTextParams.leftMargin = u.dpToPx(6); 
            subText.setLayoutParams(subTextParams);
            
            if (btnLetter.isEmpty()) {
                subText.setText("ABC");
                subText.setVisibility(View.INVISIBLE);
            }
            btn.addView(subText);
            
            btn.setOnTouchListener(new View.OnTouchListener() {
                onTouch(v, event) {
                    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        v.setBackgroundColor(uiObj.getColorInt("surface-variant"));
                    } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || 
                               event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                        v.setBackgroundColor(Color.TRANSPARENT);
                    }
                    return false;
                }
            });
            
      		final String finalKey = btnKey;
            btn.setOnClickListener(new View.OnClickListener() {
                onClick(v) {
                    start = numberDisplay.getSelectionStart();
                    end = numberDisplay.getSelectionEnd();
                    if (start < 0) start = numberDisplay.length();
                    if (end < 0) end = numberDisplay.length();
                    min = Math.min(start, end);
                    max = Math.max(start, end);
                    
                    numberDisplay.getText().replace(min, max, finalKey);
                    performLiveSearch(numberDisplay.getText().toString());
                }
            });
            
            if (btnKey.equals("0")) {
                btn.setOnLongClickListener(new View.OnLongClickListener() {
                    onLongClick(v) {
                        start = numberDisplay.getSelectionStart();
                        end = numberDisplay.getSelectionEnd();
                        if (start < 0) start = numberDisplay.length();
                        if (end < 0) end = numberDisplay.length();
                        min = Math.min(start, end);
                        max = Math.max(start, end);
                        
                        numberDisplay.getText().replace(min, max, "+");
                        performLiveSearch(numberDisplay.getText().toString());
                        return true;
                    }
                    onLongClickUseDefaultHapticFeedback(v) { return true; }
                });
            }
            row.addView(btn);
        }
        dialpadSheet.addView(row);
    }

    actionRow = new LinearLayout(activity);
    actionRow.setOrientation(LinearLayout.HORIZONTAL);
    actionRowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    actionRowParams.topMargin = u.dpToPx(16);
    actionRow.setLayoutParams(actionRowParams);
    actionRow.setGravity(Gravity.CENTER);

    hideContainer = new LinearLayout(activity);
    hideContainer.setGravity(Gravity.CENTER);
    hideContainerParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
    hideContainer.setLayoutParams(hideContainerParams);
    
    hideBtn = new android.widget.ImageButton(activity);
    hideBtnParams = new LinearLayout.LayoutParams(u.dpToPx(48), u.dpToPx(48));
    hideBtn.setLayoutParams(hideBtnParams);
    hideBtn.setBackgroundColor(Color.TRANSPARENT);
    hideBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    hideIcon = uiObj.getDrawable(activity, "mw_navigation_expand_more");
    if (hideIcon != null) {
        hideBtn.setImageDrawable(hideIcon);
        hideBtn.setColorFilter(uiObj.getColorInt("on-surface-variant"));
    }
    
    hideBtn.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            dialpadSheet.setVisibility(View.GONE);
            if (numberDisplay != null) numberDisplay.clearFocus();
            if (mainWrapper != null) mainWrapper.requestFocus();
            if (fab != null) fab.setVisibility(View.VISIBLE);
        }
    });
    hideContainer.addView(hideBtn);
    actionRow.addView(hideContainer);

    callContainer = new LinearLayout(activity);
    callContainer.setGravity(Gravity.CENTER);
    callContainerParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
    callContainer.setLayoutParams(callContainerParams);
    
    callBtn = new android.widget.ImageButton(activity);
    callBtnParams = new LinearLayout.LayoutParams(u.dpToPx(64), u.dpToPx(64));
    callBtn.setLayoutParams(callBtnParams);
    callBtnBg = new GradientDrawable();
    callBtnBg.setColor(Color.parseColor("#4CAF50"));
    callBtnBg.setCornerRadius(u.dpToPx(32));
    callBtn.setBackground(callBtnBg);
    callBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    callIcon = uiObj.getDrawable(activity, "mw_communication_call");
    if (callIcon != null) {
        callBtn.setImageDrawable(callIcon);
        callBtn.setColorFilter(Color.WHITE);
    }
    
    callBtn.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            numberToCall = numberDisplay.getText().toString().trim();
            if (!numberToCall.isEmpty()) {
                u.makeCall(numberToCall);
                dialpadSheet.setVisibility(View.GONE);
                if (fab != null) fab.setVisibility(View.VISIBLE);
                numberDisplay.setText("");
                performLiveSearch(""); 
            }
        }
    });
    callContainer.addView(callBtn);
    actionRow.addView(callContainer);

    backspaceContainer = new LinearLayout(activity);
    backspaceContainer.setGravity(Gravity.CENTER);
    backspaceContainerParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
    backspaceContainer.setLayoutParams(backspaceContainerParams);
    
    backspaceBtn = new android.widget.ImageButton(activity);
    backspaceParams = new LinearLayout.LayoutParams(u.dpToPx(48), u.dpToPx(48));
    backspaceBtn.setLayoutParams(backspaceParams);
    backspaceBtn.setBackgroundColor(Color.TRANSPARENT);
    backspaceBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    bsIcon = uiObj.getDrawable(activity, "mw_content_backspace");
    if (bsIcon != null) {
        backspaceBtn.setImageDrawable(bsIcon);
        backspaceBtn.setColorFilter(uiObj.getColorInt("on-surface-variant"));
    }
    
    backspaceBtn.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            start = numberDisplay.getSelectionStart();
            end = numberDisplay.getSelectionEnd();
            if (start < 0) start = numberDisplay.length();
            if (end < 0) end = numberDisplay.length();
            min = Math.min(start, end);
            max = Math.max(start, end);
            
            if (min != max) {
                numberDisplay.getText().delete(min, max);
                performLiveSearch(numberDisplay.getText().toString());
            } else if (min > 0) {
                numberDisplay.getText().delete(min - 1, min);
                performLiveSearch(numberDisplay.getText().toString());
            }
        }
    });
    
    backspaceBtn.setOnLongClickListener(new View.OnLongClickListener() {
        onLongClick(v) {
            numberDisplay.setText("");
            performLiveSearch("");
            return true;
        }
        onLongClickUseDefaultHapticFeedback(v) { return true; }
    });
    backspaceContainer.addView(backspaceBtn);
    actionRow.addView(backspaceContainer);

    dialpadSheet.addView(actionRow);
    return dialpadSheet;
}

refreshListView() {
    cachedContactPhotos = null;
    cachedContactsList = null;
    if (currentTab.equals("calllog")) {
        callLogTab.performClick();
    } else {
        truecallerTab.performClick();
    }
}

void removeCardFromUi(String number) {
    uiObj.mainHandler.post(new Runnable() {
        run() {
            if (isSearching) dataList = searchResults;
            else if (currentTab.equals("calllog")) dataList = callLogData;
            else dataList = truecallerLogData;

            for (int i = 0; i < dataList.size(); i++) {
                entry = dataList.get(i);
                if (entry[1].equals(number)) {
                    dataList.remove(i);
                    break;
                }
            }
            if (mainAdapter != null) mainAdapter.notifyDataSetChanged();
        }
    });
}

void openDetailsView(Activity activity, String number) {
  vd.isCallLog = currentTab.equals("calllog");
  vd.onDeleted = new Runnable() { run() { removeCardFromUi(number); } };
  vd.onClose = new Runnable() { run() { refreshListView(); } };
  
  detailView = vd.createDetailView(activity, number);
  rootContainer.addView(detailView);
}

void loadTabData(String tab) {
  searchBox.setText("");
  isSearching = false;
  finalTab = tab;
  
  executorService.submit(new Runnable() {
    run() {
      freshData = finalTab.equals("calllog") ? loadCallLogData(null) : loadTruecallerLogData(null);
      
      uiObj.mainHandler.post(new Runnable() {
        run() {
          if (finalTab.equals("calllog")) {
            callLogData = (ArrayList) freshData;
          } else {
            truecallerLogData = (ArrayList) freshData;
          }
          
          if (mainAdapter != null) {
            mainAdapter.notifyDataSetChanged();
          }
        }
      });
    }
  });
}

Object createListAdapter(Activity activity) {
    return tasker.implementClass(BaseAdapter.class, new com.joaomgcd.taskerm.action.java.ClassImplementation() {
        run(Callable superCaller, String methodName, Object[] args) {
            
            ArrayList dataList;
            if (isSearching) {
                dataList = searchResults;
            } else if (currentTab.equals("calllog")) {
                dataList = callLogData;
            } else {
                dataList = truecallerLogData;
            }

            if (methodName.equals("getCount")) {
                return dataList.size();
            }

            if (methodName.equals("getItem")) {
                int pos = ((Integer)args[0]).intValue();
                return dataList.get(pos);
            }

            if (methodName.equals("getItemId")) {
                int pos = ((Integer)args[0]).intValue();
                return (long) pos;
            }

            if (methodName.equals("getView")) {
                int position = ((Integer)args[0]).intValue();
                View convertView = (View)args[1];
                ViewGroup parent = (ViewGroup)args[2];
                
                row = null;
                if (convertView == null) {
                    row = createListRowView(parent);
                } else {
                    row = convertView;
                }
                
                Object[] entry = dataList.get(position);
                bindListRowView(row, entry, activity);
                
                return row;
            }

            return superCaller.call();
        }
    });
}

createListRowView(parent) {
    card = new LinearLayout(parent.getContext());
    card.setOrientation(LinearLayout.HORIZONTAL);
    card.setPadding(u.dpToPx(12), u.dpToPx(12), u.dpToPx(12), u.dpToPx(12));
    card.setGravity(Gravity.CENTER_VERTICAL);
    cardBg = uiObj.createCardBackground(uiObj.getThemeColor("surface"), 12);
    card.setBackground(cardBg);
    
    avatarView = new ImageView(parent.getContext());
    avatarSize = u.dpToPx(48);
    avatarParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
    avatarParams.rightMargin = u.dpToPx(12);
    avatarView.setLayoutParams(avatarParams);
    avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    card.addView(avatarView);

    infoLayout = new LinearLayout(parent.getContext());
    infoLayout.setOrientation(LinearLayout.VERTICAL);
    infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
    
    nameText = new TextView(parent.getContext());
    nameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    nameText.setTypeface(null, Typeface.BOLD);
    nameText.setTextColor(uiObj.getColorInt("on-surface"));
    infoLayout.addView(nameText);

    numberText = new TextView(parent.getContext());
    numberText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    numberText.setTextColor(uiObj.getColorInt("on-surface-variant"));
    infoLayout.addView(numberText);

    timeText = new TextView(parent.getContext());
    timeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    timeText.setTextColor(uiObj.getColorInt("on-surface-variant"));
    infoLayout.addView(timeText);

    noteText = new TextView(parent.getContext());
    noteText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    noteText.setTypeface(null, Typeface.BOLD);
    noteText.setTextColor(uiObj.getColorInt("on-secondary"));
    noteText.setPadding(u.dpToPx(8), u.dpToPx(4), u.dpToPx(8), u.dpToPx(4));
    noteBg = new GradientDrawable();
    noteBg.setColor(uiObj.getColorInt("secondary"));
    noteBg.setCornerRadius(u.dpToPx(8));
    noteParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    noteParams.topMargin = u.dpToPx(4);
    noteText.setLayoutParams(noteParams);
    noteText.setVisibility(View.GONE);
    infoLayout.addView(noteText);

    spamText = new TextView(parent.getContext());
    spamText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    spamText.setTextColor(Color.WHITE);
    spamText.setPadding(u.dpToPx(6), u.dpToPx(2), u.dpToPx(6), u.dpToPx(2));
    spamBg = new GradientDrawable();
    spamBg.setColor(Color.parseColor("#D32F2F"));
    spamBg.setCornerRadius(u.dpToPx(4));
    spamText.setBackground(spamBg);
    spamParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    spamParams.topMargin = u.dpToPx(4);
    spamText.setLayoutParams(spamParams);
    spamText.setVisibility(View.GONE);
    infoLayout.addView(spamText);

    card.addView(infoLayout);
    
    holder = new Object[6];
    holder[0] = avatarView;
    holder[1] = nameText;
    holder[2] = numberText;
    holder[3] = timeText;
    holder[4] = noteText;
    holder[5] = spamText;
    
    card.setTag(holder);
    return card;
}

bindListRowView(rowView, entry, activity) {
    name = entry[0];
    number = entry[1];
    image = entry[2];
    note = entry[3];
    time = entry[4];
    isVerified = entry[5];
    spamScore = entry[6];
    callType = entry[7];

    holder = rowView.getTag();
    avatarView = holder[0];
    nameText = holder[1];
    numberText = holder[2];
    timeText = holder[3];
    noteText = holder[4];
    spamText = holder[5];

    uiObj.cancelImageLoad(avatarView);

    nameText.setText(name);
    numberText.setText(number);
    timeText.setText(time);
    
    iconName = null;
    tintColor = uiObj.getColorInt("on-surface-variant");
    if (callType == 1) { iconName = "mw_communication_call_received"; }
    else if (callType == 2) { iconName = "mw_communication_call_made"; }
    else if (callType == 3) { iconName = "mw_communication_call_missed"; tintColor = Color.parseColor("#D32F2F"); }
    else if (callType == 5) { iconName = "mw_communication_call_missed_outgoing"; }
    else if (callType == 6) { iconName = "mw_content_block"; tintColor = Color.parseColor("#D32F2F"); }

    if (iconName != null) {
        drawable = uiObj.getDrawable(activity, iconName);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(tintColor);
            drawable.setBounds(0, 0, u.dpToPx(14), u.dpToPx(14));
            timeText.setCompoundDrawablePadding(u.dpToPx(4));
            timeText.setCompoundDrawables(drawable, null, null, null);
            timeText.setGravity(Gravity.CENTER_VERTICAL);
        } else {
            timeText.setCompoundDrawables(null, null, null, null);
        }
    } else {
        timeText.setCompoundDrawables(null, null, null, null);
    }
    
    avatarView.setImageBitmap(uiObj.createInitialBitmap(name, 48));
    uiObj.loadImageAsync(image, avatarView, name, 48);

    if (u.isValidString(note)) {
        noteText.setText(note);
        noteText.setVisibility(View.VISIBLE);
    } else {
        noteText.setVisibility(View.GONE);
    }

    if (spamScore > 0) {
        spamText.setText("Spam: " + spamScore);
        spamText.setVisibility(View.VISIBLE);
    } else {
        spamText.setVisibility(View.GONE);
    }

    final String finalNumber = number;
    final Object[] finalEntry = entry;
    longPressTriggered = new boolean[]{false};
    
    rowView.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            if (!longPressTriggered[0]) {
                openDetailsView(activity, finalNumber);
            }
            longPressTriggered[0] = false;
        }
    });

    uiObj.setupLongPressHandler(rowView, new Runnable() {
        run() {
            longPressTriggered[0] = true;
            showContextMenu(activity, finalEntry);
        }
    });
}

createListView(activity) {
    mainLayout = new LinearLayout(activity);
    mainLayout.setOrientation(LinearLayout.VERTICAL);
    mainLayout.setBackgroundColor(uiObj.getColorInt("surface-variant"));
    
    searchLayout = new LinearLayout(activity);
    searchLayout.setOrientation(LinearLayout.HORIZONTAL);
    searchLayout.setPadding(u.dpToPx(16), u.dpToPx(12), u.dpToPx(16), u.dpToPx(12));
    searchLayout.setBackgroundColor(uiObj.getColorInt("surface"));
    searchLayout.setGravity(Gravity.CENTER_VERTICAL);
    
    searchBox = new EditText(activity);
    searchBox.setHint("Search logs...");
    searchBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    searchBox.setTextColor(uiObj.getColorInt("on-surface"));
    searchBox.setHintTextColor(uiObj.getColorInt("on-surface-variant"));
    searchBox.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    searchBox.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
    searchBox.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
    searchBoxBg = uiObj.createCardBackground(uiObj.getThemeColor("surface-variant"), 24);
    searchBox.setBackground(searchBoxBg);
    searchBox.setPadding(u.dpToPx(12), u.dpToPx(8), u.dpToPx(12), u.dpToPx(8));
    searchLayout.addView(searchBox);
    
    searchButton = new android.widget.ImageButton(activity);
    searchButtonParams = new LinearLayout.LayoutParams(u.dpToPx(40), u.dpToPx(40));
    searchButtonParams.leftMargin = u.dpToPx(8);
    searchButton.setLayoutParams(searchButtonParams);
    searchButtonBg = new GradientDrawable();
    searchButtonBg.setColor(uiObj.getColorInt("primary"));
    searchButtonBg.setCornerRadius(u.dpToPx(20));
    searchButton.setBackground(searchButtonBg);
    searchButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    searchButton.setPadding(u.dpToPx(8), u.dpToPx(8), u.dpToPx(8), u.dpToPx(8));
    searchIcon = uiObj.getDrawable(activity, "mw_action_search");
    if (searchIcon != null) {
        searchButton.setImageDrawable(searchIcon);
        searchButton.setColorFilter(uiObj.getColorInt("on-primary"));
    }
    
    menuButton = new android.widget.ImageButton(activity);
    menuButtonParams = new LinearLayout.LayoutParams(u.dpToPx(40), u.dpToPx(40));
    menuButtonParams.leftMargin = u.dpToPx(8);
    menuButton.setLayoutParams(menuButtonParams);
    menuButtonBg = new GradientDrawable();
    menuButtonBg.setColor(uiObj.getColorInt("surface"));
    menuButtonBg.setCornerRadius(u.dpToPx(20));
    menuButton.setBackground(menuButtonBg);
    menuButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    menuButton.setPadding(u.dpToPx(8), u.dpToPx(8), u.dpToPx(8), u.dpToPx(8));
    menuIcon = uiObj.getDrawable(activity, "mw_navigation_more_vert");
    if (menuIcon != null) {
        menuButton.setImageDrawable(menuIcon);
        menuButton.setColorFilter(uiObj.getColorInt("on-surface-variant"));
    }
    menuButton.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            showStyledMenuDialog(activity);
        }
    });
    searchLayout.addView(searchButton);
    
    searchBox.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
        onEditorAction(v, actionId, event) {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchButton.performClick();
                return true;
            }
            return false;
        }
    });
    
    searchButton.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            imm = context.getSystemService(context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            if (searchBox != null) searchBox.clearFocus();
            if (mainWrapper != null) mainWrapper.requestFocus();
            
            searchQuery = searchBox.getText().toString().trim();
            if (searchQuery.isEmpty()) {
                tasker.showToast("Please enter a search query");
                return;
            }
            uiObj.mainHandler.removeCallbacksAndMessages(null);
            cancelPendingLoads();
            isPhoneNumber = Patterns.PHONE.matcher(searchQuery).matches();
            if (isPhoneNumber) {
                details = d.loadNumberDetails(searchQuery);
                if (d.needsUpdate(details)) {
                    tasker.showToast("Fetching fresh data...");
          			final String finalSearchQuery = searchQuery;
                    executorService.submit(new Runnable() {
                        run() {
                            try {
                                d.fetchFromTruecaller(finalSearchQuery);
                            } catch (Exception e) {
                                tasker.showToast(e.getMessage());
                            }
                            uiObj.mainHandler.post(new Runnable() {
                                run() {
                                    performSearch(finalSearchQuery);
                                }
                            });
                        }
                    });
                    return;
                }
            }
            performSearch(searchQuery);
        }
        performSearch(query) {
      	final String finalQuery = query;
            executorService.submit(new Runnable() {
                run() {
                    callLogResults = loadCallLogData(finalQuery);
                    truecallerResults = loadTruecallerLogData(finalQuery);
                    contactsResults = searchContacts(finalQuery);
                    allResults = new ArrayList();
                    allResults.addAll(callLogResults);
                    allResults.addAll(truecallerResults);
                    allResults.addAll(contactsResults);
                    
          			final ArrayList finalResults = allResults;
                    uiObj.mainHandler.post(new Runnable() {
                        run() {
                            searchResults = finalResults;
                            isSearching = true;
                            if (mainAdapter != null) mainAdapter.notifyDataSetChanged();
                            mainListView.post(new Runnable() {
                                run() {
                                    mainListView.setSelection(0);
                                }
                            });
                            tasker.showToast("Found " + searchResults.size() + " results");
                        }
                    });
                }
            });
        }
    });
    
    logsTabContainer = new LinearLayout(activity);
    logsTabContainer.setOrientation(LinearLayout.VERTICAL);
    logsTabContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
    logsTabHeaderLayout = new LinearLayout(activity);
    logsTabHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
    logsTabHeaderLayout.setBackgroundColor(uiObj.getColorInt("surface"));
    logsTabHeaderLayout.setElevation(u.dpToPx(4));
    logsTabHeaderLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    logsTabsWithIndicator = new LinearLayout(activity);
    logsTabsWithIndicator.setOrientation(LinearLayout.VERTICAL);
    logsTabsWithIndicator.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
    logsTabButtonLayout = new LinearLayout(activity);
    logsTabButtonLayout.setOrientation(LinearLayout.HORIZONTAL);
    logsTabButtonLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    
    callLogTab = new TextView(activity);
    callLogTab.setText("Call Log");
    callLogTab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    callLogTab.setGravity(Gravity.CENTER);
    callLogTab.setPadding(u.dpToPx(16), u.dpToPx(12), u.dpToPx(16), u.dpToPx(12));
    callLogTab.setTextColor(uiObj.getColorInt("primary"));
    callLogTab.setTypeface(null, Typeface.BOLD);
    callLogTab.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
    
    truecallerTab = new TextView(activity);
    truecallerTab.setText("Truecaller Log");
    truecallerTab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    truecallerTab.setGravity(Gravity.CENTER);
    truecallerTab.setPadding(u.dpToPx(16), u.dpToPx(12), u.dpToPx(16), u.dpToPx(12));
    truecallerTab.setTextColor(uiObj.getColorInt("on-surface-variant"));
    truecallerTab.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
    
    logsTabButtonLayout.addView(callLogTab);
    logsTabButtonLayout.addView(truecallerTab);
    logsTabIndicator = new View(activity);
    logsTabIndicatorParams = new LinearLayout.LayoutParams(0, u.dpToPx(3));
    logsTabIndicatorParams.weight = 1.0f;
    logsTabIndicator.setLayoutParams(logsTabIndicatorParams);
    logsTabIndicatorBg = new GradientDrawable();
    logsTabIndicatorBg.setColor(uiObj.getColorInt("primary"));
    logsTabIndicatorBg.setCornerRadius(u.dpToPx(3));
    logsTabIndicator.setBackground(logsTabIndicatorBg);
    logsIndicatorContainer = new LinearLayout(activity);
    logsIndicatorContainer.setOrientation(LinearLayout.HORIZONTAL);
    logsIndicatorContainerParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 
        u.dpToPx(3)
    );
    logsIndicatorContainer.setLayoutParams(logsIndicatorContainerParams);
    logsIndicatorContainer.setPadding(u.dpToPx(16), 0, u.dpToPx(16), 0);
    logsSpacer = new View(activity);
    logsSpacerParams = new LinearLayout.LayoutParams(0, u.dpToPx(3));
    logsSpacerParams.weight = 1.0f;
    logsSpacer.setLayoutParams(logsSpacerParams);
    logsIndicatorContainer.addView(logsTabIndicator);
    logsIndicatorContainer.addView(logsSpacer);
    logsTabsWithIndicator.addView(logsTabButtonLayout);
    logsTabsWithIndicator.addView(logsIndicatorContainer);
    logsTabHeaderLayout.addView(logsTabsWithIndicator);
    menuButtonParams2 = new LinearLayout.LayoutParams(u.dpToPx(48), u.dpToPx(48));
    menuButtonParams2.rightMargin = u.dpToPx(8);
    menuButtonParams2.gravity = Gravity.CENTER_VERTICAL;
    menuButton.setLayoutParams(menuButtonParams2);
    logsTabHeaderLayout.addView(menuButton);
    
    contentFrame = new FrameLayout(activity);
    contentFrame.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
    
    mainListView = new android.widget.ListView(activity);
    mainListView.setDivider(null); 
    mainListView.setDividerHeight(u.dpToPx(8));
    mainListView.setPadding(u.dpToPx(8), u.dpToPx(8), u.dpToPx(8), u.dpToPx(8));
    mainListView.setClipToPadding(false);
    mainListView.setScrollBarStyle(android.view.View.SCROLLBARS_OUTSIDE_OVERLAY);
    
    mainListView.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
        onScrollStateChanged(view, scrollState) {
            if (scrollState == android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                if (dialpadView != null && dialpadView.getVisibility() == View.VISIBLE) {
                    dialpadView.setVisibility(View.GONE);
                    if (numberDisplay != null) numberDisplay.clearFocus();
                    if (fab != null) fab.setVisibility(View.VISIBLE);
                    if (mainWrapper != null) mainWrapper.requestFocus();
                }
                
                imm = context.getSystemService(context.INPUT_METHOD_SERVICE);
                if (imm != null && searchBox != null) {
                    imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
                    searchBox.clearFocus();
                }
            }
        }
        onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount) {}
    });
    
    mainAdapter = createListAdapter(activity);
    mainListView.setAdapter(mainAdapter);
    
    contentFrame.addView(mainListView);
    
    logsTabContainer.addView(logsTabHeaderLayout);
    logsTabContainer.addView(searchLayout);
    logsTabContainer.addView(contentFrame);
    mainLayout.addView(logsTabContainer);

    dialpadView = createDialpadView(activity);
    mainLayout.addView(dialpadView);

    callLogTab.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            currentTab = "calllog";
            callLogTab.setTextColor(uiObj.getColorInt("primary"));
            callLogTab.setTypeface(null, Typeface.BOLD);
            truecallerTab.setTextColor(uiObj.getColorInt("on-surface-variant"));
            truecallerTab.setTypeface(null, Typeface.NORMAL);
            
            logsIndicatorContainer.removeAllViews();
            if (logsTabIndicator.getParent() != null) ((ViewGroup)logsTabIndicator.getParent()).removeView(logsTabIndicator);
            if (logsSpacer.getParent() != null) ((ViewGroup)logsSpacer.getParent()).removeView(logsSpacer);
            
            logsIndicatorContainer.addView(logsTabIndicator);
            logsIndicatorContainer.addView(logsSpacer);
            mainListView.setSelection(0);
            loadTabData("calllog");
        }
    });
    
    truecallerTab.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            currentTab = "truecallerlog";
            callLogTab.setTextColor(uiObj.getColorInt("on-surface-variant"));
            callLogTab.setTypeface(null, Typeface.NORMAL);
            truecallerTab.setTextColor(uiObj.getColorInt("primary"));
            truecallerTab.setTypeface(null, Typeface.BOLD);
            
            logsIndicatorContainer.removeAllViews();
            if (logsSpacer.getParent() != null) ((ViewGroup)logsSpacer.getParent()).removeView(logsSpacer);
            if (logsTabIndicator.getParent() != null) ((ViewGroup)logsTabIndicator.getParent()).removeView(logsTabIndicator);
            
            logsIndicatorContainer.addView(logsSpacer);
            logsIndicatorContainer.addView(logsTabIndicator);
            mainListView.setSelection(0);
            loadTabData("truecallerlog");
        }
    });

    mainWrapper = new FrameLayout(activity);
    mainWrapper.addView(mainLayout);

    fab = new android.widget.ImageButton(activity);
    fabParams = new FrameLayout.LayoutParams(u.dpToPx(56), u.dpToPx(56));
    fabParams.gravity = Gravity.BOTTOM | Gravity.END;
    fabParams.bottomMargin = u.dpToPx(24);
    fabParams.rightMargin = u.dpToPx(24);
    fab.setLayoutParams(fabParams);
    
    fabBg = new GradientDrawable();
    fabBg.setColor(uiObj.getColorInt("primary-container"));
    fabBg.setCornerRadius(u.dpToPx(16));
    fab.setBackground(fabBg);
    fab.setElevation(u.dpToPx(6));
    
    dialpadIcon = uiObj.getDrawable(activity, "mw_communication_dialpad");
    if (dialpadIcon != null) {
        fab.setImageDrawable(dialpadIcon);
        fab.setColorFilter(uiObj.getColorInt("on-primary-container"));
    }
    
    fab.setOnClickListener(new View.OnClickListener() {
        onClick(v) {
            if (dialpadView.getVisibility() == View.GONE) {
                dialpadView.setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
            } else {
                dialpadView.setVisibility(View.GONE);
            }
        }
    });

    mainWrapper.addView(fab);
    mainWrapper.setFocusableInTouchMode(true);
    mainWrapper.requestFocus();
    
    backKeyListener = new View.OnKeyListener() {
        onKey(v, keyCode, event) {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                if (dialpadView != null && dialpadView.getVisibility() == View.VISIBLE) {
                    dialpadView.setVisibility(View.GONE);
                    if (numberDisplay != null) numberDisplay.clearFocus();
                    if (fab != null) fab.setVisibility(View.VISIBLE);
                    if (mainWrapper != null) mainWrapper.requestFocus();
                    return true;
                } else if (isSearching || (searchBox != null && !searchBox.getText().toString().trim().isEmpty()) || (numberDisplay != null && !numberDisplay.getText().toString().isEmpty())) {
                    if (searchBox != null) {
                        searchBox.setText("");
                        searchBox.clearFocus();
                    }
                    if (numberDisplay != null) numberDisplay.setText("");
                    performLiveSearch(""); 
                    if (mainWrapper != null) mainWrapper.requestFocus();
                    return true;
                } else {
                    cleanup();
                    if (currentAppTask != null) {
                        currentAppTask.finishAndRemoveTask();
                    } else {
                        activity.finish();
                    }
                    return true;
                }
            }
            return false;
        }
    };
    
    mainWrapper.setOnKeyListener(backKeyListener);
    if (searchBox != null) searchBox.setOnKeyListener(backKeyListener);
    if (mainListView != null) mainListView.setOnKeyListener(backKeyListener);
    if (dialpadView != null) dialpadView.setOnKeyListener(backKeyListener);
    if (numberDisplay != null) numberDisplay.setOnKeyListener(backKeyListener);
    
    callLogTab.performClick();
    return mainWrapper;
}

activityConsumer = new Consumer() {
    accept(activityObj) {
        activity = activityObj;
        activity.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        
        rootContainer = new FrameLayout(activity);
        
        rootContainer.setOnApplyWindowInsetsListener(new android.view.View.OnApplyWindowInsetsListener() {
            onApplyWindowInsets(v, insets) {
                v.setPadding(
                    insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom()
                );
                return insets;
            }
        });
        
        vd.rootContainer = rootContainer;
        
        listView = createListView(activity);
        rootContainer.addView(listView);
        
        activity.setContentView(rootContainer);
        rootContainer.requestApplyInsets();
        
        currentAppTask = uiObj.setupRecents(activity, taskDescription);
        
        activity.getWindow().getDecorView().addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener() {
            onViewAttachedToWindow(v) {}
            onViewDetachedFromWindow(v) {
                u.logToFile("closing truecaller list view");
                cleanup();
            }
        });
    }
};

tasker.doWithActivity(activityConsumer);