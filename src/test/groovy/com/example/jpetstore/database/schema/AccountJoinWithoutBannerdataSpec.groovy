package com.example.jpetstore.database.schema

/**
 * AC-neg1（否定AC）: bannerdata 行が無くてもログイン/アカウント取得が失敗しないことを検証する。
 * flyway/sql-test のフィクスチャ（V01_000_001）を適用し、m_account/m_signon/m_profile の
 * 3表JOINが bannerdata に依存せず1件取得できることを確認する（INNER JOIN依存の解消の実証）。
 */
class AccountJoinWithoutBannerdataSpec extends AccountFixtureSpecBase {

    def "bannerdata テーブルは存在しない"() {
        expect:
        !tableExists("bannerdata")
    }

    def "AC-neg1: bannerdata無しで account+signon+profile がJOINで1件取得できる"() {
        given:
        def row = sql.firstRow("""
            SELECT a.user_id, a.username, s.password_hash, p.language_preference
              FROM m_account a
              JOIN m_signon s ON s.user_id = a.user_id
              JOIN m_profile p ON p.user_id = a.user_id
             WHERE a.username = 'ac_neg1_user'
        """)

        expect:
        row != null
        row.username == "ac_neg1_user"
        row.password_hash != null
        row.language_preference == "english"
    }
}
