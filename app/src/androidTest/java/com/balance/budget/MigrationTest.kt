package com.balance.budget

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.balance.budget.data.local.BudgetDatabase
import com.balance.budget.data.local.MIGRATION_1_2
import com.balance.budget.data.local.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the v1 → v2 migration (adds `import_candidates`) preserves existing
 * financial data and produces the schema Room expects. Runs against the framework
 * SQLite via [MigrationTestHelper] (the migration SQL is identical under SQLCipher).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BudgetDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesDataAndAddsTable() {
        // Seed a v1 database with a category and an expense.
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO categories (id, name, icon_key, color_hex, is_default, is_archived, sort_order) " +
                    "VALUES (1, 'Food', 'food', '#E0795B', 1, 0, 0)"
            )
            execSQL(
                "INSERT INTO expenses (id, amount_minor, category_id, note, timestamp, created_at, source, merchant) " +
                    "VALUES (1, 15000, 1, 'Lunch', 1700000000000, 1700000000000, 'MANUAL', NULL)"
            )
            close()
        }

        // Migrate and validate the resulting schema against 2.json.
        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        // The v1 expense survived the migration.
        db.query("SELECT amount_minor FROM expenses WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(15000L, c.getLong(0))
        }
        // The new staging table exists and starts empty.
        db.query("SELECT COUNT(*) FROM import_candidates").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate2To3_addsAdjustmentsTableAndSports() {
        // Seed a v2 database the way an older install looked: defaults, no Sports.
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO categories (id, name, icon_key, color_hex, is_default, is_archived, sort_order) " +
                    "VALUES (1, 'Food', 'food', '#E0795B', 1, 0, 0)"
            )
            execSQL(
                "INSERT INTO expenses (id, amount_minor, category_id, note, timestamp, created_at, source, merchant) " +
                    "VALUES (1, 15000, 1, 'Lunch', 1700000000000, 1700000000000, 'MANUAL', NULL)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        // Existing financial data survived.
        db.query("SELECT amount_minor FROM expenses WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(15000L, c.getLong(0))
        }
        // The new ledger table exists and starts empty.
        db.query("SELECT COUNT(*) FROM budget_adjustments").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
        // Sports was back-filled exactly once, with the expected styling.
        db.query("SELECT icon_key, color_hex FROM categories WHERE name = 'Sports'").use { c ->
            assertEquals(1, c.count)
            assertTrue(c.moveToFirst())
            assertEquals("sports", c.getString(0))
            assertEquals("#7FB069", c.getString(1))
        }
        db.close()
    }

    @Test
    fun migrate2To3_doesNotClobberOrDuplicateUserSports() {
        // An install where the user already made their own "Sports" category.
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO categories (id, name, icon_key, color_hex, is_default, is_archived, sort_order) " +
                    "VALUES (5, 'Sports', 'games', '#123456', 0, 0, 3)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        // Still exactly one "Sports", and it's the user's row — untouched.
        db.query("SELECT id, icon_key, color_hex FROM categories WHERE name = 'Sports'").use { c ->
            assertEquals(1, c.count)
            assertTrue(c.moveToFirst())
            assertEquals(5L, c.getLong(0))
            assertEquals("games", c.getString(1))
            assertEquals("#123456", c.getString(2))
        }
        db.close()
    }

    @Test
    fun migrateAll_1To3_chained() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO categories (id, name, icon_key, color_hex, is_default, is_archived, sort_order) " +
                    "VALUES (1, 'Food', 'food', '#E0795B', 1, 0, 0)"
            )
            close()
        }

        // Run the whole chain and validate against 3.json.
        val db = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_1_2, MIGRATION_2_3)

        db.query("SELECT COUNT(*) FROM import_candidates").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM budget_adjustments").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM categories WHERE name = 'Sports'").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0))
        }
        db.close()
    }
}
