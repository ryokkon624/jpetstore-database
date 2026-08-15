package com.example.jpetstore.database.schema

/**
 * V00_000_007__create_login_attempt.sql の表明テスト。
 *
 * #20 AC1(SBD-6): レート制限/ロックアウトの保持先。Draft 1（分離表＋前段ゲート）採用。
 * username を PK（FKなし）とし、実在/非実在usernameを対称に扱うことで列挙耐性を保つ
 * （t_audit_log.actor_user_id の no-FK 設計判断と同じ思想）。
 */
class LoginAttemptTableSpec extends SchemaMigrationSpecBase {

    def "t_login_attempt が作成されている"() {
        expect:
        tableExists("t_login_attempt")
    }

    def "レート制限/ロックアウト判定に必要な列が揃っている"() {
        given:
        def names = columnNamesOf("t_login_attempt")

        expect:
        names.containsAll([
                "username", "failed_attempt_count", "first_failed_at", "lock_until",
        ])
    }

    def "usernameがPKである"() {
        given:
        def column = columnOf("t_login_attempt", "username")

        expect:
        column.IS_NULLABLE == "NO"
        column.CHARACTER_MAXIMUM_LENGTH == 80
    }

    def "usernameにFK制約が無い(実在/非実在usernameを対称に扱う列挙耐性)"() {
        given:
        def fkCount = sql.firstRow("""
            SELECT COUNT(*) AS cnt
              FROM information_schema.KEY_COLUMN_USAGE
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = 't_login_attempt'
               AND REFERENCED_TABLE_NAME IS NOT NULL
        """)

        expect:
        fkCount.cnt == 0
    }

    def "WHO6列が付与されている"() {
        given:
        def names = columnNamesOf("t_login_attempt")

        expect:
        names.containsAll([
                "create_user_id", "create_program", "created_at",
                "update_user_id", "update_program", "updated_at",
        ])
    }

    def "version列は付与されていない(一過性のセキュリティ運用状態・単文アトミック更新のため楽観ロック対象外)"() {
        expect:
        columnOf("t_login_attempt", "version") == null
    }
}
