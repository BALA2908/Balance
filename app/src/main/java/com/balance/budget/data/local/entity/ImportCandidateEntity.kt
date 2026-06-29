package com.balance.budget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A parsed-but-unconfirmed transaction from a UPI/bank notification (Phase 4
 * auto-import). These are staged here and shown in a review queue — never
 * inserted as real expenses until the user confirms. No foreign key on
 * [suggestedCategoryId] (it's only a hint) so this stays a simple staging table.
 *
 * Introduced in schema v2 (see Migration_1_2).
 */
@Entity(tableName = "import_candidates")
data class ImportCandidateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val merchant: String? = null,
    @ColumnInfo(name = "raw_text") val rawText: String,
    @ColumnInfo(name = "source_app") val sourceApp: String,
    @ColumnInfo(name = "posted_at") val postedAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "suggested_category_id") val suggestedCategoryId: Long? = null,
)
