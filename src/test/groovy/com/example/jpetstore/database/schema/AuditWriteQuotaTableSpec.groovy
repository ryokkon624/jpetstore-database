package com.example.jpetstore.database.schema

/**
 * V00_000_014__create_audit_write_quota.sql の表明テスト。
 *
 * #39 AC3(N14): 未認証由来のAUTHZ_FAILURE監査writeを一定窓内で上限付きにする（D7＝secure-by-default系の
 * 試行カウンタ・レート制限状態はDB-backed）。t_register_attempt（client_ip PK・no-FK）と同型。
 */
class AuditWriteQuotaTableSpec extends SchemaMigrationSpecBase {

    def "t_audit_write_quota が作成されている"() {
        expect:
        tableExists("t_audit_write_quota")
    }

    def "窓内上限判定に必要な列が揃っている"() {
        given:
        def names = columnNamesOf("t_audit_write_quota")

        expect:
        names.containsAll([
                "client_ip", "write_count", "suppressed_count", "window_started_at", "window_expires_at",
        ])
    }

    def "client_ipがPKである"() {
        given:
        def column = columnOf("t_audit_write_quota", "client_ip")

        expect:
        column.IS_NULLABLE == "NO"
        column.CHARACTER_MAXIMUM_LENGTH == 45
    }

    def "client_ipにFK制約が無い(m_accountに紐づかない一過性のIPキー)"() {
        given:
        def fkCount = sql.firstRow("""
            SELECT COUNT(*) AS cnt
              FROM information_schema.KEY_COLUMN_USAGE
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = 't_audit_write_quota'
               AND REFERENCED_TABLE_NAME IS NOT NULL
        """)

        expect:
        fkCount.cnt == 0
    }

    def "write_count/suppressed_countの既定値は0である"() {
        given:
        def defaultOf = { String columnName ->
            sql.firstRow("""
                SELECT COLUMN_DEFAULT
                  FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = 't_audit_write_quota'
                   AND COLUMN_NAME = ${columnName}
            """).COLUMN_DEFAULT
        }

        expect:
        defaultOf("write_count") == "0"
        defaultOf("suppressed_count") == "0"
    }

    def "WHO6列が付与されている"() {
        given:
        def names = columnNamesOf("t_audit_write_quota")

        expect:
        names.containsAll([
                "create_user_id", "create_program", "created_at",
                "update_user_id", "update_program", "updated_at",
        ])
    }

    def "version列は付与されていない(一過性のセキュリティ運用状態・単文アトミック更新のため楽観ロック対象外)"() {
        expect:
        columnOf("t_audit_write_quota", "version") == null
    }
}
