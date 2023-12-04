## Features

* Caller id using Truecaller database
  * Swipe up/down to move scene
  * Swipe left/right to destroy scene
* Filter calls
  * Allow selected numbers or name
  * Block all calls
  * Block Spam calls using Truecaller database
  * Block Calls by name like name containing spam, fraud etc
  * Block Private calls
  * Block unsaved numbers
* Call log Scene to view device call logs
  * Long Tap Call logs tab to force refresh call log
* Settings Scene to change some options
* Call Filter Scene to configure call blocking options
* Number Information Scene to view information about a number
  * SMS button: LongTap to open in WhatsApp
  * Delete button: Tap to delete number from current tab and LongTap to delete from both tabs
* Missed call notification for unknown number
* Monitors Clipboard for numbers (Requires ADB WiFi or Root)
* Update device call logs for specific dialers
* Can be used as Standalone app by compiling with [App Factory](https://play.google.com/store/apps/details?id=net.dinglisch.android.appfactory)

## Changelog

### 2023-12-04
* Added option to update own Truecaller name in general settings 3 dot menu
* Fixed some errors

### 2023-11-25
* Scenes now works in Landscape orientation
* Call Log view: reuse previously created scene
* Removed unnecessary Accessibility Service Check before creating callerid scene. (add Tasker to Keep Accessibility Running manually)
* Removed some dialogs for App Factory

### 2023-10-20
* Updated searchWarning database
* Added last call info to callerid

### 2023-10-02
* User filters: Tapping once any user filter will enable/disable that user filter. Text will become **bold** for enabled and ~~strikethrough~~ for disabled filters.

### 2023-09-18
* Added a checkbox to enable/disable user filters
* Some fixes

### 2023-08-26
* Some changes and fixes

### 2023-08-06
* Added option to block all calls (except allowed numbers in user filter)
* Added option to block unsaved numbers (numbers not saved in phonebook)
* Unblock numbers directly from notification
* Fixed some bugs

### 2023-08-02
* Call blocker can now block calls by Carrier name, Country code, Dialing code
* Fixed some bugs

### 2023-07-29
* Call blocking for Android 9 and lower
* Fixed some bugs

### 2023-07-28
* Some tweaks to scene ui with theme support
* Support for Deleting call log from scene
* Improved accessibility service checking
* Added some hints for login errors

### 2023-07-19
* Added contact photo to Number information scene
* Fixed a bug where contact photo is downloaded evertime instead of once a month
* Minor changes to login process

### 2023-07-15
* Handle more login errors
* Fixed call blocking bug

### 2023-07-12
* Improved Accessibility Service detection before showing Blocking Overlay+
* Added Call, Sms to Blocked/Missed call notification
* Fixed bugs

### 2023-07-08

* Split settings scene and task
* Improved Number information scene using webview
* Clicking Blocked/Missed call notification will show Number information scene
* Many improvements to scenes

### 2023-07-02

* Added Clipboard Monitor to quickly search from clipboard
* Improved Searching in call log view
* Misc changes

### 2023-06-27

* Fixed Call block notification

### 2023-06-20

* Disabled importing installationid as Truecaller data no more contains installationid. (Importing installationid will still work if you manually set %TC_installation_id)
* Revert Don't use Blocking Overlay+ if device is unlocked

### 2023-06-13

* Themes for callerid Overlay
* Call log view caches call log for faster startup. A Refresh button is added to clear cache and reload call log.
* Added date to Truecaller log view
* Don't use Blocking Overlay+ if device is unlocked
* Some Fixes

### 2023-05-15

* Some Fixes

### 2023-05-10

* Swipe left/right caller id overlay to dismiss

### 2023-05-05

* Added Notification caller id
* Added Missed Call Notification
