scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

u = util();
d = data();
uiObj = ui();
vd = viewDetail(uiObj);

par1 = tasker.getVariable("par1");
if (!u.isValidString(par1) || !Patterns.PHONE.matcher(par1).matches()) {
    tasker.showToast("Truecaller - Invalid number");
    return;
}

detailNumber = par1;
vd.isStandalone = true;

currentAppTask = null;
rootContainer = null;

cleanup() {
    u.logToFile("Cleaning up standalone detail resources...");
    uiObj.cleanup();
}

am = context.getSystemService(Context.ACTIVITY_SERVICE);
appTasks = am.getAppTasks();
for (int i = 0; i < appTasks.size(); i++) {
    task = appTasks.get(i);
    taskInfo = task.getTaskInfo();
    if (taskInfo != null && taskInfo.taskDescription != null) {
        label = taskInfo.taskDescription.getLabel();
        if ("Truecaller-Detail".equals(label)) {
            task.finishAndRemoveTask();
        }
    }
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
        
        detailView = vd.createDetailView(activity, detailNumber);
        rootContainer.addView(detailView);
        
        activity.setContentView(rootContainer);
        rootContainer.requestApplyInsets();
        
        am = activity.getSystemService(Context.ACTIVITY_SERVICE);
        appTasks = am.getAppTasks();
        activityTaskId = activity.getTaskId();
        for (int i = 0; i < appTasks.size(); i++) {
            task = appTasks.get(i);
            if (task.getTaskInfo().taskId == activityTaskId) {
                task.setExcludeFromRecents(false);
                vd.currentAppTask = task;
                break;
            }
        }

        activity.setTaskDescription(new ActivityManager.TaskDescription("Truecaller-Detail"));
        
        activity.getWindow().getDecorView().addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener() {
            onViewAttachedToWindow(v) {}
            onViewDetachedFromWindow(v) {
                cleanup();
            }
        });
    }
};

tasker.doWithActivity(activityConsumer);