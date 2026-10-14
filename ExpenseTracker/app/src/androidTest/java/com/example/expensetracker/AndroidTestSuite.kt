package com.example.expensetracker

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    //uiTest
    ExpenseItemUiTest::class,
    BudgetScreenUiTest::class,
    ChartsUiTest::class,
    ExpenseEntryFormUiTest::class,
    NavigationUiTest::class,

)
class AndroidTestSuite
