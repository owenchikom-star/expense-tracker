package com.example.expensetracker

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    BootReceiverTest::class,
    CategoryPredictorTest::class,
    ConvertersTest::class,
    ExpenseDaoTest::class,
    ReceiptParserTest::class,
    RoomCategoryBudgetRepositoryTest::class,
    ExpenseViewModelTest::class,
)
class UnitTestSuite