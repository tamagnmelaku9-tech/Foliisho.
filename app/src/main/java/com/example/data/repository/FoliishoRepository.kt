package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.models.DriverProfile
import com.example.data.models.FuelStation
import com.example.data.models.FuelStockStatus
import com.example.data.models.FuelType
import com.example.data.models.QueueTicket
import com.example.data.models.StationOperatingStatus
import com.example.data.models.StationQueueState
import com.example.data.models.TicketStatus
import com.example.data.models.VehicleCategory
import com.example.data.models.toDriverProfileSafe
import com.example.data.models.toFuelStationSafe
import com.example.data.models.toQueueTicketSafe
import com.example.data.models.toStationQueueStateSafe
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class FoliishoRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Foliisho.")
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    companion object {
        val defaultHawassaStations = listOf(
            FuelStation(
                stationId = "total_piassa",
                name = "TotalEnergies Piassa",
                nameAmharic = "ቶታል ኢነርጂስ ፒያሳ",
                address = "Piassa Square, Main Road",
                addressAmharic = "ፒያሳ አደባባይ፣ ዋናው መንገድ",
                latitude = 7.0583,
                longitude = 38.4741,
                operatingStatus = StationOperatingStatus.OPEN.name,
                benzeneStock = FuelStockStatus.AVAILABLE.name,
                dieselStock = FuelStockStatus.AVAILABLE.name,
                avgServiceMinutesBenzene = 3L,
                avgServiceMinutesDiesel = 4L
            ),
            FuelStation(
                stationId = "noc_tabor",
                name = "NOC Shell / Tabor",
                nameAmharic = "ኖክ ታቦር ማደያ",
                address = "Mount Tabor Ring Road",
                addressAmharic = "ታቦር ተራራ ቀለበት መንገድ",
                latitude = 7.0421,
                longitude = 38.4682,
                operatingStatus = StationOperatingStatus.OPEN.name,
                benzeneStock = FuelStockStatus.AVAILABLE.name,
                dieselStock = FuelStockStatus.LOW_STOCK.name,
                avgServiceMinutesBenzene = 3L,
                avgServiceMinutesDiesel = 5L
            ),
            FuelStation(
                stationId = "oilibya_tada",
                name = "Oilibya / Tada",
                nameAmharic = "ኦይሊቢያ ታዳ ማደያ",
                address = "Tada Junction, Industrial Area",
                addressAmharic = "ታዳ መገናኛ፣ የኢንዱስትሪ መንደር",
                latitude = 7.0655,
                longitude = 38.4812,
                operatingStatus = StationOperatingStatus.OPEN.name,
                benzeneStock = FuelStockStatus.OUT_OF_STOCK.name,
                dieselStock = FuelStockStatus.AVAILABLE.name,
                avgServiceMinutesBenzene = 4L,
                avgServiceMinutesDiesel = 4L
            ),
            FuelStation(
                stationId = "yehualaweshet",
                name = "Yehualaweshet Station",
                nameAmharic = "የኋለእሸት ማደያ",
                address = "Near Hawassa University Main Campus",
                addressAmharic = "ሀዋሳ ዩኒቨርሲቲ ዋና ግቢ አጠገብ",
                latitude = 7.0490,
                longitude = 38.4895,
                operatingStatus = StationOperatingStatus.OPEN.name,
                benzeneStock = FuelStockStatus.AVAILABLE.name,
                dieselStock = FuelStockStatus.AVAILABLE.name,
                avgServiceMinutesBenzene = 3L,
                avgServiceMinutesDiesel = 4L
            )
        )
    }

    // Seed default Hawassa fuel stations if collection is empty
    suspend fun seedHawassaStationsIfEmpty() {
        try {
            val snapshot = db.collection("stations").get().await()
            if (snapshot.isEmpty) {
                val initialStations = defaultHawassaStations

                for (station in initialStations) {
                    db.collection("stations").document(station.stationId).set(
                        mapOf(
                            "stationId" to station.stationId,
                            "name" to station.name,
                            "nameAmharic" to station.nameAmharic,
                            "address" to station.address,
                            "addressAmharic" to station.addressAmharic,
                            "latitude" to station.latitude,
                            "longitude" to station.longitude,
                            "operatingStatus" to station.operatingStatus,
                            "benzeneStock" to station.benzeneStock,
                            "dieselStock" to station.dieselStock,
                            "avgServiceMinutesBenzene" to station.avgServiceMinutesBenzene,
                            "avgServiceMinutesDiesel" to station.avgServiceMinutesDiesel,
                            "queueCapacity" to station.queueCapacity,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()

                    // Ensure matching initial queue state exists
                    db.collection("queueState").document(station.stationId).set(
                        mapOf(
                            "stationId" to station.stationId,
                            "currentServingTicketId" to null,
                            "currentServingPlate" to null,
                            "lastSequenceNumber" to 100L,
                            "waitingCount" to 0L,
                            "isPaused" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                }
                // Ensure TotalEnergies Piassa has active sample tickets so queue shows live state immediately
                val piassaQueueDoc = db.collection("queueState").document("total_piassa").get().await()
                if (!piassaQueueDoc.exists() || (piassaQueueDoc.getLong("waitingCount") ?: 0L) == 0L) {
                    db.collection("queueState").document("total_piassa").set(
                        mapOf(
                            "stationId" to "total_piassa",
                            "currentServingTicketId" to "HW-B101",
                            "currentServingPlate" to "3-B10492 SD",
                            "lastSequenceNumber" to 102L,
                            "waitingCount" to 1L,
                            "isPaused" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()

                    db.collection("tickets").document("HW-B101").set(
                        mapOf(
                            "ticketId" to "HW-B101",
                            "userId" to "sim_driver_101",
                            "stationId" to "total_piassa",
                            "driverName" to "Abebe Kebede",
                            "driverPhone" to "0911223344",
                            "vehiclePlate" to "3-B10492 SD",
                            "vehicleCategory" to VehicleCategory.BAJAJ.name,
                            "fuelType" to FuelType.BENZENE.name,
                            "sequenceNumber" to 101L,
                            "status" to TicketStatus.SERVING.name,
                            "estimatedWaitMinutes" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "calledAt" to FieldValue.serverTimestamp(),
                            "serviceStartedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()

                    db.collection("tickets").document("HW-B102").set(
                        mapOf(
                            "ticketId" to "HW-B102",
                            "userId" to "sim_driver_102",
                            "stationId" to "total_piassa",
                            "driverName" to "Tadesse Haile",
                            "driverPhone" to "0911556677",
                            "vehiclePlate" to "2-A55839 AA",
                            "vehicleCategory" to VehicleCategory.TAXI.name,
                            "fuelType" to FuelType.BENZENE.name,
                            "sequenceNumber" to 102L,
                            "status" to TicketStatus.WAITING.name,
                            "estimatedWaitMinutes" to 3L,
                            "createdAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "stations")
        }
    }

    // Observe all stations in Hawassa with safe fallback
    fun observeStations(): Flow<List<FuelStation>> =
        db.collection("stations")
            .snapshots()
            .map { snapshot ->
                val list = snapshot.documents.map { doc -> doc.toFuelStationSafe() }
                if (list.isNotEmpty()) list else defaultHawassaStations
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, "stations")
                emit(defaultHawassaStations)
            }

    // Observe all station queue states with safe fallback
    fun observeQueueStates(): Flow<Map<String, StationQueueState>> =
        db.collection("queueState")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    val qs = doc.toStationQueueStateSafe()
                    qs.stationId to qs
                }.toMap()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, "queueState")
                emit(emptyMap())
            }

    // Observe active ticket for driver with safe fallback
    fun observeActiveTicket(userId: String): Flow<QueueTicket?> =
        db.collection("tickets")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc -> doc.toQueueTicketSafe() }
                    .firstOrNull { it.status in listOf(
                        TicketStatus.WAITING.name,
                        TicketStatus.APPROACHING.name,
                        TicketStatus.CALLED.name,
                        TicketStatus.SERVING.name
                    ) }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, "tickets")
                emit(null)
            }

    // Observe driver's past ticket history with safe fallback
    fun observeTicketHistory(userId: String): Flow<List<QueueTicket>> =
        db.collection("tickets")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc -> doc.toQueueTicketSafe() }
                    .filter { it.status in listOf(
                        TicketStatus.COMPLETED.name,
                        TicketStatus.CANCELLED.name,
                        TicketStatus.SKIPPED.name
                    ) }
                    .sortedByDescending { it.sequenceNumber }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, "tickets")
                emit(emptyList())
            }

    // Observe driver profile with safe fallback
    fun observeDriverProfile(userId: String): Flow<DriverProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toDriverProfileSafe() else null
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error as? Exception ?: Exception(error), OperationType.READ, "users/$userId")
                emit(null)
            }

    // Save/update driver profile
    suspend fun saveDriverProfile(profile: DriverProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "fullName" to profile.fullName.trim(),
            "phoneNumber" to profile.phoneNumber.trim(),
            "vehiclePlate" to profile.vehiclePlate.trim().uppercase(),
            "vehicleCategory" to profile.vehicleCategory,
            "fuelPreference" to profile.fuelPreference,
            "language" to profile.language,
            "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // Atomic virtual queue booking
    suspend fun bookQueueTicket(
        stationId: String,
        driverName: String,
        driverPhone: String,
        vehiclePlate: String,
        vehicleCategory: String,
        fuelType: FuelType
    ): QueueTicket {
        val uid = requireUserId()
        val queueStateRef = db.collection("queueState").document(stationId)
        val ticketsCollection = db.collection("tickets")

        return db.runTransaction { transaction ->
            val queueSnap = transaction.get(queueStateRef)
            val currentLastSeq = queueSnap.getLong("lastSequenceNumber") ?: 100L
            val currentWaiting = queueSnap.getLong("waitingCount") ?: 0L
            val isPaused = queueSnap.getBoolean("isPaused") ?: false

            if (isPaused) {
                throw IllegalStateException("Queue is currently paused by station management.")
            }

            val nextSeq = currentLastSeq + 1L
            val prefix = if (fuelType == FuelType.BENZENE) "B" else "D"
            val ticketId = "HW-$prefix$nextSeq"

            val avgMinutes = if (fuelType == FuelType.BENZENE) 3L else 4L
            val estimatedWait = (currentWaiting + 1L) * avgMinutes

            // Update queue state atomically
            transaction.update(
                queueStateRef,
                mapOf(
                    "lastSequenceNumber" to nextSeq,
                    "waitingCount" to currentWaiting + 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Create ticket
            val ticketData = mapOf(
                "ticketId" to ticketId,
                "userId" to uid,
                "stationId" to stationId,
                "driverName" to driverName,
                "driverPhone" to driverPhone,
                "vehiclePlate" to vehiclePlate.uppercase(),
                "vehicleCategory" to vehicleCategory,
                "fuelType" to fuelType.name,
                "sequenceNumber" to nextSeq,
                "status" to TicketStatus.WAITING.name,
                "estimatedWaitMinutes" to estimatedWait,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val ticketDocRef = ticketsCollection.document(ticketId)
            transaction.set(ticketDocRef, ticketData)

            QueueTicket(
                ticketId = ticketId,
                userId = uid,
                stationId = stationId,
                driverName = driverName,
                driverPhone = driverPhone,
                vehiclePlate = vehiclePlate.uppercase(),
                vehicleCategory = vehicleCategory,
                fuelType = fuelType.name,
                sequenceNumber = nextSeq,
                status = TicketStatus.WAITING.name,
                estimatedWaitMinutes = estimatedWait
            )
        }.await()
    }

    // Cancel an active ticket
    suspend fun cancelTicket(ticket: QueueTicket) {
        val uid = requireUserId()
        val ticketRef = db.collection("tickets").document(ticket.ticketId)
        val queueStateRef = db.collection("queueState").document(ticket.stationId)

        try {
            db.runTransaction { transaction ->
                val queueSnap = transaction.get(queueStateRef)
                val currentWaiting = queueSnap.getLong("waitingCount") ?: 1L
                val newWaiting = maxOf(0L, currentWaiting - 1L)

                transaction.update(
                    queueStateRef,
                    mapOf(
                        "waitingCount" to newWaiting,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                transaction.update(
                    ticketRef,
                    mapOf(
                        "status" to TicketStatus.CANCELLED.name,
                        "completedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ticketRef.path)
        }
    }

    // Manager / Simulation operations: Call Next Ticket
    suspend fun managerCallNextTicket(stationId: String): QueueTicket? {
        val ticketsRef = db.collection("tickets")
        val queueStateRef = db.collection("queueState").document(stationId)

        return try {
            val nextTicketSnap = ticketsRef
                .whereEqualTo("stationId", stationId)
                .whereEqualTo("status", TicketStatus.WAITING.name)
                .get()
                .await()

            val nextTicketDoc = nextTicketSnap.documents
                .minByOrNull { (it.get("sequenceNumber") as? Number)?.toLong() ?: Long.MAX_VALUE }
                ?: return null

            val ticketId = nextTicketDoc.id
            val plate = nextTicketDoc.getString("vehiclePlate") ?: ""

            db.runTransaction { transaction ->
                transaction.update(
                    ticketsRef.document(ticketId),
                    mapOf(
                        "status" to TicketStatus.CALLED.name,
                        "calledAt" to FieldValue.serverTimestamp()
                    )
                )

                transaction.update(
                    queueStateRef,
                    mapOf(
                        "currentServingTicketId" to ticketId,
                        "currentServingPlate" to plate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            nextTicketDoc.toQueueTicketSafe()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tickets")
            null
        }
    }

    // Manager / Simulation operations: Complete Serving Ticket
    suspend fun managerCompleteService(stationId: String, ticketId: String) {
        val ticketsRef = db.collection("tickets").document(ticketId)
        val queueStateRef = db.collection("queueState").document(stationId)

        try {
            db.runTransaction { transaction ->
                val queueSnap = transaction.get(queueStateRef)
                val currentWaiting = queueSnap.getLong("waitingCount") ?: 1L
                val newWaiting = maxOf(0L, currentWaiting - 1L)

                transaction.update(
                    ticketsRef,
                    mapOf(
                        "status" to TicketStatus.COMPLETED.name,
                        "completedAt" to FieldValue.serverTimestamp()
                    )
                )

                transaction.update(
                    queueStateRef,
                    mapOf(
                        "currentServingTicketId" to null,
                        "currentServingPlate" to null,
                        "waitingCount" to newWaiting,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ticketsRef.path)
        }
    }

    // Manager / Simulation operations: Update station stock
    suspend fun updateStationStock(stationId: String, benzeneStock: FuelStockStatus, dieselStock: FuelStockStatus) {
        try {
            db.collection("stations").document(stationId).update(
                mapOf(
                    "benzeneStock" to benzeneStock.name,
                    "dieselStock" to dieselStock.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "stations/$stationId")
        }
    }
}
