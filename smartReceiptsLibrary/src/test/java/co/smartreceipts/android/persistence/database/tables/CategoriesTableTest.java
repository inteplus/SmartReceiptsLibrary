package co.smartreceipts.android.persistence.database.tables;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.smartreceipts.android.R;
import co.smartreceipts.android.model.Category;
import co.smartreceipts.android.model.Column;
import co.smartreceipts.android.model.ColumnDefinitions;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.factory.CategoryBuilderFactory;
import co.smartreceipts.android.model.impl.columns.BlankColumn;
import co.smartreceipts.android.model.impl.columns.receipts.ReceiptPaymentMethodColumn;
import co.smartreceipts.android.persistence.database.tables.columns.CSVTableColumns;
import co.smartreceipts.android.persistence.database.tables.columns.CategoriesTableColumns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class CategoriesTableTest {

    private static final String NAME1 = "name1";
    private static final String NAME2 = "name2";
    private static final String CODE1 = "code1";
    private static final String CODE2 = "code2";

    // Class under test
    CategoriesTable mCategoriesTable;

    @Mock
    SQLiteDatabase mSQLiteDatabase;

    @Mock
    TableDefaultsCustomizer mTableDefaultsCustomizer;

    @Captor
    ArgumentCaptor<String> mSqlCaptor;

    SQLiteOpenHelper mSQLiteOpenHelper;

    Category mCategory1;

    Category mCategory2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mSQLiteOpenHelper = new TestSQLiteOpenHelper(RuntimeEnvironment.application);
        mCategoriesTable = new CategoriesTable(mSQLiteOpenHelper);

        // Pre-cache some utilities
        mCategory1 = new CategoryBuilderFactory().setName(NAME1).setCode(CODE1).build();
        mCategory2 = new CategoryBuilderFactory().setName(NAME2).setCode(CODE2).build();

        // Now create the table and insert some defaults
        mCategoriesTable.onCreate(mSQLiteOpenHelper.getWritableDatabase(), mTableDefaultsCustomizer);
        mCategoriesTable.insertCategoryNoCache(mCategory1.getName(), mCategory1.getCode());
        mCategoriesTable.insertCategoryNoCache(mCategory2.getName(), mCategory2.getCode());
    }

    @After
    public void tearDown() {
        mSQLiteOpenHelper.getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + mCategoriesTable.getTableName());
    }

    @Test
    public void getTableName() {
        assertEquals(CategoriesTableColumns.TABLE_NAME, mCategoriesTable.getTableName());
    }

    @Test
    public void onCreate() {
        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mCategoriesTable.onCreate(mSQLiteDatabase, customizer);
        verify(mSQLiteDatabase).execSQL(mSqlCaptor.capture());
        verify(customizer).insertCategoryDefaults(mCategoriesTable);

        assertTrue(mSqlCaptor.getValue().contains(CategoriesTableColumns.TABLE_NAME));
        assertTrue(mSqlCaptor.getValue().contains(CategoriesTableColumns.COLUMN_NAME));
        assertTrue(mSqlCaptor.getValue().contains(CategoriesTableColumns.COLUMN_CODE));
        assertTrue(mSqlCaptor.getValue().contains(CategoriesTableColumns.COLUMN_BREAKDOWN));
    }

    @Test
    public void onUpgrade() {
        final int oldVersion = 1;
        final int newVersion = 5;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mCategoriesTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertCategoryDefaults(mCategoriesTable);

        assertTrue(mSqlCaptor.getValue().contains("ALTER"));
        assertTrue(mSqlCaptor.getValue().contains(CategoriesTableColumns.TABLE_NAME));
        assertTrue(mSqlCaptor.getValue().contains(CategoriesTableColumns.COLUMN_BREAKDOWN));
    }

    @Test
    public void onUpgradeAlreadyOccurred() {
        final int oldVersion = 3;
        final int newVersion = 14;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mCategoriesTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, never()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertCategoryDefaults(mCategoriesTable);
    }

    @Test
    public void getCategories() {
        final ArrayList<Category> categories = mCategoriesTable.getCategoriesList();
        assertEquals(categories, Arrays.asList(mCategory1, mCategory2));
        
    }

    @Test
    public void insert() {
        final ArrayList<Category> oldCategories = mCategoriesTable.getCategoriesList();
        final int oldSize = oldCategories.size();

        final String name = "NewName";
        final String code = "NewCode";
        final Category insertCategory = new CategoryBuilderFactory().setName(name).setCode(code).build();
        assertTrue(mCategoriesTable.insertCategory(insertCategory));

        final ArrayList<Category> newCategories = mCategoriesTable.getCategoriesList();
        assertEquals(oldSize + 1, newCategories.size());
        assertTrue(newCategories.contains(insertCategory));
        assertEquals(mCategoriesTable.getCategoryCode(name), code);
    }

    @Test
    public void insertNoCache() {
        final String name = "NewName";
        final String code = "NewCode";
        final Category insertCategory = new CategoryBuilderFactory().setName(name).setCode(code).build();
        assertTrue(mCategoriesTable.insertCategoryNoCache(insertCategory.getName(), insertCategory.getCode()));
    }

    @Test
    public void update() {
        final ArrayList<Category> oldCategories = mCategoriesTable.getCategoriesList();
        final int oldSize = oldCategories.size();
        assertTrue(oldCategories.contains(mCategory1));

        final String name = "NewName";
        final String code = "NewCode";
        final Category updateCategory = new CategoryBuilderFactory().setName(name).setCode(code).build();
        assertTrue(mCategoriesTable.updateCategory(mCategory1, updateCategory));

        final ArrayList<Category> newCategories = mCategoriesTable.getCategoriesList();
        assertEquals(oldSize, newCategories.size());
        assertTrue(newCategories.contains(updateCategory));
        assertFalse(newCategories.contains(mCategory1));
        assertEquals(mCategoriesTable.getCategoryCode(name), code);
    }

    @Test
    public void delete() {
        final ArrayList<Category> oldCategories = mCategoriesTable.getCategoriesList();
        assertTrue(oldCategories.contains(mCategory1));
        assertTrue(oldCategories.contains(mCategory2));

        assertTrue(mCategoriesTable.deleteCategory(mCategory1));
        assertTrue(mCategoriesTable.deleteCategory(mCategory2));

        final ArrayList<Category> newCategories = mCategoriesTable.getCategoriesList();
        assertTrue(newCategories.isEmpty());
    }

}