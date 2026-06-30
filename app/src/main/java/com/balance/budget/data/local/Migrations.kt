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
 * v3 → v4: adds the `accounts` (wallet/payment-method) table with the default
 *          set seeded, and a nullable `expenses.account_id` FK (SET NULL). Existing
 *          expenses are back-filled to the default "Cash" account via a table
 *          rebuild (SQLite can't add a FK column in place cleanly).
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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. accounts table (CREATE matches Room's generated v4 schema).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `accounts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`icon_key` TEXT NOT NULL, " +
                "`color_hex` TEXT NOT NULL, " +
                "`opening_balance_minor` INTEGER, " +
                "`is_default` INTEGER NOT NULL, " +
                "`is_archived` INTEGER NOT NULL, " +
                "`sort_order` INTEGER NOT NULL)"
        )

        // 2. Seed the same default wallets a fresh install gets (Cash is default).
        val acc = "INSERT INTO `accounts` " +
            "(`name`,`type`,`icon_key`,`color_hex`,`opening_balance_minor`,`is_default`,`is_archived`,`sort_order`) VALUES "
        db.execSQL(acc + "('Cash','CASH','cash','#8FB996',NULL,1,0,0)")
        db.execSQL(acc + "('Bank','BANK','bank','#7E97C9',NULL,0,0,1)")
        db.execSQL(acc + "('UPI','WALLET','upi','#F0A868',NULL,0,0,2)")
        db.execSQL(acc + "('Card','CARD','card','#B98BC9',NULL,0,0,3)")

        // 3. Rebuild expenses with the new account_id FK column, back-filling every
        //    existing expense to the default Cash account. (SQLite can't add a FK
        //    column in place, so we copy → drop → rename — the canonical Room recipe.)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expenses_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`amount_minor` INTEGER NOT NULL, " +
                "`category_id` INTEGER NOT NULL, " +
                "`note` TEXT, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`merchant` TEXT, " +
                "`account_id` INTEGER, " +
                "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL(
            "INSERT INTO `expenses_new` " +
                "(`id`,`amount_minor`,`category_id`,`note`,`timestamp`,`created_at`,`source`,`merchant`,`account_id`) " +
                "SELECT `id`,`amount_minor`,`category_id`,`note`,`timestamp`,`created_at`,`source`,`merchant`," +
                "(SELECT `id` FROM `accounts` WHERE `is_default` = 1 LIMIT 1) FROM `expenses`"
        )
        db.execSQL("DROP TABLE `expenses`")
        db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category_id` ON `expenses` (`category_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_timestamp` ON `expenses` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_account_id` ON `expenses` (`account_id`)")
    }
}

/** v4 → v5: tags + the expense↔tag junction (both additive, no data touched). */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`color_hex` TEXT NOT NULL, " +
                "`sort_order` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expense_tags` (" +
                "`expense_id` INTEGER NOT NULL, " +
                "`tag_id` INTEGER NOT NULL, " +
                "PRIMARY KEY(`expense_id`, `tag_id`), " +
                "FOREIGN KEY(`expense_id`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_tags_tag_id` ON `expense_tags` (`tag_id`)")
    }
}

/** v5 → v6: user-defined auto-categorization rules (additive). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `category_rules` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`pattern` TEXT NOT NULL, " +
                "`category_id` INTEGER NOT NULL, " +
                "`sort_order` INTEGER NOT NULL, " +
                "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_rules_category_id` ON `category_rules` (`category_id`)")
    }
}

/** v6 → v7: manual savings goals (additive). */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `savings_goals` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`icon_key` TEXT NOT NULL, " +
                "`color_hex` TEXT NOT NULL, " +
                "`target_minor` INTEGER NOT NULL, " +
                "`saved_minor` INTEGER NOT NULL, " +
                "`sort_order` INTEGER NOT NULL)"
        )
    }
}
