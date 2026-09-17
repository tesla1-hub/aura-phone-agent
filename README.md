# AURA

This repository contains a Kotlin Android prototype for an agentic phone assistant called AURA.

## What is included
- Android app shell with a Material 3 UI
- foreground service scaffold
- accessibility service scaffold
- notification listener scaffold
- boot receiver for auto-start
- guardian approval pattern for risky actions
- memory store for basic local decision tracking

## How to run
1. Open the project in Android Studio.
2. Let Gradle sync complete.
3. Connect a physical Android device.
4. Select the `app` configuration and click Run.

## Important
This is a prototype. The accessibility service, screen monitoring, calls, SMS, and notification behavior are intentionally scaffolded and should be tested cautiously on a device you control.

## Permissions to grant
- Accessibility access
- Notification access
- Foreground service
- SMS, phone, and notification permissions when needed
- Boot complete permission if you want AURA to auto-start
