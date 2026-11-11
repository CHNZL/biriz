package com.example.util

import android.content.Context
import com.example.data.model.GroupColorTheme
import com.example.data.model.GroupDistributionConfig
import com.example.data.model.GroupDistributionMode
import com.example.data.model.GroupMakerState
import com.example.data.model.GroupTeam
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Grup Oluşturma Modülü Kalıcı Durum ve Bulut Senkronizasyon Yardımcısı
 *
 * Öğretmenin oluşturduğu grupları, liderleri, grup isimlerini, renk temalarını
 * ve boşta kalan öğrencileri hem yerel önbellekte hem de Firebase Firestore'da canlı olarak saklar.
 */
object GroupMakerPersistenceHelper {
    private const val PREFS_NAME = "biriz_group_maker_storage"
    private const val KEY_PREFIX = "group_state_"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }

    private fun serializeState(state: GroupMakerState): JSONObject {
        return JSONObject().apply {
            val configObj = JSONObject().apply {
                put("mode", state.config.mode.name)
                put("targetGroupCount", state.config.targetGroupCount)
                put("targetStudentsPerGroup", state.config.targetStudentsPerGroup)
                put("balanceGender", state.config.balanceGender)
                val excArray = JSONArray()
                state.config.excludedStudentIds.forEach { excArray.put(it) }
                put("excludedStudentIds", excArray)
            }
            put("config", configObj)

            val groupsArray = JSONArray()
            state.groups.forEach { group ->
                val gObj = JSONObject().apply {
                    put("id", group.id)
                    put("name", group.name)
                    put("colorTheme", group.colorTheme.id)
                    put("leaderStudentId", group.leaderStudentId ?: "")
                    val sArray = JSONArray()
                    group.studentIds.forEach { sArray.put(it) }
                    put("studentIds", sArray)
                }
                groupsArray.put(gObj)
            }
            put("groups", groupsArray)

            val unassignedArray = JSONArray()
            state.unassignedStudentIds.forEach { unassignedArray.put(it) }
            put("unassigned", unassignedArray)

            put("isPresenterMode", state.isPresenterMode)
        }
    }

    private fun deserializeState(json: JSONObject): GroupMakerState {
        val configObj = json.optJSONObject("config")
        val modeName = configObj?.optString("mode", GroupDistributionMode.BY_GROUP_COUNT.name) ?: GroupDistributionMode.BY_GROUP_COUNT.name
        val mode = try { GroupDistributionMode.valueOf(modeName) } catch (_: Exception) { GroupDistributionMode.BY_GROUP_COUNT }
        val targetGroupCount = configObj?.optInt("targetGroupCount", 4) ?: 4
        val targetStudentsPerGroup = configObj?.optInt("targetStudentsPerGroup", 5) ?: 5
        val balanceGender = configObj?.optBoolean("balanceGender", true) ?: true
        val excluded = mutableSetOf<String>()
        val excArray = configObj?.optJSONArray("excludedStudentIds")
        if (excArray != null) {
            for (i in 0 until excArray.length()) {
                excluded.add(excArray.getString(i))
            }
        }

        val config = GroupDistributionConfig(
            mode = mode,
            targetGroupCount = targetGroupCount,
            targetStudentsPerGroup = targetStudentsPerGroup,
            balanceGender = balanceGender,
            excludedStudentIds = excluded
        )

        val groups = mutableListOf<GroupTeam>()
        val groupsArray = json.optJSONArray("groups")
        if (groupsArray != null) {
            for (i in 0 until groupsArray.length()) {
                val gObj = groupsArray.getJSONObject(i)
                val id = gObj.optString("id", "group_$i")
                val name = gObj.optString("name", "${i + 1}. Grup")
                val themeId = gObj.optString("colorTheme", "indigo")
                val theme = GroupColorTheme.fromId(themeId)
                val leaderRaw = gObj.optString("leaderStudentId", "")
                val leader = leaderRaw.ifBlank { null }

                val sList = mutableListOf<String>()
                val sArray = gObj.optJSONArray("studentIds")
                if (sArray != null) {
                    for (j in 0 until sArray.length()) {
                        sList.add(sArray.getString(j))
                    }
                }

                groups.add(
                    GroupTeam(
                        id = id,
                        name = name,
                        colorTheme = theme,
                        studentIds = sList,
                        leaderStudentId = leader
                    )
                )
            }
        }

        val unassigned = mutableListOf<String>()
        val unassignedArray = json.optJSONArray("unassigned")
        if (unassignedArray != null) {
            for (i in 0 until unassignedArray.length()) {
                unassigned.add(unassignedArray.getString(i))
            }
        }

        val isPresenter = json.optBoolean("isPresenterMode", false)

        return GroupMakerState(
            config = config,
            groups = groups,
            unassignedStudentIds = unassigned,
            isPresenterMode = isPresenter
        )
    }

    fun saveState(
        context: Context,
        classId: String,
        state: GroupMakerState
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = serializeState(state).toString()
            val editor = prefs.edit()
            if (classId.isNotBlank()) {
                editor.putString(KEY_PREFIX + classId, jsonStr)
            }
            editor.putString(KEY_PREFIX + "default", jsonStr)
            editor.apply()

            if (classId.isNotBlank() && classId != "default") {
                syncScope.launch {
                    try {
                        val db = getFirestore(context) ?: return@launch
                        db.collection("classrooms").document(classId)
                            .collection("groups").document("current")
                            .set(
                                mapOf(
                                    "classId" to classId,
                                    "groupsJson" to jsonStr,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            )
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadState(context: Context, classId: String): GroupMakerState? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = (if (classId.isNotBlank()) prefs.getString(KEY_PREFIX + classId, null) else null)
                ?: prefs.getString(KEY_PREFIX + "default", null)
                ?: return null
            return deserializeState(JSONObject(jsonStr))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun observeFirestoreGroups(
        context: Context,
        classId: String,
        onUpdate: (GroupMakerState) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return try {
            db.collection("classrooms").document(classId)
                .collection("groups").document("current")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    if (!snapshot.exists()) {
                        val local = loadState(context, classId)
                        if (local != null && local.groups.isNotEmpty()) {
                            saveState(context, classId, local)
                        }
                        return@addSnapshotListener
                    }
                    try {
                        val groupsJson = snapshot.getString("groupsJson")
                        if (!groupsJson.isNullOrBlank()) {
                            val state = deserializeState(JSONObject(groupsJson))
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString(KEY_PREFIX + classId, groupsJson)
                                .putString(KEY_PREFIX + "default", groupsJson)
                                .apply()
                            onUpdate(state)
                        } else {
                            val local = loadState(context, classId)
                            if (local != null && local.groups.isNotEmpty()) {
                                saveState(context, classId, local)
                            }
                        }
                    } catch (_: Exception) {}
                }
        } catch (_: Exception) {
            null
        }
    }
}
