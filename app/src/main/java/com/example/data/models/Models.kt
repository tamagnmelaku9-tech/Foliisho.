package com.example.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class FuelType {
    BENZENE,
    DIESEL
}

enum class FuelStockStatus {
    AVAILABLE,
    LOW_STOCK,
    OUT_OF_STOCK
}

enum class StationOperatingStatus {
    OPEN,
    PAUSED,
    CLOSED
}

enum class TicketStatus {
    WAITING,
    APPROACHING,
    CALLED,
    SERVING,
    COMPLETED,
    SKIPPED,
    CANCELLED
}

enum class VehicleCategory {
    BAJAJ,
    TAXI,
    MINIBUS,
    PRIVATE_CAR,
    OTHER
}

@IgnoreExtraProperties
data class DriverProfile(
    @DocumentId val userId: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val vehiclePlate: String = "",
    val vehicleCategory: String = VehicleCategory.BAJAJ.name,
    val fuelPreference: String = FuelType.BENZENE.name,
    val language: String = "am",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FuelStation(
    @DocumentId val stationId: String = "",
    val name: String = "",
    val nameAmharic: String = "",
    val address: String = "",
    val addressAmharic: String = "",
    val latitude: Double = 7.0504,
    val longitude: Double = 38.4716,
    val operatingStatus: String = StationOperatingStatus.OPEN.name,
    val benzeneStock: String = FuelStockStatus.AVAILABLE.name,
    val dieselStock: String = FuelStockStatus.AVAILABLE.name,
    val avgServiceMinutesBenzene: Long = 3L,
    val avgServiceMinutesDiesel: Long = 4L,
    val queueCapacity: Long = 100L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QueueTicket(
    @DocumentId val ticketId: String = "",
    val userId: String = "",
    val stationId: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val vehiclePlate: String = "",
    val vehicleCategory: String = VehicleCategory.BAJAJ.name,
    val fuelType: String = FuelType.BENZENE.name,
    val sequenceNumber: Long = 0L,
    val status: String = TicketStatus.WAITING.name,
    val estimatedWaitMinutes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val calledAt: Timestamp? = null,
    @ServerTimestamp val serviceStartedAt: Timestamp? = null,
    @ServerTimestamp val completedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StationQueueState(
    @DocumentId val stationId: String = "",
    val currentServingTicketId: String? = null,
    val currentServingPlate: String? = null,
    val lastSequenceNumber: Long = 0L,
    val waitingCount: Long = 0L,
    val isPaused: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Safe, crash-resilient deserializers preventing Long-to-Int reflection crashes or null attribute faults
fun DocumentSnapshot.toFuelStationSafe(): FuelStation {
    return try {
        FuelStation(
            stationId = id.ifBlank { getString("stationId") ?: "st_${System.currentTimeMillis()}" },
            name = getString("name") ?: "Station",
            nameAmharic = getString("nameAmharic") ?: "",
            address = getString("address") ?: "Hawassa",
            addressAmharic = getString("addressAmharic") ?: "",
            latitude = (get("latitude") as? Number)?.toDouble() ?: 7.0504,
            longitude = (get("longitude") as? Number)?.toDouble() ?: 38.4716,
            operatingStatus = getString("operatingStatus") ?: StationOperatingStatus.OPEN.name,
            benzeneStock = getString("benzeneStock") ?: FuelStockStatus.AVAILABLE.name,
            dieselStock = getString("dieselStock") ?: FuelStockStatus.AVAILABLE.name,
            avgServiceMinutesBenzene = (get("avgServiceMinutesBenzene") as? Number)?.toLong() ?: 3L,
            avgServiceMinutesDiesel = (get("avgServiceMinutesDiesel") as? Number)?.toLong() ?: 4L,
            queueCapacity = (get("queueCapacity") as? Number)?.toLong() ?: 100L,
            updatedAt = try { getTimestamp("updatedAt") } catch (_: Throwable) { null }
        )
    } catch (_: Throwable) {
        FuelStation(stationId = id.ifBlank { "hawassa_station" }, name = "Hawassa Fuel Station")
    }
}

fun DocumentSnapshot.toQueueTicketSafe(): QueueTicket {
    return try {
        QueueTicket(
            ticketId = id.ifBlank { getString("ticketId") ?: "HW-000" },
            userId = getString("userId") ?: "",
            stationId = getString("stationId") ?: "",
            driverName = getString("driverName") ?: "Driver",
            driverPhone = getString("driverPhone") ?: "",
            vehiclePlate = getString("vehiclePlate") ?: "--",
            vehicleCategory = getString("vehicleCategory") ?: VehicleCategory.BAJAJ.name,
            fuelType = getString("fuelType") ?: FuelType.BENZENE.name,
            sequenceNumber = (get("sequenceNumber") as? Number)?.toLong() ?: 0L,
            status = getString("status") ?: TicketStatus.WAITING.name,
            estimatedWaitMinutes = (get("estimatedWaitMinutes") as? Number)?.toLong() ?: 0L,
            createdAt = try { getTimestamp("createdAt") } catch (_: Throwable) { null },
            calledAt = try { getTimestamp("calledAt") } catch (_: Throwable) { null },
            serviceStartedAt = try { getTimestamp("serviceStartedAt") } catch (_: Throwable) { null },
            completedAt = try { getTimestamp("completedAt") } catch (_: Throwable) { null }
        )
    } catch (_: Throwable) {
        QueueTicket(ticketId = id.ifBlank { "HW-ERR" })
    }
}

fun DocumentSnapshot.toStationQueueStateSafe(): StationQueueState {
    return try {
        StationQueueState(
            stationId = id.ifBlank { getString("stationId") ?: "" },
            currentServingTicketId = getString("currentServingTicketId"),
            currentServingPlate = getString("currentServingPlate"),
            lastSequenceNumber = (get("lastSequenceNumber") as? Number)?.toLong() ?: 100L,
            waitingCount = (get("waitingCount") as? Number)?.toLong() ?: 0L,
            isPaused = getBoolean("isPaused") ?: false,
            updatedAt = try { getTimestamp("updatedAt") } catch (_: Throwable) { null }
        )
    } catch (_: Throwable) {
        StationQueueState(stationId = id)
    }
}

fun DocumentSnapshot.toDriverProfileSafe(): DriverProfile {
    return try {
        DriverProfile(
            userId = id.ifBlank { getString("userId") ?: "" },
            fullName = getString("fullName") ?: "Hawassa Driver",
            phoneNumber = getString("phoneNumber") ?: "0911223344",
            vehiclePlate = getString("vehiclePlate") ?: "3-A12345 SD",
            vehicleCategory = getString("vehicleCategory") ?: VehicleCategory.BAJAJ.name,
            fuelPreference = getString("fuelPreference") ?: FuelType.BENZENE.name,
            language = getString("language") ?: "am",
            createdAt = try { getTimestamp("createdAt") } catch (_: Throwable) { null },
            updatedAt = try { getTimestamp("updatedAt") } catch (_: Throwable) { null }
        )
    } catch (_: Throwable) {
        DriverProfile(userId = id)
    }
}
