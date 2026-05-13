scriptPath = getSourceFileInfo();
scriptParentDir = new File(scriptPath).getParentFile();
BASE_DIR = scriptParentDir.getAbsolutePath();
BASE_BSH = BASE_DIR + "/bsh/";
addClassPath(BASE_BSH);
importCommands(BASE_BSH);

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.joaomgcd.taskerm.action.java.JavaCodeException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;

u = util();
d = data();
uiObj = ui();

/* Prevent double-start and clean up previous instance */
tcRemovalRunnable = tasker.getJavaVariable("tcRemovalRunnable");
if (tcRemovalRunnable != null && tcRemovalRunnable != void) {
    try {
        tcRemovalRunnable.run();
    } catch(Exception e) {
        u.logToFile("Error cleaning up previous instance: " + e.getMessage());
    }
}

number = null;
par1 = tasker.getVariable("par1");
if (u.isValidString(par1) && Patterns.PHONE.matcher(par1).matches()) {
  number = par1;
}

if (number == null) {
  throw new JavaCodeException("par1 is required");
}

dbPath = tasker.getVariable("TC_dbpath");
if (!u.isValidString(dbPath)) {
  throw new JavaCodeException("TC_dbpath variable is not set");
}

warningDataCsv = tasker.getVariable("warning_data");
warningMap = d.getWarningMap(warningDataCsv);

overlayView = null;
overlayParams = null;
windowManager = null;

View createOverlayView(Context ctx, WindowManager.LayoutParams layoutParams) {
  wrapper = new FrameLayout(ctx);
  
  card = new LinearLayout(ctx);
  card.setOrientation(LinearLayout.VERTICAL);
  card.setPadding(u.dpToPx(12), u.dpToPx(12), u.dpToPx(12), u.dpToPx(12));
  
  cardBg = uiObj.createCardBackground(uiObj.getThemeColor("surface"), 12);
  card.setBackground(cardBg);
  card.setMinimumHeight(u.dpToPx(140));
  
  details = d.loadNumberDetails(number);
  
  name = number;
  jsonString = null;
  spamScore = 0;
  isVerified = false;
  image = null;
  note = null;
  dataObj = null;
  
  if (details != null) {
    name = (String) details[0];
    jsonString = (String) details[1];
    spamScore = ((Integer) details[2]).intValue();
    isVerified = ((Boolean) details[3]).booleanValue();
    image = (String) details[4];
    note = (String) details[5];
    
    if (jsonString != null && !jsonString.equals("<null>")) {
      try {
        json = new JSONObject(jsonString);
        dataArray = json.getJSONArray("data");
        if (dataArray.length() > 0) {
          dataObj = dataArray.getJSONObject(0);
        }
      } catch(Exception e) {
        u.logToFile("Error parsing JSON: " + e.getMessage());
      }
    }
  }
  
  if (!isVerified && spamScore > 0) {
    cardBg = uiObj.createCardBackground("#D32F2F", 12);
    card.setBackground(cardBg);
  }
  
  firstRow = new LinearLayout(ctx);
  firstRow.setOrientation(LinearLayout.HORIZONTAL);
  firstRow.setGravity(Gravity.TOP);
  firstRowParams = new LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT,
    LinearLayout.LayoutParams.WRAP_CONTENT
  );
  firstRow.setLayoutParams(firstRowParams);
  firstRow.setPadding(0, 0, u.dpToPx(36), 0);
  
  avatarView = new ImageView(ctx);
  avatarSize = u.dpToPx(64);
  avatarParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
  avatarParams.rightMargin = u.dpToPx(12);
  avatarView.setLayoutParams(avatarParams);
  avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
  
  avatarBg = new GradientDrawable();
  avatarBg.setShape(GradientDrawable.OVAL);
  avatarBg.setColor(Color.parseColor(uiObj.getThemeColor("primary")));
  avatarView.setBackground(avatarBg);
  
  uiObj.loadImageAsync(image, avatarView, name, 64);
  firstRow.addView(avatarView);
  
  firstRowInfo = new LinearLayout(ctx);
  firstRowInfo.setOrientation(LinearLayout.VERTICAL);
  firstRowInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
  
  nameRow = new LinearLayout(ctx);
  nameRow.setOrientation(LinearLayout.HORIZONTAL);
  nameRow.setGravity(Gravity.CENTER_VERTICAL);
  
  nameText = new TextView(ctx);
  nameText.setText(name);
  nameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
  nameText.setTypeface(null, Typeface.BOLD);
  nameText.setTextColor(Color.parseColor(!isVerified && spamScore > 0 ? "#FFFFFF" : uiObj.getThemeColor("on-surface")));
  nameText.setSingleLine(true);
  nameText.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
  nameText.setMarqueeRepeatLimit(-1);
  nameText.setSelected(true);
  nameRow.addView(nameText);
  
  if (isVerified) {
    verifiedIcon = new ImageView(ctx);
    verifiedIconSize = u.dpToPx(16);
    verifiedIconParams = new LinearLayout.LayoutParams(verifiedIconSize, verifiedIconSize);
    verifiedIconParams.leftMargin = u.dpToPx(4);
    verifiedIcon.setLayoutParams(verifiedIconParams);
    verifiedDrawable = uiObj.getDrawable(ctx, "mw_action_verified_user");
    if (verifiedDrawable != null) {
      verifiedIcon.setImageDrawable(verifiedDrawable);
      verifiedIcon.setColorFilter(Color.parseColor(uiObj.getThemeColor("tertiary")));
    }
    nameRow.addView(verifiedIcon);
  }
  
  firstRowInfo.addView(nameRow);
  
  numberText = new TextView(ctx);
  numberStr = number;
  if (dataObj != null) {
      infoList = u.extractPhoneInfo(dataObj);
      for (int i = 0; i < infoList.size(); i++) {
          numberStr += " • " + infoList.get(i);
      }
  }
  numberText.setText(numberStr);
  numberText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
  numberText.setTextColor(Color.parseColor(!isVerified && spamScore > 0 ? "#FFCCCB" : uiObj.getThemeColor("on-surface-variant")));
  numberText.setMaxLines(1);
  numberText.setEllipsize(android.text.TextUtils.TruncateAt.END);
  firstRowInfo.addView(numberText);
  
  if (dataObj != null) {
    try {
      if (dataObj.has("addresses")) {
        addresses = dataObj.getJSONArray("addresses");
        if (addresses.length() > 0) {
          addressStr = u.parseAddress(addresses.getJSONObject(0));
          if (addressStr != null) {
            addressText = new TextView(ctx);
            addressText.setText(addressStr);
            addressText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            addressText.setTextColor(Color.parseColor(!isVerified && spamScore > 0 ? "#FFCCCB" : uiObj.getThemeColor("on-surface-variant")));
            addressText.setSingleLine(true);
            addressText.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
            addressText.setMarqueeRepeatLimit(-1);
            addressText.setSelected(true);
            addressTextParams = new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT,
              LinearLayout.LayoutParams.WRAP_CONTENT
            );
            addressTextParams.topMargin = u.dpToPx(4);
            addressText.setLayoutParams(addressTextParams);
            firstRowInfo.addView(addressText);
          }
        }
      }
    } catch(Exception e) {}
  }
  
  firstRow.addView(firstRowInfo);
  card.addView(firstRow);
  
  secondRowContainer = new FrameLayout(ctx);
  secondRowContainerParams = new LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT,
    0,
    1.0f
  );
  secondRowContainerParams.topMargin = u.dpToPx(8);
  secondRowContainer.setLayoutParams(secondRowContainerParams);
  
  secondRow = new LinearLayout(ctx);
  secondRow.setOrientation(LinearLayout.VERTICAL);
  secondRowParams = new FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    FrameLayout.LayoutParams.WRAP_CONTENT
  );
  secondRowParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
  secondRow.setLayoutParams(secondRowParams);
  
  badgesContainer = uiObj.createBadgesContainer(ctx, isVerified, spamScore, dataObj, warningMap);
  if (badgesContainer != null) {
    secondRow.addView(badgesContainer);
  }

  lastCallData = d.getLastCall(number);
  if (lastCallData != null) {
    callType = ((Integer) lastCallData[0]).intValue();
    callDate = ((Long) lastCallData[1]).longValue();
    callDuration = ((Integer) lastCallData[2]).intValue();
    
    callTypeStr = "Call";
    if (callType == 1) callTypeStr = "Incoming";
    else if (callType == 2) callTypeStr = "Outgoing";
    else if (callType == 3) callTypeStr = "Missed";
    else if (callType == 5) callTypeStr = "Rejected";
    else if (callType == 6) callTypeStr = "Blocked";
    
    timeAgoStr = u.formatRelativeTime(callDate);
    durationStr = u.formatDuration(callDuration);
    lastCallStr = callTypeStr + " • " + timeAgoStr + durationStr;
    lastCallText = new TextView(ctx);
    lastCallText.setText(lastCallStr);
    lastCallText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    lastCallText.setTextColor(Color.parseColor(!isVerified && spamScore > 0 ? "#FFCCCB" : uiObj.getThemeColor("on-surface-variant")));
    lastCallTextParams = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    );
    lastCallTextParams.topMargin = u.dpToPx(2);
    lastCallText.setLayoutParams(lastCallTextParams);
    secondRow.addView(lastCallText);
  }
  
  if (note != null && !note.equals("<null>") && !note.trim().isEmpty()) {
    noteText = new TextView(ctx);
    noteText.setText(note);
    noteText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    noteText.setTypeface(null, Typeface.ITALIC);
    noteText.setTextColor(Color.parseColor(!isVerified && spamScore > 0 ? "#FFCCCB" : uiObj.getThemeColor("on-surface-variant")));
    noteText.setMaxLines(2);
    noteText.setEllipsize(android.text.TextUtils.TruncateAt.END);
    noteTextParams = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    );
    noteTextParams.topMargin = u.dpToPx(2);
    noteText.setLayoutParams(noteTextParams);
    secondRow.addView(noteText);
  }
  
  secondRowContainer.addView(secondRow);
  card.addView(secondRowContainer);
  
  closeButton = new android.widget.ImageButton(ctx);
  closeButtonSize = u.dpToPx(28);
  closeButtonParams = new FrameLayout.LayoutParams(closeButtonSize, closeButtonSize);
  closeButtonParams.gravity = Gravity.TOP | Gravity.RIGHT;
  closeButtonParams.topMargin = u.dpToPx(8);
  closeButtonParams.rightMargin = u.dpToPx(8);
  closeButton.setLayoutParams(closeButtonParams);
  
  closeButtonBg = new GradientDrawable();
  closeButtonBg.setColor(Color.parseColor(!isVerified && spamScore > 0 ? "#B71C1C" : uiObj.getThemeColor("surface-variant")));
  closeButtonBg.setCornerRadius(u.dpToPx(14));
  closeButton.setBackground(closeButtonBg);
  closeButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
  closeButton.setPadding(u.dpToPx(6), u.dpToPx(6), u.dpToPx(6), u.dpToPx(6));
  
  closeIcon = uiObj.getDrawable(ctx, "mw_navigation_close");
  if (closeIcon != null) {
    closeButton.setImageDrawable(closeIcon);
    closeButton.setColorFilter(Color.parseColor(!isVerified && spamScore > 0 ? "#FFFFFF" : uiObj.getThemeColor("on-surface")));
  }
  
  closeButton.setOnClickListener(new View.OnClickListener() {
    onClick(View v) {
      removeOverlay();
    }
  });
  
  wrapper.setTag(layoutParams);
  wrapper.addView(card);
  wrapper.addView(closeButton);
  
  startX = 0.0f;
  startY = 0.0f;
  isDragging = false;
  initialY = 0;

  card.setOnTouchListener(new View.OnTouchListener() {
    onTouch(View v, MotionEvent event) {
      safeParams = (WindowManager.LayoutParams) wrapper.getTag();
      if (safeParams == null) return false;
      
      action = event.getAction();
      
      if (action == MotionEvent.ACTION_DOWN) {
        startX = event.getRawX();
        startY = event.getRawY();
        isDragging = false;
        initialY = safeParams.y;
        return true;
        
      } else if (action == MotionEvent.ACTION_MOVE) {
        dx = event.getRawX() - startX;
        dy = event.getRawY() - startY;
        absDx = Math.abs(dx);
        absDy = Math.abs(dy);
        
        if (!isDragging && absDy > u.dpToPx(10) && absDy > absDx * 1.5) {
          isDragging = true;
        }
        
        if (isDragging && windowManager != null && wrapper != null) {
          safeParams.y = initialY + (int) dy;
          if (safeParams.y < 0) safeParams.y = 0;
          displayMetrics = context.getResources().getDisplayMetrics();
          maxY = displayMetrics.heightPixels - u.dpToPx(150);
          if (safeParams.y > maxY) safeParams.y = maxY;
          
          try {
            windowManager.updateViewLayout(wrapper, safeParams);
          } catch(Exception e) {}
          return true;
        }
        
      } else if (action == MotionEvent.ACTION_UP) {
        dx = event.getRawX() - startX;
        dy = event.getRawY() - startY;
        absDx = Math.abs(dx);
        absDy = Math.abs(dy);
        
        if (isDragging) {
          tasker.setVariable("TC_overlay_position", String.valueOf(safeParams.y));
          return true;
        }
        
        if (absDx > u.dpToPx(50) && absDx > absDy * 1.5) {
          removeOverlay();
          return true;
        }
      }
      
      return false;
    }
  });
  
  return wrapper;
}

void showOverlay() {
  if (overlayView != null) removeOverlay();
  
  accessibilityService = tasker.getAccessibilityService();
  useAccessibility = (accessibilityService != null);

  if (useAccessibility) {
    windowManager = accessibilityService.getSystemService(Context.WINDOW_SERVICE);
    tasker.log("Using WindowManager from Accessibility Service");
  } else {
    windowManager = context.getSystemService(Context.WINDOW_SERVICE);
    tasker.log("Using WindowManager from regular context");
  }
  
  if (!useAccessibility && android.os.Build.VERSION.SDK_INT >= 23) {
    canDraw = android.provider.Settings.canDrawOverlays(context);
    u.logToFile("Can draw overlays permission: " + canDraw);
    if (!canDraw) {
      throw new JavaCodeException("System Alert Window permission not granted. Enable in Settings.");
    }
  }
  
  overlayParams = new WindowManager.LayoutParams();
  
  if (useAccessibility) {
    overlayParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
  } else {
    if (android.os.Build.VERSION.SDK_INT >= 26) {
      overlayParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    } else {
      overlayParams.type = WindowManager.LayoutParams.TYPE_PHONE;
    }
  }
  
  overlayParams.format = PixelFormat.TRANSLUCENT;
  overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
                        
  displayMetrics = context.getResources().getDisplayMetrics();
  cardWidth = (int) (displayMetrics.widthPixels * 0.95);

  overlayParams.width = cardWidth;
  overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
  overlayParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
  
  savedPosition = tasker.getVariable("TC_overlay_position");
  if (savedPosition != null && !savedPosition.isEmpty()) {
    try {
      overlayParams.y = Integer.parseInt(savedPosition);
    } catch(Exception e) {
      overlayParams.y = u.dpToPx(100);
    }
  } else {
    overlayParams.y = u.dpToPx(100);
  }
  
  overlayView = createOverlayView(context, overlayParams);
  
  try {
    windowManager.addView(overlayView, overlayParams);
    u.logToFile("Overlay shown using " + (useAccessibility ? "Accessibility Service" : "System Alert Window"));
  } catch(Exception e) {
    u.logToFile("Error showing overlay: " + e.getMessage());
    if (useAccessibility) {
      try {
        u.logToFile("Trying fallback to TYPE_APPLICATION_OVERLAY");
        overlayParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        windowManager.addView(overlayView, overlayParams);
        u.logToFile("Overlay shown using fallback TYPE_APPLICATION_OVERLAY");
      } catch(Exception e2) {
        u.logToFile("Fallback also failed: " + e2.getMessage());
        throw new JavaCodeException("Failed to show overlay: " + e2.getMessage());
      }
    } else {
      throw new JavaCodeException("Failed to show overlay: " + e.getMessage());
    }
  }
}

void removeOverlay() {
  if (overlayView != null && windowManager != null) {
    try {
      windowManager.removeView(overlayView);
      u.logToFile("Overlay removed.");
    } catch(Exception e) {
      u.logToFile("Error removing overlay: " + e.getMessage());
    }
  }
  
  tasker.setJavaVariable("tcRemovalRunnable", null);
  uiObj.cleanup();
  
  overlayView = null;
  windowManager = null;
  overlayParams = null;
}

cleanupCallback = new Runnable() {
  run() {
    removeOverlay();
  }
};
tasker.setJavaVariable("tcRemovalRunnable", cleanupCallback);

uiObj.mainHandler.post(new Runnable() {
  run() {
    showOverlay();
  }
});