package com.example.jpetstore.database.schema

/**
 * V00_000_012__create_register_attempt.sql の表明テスト。
 *
 * #13 AC-neg2(SBD-6): 登録エンドポイントの総当り/列挙をレート制限で抑止する。
 * t_login_attempt（username PK）と同型だが、キーは IP アドレス（登録には username が
 * まだ存在しないため）。X-Forwarded-For は信頼しない前提（Sprint2 教訓）。
 */
class RegisterAttemptTableSpec extends SchemaMigrationSpecBase {

    def "t_register_attempt が作成されている"() {
        expect:
        tableExists("t_register_attempt")
    }

    def "レート制限判定に必要な列が揃っている"() {
        given:
        def names = columnNamesOf("t_register_attempt")

        expect:
        names.containsAll([
                "client_ip", "attempt_count", "first_attempt_at", "lock_until",
        ])
    }

    def "client_ipがPKである"() {
        given:
        def column = columnOf("t_register_attempt", "client_ip")

        expect:
        column.IS_NULLABLE == "NO"
        column.CHARACTER_MAXIMUM_LENGTH == 45
    }

    def "client_ipにFK制約が無い(m_accountに紐づかない一過性のIPキー)"() {
        given:
        def fkCount = sql.firstRow("""
            SELECT COUNT(*) AS cnt
              FROM information_schema.KEY_COLUMN_USAGE
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = 't_register_attempt'
               AND REFERENCED_TABLE_NAME IS NOT NULL
        """)

        expect:
        fkCount.cnt == 0
    }

    def "WHO6列が付与されている"() {
        given:
        def names = columnNamesOf("t_register_attempt")

        expect:
        names.containsAll([
                "create_user_id", "create_program", "created_at",
                "update_user_id", "update_program", "updated_at",
        ])
    }

    def "version列は付与されていない(一過性のセキュリティ運用状態・単文アトミック更新のため楽観ロック対象外)"() {
        expect:
        columnOf("t_register_attempt", "version") == null
    }
}
