package com.balance.budget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.balance.budget.core.ui.theme.BalanceTheme
import com.balance.budget.data.categorize.Categorizer
import com.balance.budget.data.categorize.CategorizerStore
import com.balance.budget.data.local.BudgetDatabase
import com.balance.budget.data.local.seed.DefaultCategories
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.feature.quickadd.QuickAddSheetContent
import com.balance.budget.feature.quickadd.QuickAddViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The core loop, end to end: type an amount, pick a category, save, and confirm
 * exactly one expense lands in the database. Runs against a real in-memory Room
 * DB (unencrypted for the test) and the real Quick Add UI + view model.
 */
@RunWith(AndroidJUnit4::class)
class AddExpenseFlowTest {

    @get:Rule val compose = createComposeRule()

    private lateinit var db: BudgetDatabase
    private lateinit var viewModel: QuickAddViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, BudgetDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking { db.categoryDao().insertAll(DefaultCategories.list) }

        viewModel = QuickAddViewModel(
            expenseRepository = ExpenseRepository(db.expenseDao(), clock = { 1_700_000_000_000L }),
            categoryRepository = CategoryRepository(db.categoryDao()),
            categorizer = Categorizer(CategorizerStore(context)),
            clock = { 1_700_000_000_000L },
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun typeAmount_pickCategory_save_persistsOneExpense() {
        compose.setContent {
            BalanceTheme {
                QuickAddSheetContent(onClose = {}, viewModel = viewModel)
            }
        }

        // Categories load asynchronously from Room; wait for the chip to appear.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription("Category: Food")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Type ₹250 on the number pad.
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("5").performClick()
        compose.onNodeWithText("0").performClick()

        // Pick the Food category.
        compose.onNodeWithContentDescription("Category: Food").performClick()

        // The Save button should now be present; tap it.
        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()

        // Wait until exactly one expense has been written.
        compose.waitUntil(timeoutMillis = 5_000) {
            runBlocking { db.expenseDao().observeCount().first() } == 1
        }

        val rows = runBlocking { db.expenseDao().observeAll().first() }
        assertEquals(1, rows.size)
        assertEquals(250_00L, rows.first().expense.amountMinor)
        assertEquals("Food", rows.first().category.name)
    }
}
