# Foliisho (ፎሊሾ) — Hawassa Smart Fuel Queue Management & Digital Booking System

A real-time, location-aware fuel queue management platform designed for Hawassa, Ethiopia. Foliisho eliminates destructive physical vehicle gridlocks by establishing organized virtual queues connecting Hawassa fuel station managers with drivers across private vehicles, Bajaj, taxis, and minibuses.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions incorporate your Phase 1 clarifications:
> - **Strict Role Separation**: The Android application is dedicated exclusively to **Drivers** (station discovery, ticket reservation, live position tracking, geofenced arrival, and turn alerts). The **Manager Dashboard** is deployed as a standalone responsive web application optimized for desktop and tablet browsers for station coordinators.
> - **Bilingual Amharic Default**: The Driver app defaults to **Amharic (አማርኛ)** on launch with a prominent instant English toggle in the top bar. High-readability Ethiopic typography is applied across all status indicators, alerts, and ticket summaries.
> - **Real-Time Cross-App Firebase Sync**: Both applications communicate via a shared Cloud Firestore database. Actions triggered on the Manager Web Dashboard (calling tickets, changing fuel stock, completing service) immediately propagate in real-time to the Driver Android app with sound and visual alerts.
> - **Interactive Simulation Mode**: In addition to real-time production Firebase listeners, the system includes a synchronized test mode with a fast-forward simulation clock to test the 10-minute approaching notification and queue transitions without waiting for real-world hours.

---

## 1. Overview & Core Concept

### What It Does
Foliisho transforms chaotic fuel lines in Hawassa into an orderly, transparent digital queue:
1. **Drivers** can see live Benzene and Diesel availability across Hawassa stations (e.g., TotalEnergies Piassa, NOC Shell Tabor, Oilibya Tada, Yehualaweshet), view queue lengths and estimated waiting times, reserve a virtual queue ticket before heading to the station, and receive timely notifications when their turn approaches.
2. **Station Managers** operate a clean, high-throughput web terminal to call the next vehicle, start and complete service, adjust fuel inventory levels (Available, Low Stock, Out of Stock), calibrate average pump service speeds, and pause queues during tanker deliveries.

### Target Personas & Context
- **Hawassa Drivers & Transporters**: Bajaj drivers, taxi drivers, minibus operators, and private vehicle owners navigating recurring fuel shortages who need predictable waiting times and protection against lost working hours.
- **Station Managers & Pump Attendants**: Station supervisors who require an intuitive, low-friction control panel to manage queue flow, maintain public order, and avoid roadside bottlenecks.

### Key Value
- **Zero Wasted Fuel**: Eliminates hours spent idling in physical lines.
- **Traffic Decongestion**: Vehicles stay parked or engaged in productive transport until their 10-minute arrival window is called.
- **Fairness & Auditability**: Atomic queue numbers (e.g., `HW-B104`, `HW-D205`) prevent line jumping and duplicate vehicle reservations.

---

## 2. User Experience & Visual Design

### Driver Android Application (Jetpack Compose)
1. **Splash & Authentication**:
   - Amharic-first interface with top-right language switch (`አማርኛ` / `English`).
   - Secure Google Sign-In via Jetpack `CredentialManager` backed by Firebase Auth.
   - Profile setup: Driver Full Name, Ethiopian Phone (`+251 9...`), Vehicle Plate (e.g., `3-A12345 SD`), Vehicle Category (Bajaj, Taxi, Minibus, Private Car), and Preferred Fuel (Benzene / Diesel).
2. **Station Discovery & Live Feed**:
   - Search and filter by fuel availability (Benzene / Diesel, Open stations).
   - Station cards showing distance from Hawassa city center / GPS location, live Benzene & Diesel status badges, current queue length, and estimated wait duration in minutes.
3. **Queue Booking Flow**:
   - Fuel selection dialog with active stock verification.
   - Duplicate prevention check (single active ticket per vehicle across Hawassa stations).
   - Instant ticket issuance generating atomic sequence ID (e.g., `HW-B104`).
4. **Live Queue Tracking Screen**:
   - Prominent ticket badge with status (`በመጠባበቅ ላይ / Waiting`, `እየቀረበ / Approaching`, `ተጠርቷል / Called`, `በአገልግሎት ላይ / Serving`).
   - Live counter: "Currently Serving", "Vehicles Ahead of You", and dynamic wait timer.
   - Station details with Hawassa coordinates, interactive Google Maps directions, and geofence status (detecting when driver enters station perimeter).
5. **3-Stage Alert Notification Center**:
   - **Stage 1 (Confirmed)**: Immediate booking receipt.
   - **Stage 2 (Approaching - ~10 min)**: Amharic: *"ተራዎ እየቀረበ ነው! እባክዎ ወደ ማደያው ለመምጣት ይዘጋጁ።"* / English: *"Your turn is approaching! Please prepare to arrive at the fuel station."*
   - **Stage 3 (Called)**: Amharic: *"ተራዎ ተጠርቷል። እባክዎ ወደ ማደያው ይምጡ።"* / English: *"Your turn has been called. Please proceed to the station."*

### Manager Web Dashboard (Responsive Web Console)
1. **Overview Bar**: Total vehicles served today, active queue count, waiting tickets, and live Benzene/Diesel availability switches.
2. **Primary Queue Terminal**:
   - Big-button action controls: **[Call Next Ticket]**, **[Start Service]**, **[Complete Service]**, **[Skip / No-Show]**, **[Pause Queue]**.
   - Serving card showing current vehicle plate, driver name, fuel type, and service timer.
3. **Stock & Pump Speed Settings**:
   - Instant toggles for Benzene / Diesel (`Available`, `Low Stock`, `Out of Stock`).
   - Service duration slider (e.g., 3 mins for Benzene, 4 mins for Diesel) triggering instant recalculation of all driver wait times.
4. **Live Queue Board**: Real-time table of all queued tickets with search, filtering, and timestamp audit history.

### Design System Tokens & Aesthetics
- **Theme**: Ethiopian Energy & Mobility palette. Deep Slate Navy (`#0F172A`) for high-contrast professional trust, Vibrant Solar Amber/Gold (`#F59E0B`) representing fuel flow, Emerald Green (`#10B981`) for fuel availability, and Coral Red (`#EF4444`) for depleted tanks.
- **Typography**: Clean high-legibility sans paired with localized Ethiopic font styling to ensure Amharic glyphs render without truncation or awkward line breaks.
- **Touch Ergonomics**: All driver mobile buttons adhere to strict 48dp+ tap targets with M3 ripple feedback.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale & Trade-Off |
| :--- | :--- | :--- |
| **Separate Applications** | Android Driver app + Standalone Web Manager Dashboard | Matches real-world roles: drivers are mobile-first on Ethiopian cellular networks; station managers use office PCs or pump tablets. |
| **Database & Realtime Layer** | Cloud Firestore with Snapshot Listeners | Provides sub-second cross-device synchronization between web manager clicks and mobile phone screens without custom WebSocket server maintenance. |
| **Atomic Queue Generation** | Firestore server-side sequence counters | Guarantees zero duplicate ticket numbers even under concurrent booking bursts from multiple drivers. |
| **Geofencing Verification** | GPS distance calculation with manual fallback | Accommodates drivers with flaky GPS signals or battery-saving modes without blocking them at the pump. |
| **Offline Resilience** | Local Room cache + Firestore offline persistence | Drivers retain access to their active ticket barcode and queue position even if mobile data drops temporarily. |

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────┐
│               SHARED FIRESTORE BACKEND                │
│                                                        │
│  /stations/{id}          /tickets/{id}                 │
│  /queueState/{id}        /fuelInventory/{id}           │
│  /users/{id}             /auditLogs/{id}               │
└───────────────▲────────────────────────▲───────────────┘
                │                        │
       Real-Time Listeners       Real-Time Updates
                │                        │
┌───────────────┴───────────────┐ ┌──────┴───────────────┐
│     DRIVER MOBILE APP         │ │ MANAGER WEB DASHBOARD │
│      (Android / Compose)      │ │   (Desktop / Tablet)  │
│                               │ │                       │
│ • Amharic / English UI        │ │ • Call Next / Skip    │
│ • Hawassa Station Discovery   │ │ • Start / Finish Pump │
│ • GPS Geofence & Map View     │ │ • Benzene/Diesel Stock│
│ • Atomic Ticket Booking       │ │ • Wait Time Config    │
│ • 3-Stage Alert Listener      │ │ • Daily Queue Stats   │
│ • Local Cache & Offline State │ │ • Simulation Clock    │
└───────────────────────────────┘ └───────────────────────┘
```

### Data Models & Collections

1. **`stations`**:
   - `id`: String (`total_piassa`, `noc_tabor`, `oilibya_tada`, `yehualaweshet`)
   - `name`: Localized name (Amharic & English)
   - `address`: Neighborhood / Kebele in Hawassa
   - `latitude`, `longitude`: Geocoordinates
   - `operatingStatus`: `OPEN`, `PAUSED`, `CLOSED`
   - `benzeneStock`: `AVAILABLE`, `LOW_STOCK`, `OUT_OF_STOCK`
   - `dieselStock`: `AVAILABLE`, `LOW_STOCK`, `OUT_OF_STOCK`
   - `avgServiceMinutesBenzene`: Int (default: 3)
   - `avgServiceMinutesDiesel`: Int (default: 4)

2. **`tickets`**:
   - `id`: String (e.g., `HW-B104`)
   - `stationId`: String
   - `userId`: String (Driver UID)
   - `driverName`: String
   - `driverPhone`: String
   - `vehiclePlate`: String (e.g., `3-A12345 SD`)
   - `vehicleCategory`: `BAJAJ`, `TAXI`, `MINIBUS`, `PRIVATE_CAR`, `OTHER`
   - `fuelType`: `BENZENE`, `DIESEL`
   - `sequenceNumber`: Long
   - `status`: `WAITING`, `APPROACHING`, `CALLED`, `SERVING`, `COMPLETED`, `SKIPPED`, `CANCELLED`
   - `estimatedWaitMinutes`: Int
   - `createdAt`, `calledAt`, `serviceStartedAt`, `completedAt`: Timestamps

3. **`queueState`**:
   - `stationId`: Document ID
   - `currentServingTicketId`: String?
   - `lastSequenceNumber`: Long
   - `waitingCount`: Int
   - `isPaused`: Boolean
   - `updatedAt`: Timestamp

4. **`auditLogs`**:
   - `eventId`, `actorId`, `stationId`, `ticketId`, `action`, `timestamp`, `details`

---

## 5. Phased Implementation Roadmap

1. **Phase 1: Backend Foundation & Firestore Provisioning**
   - Extract debug keystore fingerprints (`keytool`) and provision Firebase (`firestore`, `ai_logic`) via RPC action.
   - Configure `firestore.rules` and `firebase-blueprint.json` ensuring atomic queue integrity and Google Sign-In access control.
   - Validate security rules with automated rules unit tests.

2. **Phase 2: Driver Android Application (Jetpack Compose)**
   - Localization engine: Amharic (አማርኛ) resource strings and instant English toggle.
   - Authentication & Driver Profile onboarding with Ethiopian plate validation.
   - Station Discovery screen with Hawassa fuel stations, fuel badges, and map view.
   - Ticket reservation engine with duplicate vehicle checks and atomic sequence allocation.
   - Real-time Ticket Tracker with 3-stage approaching & turn notification handlers and geofenced arrival status.

3. **Phase 3: Manager Web Dashboard & Embedded Server**
   - Responsive web dashboard built with HTML5, Tailwind CSS, and Firebase Firestore JavaScript SDK.
   - Full operator controls: Call Next, Start Service, Complete Service, Skip, Cancel, Stock management, and Wait-time calibration.
   - Served via local development server on port 3000/8080 and accessible directly via the web URL.

4. **Phase 4: Synchronized Simulation Engine & Verification**
   - Dual testing tools: fast-forward simulation clock to advance wait times and trigger the 10-minute alert in real time.
   - End-to-end integration verification between Web Manager actions and Android Driver UI updates.
   - Final `compile_applet` compilation check and verification.
