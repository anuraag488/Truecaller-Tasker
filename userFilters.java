scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Patterns;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.util.function.Consumer;

u = util();
d = data();
uiObj = ui();

par1 = tasker.getVariable("par1");
par2 = tasker.getVariable("par2");

filterNumber = "";
if (u.isValidString(par1) && Patterns.PHONE.matcher(par1).matches()) {
    filterNumber = par1;
}

filterType = null;
if (u.isValidString(par2)) {
    filterType = par2;
}

/* Cleanup existing User Filter tasks to avoid stacking */
uiObj.handleExistingTask("Truecaller-Filters", true);

activityConsumer = new Consumer() {
    accept(activityObj) {
        activity = (Activity) activityObj;
        
        activity.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        
        /* Set a transparent frame as the main activity view */
        rootContainer = new FrameLayout(activity);
        rootContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        activity.setContentView(rootContainer);
        
        /* Set Task description for recents menu */
        uiObj.setupRecents(activity, "Truecaller-Filters");
        
        /* Prepare parameters */
        details = null;
        name = "";
        if (!filterNumber.isEmpty()) {
            details = d.loadNumberDetails(filterNumber);
            if (details != null && details[0] != null) {
                name = (String) details[0];
            } else {
                name = filterNumber;
            }
        }
        
        /* Show the dialog */
        dialog = uiObj.showUserFiltersDialog(activity, filterType, filterNumber, name, details);
        
        /* Close the transparent activity completely when the dialog is closed */
        if (dialog != null) {
            dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                onDismiss(android.content.DialogInterface dAlert) {
                    uiObj.cleanup();
                    activity.finishAndRemoveTask();
                }
            });
        } else {
            uiObj.cleanup();
            activity.finishAndRemoveTask();
        }
    }
};

tasker.doWithActivity(activityConsumer);