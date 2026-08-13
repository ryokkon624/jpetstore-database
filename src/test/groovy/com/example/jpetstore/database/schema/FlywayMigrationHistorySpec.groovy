package com.example.jpetstore.database.schema

/**
 * AC1: HSQLDB→MySQL 8.4、iBATIS→MyBatis 前提の Flyway マイグレーション基盤を検証する。
 * DB名 jpetstore_db に対して V00_000_003〜006 が成功裡に適用されていることを
 * flyway_schema_history で確認する。
 */
class FlywayMigrationHistorySpec extends SchemaMigrationSpecBase {

    def "対象データベース名は jpetstore_db である"() {
        expect:
        DB_NAME == "jpetstore_db"
        sql.firstRow("SELECT DATABASE() AS db")?.db == "jpetstore_db"
    }

    def "V00_000_003〜006 のマイグレーションが成功で適用されている"() {
        given:
        def rows = sql.rows("""
            SELECT script, success
              FROM flyway_schema_history
             WHERE script LIKE 'V00_000_00%create%'
             ORDER BY installed_rank
        """)
        def scripts = rows.collect { it.script as String }

        expect:
        scripts.any { it.contains("V00_000_003") }
        scripts.any { it.contains("V00_000_004") }
        scripts.any { it.contains("V00_000_005") }
        scripts.any { it.contains("V00_000_006") }
        rows.every { it.success == true || it.success == 1 }
    }
}
