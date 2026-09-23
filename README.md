# Distraction hider for WhatsApp
It was made entirely by Claude

There can be errors because I don't know anything about Kotlin.

This project has not been tested yet on any device

# Inspired by Digital Habits: Focus
Repo link - https://github.com/digitalhabits/dh-focus-android

# Disclaimer
This project is not affiliated with or endorsed by WhatsApp or Meta

# My Focus (starter)

Open this folder in Android Studio, let it sync, then Run on a phone (Android 8.0+).
Then: open the app -> "Open accessibility settings" -> turn on "My Focus".

## Choose what gets hidden
Edit the rules in the app. One rule per line:

    package | id or text | value | optional parents to climb

- `id`   : the element's view ID, e.g. `com.example.app:id/feed_container`
- `text` : text or content description to look for (partial, not case-sensitive)
- 4th field: if the match is only a small label, climb N parents to cover the whole block

Find IDs with Android Studio > Tools > Layout Inspector (or the Accessibility Scanner app).

## Choose how it looks
In the app: solid color (any hex) or blur. Blur needs Android 12+ and a device with
window blur enabled; otherwise it falls back to solid.

Overlays let taps and scrolling pass through. To block taps instead, remove
FLAG_NOT_TOUCHABLE in FocusService.layoutParams().

## ID finder (step 4 in the app)
IDs change between app versions, so find them on your own phone:
1. Enter the package (WhatsApp = `com.whatsapp`, WhatsApp Business = `com.whatsapp.w4b`), tap "Start scan".
2. Open WhatsApp and visit Chats, Groups, Channels, etc.
3. Come back, tap "Stop and show IDs". IDs seen many times at once (like 8x) are usually avatars in a list.
4. Copy an ID into the rules: `com.whatsapp | id | com.whatsapp:id/<name>`
If nothing avatar-like shows up, the app hides its avatars from accessibility; then a position-based rule is needed.

# Motive 
The project is for developers who are interested in anything related to WhatsApp.
This is an Idea for them.
