package com.aistudio.lioracamward.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scans ORDER BY startedAt DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: String): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteScanById(id: String)

    @Query("DELETE FROM scans")
    suspend fun clearAllScans()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<FindingEntity>)

    @Query("SELECT * FROM findings WHERE scanId = :scanId ORDER BY timestamp ASC")
    fun getFindingsForScan(scanId: String): Flow<List<FindingEntity>>

    @Query("SELECT * FROM findings WHERE scanId = :scanId ORDER BY timestamp ASC")
    suspend fun getFindingsListForScan(scanId: String): List<FindingEntity>
}
