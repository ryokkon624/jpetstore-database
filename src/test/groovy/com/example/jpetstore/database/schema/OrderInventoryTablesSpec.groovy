package com.example.jpetstore.database.schema

/**
 * V00_000_005__create_order_tables.sql の表明テスト。
 * AC4（金額はdecimal）・AC5（カード列除外）・AC7（純追記表/在庫表はversion非付与）を検証する。
 * courier/locale 除外（ID-21）・orderstatus 廃止/sequence 廃止（ID-22/ID-23）も併せて検証する。
 */
class OrderInventoryTablesSpec extends SchemaMigrationSpecBase {

    def "t_order / t_order_line / t_inventory が作成されている"() {
        expect:
        tableExists("t_order")
        tableExists("t_order_line")
        tableExists("t_inventory")
    }

    def "t_order.user_id は m_account を参照し、status_code(placeholder)を持つ"() {
        expect:
        columnOf("t_order", "user_id") != null
        columnOf("t_order", "status_code") != null
    }

    def "AC4: t_order.total_price は decimal(10,2) である"() {
        given:
        def col = columnOf("t_order", "total_price")

        expect:
        col.DATA_TYPE == "decimal"
        col.NUMERIC_PRECISION == 10
        col.NUMERIC_SCALE == 2
    }

    def "AC4: t_order_line.unit_price は decimal(10,2) である"() {
        given:
        def col = columnOf("t_order_line", "unit_price")

        expect:
        col.DATA_TYPE == "decimal"
        col.NUMERIC_PRECISION == 10
        col.NUMERIC_SCALE == 2
    }

    def "AC5: t_order にカード関連列(creditcard/exprdate/cardtype)は存在しない"() {
        given:
        def names = columnNamesOf("t_order")*.toLowerCase()

        expect:
        !names.any { it in ["creditcard", "credit_card", "exprdate", "expiration_date", "cardtype", "card_type"] }
    }

    def "ID-21: t_order に courier/locale 列は存在しない"() {
        given:
        def names = columnNamesOf("t_order")*.toLowerCase()

        expect:
        !names.contains("courier")
        !names.contains("locale")
    }

    def "ID-22: orderstatus テーブルは存在しない（状態変更はaudit_logに記録）"() {
        expect:
        !tableExists("orderstatus")
    }

    def "ID-23: sequence テーブルは存在しない（AUTO_INCREMENTに置換）"() {
        expect:
        !tableExists("sequence")
    }

    def "t_inventory.item_id は m_item を参照し quantity を持つ"() {
        expect:
        columnOf("t_inventory", "item_id") != null
        columnOf("t_inventory", "quantity") != null
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
        table << ["t_order", "t_order_line", "t_inventory"]
    }

    def "AC7: #table に version 列は付与されていない（純追記表/在庫表）"() {
        expect:
        columnOf(table, "version") == null

        where:
        table << ["t_order", "t_order_line", "t_inventory"]
    }

    def "Q1(#9): t_order に (user_id, order_id) 複合索引が seq順(user_id=1, order_id=2)で存在する"() {
        given:
        def cols = indexColumnsOf("t_order", "idx_t_order_user_id_order_id")

        expect:
        cols.size() == 2
        cols[0].SEQ_IN_INDEX == 1
        cols[0].COLUMN_NAME == "user_id"
        cols[1].SEQ_IN_INDEX == 2
        cols[1].COLUMN_NAME == "order_id"
    }

    def "Q1(#9): 旧単一列索引 idx_t_order_user_id は複合索引に置換され残っていない（重複索引防止）"() {
        expect:
        !indexExists("t_order", "idx_t_order_user_id")
    }
}
