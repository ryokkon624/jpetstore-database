package com.example.jpetstore.database.schema

/**
 * V00_000_010__create_cart_tables.sql の表明テスト。
 * AC2（単一表+UNIQUE(cart_id,item_id)で幽霊行=ID-17を構造的に不能にする）・
 * D3（t_cart_item に version 列を付与しない意図的逸脱）を検証する。
 */
class CartTablesSpec extends SchemaMigrationSpecBase {

    def "t_cart / t_cart_item が作成されている"() {
        expect:
        tableExists("t_cart")
        tableExists("t_cart_item")
    }

    def "t_cart.user_id は m_account を参照し、1ユーザ1カート(UNIQUE)である"() {
        given:
        def col = columnOf("t_cart", "user_id")

        expect:
        col != null
        col.IS_NULLABLE == "NO"
    }

    def "t_cart_item.item_id は m_item を参照し quantity(int) を持つ"() {
        expect:
        columnOf("t_cart_item", "item_id") != null
        columnOf("t_cart_item", "quantity") != null
        columnOf("t_cart_item", "quantity").DATA_TYPE == "int"
    }

    def "AC2/ID-17: t_cart_item は cart_id+item_id の複合UNIQUE制約を持つ(幽霊行の構造的排除)"() {
        expect:
        sql.firstRow("""
            SELECT INDEX_NAME
              FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = 't_cart_item'
               AND INDEX_NAME = 'uk_t_cart_item_cart_id_item_id'
               AND NON_UNIQUE = 0
        """) != null
    }

    def "D3: t_cart / t_cart_item に version 列は付与されていない(意図的逸脱)"() {
        expect:
        columnOf(table, "version") == null

        where:
        table << ["t_cart", "t_cart_item"]
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
        table << ["t_cart", "t_cart_item"]
    }
}
