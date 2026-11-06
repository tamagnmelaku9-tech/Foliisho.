import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";
import { 
  getFirestore, 
  doc, 
  collection, 
  query, 
  where, 
  onSnapshot, 
  updateDoc, 
  setDoc, 
  serverTimestamp, 
  runTransaction, 
  getDocs,
  getDoc
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore.js";

// Firebase Web Configuration using provisioned project credentials
const firebaseConfig = {
  apiKey: "AIzaSyALgtnlKFG6z41ivRiJ1rL8TXqyOWSpqmY",
  authDomain: "gen-lang-client-0680075947.firebaseapp.com",
  projectId: "gen-lang-client-0680075947",
  storageBucket: "gen-lang-client-0680075947.firebasestorage.app",
  messagingSenderId: "742286939111",
  appId: "1:742286939111:web:foliisho"
};

const app = initializeApp(firebaseConfig);
const DATABASE_ID = "ai-studio-android-foliisho-9bcf1f52-3b85-4df6-b770-9d0f6065da66";
const db = getFirestore(app, DATABASE_ID);

let currentStationId = "total_piassa";
let stationData = null;
let queueStateData = null;
let currentTickets = [];

// DOM Elements
const stationSelect = document.getElementById("stationSelect");
const statQueueCount = document.getElementById("statQueueCount");
const statCurrentServing = document.getElementById("statCurrentServing");
const statCompletedToday = document.getElementById("statCompletedToday");
const activeTicketId = document.getElementById("activeTicketId");
const activeDriverName = document.getElementById("activeDriverName");
const activePlate = document.getElementById("activePlate");
const activeFuelType = document.getElementById("activeFuelType");
const activeVehicleCategory = document.getElementById("activeVehicleCategory");
const queuePauseBadge = document.getElementById("queuePauseBadge");
const queueTableBody = document.getElementById("queueTableBody");
const badgeBenzeneStock = document.getElementById("badgeBenzeneStock");
const badgeDieselStock = document.getElementById("badgeDieselStock");
const btnCallNext = document.getElementById("btnCallNext");
const btnStartService = document.getElementById("btnStartService");
const btnCompleteService = document.getElementById("btnCompleteService");
const btnSkipTicket = document.getElementById("btnSkipTicket");
const btnPauseQueue = document.getElementById("btnPauseQueue");
const btnGenerateSampleTicket = document.getElementById("btnGenerateSampleTicket");
const btnFastForwardClock = document.getElementById("btnFastForwardClock");

// Unsubscribers for real-time listeners
let unsubStation = null;
let unsubQueueState = null;
let unsubTickets = null;

function subscribeToStation(stationId) {
  if (unsubStation) unsubStation();
  if (unsubQueueState) unsubQueueState();
  if (unsubTickets) unsubTickets();

  currentStationId = stationId;

  // 1. Listen to Station document
  const stationRef = doc(db, "stations", stationId);
  unsubStation = onSnapshot(stationRef, (snap) => {
    if (snap.exists()) {
      stationData = snap.data();
      updateStationUI();
    }
  });

  // 2. Listen to QueueState document
  const queueStateRef = doc(db, "queueState", stationId);
  unsubQueueState = onSnapshot(queueStateRef, (snap) => {
    if (snap.exists()) {
      queueStateData = snap.data();
      updateQueueStateUI();
    }
  });

  // 3. Listen to Active Tickets for this station
  const ticketsQuery = query(
    collection(db, "tickets"),
    where("stationId", "==", stationId)
  );

  unsubTickets = onSnapshot(ticketsQuery, (snap) => {
    currentTickets = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    updateTicketsTable();
  });
}

function updateStationUI() {
  if (!stationData) return;
  
  // Stock badges
  updateStockBadge(badgeBenzeneStock, stationData.benzeneStock);
  updateStockBadge(badgeDieselStock, stationData.dieselStock);
}

function updateStockBadge(element, status) {
  element.textContent = status || "AVAILABLE";
  if (status === "AVAILABLE") {
    element.className = "text-xs px-2 py-0.5 rounded font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30";
  } else if (status === "LOW_STOCK") {
    element.className = "text-xs px-2 py-0.5 rounded font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30";
  } else {
    element.className = "text-xs px-2 py-0.5 rounded font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30";
  }
}

function updateQueueStateUI() {
  if (!queueStateData) return;

  const count = queueStateData.waitingCount || 0;
  statQueueCount.textContent = count;

  const servingTicketId = queueStateData.currentServingTicketId;
  statCurrentServing.textContent = servingTicketId || "--";

  if (queueStateData.isPaused) {
    queuePauseBadge.classList.remove("hidden");
    btnPauseQueue.textContent = "▶️ ወረፋውን ቀጥል / Resume Queue";
  } else {
    queuePauseBadge.classList.add("hidden");
    btnPauseQueue.textContent = "⏸️ ወረፋውን አቁም / Pause Queue";
  }

  // Find active ticket if serving
  const servingTicket = currentTickets.find(t => t.ticketId === servingTicketId || t.status === "SERVING" || t.status === "CALLED");
  if (servingTicket) {
    activeTicketId.textContent = servingTicket.ticketId;
    activeDriverName.textContent = servingTicket.driverName || "Driver";
    activePlate.textContent = servingTicket.vehiclePlate || "--";
    activeFuelType.textContent = servingTicket.fuelType || "BENZENE";
    activeVehicleCategory.textContent = `${servingTicket.vehicleCategory || 'Vehicle'} • [${servingTicket.status}]`;
  } else {
    activeTicketId.textContent = "--";
    activeDriverName.textContent = "ምንም ንቁ ተሽከርካሪ የለም (Waiting for Next Call)";
    activePlate.textContent = "--";
    activeFuelType.textContent = "--";
    activeVehicleCategory.textContent = "--";
  }
}

function updateTicketsTable() {
  const waitingTickets = currentTickets
    .filter(t => t.status === "WAITING" || t.status === "APPROACHING" || t.status === "CALLED" || t.status === "SERVING")
    .sort((a, b) => (a.sequenceNumber || 0) - (b.sequenceNumber || 0));

  const completedCount = currentTickets.filter(t => t.status === "COMPLETED").length;
  statCompletedToday.textContent = completedCount;

  queueTableBody.innerHTML = "";

  if (waitingTickets.length === 0) {
    queueTableBody.innerHTML = `<tr><td colspan="6" class="py-6 text-center text-slate-500 font-normal">በአሁኑ ሰዓት በወረፋ ላይ ያለ ተሽከርካሪ የለም (Queue is clear).</td></tr>`;
    return;
  }

  waitingTickets.forEach((ticket, idx) => {
    const tr = document.createElement("tr");
    tr.className = "hover:bg-brand-navy/40 transition";

    const statusPill = getStatusPill(ticket.status);

    tr.innerHTML = `
      <td class="py-3 px-4 font-mono text-slate-400">#${ticket.sequenceNumber}</td>
      <td class="py-3 px-4 font-mono font-bold text-white">${ticket.ticketId}</td>
      <td class="py-3 px-4 text-brand-amber font-mono font-semibold">${ticket.vehiclePlate}</td>
      <td class="py-3 px-4 text-xs font-semibold text-slate-300">${ticket.fuelType}</td>
      <td class="py-3 px-4">${statusPill}</td>
      <td class="py-3 px-4 text-right text-xs font-mono text-slate-400">~${ticket.estimatedWaitMinutes || (idx + 1) * 3} min</td>
    `;
    queueTableBody.appendChild(tr);
  });

  updateQueueStateUI();
}

function getStatusPill(status) {
  if (status === "SERVING") {
    return `<span class="px-2 py-0.5 rounded text-xs font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">SERVING</span>`;
  } else if (status === "CALLED") {
    return `<span class="px-2 py-0.5 rounded text-xs font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30">CALLED</span>`;
  } else if (status === "APPROACHING") {
    return `<span class="px-2 py-0.5 rounded text-xs font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30">APPROACHING</span>`;
  } else {
    return `<span class="px-2 py-0.5 rounded text-xs font-medium bg-slate-700 text-slate-300">WAITING</span>`;
  }
}

// Action: Call Next Ticket
btnCallNext.addEventListener("click", async () => {
  const waitingTickets = currentTickets
    .filter(t => t.status === "WAITING" || t.status === "APPROACHING")
    .sort((a, b) => (a.sequenceNumber || 0) - (b.sequenceNumber || 0));

  if (waitingTickets.length === 0) {
    alert("ተራ የሚጠብቅ ተሽከርካሪ የለም (No eligible waiting tickets).");
    return;
  }

  const nextTicket = waitingTickets[0];
  try {
    await updateDoc(doc(db, "tickets", nextTicket.ticketId), {
      status: "CALLED",
      calledAt: serverTimestamp()
    });

    await updateDoc(doc(db, "queueState", currentStationId), {
      currentServingTicketId: nextTicket.ticketId,
      currentServingPlate: nextTicket.vehiclePlate,
      updatedAt: serverTimestamp()
    });
  } catch (err) {
    console.error("Failed to call next ticket:", err);
  }
});

// Action: Start Service
btnStartService.addEventListener("click", async () => {
  const activeTicket = currentTickets.find(t => t.ticketId === queueStateData?.currentServingTicketId || t.status === "CALLED");
  if (!activeTicket) {
    alert("አሁን የተጠራ ትኬት የለም። በመጀመሪያ 'Call Next' ይጫኑ።");
    return;
  }

  try {
    await updateDoc(doc(db, "tickets", activeTicket.ticketId), {
      status: "SERVING",
      serviceStartedAt: serverTimestamp()
    });
  } catch (err) {
    console.error("Failed to start service:", err);
  }
});

// Action: Complete Service
btnCompleteService.addEventListener("click", async () => {
  const activeTicket = currentTickets.find(t => t.ticketId === queueStateData?.currentServingTicketId || t.status === "SERVING" || t.status === "CALLED");
  if (!activeTicket) {
    alert("የሚጠናቀቅ ንቁ ትኬት የለም።");
    return;
  }

  try {
    const queueStateRef = doc(db, "queueState", currentStationId);
    await runTransaction(db, async (transaction) => {
      const qSnap = await transaction.get(queueStateRef);
      const currentWaiting = (qSnap.data()?.waitingCount || 1);
      const newWaiting = Math.max(0, currentWaiting - 1);

      transaction.update(doc(db, "tickets", activeTicket.ticketId), {
        status: "COMPLETED",
        completedAt: serverTimestamp()
      });

      transaction.update(queueStateRef, {
        currentServingTicketId: null,
        currentServingPlate: null,
        waitingCount: newWaiting,
        updatedAt: serverTimestamp()
      });
    });
  } catch (err) {
    console.error("Failed to complete service:", err);
  }
});

// Action: Skip Ticket
btnSkipTicket.addEventListener("click", async () => {
  const activeTicket = currentTickets.find(t => t.ticketId === queueStateData?.currentServingTicketId || t.status === "CALLED");
  if (!activeTicket) {
    alert("የሚዘለል የተጠራ ትኬት የለም።");
    return;
  }

  if (confirm(`ትኬት ${activeTicket.ticketId} (${activeTicket.vehiclePlate}) በወቅቱ ስላልቀረበ እንዲዘለል ይፈልጋሉ?`)) {
    try {
      await updateDoc(doc(db, "tickets", activeTicket.ticketId), {
        status: "SKIPPED",
        completedAt: serverTimestamp()
      });

      await updateDoc(doc(db, "queueState", currentStationId), {
        currentServingTicketId: null,
        currentServingPlate: null,
        updatedAt: serverTimestamp()
      });
    } catch (err) {
      console.error("Failed to skip ticket:", err);
    }
  }
});

// Action: Pause / Resume Queue
btnPauseQueue.addEventListener("click", async () => {
  const isCurrentlyPaused = queueStateData?.isPaused || false;
  try {
    await updateDoc(doc(db, "queueState", currentStationId), {
      isPaused: !isCurrentlyPaused,
      updatedAt: serverTimestamp()
    });
  } catch (err) {
    console.error("Failed to toggle queue pause:", err);
  }
});

// Action: Set Stock
window.setStock = async function(fuelType, status) {
  try {
    const payload = {};
    if (fuelType === "benzene") {
      payload.benzeneStock = status;
    } else {
      payload.dieselStock = status;
    }
    payload.updatedAt = serverTimestamp();

    await updateDoc(doc(db, "stations", currentStationId), payload);
  } catch (err) {
    console.error("Failed to update stock:", err);
  }
};

const HAWASSA_INITIAL_STATIONS = [
  {
    stationId: "total_piassa",
    name: "TotalEnergies Piassa",
    nameAmharic: "ቶታል ኢነርጂስ ፒያሳ",
    address: "Piassa Square, Main Road",
    addressAmharic: "ፒያሳ አደባባይ፣ ዋናው መንገድ",
    latitude: 7.0583,
    longitude: 38.4741,
    operatingStatus: "OPEN",
    benzeneStock: "AVAILABLE",
    dieselStock: "AVAILABLE",
    avgServiceMinutesBenzene: 3,
    avgServiceMinutesDiesel: 4,
    queueCapacity: 100
  },
  {
    stationId: "noc_tabor",
    name: "NOC Shell / Tabor",
    nameAmharic: "ኖክ ታቦር ማደያ",
    address: "Mount Tabor Ring Road",
    addressAmharic: "ታቦር ተራራ ቀለበት መንገድ",
    latitude: 7.0421,
    longitude: 38.4682,
    operatingStatus: "OPEN",
    benzeneStock: "AVAILABLE",
    dieselStock: "LOW_STOCK",
    avgServiceMinutesBenzene: 3,
    avgServiceMinutesDiesel: 5,
    queueCapacity: 100
  },
  {
    stationId: "oilibya_tada",
    name: "Oilibya / Tada",
    nameAmharic: "ኦይሊቢያ ታዳ ማደያ",
    address: "Tada Junction, Industrial Area",
    addressAmharic: "ታዳ መገናኛ፣ የኢንዱስትሪ መንደር",
    latitude: 7.0655,
    longitude: 38.4812,
    operatingStatus: "OPEN",
    benzeneStock: "OUT_OF_STOCK",
    dieselStock: "AVAILABLE",
    avgServiceMinutesBenzene: 4,
    avgServiceMinutesDiesel: 4,
    queueCapacity: 100
  },
  {
    stationId: "yehualaweshet",
    name: "Yehualaweshet Station",
    nameAmharic: "የኋለእሸት ማደያ",
    address: "Near Hawassa University Main Campus",
    addressAmharic: "ሀዋሳ ዩኒቨርሲቲ ዋና ግቢ አጠገብ",
    latitude: 7.0490,
    longitude: 38.4895,
    operatingStatus: "OPEN",
    benzeneStock: "AVAILABLE",
    dieselStock: "AVAILABLE",
    avgServiceMinutesBenzene: 3,
    avgServiceMinutesDiesel: 4,
    queueCapacity: 100
  }
];

async function seedInitialStationsIfEmpty() {
  try {
    for (const station of HAWASSA_INITIAL_STATIONS) {
      const sRef = doc(db, "stations", station.stationId);
      await setDoc(sRef, {
        ...station,
        updatedAt: serverTimestamp()
      }, { merge: true });

      const qRef = doc(db, "queueState", station.stationId);
      const isPiassa = station.stationId === "total_piassa";
      const qSnap = await getDoc(qRef);

      if (!qSnap.exists() || (isPiassa && (qSnap.data()?.waitingCount === 0 || !qSnap.data()?.waitingCount))) {
        await setDoc(qRef, {
          stationId: station.stationId,
          currentServingTicketId: isPiassa ? "HW-B101" : null,
          currentServingPlate: isPiassa ? "3-B10492 SD" : null,
          lastSequenceNumber: isPiassa ? 102 : 100,
          waitingCount: isPiassa ? 1 : 0,
          isPaused: false,
          updatedAt: serverTimestamp()
        }, { merge: true });

        if (isPiassa) {
          const t1Ref = doc(db, "tickets", "HW-B101");
          await setDoc(t1Ref, {
            ticketId: "HW-B101",
            userId: "sim_driver_101",
            stationId: "total_piassa",
            driverName: "Abebe Kebede",
            driverPhone: "0911223344",
            vehiclePlate: "3-B10492 SD",
            vehicleCategory: "Bajaj",
            fuelType: "BENZENE",
            sequenceNumber: 101,
            status: "SERVING",
            estimatedWaitMinutes: 0,
            createdAt: serverTimestamp(),
            calledAt: serverTimestamp(),
            serviceStartedAt: serverTimestamp()
          }, { merge: true });

          const t2Ref = doc(db, "tickets", "HW-B102");
          await setDoc(t2Ref, {
            ticketId: "HW-B102",
            userId: "sim_driver_102",
            stationId: "total_piassa",
            driverName: "Tadesse Haile",
            driverPhone: "0911556677",
            vehiclePlate: "2-A55839 AA",
            vehicleCategory: "Taxi",
            fuelType: "BENZENE",
            sequenceNumber: 102,
            status: "WAITING",
            estimatedWaitMinutes: 3,
            createdAt: serverTimestamp()
          }, { merge: true });
        }
      }
    }
    console.log("Hawassa fuel stations and sample queues auto-seeded successfully.");
  } catch (err) {
    console.warn("Auto-seeding warning:", err);
  }
}

// Action: Generate Sample Driver Booking
btnGenerateSampleTicket.addEventListener("click", async () => {
  const sampleNames = ["Abebe Kebede", "Tadesse Haile", "Dawit Melaku", "Almaz Tesfaye", "Mulugeta Bekele"];
  const samplePlates = ["3-B10492 SD", "2-A55839 AA", "3-C98124 SD", "3-A44721 SD", "1-T88102 SD"];
  const categories = ["Bajaj", "Taxi", "Minibus", "Private Car"];
  
  const randName = sampleNames[Math.floor(Math.random() * sampleNames.length)];
  const randPlate = samplePlates[Math.floor(Math.random() * samplePlates.length)];
  const randCat = categories[Math.floor(Math.random() * categories.length)];
  const fuel = Math.random() > 0.4 ? "BENZENE" : "DIESEL";

  const targetStationId = currentStationId || "total_piassa";
  const queueStateRef = doc(db, "queueState", targetStationId);

  try {
    await runTransaction(db, async (transaction) => {
      const qSnap = await transaction.get(queueStateRef);
      const qData = qSnap.exists() ? qSnap.data() : { lastSequenceNumber: 100, waitingCount: 0, isPaused: false };
      const lastSeq = (qData.lastSequenceNumber || 100) + 1;
      const waiting = (qData.waitingCount || 0) + 1;

      const prefix = fuel === "BENZENE" ? "B" : "D";
      const ticketId = `HW-${prefix}${lastSeq}`;

      transaction.set(queueStateRef, {
        stationId: targetStationId,
        lastSequenceNumber: lastSeq,
        waitingCount: waiting,
        isPaused: qData.isPaused || false,
        updatedAt: serverTimestamp()
      }, { merge: true });

      const ticketRef = doc(db, "tickets", ticketId);
      transaction.set(ticketRef, {
        ticketId,
        userId: `sim_driver_${Date.now()}`,
        stationId: targetStationId,
        driverName: randName,
        driverPhone: "0911000000",
        vehiclePlate: randPlate,
        vehicleCategory: randCat,
        fuelType: fuel,
        sequenceNumber: lastSeq,
        status: "WAITING",
        estimatedWaitMinutes: waiting * 3,
        createdAt: serverTimestamp()
      });
    });
    console.log("Sample booking created for " + targetStationId);
  } catch (err) {
    console.error("Failed to generate sample ticket:", err);
  }
});

// Station change handler
stationSelect.addEventListener("change", (e) => {
  subscribeToStation(e.target.value);
});

// Initial boot
seedInitialStationsIfEmpty().then(() => {
  subscribeToStation(currentStationId);
});
