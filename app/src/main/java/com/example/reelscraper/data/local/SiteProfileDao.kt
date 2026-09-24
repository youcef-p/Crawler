package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.reelscraper.data.model.ExtractionRule
import com.example.reelscraper.data.model.SiteProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteProfileDao {
    // Site Profile queries
    @Query("SELECT * FROM site_profiles ORDER BY domain ASC")
    fun getAllProfiles(): Flow<List<SiteProfile>>

    @Query("SELECT * FROM site_profiles WHERE isEnabled = 1")
    suspend fun getEnabledProfiles(): List<SiteProfile>

    @Query("SELECT * FROM site_profiles WHERE domain = :domain LIMIT 1")
    suspend fun getProfileForDomain(domain: String): SiteProfile?

    @Query("SELECT * FROM site_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): SiteProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SiteProfile): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfiles(profiles: List<SiteProfile>): List<Long>

    @Update
    suspend fun updateProfile(profile: SiteProfile)

    @Delete
    suspend fun deleteProfile(profile: SiteProfile)

    @Query("DELETE FROM site_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    // Extraction Rule queries
    @Query("SELECT * FROM extraction_rules WHERE domain = :domain AND enabled = 1 ORDER BY priority DESC")
    suspend fun getRulesForDomain(domain: String): List<ExtractionRule>

    @Query("SELECT * FROM extraction_rules ORDER BY domain ASC, priority DESC")
    fun getAllRules(): Flow<List<ExtractionRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ExtractionRule): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRules(rules: List<ExtractionRule>): List<Long>

    @Update
    suspend fun updateRule(rule: ExtractionRule)

    @Delete
    suspend fun deleteRule(rule: ExtractionRule)

    @Query("DELETE FROM extraction_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM extraction_rules WHERE domain = :domain")
    suspend fun deleteRulesForDomain(domain: String)
}
