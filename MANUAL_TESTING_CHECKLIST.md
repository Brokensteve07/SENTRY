# SENTRY Manual Testing & Hackathon Demonstration Checklist

This document provides a systematic verification guide for testing SENTRY on physical Android devices or emulators, specifically tailored for the **iQOO Hackathon Live Demonstration**.

---

## 1. Initial Launch & Permissions
- [ ] **Cold Launch**: App opens with dark, sleek OLED futuristic theme without crashes.
- [ ] **Header**: "SENTRY" brand and "AI SAFETY LAYER" subtitle visible.
- [ ] **Status Badge**: Initial status shows `STANDBY` with muted gray indicator.
- [ ] **Permissions Prompt**: Dialog requests Camera, Microphone, and Location permissions.
- [ ] **Permission Fallback**: Denying a permission gracefully updates the Sensor Subsystems row to `Degraded` without closing the app.

---

## 2. Sensor Subsystems Verification
- [ ] **Camera Status**: Shows `Active` when protection starts and camera is available.
- [ ] **Audio Status**: Displays `Active` when microphone monitoring begins.
- [ ] **Motion Status**: Shows `Active` when accelerometer/gyroscope listeners register.
- [ ] **Location Status**: Shows `Active` or `Standby` based on GPS signal.

---

## 3. Core Protection Engine (Start / Stop)
- [ ] Tap **[ START PROTECTION ]**:
  - Button transitions to dark slate with label `[ STOP PROTECTION ]`.
  - Status badge turns to glowing green `PROTECTED`.
  - Camera preview and HUD overlays activate.
  - Sensor streams begin continuous fusion into Context Engine.
- [ ] Tap **[ STOP PROTECTION ]**:
  - Camera feed stops, sensors unregister to save battery.
  - Status returns to `STANDBY`.

---

## 4. Mode Switching
- [ ] **Pedestrian Mode**: Targets approaching vehicles, horns, obstacles, and road crossings.
- [ ] **Home Mode**: Optimized for smoke/fire, emergency sirens, and glass shattering sounds.
- [ ] **Elderly Mode**: High sensitivity to sudden fall motion signatures and prolonged inactivity.
- [ ] **Rider Mode**: High sensitivity to sudden deceleration, rapid braking, and blindspot vehicles.
- [ ] **Demo Mode**: Enables one-tap controlled scenario injection for presentations.

---

## 5. Hackathon 60-Second Presentation Scenarios

### Scenario 1: Vehicle Approaching From Right (Primary Pitch)
- [ ] Tap **"1. Car Right + Horn"** chip.
- [ ] **Camera HUD**: Bounding box appears on the right third of the screen with label `Car 93% [APPROACHING]`.
- [ ] **Top Indicator**: `RIGHT` sector tag lights up in neon cyan.
- [ ] **Risk Score**: Radial gauge climbs rapidly to **91–95 (HIGH RISK)** in glowing red.
- [ ] **Visual Warning Banner**: Displays `⚠️ VEHICLE APPROACHING FROM YOUR RIGHT` with an arrow pointing right.
- [ ] **Voice TTS**: Speaks cleanly: *"Vehicle approaching from your right."*
- [ ] **Haptics**: Device vibrates with right-biased directional double pulse.
- [ ] **Debug Panel**: Open "DECISION PIPELINE & DEBUG" to show judges:
  - `Camera AI: Car (93%)`
  - `Position: RIGHT`
  - `Movement: APPROACHING`
  - `Audio Class: HORN (88%) - 82 dB`
  - `Motion State: WALKING`
  - `Phone Orientation: AWAY`
  - `User Inattention: YES (Facing Away while walking)`
  - `Risk Calculation: 91 / 100 (HIGH)`
  - `Decision: WARN_USER_URGENT`

### Scenario 2: Obstacle Ahead
- [ ] Tap **"2. Obstacle Ahead"** chip.
- [ ] **Camera HUD**: Bounding box in center with label `Obstacle [STATIONARY]`.
- [ ] **Warning Banner**: Displays `⚠️ OBSTACLE AHEAD` with upward arrow.
- [ ] **Voice TTS**: Speaks *"Obstacle ahead."*
- [ ] **Risk Score**: Evaluates in CAUTION range (~45).

### Scenario 3: Vehicle Approaching From Left
- [ ] Tap **"3. Truck Left"** chip.
- [ ] **Camera HUD**: Bounding box on left sector with label `Truck 91% [APPROACHING]`.
- [ ] **Warning Banner**: Displays `⚠️ VEHICLE APPROACHING FROM YOUR LEFT` with left arrow.
- [ ] **Voice TTS**: Speaks *"Vehicle approaching from your left."*

### Scenario 4: Area Safe / Threat Cleared
- [ ] Tap **"4. Clear / Safe"** chip.
- [ ] Risk score drops to **0 (NORMAL)** in neon green.
- [ ] Warning banner dismisses with smooth fade animation.
- [ ] Audio alerts remain silent.

### Scenario 5: Elderly Fall Emergency Workflow
- [ ] Tap **"5. Elderly Fall SOS"** chip.
- [ ] **SOS Dialog**: Prominently pops up with red shield icon:
  - Header: `POSSIBLE FALL DETECTED`
  - 10-second countdown timer counting down in bold red numerals.
  - `[ CANCEL ]` button to dismiss false alarm.
  - `[ GET HELP ]` button to instantly dispatch emergency assistance.
- [ ] Tap `[ CANCEL ]`: Dialog closes and timer stops cleanly.

---

## 6. Anti-Spam & Audio Cooldown
- [ ] Repeatedly trigger Scenario 1 within 3 seconds.
- [ ] Verify voice warning does **NOT** spam repeatedly every frame.
- [ ] Visual warning stays active while TTS respects the 3.8-second hysteresis cooldown.
