package com.example.util

import android.content.Context
import com.example.data.model.GroupColorTheme
import com.example.data.model.GroupDistributionConfig
import com.example.data.model.GroupDistributionMode
import com.example.data.model.GroupMakerState
import com.example.data.model.GroupTeam
import org.json.JSONArray
import org.json.JSONObject

/**
 * Grup Oluşturma Modülü Kalıcı Durum Yardımcısı
 *
 * Öğretmenin oluşturduğu grupları, liderleri, grup isimlerini, renk temalarını
 * ve boşta kalan öğrencileri SharedPreferences üzerinde kalıcı olarak saklar.
 * Kullanıcı sayfaya çıkıp tekrar girse veya uygulama yeniden açılsa dahi
 * son bıraktığı grup düzeni korunur.
 */
object GroupMakerPersistenceHelper {
    private const val PREFS_NAME = "biriz_group_maker_storage"
    private const val KEY_PREFIX = "group_state_"

    fun saveState(
        context: Context,
        classId: String,
        state: GroupMakerState
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject().apply {
                // Config
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

                // Groups
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

                // Unassigned
                val unassignedArray = JSONArray()
                state.unassignedStudentIds.forEach { unassignedArray.put(it) }
                put("unassigned", unassignedArray)

                put("isPresenterMode", state.isPresenterMode)
            }

            val key = if (classId.isNotBlank()) KEY_PREFIX + classId else KEY_PREFIX + "default"
            prefs.edit().putString(key, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadState(context: Context, classId: String): GroupMakerState? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = if (classId.isNotBlank()) KEY_PREFIX + classId else KEY_PREFIX + "default"
            val jsonStr = prefs.getString(key, null) ?: return null
            val json = JSONObject(jsonStr)

            // Config
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

            // Groups
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

            // Unassigned
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
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
