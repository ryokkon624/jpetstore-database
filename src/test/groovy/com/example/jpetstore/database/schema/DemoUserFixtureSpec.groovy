package com.example.jpetstore.database.schema

/**
 * #19 AC2/AC-neg1: ログイン実証用のデモユーザー（demo_user）フィクスチャを検証する。
 *
 * - AC1: password_hash は平文ではなく bcrypt 形式（{bcrypt}$2 プレフィックス）で保存されている。
 * - AC2/AC-neg1: 既定弱資格情報（j2ee/j2ee 等）をシードに残さない。demo_user は j2ee ではない。
 */
class DemoUserFixtureSpec extends AccountFixtureSpecBase {

    def "demo_userがaccount+signon+profileの3表に1件ずつ存在する"() {
        given:
        def row = sql.firstRow("""
            SELECT a.user_id, a.username, s.password_hash, p.language_preference
              FROM m_account a
              JOIN m_signon s ON s.user_id = a.user_id
              JOIN m_profile p ON p.user_id = a.user_id
             WHERE a.username = 'demo_user'
        """)

        expect:
        row != null
        row.username == "demo_user"
    }

    def "demo_userのpassword_hashはbcrypt形式であり平文ではない(AC1・SBD-5)"() {
        given:
        def row = sql.firstRow("SELECT password_hash FROM m_signon s JOIN m_account a ON a.user_id = s.user_id WHERE a.username = 'demo_user'")

        expect:
        row.password_hash != null
        row.password_hash.startsWith("{bcrypt}\$2")
        // 平文パスワードそのものが保存されていないことの直接確認。
        row.password_hash != "Sprint3-DemoLogin!26"
    }

    def "既定弱資格情報(j2ee等)のusernameはシードに存在しない(AC2/AC-neg1・SBD-6)"() {
        given:
        def weakUsernames = ["j2ee", "admin", "test", "guest"]

        expect:
        weakUsernames.every { weak ->
            sql.firstRow("SELECT 1 FROM m_account WHERE username = ${weak}") == null
        }
    }

    def "R__test_userは再適用してもdemo_userが重複登録されない(冪等性)"() {
        given:
        def before = sql.firstRow("SELECT COUNT(*) AS cnt FROM m_account WHERE username = 'demo_user'").cnt

        when:
        migrate("filesystem:flyway/sql", "filesystem:flyway/sql-test")

        then:
        def after = sql.firstRow("SELECT COUNT(*) AS cnt FROM m_account WHERE username = 'demo_user'").cnt
        before == 1
        after == 1
    }
}
