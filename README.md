# Outdoor Worker Location Tracker System (OWLTS)

A full-stack real-time location tracking system built for outdoor workers and their supervisors, combining a native Android app with a web dashboard, powered by Firebase and the Google Maps API.

Originally developed as my final year Computer Science dissertation project.

<!-- Add 2-3 screenshots here once uploaded, e.g.
![App screenshot](screenshots/app-map-view.png)
![Dashboard screenshot](screenshots/web-dashboard.png)
-->

## Overview

Outdoor workers (e.g. field engineers, surveyors, lone workers) often operate without a reliable way for supervisors to monitor their location or safety in real time. OWLTS addresses this with:

- A **native Android app** that workers use in the field, sharing live location in the background without requiring the app to stay open
- A **web dashboard** that supervisors use to view live worker positions, routes, and history on a map
- An **SOS alerting system** so workers can flag an emergency directly to their supervisor

## Features

- 🔐 Secure sign-in via Google Sign-In and Firebase Authentication
- 📍 Real-time location tracking with live map updates
- 🔋 Battery-efficient background sync using Android WorkManager, rather than continuous foreground polling
- 🗺️ Route and waypoint rendering on Google Maps for both live tracking and historical playback
- 🚨 SOS alerting for workers to signal an emergency
- 📊 Web dashboard for supervisors to monitor multiple workers at once

## Tech Stack

| Layer            | Technology                                  |
|-------------------|----------------------------------------------|
| Mobile app        | Android (Java/Kotlin), Android Studio        |
| Web dashboard      | HTML/CSS/JavaScript                          |
| Backend / database | Firebase (Authentication, Firestore)         |
| Maps & location    | Google Maps API                              |
| Background tasks   | Android WorkManager                          |

## Architecture

The system is split into two clients sharing a common Firebase backend:

- **Android app** — captures and pushes worker location updates to Firestore on a scheduled background interval (via WorkManager), and handles authentication and SOS alerts.
- **Web dashboard** — reads live and historical location data from Firestore and renders it on a Google Map, giving supervisors an overview of all active workers.

Real-time sync between the two clients is handled entirely through Firestore's live listeners, so updates on one side (e.g. a new SOS alert) appear on the other without a manual refresh.

## Notable Engineering Decisions

- Chose **WorkManager** over a persistent foreground service for location updates, trading a small amount of real-time precision for significantly better battery life and compliance with Android's background execution limits.
- Handled **platform-level restrictions** on background services across different Android versions, which required adapting the sync strategy rather than relying on a single implementation.
- Designed the Firestore data model to support both **live tracking and historical route playback** from the same underlying data.

## Setup

This repo uses placeholder values in place of real API keys and credentials. To run it yourself:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com), enabling **Authentication** (Google Sign-In) and **Firestore**.
2. **Android app:**
   - Replace `YOUR_WEB_CLIENT_ID` in `strings.xml` with your OAuth web client ID from the Firebase console.
   - Add your own `google-services.json` from the Firebase console (not committed to this repo).
3. **Web dashboard:**
   - Copy `firebase-config.example.js` to `firebase-config.js` and fill in your Firebase project values.
   - Replace `YOUR_MAP_API_KEY` in the Maps script tag with a Google Maps API key. In production this key should be **restricted by HTTP referrer** in the Google Cloud Console.

## Testing

The system was evaluated through unit testing and user testing against a set of functional and non-functional requirements defined at the project's outset (covering accuracy, reliability, and usability of the tracking and alerting features).

## Author

**Abuubakar Mohamed**
[LinkedIn](https://linkedin.com/in/your-profile) · [GitHub](https://github.com/modenspace)
