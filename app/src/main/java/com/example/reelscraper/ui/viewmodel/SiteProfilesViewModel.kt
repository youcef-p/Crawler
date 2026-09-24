package com.example.reelscraper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscraper.data.model.ExtractionRule
import com.example.reelscraper.data.model.SiteProfile
import com.example.reelscraper.data.repository.SiteProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SiteProfilesViewModel(
    private val repository: SiteProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<SiteProfile>> = repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules: StateFlow<List<ExtractionRule>> = repository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultProfiles()
        }
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun saveProfile(profile: SiteProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            _snackMessage.value = "Saved profile for ${profile.domain}"
        }
    }

    fun toggleProfile(profile: SiteProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile.copy(isEnabled = !profile.isEnabled))
        }
    }

    fun deleteProfile(profile: SiteProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            _snackMessage.value = "Deleted profile for ${profile.domain}"
        }
    }

    fun duplicateProfile(profile: SiteProfile) {
        viewModelScope.launch {
            val copy = profile.copy(
                id = 0,
                domain = "copy.${profile.domain}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProfile(copy)
            _snackMessage.value = "Duplicated profile for ${profile.domain}"
        }
    }

    fun saveRule(rule: ExtractionRule) {
        viewModelScope.launch {
            repository.saveRule(rule)
            _snackMessage.value = "Saved ${rule.ruleType} rule for ${rule.domain}"
        }
    }

    fun deleteRule(rule: ExtractionRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
            _snackMessage.value = "Deleted rule for ${rule.domain}"
        }
    }

    fun exportProfilesJson(): String {
        val list = profiles.value
        val arr = JSONArray()
        for (p in list) {
            val obj = JSONObject()
            obj.put("domain", p.domain)
            obj.put("isEnabled", p.isEnabled)
            obj.put("defaultDepth", p.defaultDepth)
            obj.put("maxLinksPerPage", p.maxLinksPerPage)
            obj.put("enableDynamicStreaming", p.enableDynamicStreaming)
            arr.put(obj)
        }
        return arr.toString(2)
    }

    fun importProfilesJson(json: String): Int {
        var count = 0
        viewModelScope.launch {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val profile = SiteProfile(
                        domain = obj.getString("domain"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        defaultDepth = obj.optInt("defaultDepth", 2),
                        maxLinksPerPage = obj.optInt("maxLinksPerPage", 100),
                        enableDynamicStreaming = obj.optBoolean("enableDynamicStreaming", true)
                    )
                    repository.saveProfile(profile)
                    count++
                }
                _snackMessage.value = "Imported $count site profiles"
            } catch (e: Exception) {
                _snackMessage.value = "Failed to import profiles: ${e.message}"
            }
        }
        return count
    }
}
