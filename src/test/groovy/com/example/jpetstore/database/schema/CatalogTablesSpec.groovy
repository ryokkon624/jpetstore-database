package com.example.jpetstore.database.schema

/**
 * V00_000_003__create_catalog_tables.sql の表明テスト。
 * AC1（マイグレーション基盤）・AC4（金額はdecimal）・AC7（WHO6列付与／version非付与）を検証する。
 */
class CatalogTablesSpec extends SchemaMigrationSpecBase {

    def "m_supplier / m_category / m_product / m_item が作成されている"() {
        expect:
        tableExists("m_supplier")
        tableExists("m_category")
        tableExists("m_product")
        tableExists("m_item")
    }

    def "m_product.category_id は m_category を参照する外部キー列である"() {
        expect:
        columnOf("m_product", "category_id") != null
    }

    def "m_item.product_id / supplier_id が存在する"() {
        expect:
        columnOf("m_item", "product_id") != null
        columnOf("m_item", "supplier_id") != null
    }

    def "m_item.supplier_id に明示セカンダリインデックスが付与されている（product_idと同じ扱い）"() {
        expect:
        sql.firstRow("""
            SELECT INDEX_NAME
              FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = 'm_item'
               AND COLUMN_NAME = 'supplier_id' AND INDEX_NAME = 'idx_m_item_supplier_id'
        """) != null
    }

    def "AC4: m_item.#column は decimal(10,2) である"() {
        given:
        def col = columnOf("m_item", column)

        expect:
        col.DATA_TYPE == "decimal"
        col.NUMERIC_PRECISION == 10
        col.NUMERIC_SCALE == 2

        where:
        column << ["list_price", "unit_cost"]
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
        table << ["m_supplier", "m_category", "m_product", "m_item"]
    }

    def "AC7: #table に version 列は付与されていない（参照マスタ）"() {
        expect:
        columnOf(table, "version") == null

        where:
        table << ["m_supplier", "m_category", "m_product", "m_item"]
    }
}
