package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A point-in-time record of total net worth (Σ of wallet balances), captured when
 * the user taps "record". Powers the balance-over-time chart. Manual by design —
 * we never silently fabricate wealth history.
 */
@Entity(tableName = "account_balance_snapshots")
data class BalanceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    @ColumnInfo(name = "net_worth_minor") val netWorthMinor: Long,
)
