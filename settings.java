scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.util.function.Consumer;

u = util();
uiObj = ui();


/* Cleanup existing Settings tasks to avoid stacking */
am = context.getSystemService(Context.ACTIVITY_SERVICE);
appTasks = am.getAppTasks();
for (int i = 0; i < appTasks.size(); i++) {
    task = appTasks.get(i);
    taskInfo = task.getTaskInfo();
    if (taskInfo != null && taskInfo.taskDescription != null) {
        label = taskInfo.taskDescription.getLabel();
        if ("Truecaller-Settings".equals(label)) {
            task.finishAndRemoveTask();
        }
    }
}

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
        activityTaskId = activity.getTaskId();
        for (int i = 0; i < appTasks.size(); i++) {
            task = appTasks.get(i);
            if (task.getTaskInfo().taskId == activityTaskId) {
                task.setExcludeFromRecents(false);
                break;
            }
        }
        activity.setTaskDescription(new ActivityManager.TaskDescription("Truecaller-Settings"));
        
        /* Define callback for when settings completely closes */
        uiObj.onSettingsClosed = new Runnable() {
            run() {
                uiObj.cleanup();
                activity.finishAndRemoveTask();
            }
        };

        /* Show the settings dialog */
        dialog = uiObj.showSettingsDialog(activity, null);
        
        /* Failsafe if the dialog never successfully inflates */
        if (dialog == null) {
            uiObj.cleanup();
            activity.finishAndRemoveTask();
        }
    }
};

tasker.doWithActivity(activityConsumer);