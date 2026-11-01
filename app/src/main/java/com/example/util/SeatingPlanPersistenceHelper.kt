package com.example.util

import android.content.Context
import com.example.data.model.ArchivedSeatingPlan
import com.example.data.model.DeskPosition
import com.example.data.model.DoorPosition
import com.example.data.model.SeatingConfig
import com.example.data.model.SeatingPlanState
import com.example.data.model.WindowPosition
import com.example.R
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Akıllı Sınıf Oturma Planı ve Arşivlenen Planlar için Yerel Kalıcı Depolama ve Bulut Senkronizasyon Yardımcısı.
 * - Aktif planı saklar ve geri yükler.
 * - Firebase Firestore ile buluta senkronize eder.
 * - Tarih ve saat damgalı plan arşivini yönetir.
 */
object SeatingPlanPersistenceHelper {

    private const val PREFS_NAME = "biriz_seating_storage"
    private const val KEY_ACTIVE_PREFIX = "active_plan_"
    private const val KEY_ARCHIVE_PREFIX = "archived_plans_"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (_: Exception) {
            null
        }
    }

    fun generateDefaultPlanName(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
        return "${sdf.format(Date())} Planı"
    }

    private fun serializePlanState(state: SeatingPlanState): JSONObject {
        return JSONObject().apply {
            put("activePlanName", state.activePlanName)

            // Config
            val configObj = JSONObject().apply {
                put("groupCount", state.config.groupCount)
                put("peoplePerRow", state.config.peoplePerRow)
                val rowsArray = JSONArray()
                state.config.rowsPerGroup.forEach { rowsArray.put(it) }
                put("rowsPerGroup", rowsArray)
                put("windowPosition", state.config.windowPosition.name)
                put("doorPosition", state.config.doorPosition.name)
                put("deskPosition", state.config.deskPosition.name)
            }
            put("config", configObj)

            // Assignments (seatId -> studentId)
            val assignObj = JSONObject()
            state.assignments.forEach { (k, v) -> assignObj.put(k, v) }
            put("assignments", assignObj)

            // Pinned students
            val pinnedArray = JSONArray()
            state.pinnedStudents.forEach { pinnedArray.put(it) }
            put("pinnedStudents", pinnedArray)

            // Priority students
            val priorityArray = JSONArray()
            state.priorityStudents.forEach { priorityArray.put(it) }
            put("priorityStudents", priorityArray)
        }
    }

    fun deserializeConfig(configObj: JSONObject?): SeatingConfig {
        if (configObj == null) return SeatingConfig()
        val groupCount = configObj.optInt("groupCount", 3)
        val peoplePerRow = configObj.optInt("peoplePerRow", 2)
        val rowsArray = configObj.optJSONArray("rowsPerGroup")
        val rowsList = mutableListOf<Int>()
        if (rowsArray != null) {
            for (i in 0 until rowsArray.length()) {
                rowsList.add(rowsArray.getInt(i))
            }
        } else {
            rowsList.addAll(listOf(5, 5, 5))
        }

        val winPos = try {
            WindowPosition.valueOf(configObj.optString("windowPosition", WindowPosition.LEFT.name))
        } catch (e: Exception) {
            WindowPosition.LEFT
        }
        val doorPos = try {
            DoorPosition.valueOf(configObj.optString("doorPosition", DoorPosition.SAG_ON.name))
        } catch (e: Exception) {
            DoorPosition.SAG_ON
        }
        val deskPos = try {
            DeskPosition.valueOf(configObj.optString("deskPosition", DeskPosition.LEFT.name))
        } catch (e: Exception) {
            DeskPosition.LEFT
        }

        return SeatingConfig(
            groupCount = groupCount,
            peoplePerRow = peoplePerRow,
            rowsPerGroup = rowsList,
            windowPosition = winPos,
            doorPosition = doorPos,
            deskPosition = deskPos
        )
    }

    private fun serializeConfig(config: SeatingConfig): JSONObject {
        return JSONObject().apply {
            put("groupCount", config.groupCount)
            put("peoplePerRow", config.peoplePerRow)
            val rowsArray = JSONArray()
            config.rowsPerGroup.forEach { rowsArray.put(it) }
            put("rowsPerGroup", rowsArray)
            put("windowPosition", config.windowPosition.name)
            put("doorPosition", config.doorPosition.name)
            put("deskPosition", config.deskPosition.name)
        }
    }

    fun saveSeatingConfig(context: Context, classId: String, config: SeatingConfig) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = serializeConfig(config).toString()
            val editor = prefs.edit()
            if (classId.isNotBlank()) {
                editor.putString("config_" + classId, jsonStr)
            }
            editor.putString("config_default", jsonStr)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSeatingConfig(context: Context, classId: String): SeatingConfig? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = (if (classId.isNotBlank()) prefs.getString("config_" + classId, null) else null)
                ?: prefs.getString("config_default", null)
                ?: return null
            return deserializeConfig(JSONObject(jsonStr))
        } catch (e: Exception) {
            return null
        }
    }

    private fun deserializePlanState(json: JSONObject): SeatingPlanState {
        val activeName = json.optString("activePlanName", "Varsayılan Plan")
        val config = deserializeConfig(json.optJSONObject("config"))

        val assignObj = json.optJSONObject("assignments")
        val assignments = mutableMapOf<String, String>()
        if (assignObj != null) {
            val keys = assignObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                assignments[key] = assignObj.getString(key)
            }
        }

        val pinnedArray = json.optJSONArray("pinnedStudents")
        val pinned = mutableSetOf<String>()
        if (pinnedArray != null) {
            for (i in 0 until pinnedArray.length()) {
                pinned.add(pinnedArray.getString(i))
            }
        }

        val priorityArray = json.optJSONArray("priorityStudents")
        val priority = mutableSetOf<String>()
        if (priorityArray != null) {
            for (i in 0 until priorityArray.length()) {
                priority.add(priorityArray.getString(i))
            }
        }

        return SeatingPlanState(
            config = config,
            assignments = assignments,
            pinnedStudents = pinned,
            priorityStudents = priority,
            activePlanName = activeName
        )
    }

    fun saveActivePlan(context: Context, classId: String, state: SeatingPlanState) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = serializePlanState(state)
            val jsonStr = json.toString()
            val editor = prefs.edit()
            if (classId.isNotBlank()) {
                editor.putString(KEY_ACTIVE_PREFIX + classId, jsonStr)
            }
            editor.putString(KEY_ACTIVE_PREFIX + "default", jsonStr)
            editor.apply()

            if (classId.isNotBlank() && classId != "default" && Firebase.auth.currentUser != null) {
                syncScope.launch {
                    try {
                        val db = getFirestore(context) ?: return@launch
                        val docRef = db.collection("classrooms").document(classId).collection("seating").document("current")
                        val payload = mapOf(
                            "classId" to classId,
                            "name" to state.activePlanName,
                            "seatsJson" to jsonStr,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        docRef.set(payload)
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.WRITE, "classrooms/$classId/seating/current")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncActivePlanFromFirestore(context: Context, classId: String, onLoaded: (SeatingPlanState) -> Unit) {
        if (classId.isBlank() || classId == "default" || Firebase.auth.currentUser == null) return
        val db = getFirestore(context) ?: return
        db.collection("classrooms").document(classId).collection("seating").document("current")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val seatsJson = snapshot.getString("seatsJson")
                    if (!seatsJson.isNullOrBlank()) {
                        try {
                            val state = deserializePlanState(JSONObject(seatsJson))
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            prefs.edit().putString(KEY_ACTIVE_PREFIX + classId, seatsJson).apply()
                            onLoaded(state)
                        } catch (_: Exception) {}
                    }
                }
            }
    }

    fun loadActivePlan(context: Context, classId: String): SeatingPlanState? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = (if (classId.isNotBlank()) prefs.getString(KEY_ACTIVE_PREFIX + classId, null) else null)
                ?: prefs.getString(KEY_ACTIVE_PREFIX + "default", null)
                ?: return null

            return deserializePlanState(JSONObject(jsonStr))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getArchivedPlans(context: Context, classId: String): List<ArchivedSeatingPlan> {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = if (classId.isNotBlank()) KEY_ARCHIVE_PREFIX + classId else KEY_ARCHIVE_PREFIX + "default"
            val jsonStr = prefs.getString(key, null) ?: return emptyList()

            val array = JSONArray(jsonStr)
            val list = mutableListOf<ArchivedSeatingPlan>()
            for (i in 0 until array.length()) {
                val itemObj = array.getJSONObject(i)
                val id = itemObj.optString("id", java.util.UUID.randomUUID().toString())
                val name = itemObj.optString("name", "Arşiv Planı")
                val createdAt = itemObj.optLong("createdAtMillis", System.currentTimeMillis())
                val planObj = itemObj.getJSONObject("planState")
                val state = deserializePlanState(planObj)
                list.add(ArchivedSeatingPlan(id = id, name = name, createdAtMillis = createdAt, planState = state))
            }
            return list.sortedByDescending { it.createdAtMillis }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun saveArchivedPlan(context: Context, classId: String, plan: ArchivedSeatingPlan) {
        try {
            val currentList = getArchivedPlans(context, classId).toMutableList()
            // Eğer aynı ID varsa güncelle, yoksa başa ekle
            val idx = currentList.indexOfFirst { it.id == plan.id }
            if (idx >= 0) {
                currentList[idx] = plan
            } else {
                currentList.add(0, plan)
            }

            persistArchiveList(context, classId, currentList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteArchivedPlan(context: Context, classId: String, planId: String) {
        try {
            val currentList = getArchivedPlans(context, classId).filter { it.id != planId }
            persistArchiveList(context, classId, currentList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteMultipleArchivedPlans(context: Context, classId: String, planIds: Set<String>) {
        try {
            val currentList = getArchivedPlans(context, classId).filter { !planIds.contains(it.id) }
            persistArchiveList(context, classId, currentList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAllArchivedPlans(context: Context, classId: String) {
        try {
            persistArchiveList(context, classId, emptyList())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistArchiveList(context: Context, classId: String, list: List<ArchivedSeatingPlan>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { plan ->
            val obj = JSONObject().apply {
                put("id", plan.id)
                put("name", plan.name)
                put("createdAtMillis", plan.createdAtMillis)
                put("planState", serializePlanState(plan.planState))
            }
            array.put(obj)
        }
        val key = if (classId.isNotBlank()) KEY_ARCHIVE_PREFIX + classId else KEY_ARCHIVE_PREFIX + "default"
        prefs.edit().putString(key, array.toString()).apply()
    }
}
