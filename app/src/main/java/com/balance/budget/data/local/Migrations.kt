package com.balance.budget.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations. Financial data is never destructively dropped — each schema
 * change ships a real migration here and is verified by MigrationTest against the
 * exported schema JSON.
 *
 * v1 → v2: adds the `import_candidates` staging table for auto-import (Phase 4).
 * v2 → v3: adds the `budget_adjustments` ledger (move-budget-between-categories,
 *          feeds rollover) and back-fills the new "Sports" default category for
 *          installs that were seeded before Sports existed.
 * The CREATE must match Room's generated schema exactly (column order/types);
 * MigrationTest validates this.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `import_candidates` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`amount_minor` INTEGER NOT NULL, " +
                "`merchant` TEXT, " +
                "`raw_text` TEXT NOT NULL, " +
                "`source_app` TEXT NOT NULL, " +
                "`posted_at` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`suggested_category_id` INTEGER)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // New ledger table (CREATE matches Room's generated style byte-for-byte).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budget_adjustments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`ym` INTEGER NOT NULL, " +
                "`from_category_id` INTEGER, " +
                "`to_category_id` INTEGER, " +
                "`amount_minor` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_budget_adjustments_ym` " +
                "ON `budget_adjustments` (`ym`)"
        )

        // Back-fill "Sports" for older installs — but only if the user hasn't
        // already made a category by that name, so we never clobber or duplicate.
        // New installs get it from DefaultCategories instead; this guard keeps the
        // two paths from ever producing two "Sports".
        db.execSQL(
            "INSERT INTO `categories` " +
                "(`name`, `icon_key`, `color_hex`, `is_default`, `is_archived`, `sort_order`) " +
                "SELECT 'Sports', 'sports', '#7FB069', 1, 0, 9 " +
                "WHERE NOT EXISTS (SELECT 1 FROM `categories` WHERE `name` = 'Sports')"
        )
    }
}
