package io.appstarterpack.persistence

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.test.Test
import kotlin.test.assertEquals

class DriverFactoryTest {

    private val schema = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 1L

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(null, "CREATE TABLE Item (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
            return QueryResult.Value(Unit)
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion
        ): QueryResult.Value<Unit> = QueryResult.Value(Unit)
    }

    @Test
    fun `driver creates schema and supports insert and select round-trip`() {
        val driver = DriverFactory().createDriver(schema, "test.db")

        driver.execute(null, "INSERT INTO Item VALUES (1, 'Charizard')", 0)
        driver.execute(null, "INSERT INTO Item VALUES (2, 'Pikachu')", 0)

        val results = driver.executeQuery(
            identifier = null,
            sql = "SELECT id, name FROM Item ORDER BY id",
            mapper = { cursor ->
                QueryResult.Value(buildList {
                    while (cursor.next().value) {
                        add(cursor.getLong(0)!! to cursor.getString(1)!!)
                    }
                })
            },
            parameters = 0
        )

        assertEquals(listOf(1L to "Charizard", 2L to "Pikachu"), results.value)

        driver.close()
    }
}
