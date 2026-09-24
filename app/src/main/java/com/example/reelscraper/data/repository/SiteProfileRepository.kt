package com.example.reelscraper.data.repository

import com.example.reelscraper.data.local.SiteProfileDao
import com.example.reelscraper.data.model.ExtractionRule
import com.example.reelscraper.data.model.SiteProfile
import kotlinx.coroutines.flow.Flow

interface SiteProfileRepository {
    fun getAllProfiles(): Flow<List<SiteProfile>>
    suspend fun getProfileForDomain(domain: String): SiteProfile?
    suspend fun saveProfile(profile: SiteProfile): Long
    suspend fun deleteProfile(profile: SiteProfile)
    suspend fun deleteProfileById(id: Long)

    fun getAllRules(): Flow<List<ExtractionRule>>
    suspend fun getRulesForDomain(domain: String): List<ExtractionRule>
    suspend fun saveRule(rule: ExtractionRule): Long
    suspend fun deleteRule(rule: ExtractionRule)
    suspend fun deleteRuleById(id: Long)
    suspend fun seedDefaultProfiles()
}

class SiteProfileRepositoryImpl(
    private val dao: SiteProfileDao
) : SiteProfileRepository {

    override fun getAllProfiles(): Flow<List<SiteProfile>> = dao.getAllProfiles()

    override suspend fun getProfileForDomain(domain: String): SiteProfile? =
        dao.getProfileForDomain(domain)

    override suspend fun saveProfile(profile: SiteProfile): Long =
        dao.insertProfile(profile)

    override suspend fun deleteProfile(profile: SiteProfile) =
        dao.deleteProfile(profile)

    override suspend fun deleteProfileById(id: Long) =
        dao.deleteProfileById(id)

    override fun getAllRules(): Flow<List<ExtractionRule>> = dao.getAllRules()

    override suspend fun getRulesForDomain(domain: String): List<ExtractionRule> =
        dao.getRulesForDomain(domain)

    override suspend fun saveRule(rule: ExtractionRule): Long =
        dao.insertRule(rule)

    override suspend fun deleteRule(rule: ExtractionRule) =
        dao.deleteRule(rule)

    override suspend fun deleteRuleById(id: Long) =
        dao.deleteRuleById(id)

    override suspend fun seedDefaultProfiles() {
        val defaultProfiles = listOf(
            SiteProfile(
                domain = "vimeo.com",
                isEnabled = true,
                defaultDepth = 2,
                enableDynamicStreaming = true,
                dynamicMode = "AUTOMATIC",
                enableWebViewFallback = true
            ),
            SiteProfile(
                domain = "dailymotion.com",
                isEnabled = true,
                defaultDepth = 2,
                enableDynamicStreaming = true,
                dynamicMode = "AUTOMATIC",
                enableWebViewFallback = true
            ),
            SiteProfile(
                domain = "twitch.tv",
                isEnabled = true,
                defaultDepth = 1,
                enableDynamicStreaming = true,
                dynamicMode = "AUTOMATIC",
                enableWebViewFallback = true
            ),
            SiteProfile(
                domain = "archive.org",
                isEnabled = true,
                defaultDepth = 3,
                enableHtmlExtraction = true,
                enableDynamicStreaming = false
            ),
            SiteProfile(
                domain = "wikimedia.org",
                isEnabled = true,
                defaultDepth = 2,
                enableHtmlExtraction = true,
                enableDynamicStreaming = false
            )
        )
        dao.insertProfiles(defaultProfiles)
    }
}
