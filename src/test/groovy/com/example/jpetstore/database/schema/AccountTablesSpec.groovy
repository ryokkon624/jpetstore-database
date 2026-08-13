package com.example.jpetstore.database.schema

/**
 * V00_000_004__create_account_tables.sql の表明テスト。
 * AC2（password_hash 拡張）・AC3（bannerdata依存廃止）・AC7（version楽観ロック）を検証する。
 */
class AccountTablesSpec extends SchemaMigrationSpecBase {

    def "m_account / m_signon / m_profile が作成されている"() {
        expect:
        tableExists("m_account")
        tableExists("m_signon")
        tableExists("m_profile")
    }

    def "m_account.user_id は AUTO_INCREMENT の代理キーで username は一意である"() {
        given:
        def userId = columnOf("m_account", "user_id")
        def username = columnOf("m_account", "username")

        expect:
        userId != null
        userId.COLUMN_TYPE.contains("bigint")
        username != null
        username.IS_NULLABLE == "NO"
        sql.firstRow("""
            SELECT INDEX_NAME
              FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = 'm_account'
               AND COLUMN_NAME = 'username' AND NON_UNIQUE = 0
        """) != null
    }

    def "AC2: m_signon.password_hash は varchar(255) NOT NULL である（旧varchar(25)から拡張）"() {
        given:
        def col = columnOf("m_signon", "password_hash")

        expect:
        col.DATA_TYPE == "varchar"
        col.CHARACTER_MAXIMUM_LENGTH == 255
        col.IS_NULLABLE == "NO"
    }

    def "m_signon.user_id は m_account を参照する外部キーである"() {
        expect:
        columnOf("m_signon", "user_id") != null
    }

    def "AC3: bannerdata テーブルは存在しない"() {
        expect:
        !tableExists("bannerdata")
    }

    def "AC3: m_profile に banner/mylist 関連列は存在しない"() {
        given:
        def names = columnNamesOf("m_profile")

        expect:
        !names.any { it.equalsIgnoreCase("banner_option") || it.equalsIgnoreCase("banneropt") }
        !names.any { it.equalsIgnoreCase("my_list_option") || it.equalsIgnoreCase("mylistopt") }
    }

    def "m_profile.favorite_category_id は任意設定として残る"() {
        given:
        def col = columnOf("m_profile", "favorite_category_id")

        expect:
        col != null
        col.IS_NULLABLE == "YES"
    }

    def "AC7: version 列が #table に付与されている"() {
        given:
        def col = columnOf(table, "version")

        expect:
        col != null
        col.DATA_TYPE == "bigint"
        col.IS_NULLABLE == "NO"

        where:
        table << ["m_account", "m_signon", "m_profile"]
    }

    def "AC7: #table にWHO6列が付与されている"() {
        given:
        def names = columnNamesOf(table)

        expect:
        names.containsAll([
                "create_user_id", "create_program", "created_at",
                "update_user_id", "update_program", "updated_at",
        ])

        where:
        table << ["m_account", "m_signon", "m_profile"]
    }
}
