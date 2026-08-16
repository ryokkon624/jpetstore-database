package com.example.jpetstore.database.schema

/**
 * #1 AC1/AC5/[L2]: V00_000_008__insert_catalog_master.sql の表明テスト。
 *
 * - [L2] 旧同値: legacy-jpetstore/db/mysql/jpetstore-mysql-dataload.sql の階層
 *   （category5 → product16 → item28）に忠実であることを件数・階層マッピングで固定する。
 * - AC5(SBD-18/ID-15): description は plaintext（レガシーの `<image>` 埋め込みHTMLを継承しない）。
 * - AC3: t_inventory.quantity を在庫3状態（在庫あり/残少/在庫切れ）が出るように作り分ける
 *   （N=5・qty<=0=在庫切れ／0<qty<=5=残少／qty>5=在庫あり）。
 */
class CatalogSeedSpec extends SchemaMigrationSpecBase {

    def "[L2] m_category に5件投入されている(FISH/DOGS/REPTILES/CATS/BIRDS)"() {
        given:
        def rows = sql.rows("SELECT category_id FROM m_category ORDER BY category_id")

        expect:
        rows*.category_id == ["BIRDS", "CATS", "DOGS", "FISH", "REPTILES"]
    }

    def "[L2] m_product に16件投入され、カテゴリごとの商品件数が旧同値である"() {
        given:
        def rows = sql.rows("""
            SELECT category_id, COUNT(*) AS cnt
              FROM m_product
             GROUP BY category_id
        """)
        def byCategory = rows.collectEntries { [(it.category_id): it.cnt] }

        expect:
        sql.firstRow("SELECT COUNT(*) AS cnt FROM m_product").cnt == 16
        byCategory == [FISH: 4, DOGS: 6, REPTILES: 2, CATS: 2, BIRDS: 2]
    }

    def "[L2] m_item に28件投入され、K9-RT-02の在庫アイテム数が最多(4件)である"() {
        given:
        def total = sql.firstRow("SELECT COUNT(*) AS cnt FROM m_item").cnt
        def maxProduct = sql.rows("""
            SELECT product_id, COUNT(*) AS cnt
              FROM m_item
             GROUP BY product_id
             ORDER BY cnt DESC, product_id ASC
             LIMIT 1
        """)[0]

        expect:
        total == 28
        maxProduct.product_id == "K9-RT-02"
        maxProduct.cnt == 4
    }

    def "AC5(SBD-18/ID-15): m_category.description に HTML タグが含まれない(plaintext)"() {
        given:
        def rows = sql.rows("SELECT description FROM m_category")

        expect:
        rows.every { it.description != null && !(it.description as String).contains("<") }
    }

    def "AC5(SBD-18/ID-15): m_product.description に HTML タグが含まれない(plaintext)"() {
        given:
        def rows = sql.rows("SELECT description FROM m_product")

        expect:
        rows.every { it.description != null && !(it.description as String).contains("<") }
    }

    def "AC3: t_inventory が28件投入され在庫3状態(在庫あり/残少/在庫切れ)が作り分けられている(N=5)"() {
        given:
        def total = sql.firstRow("SELECT COUNT(*) AS cnt FROM t_inventory").cnt
        def inStock = sql.firstRow("SELECT COUNT(*) AS cnt FROM t_inventory WHERE quantity > 5").cnt
        def lowStock = sql.firstRow("SELECT COUNT(*) AS cnt FROM t_inventory WHERE quantity > 0 AND quantity <= 5").cnt
        def outOfStock = sql.firstRow("SELECT COUNT(*) AS cnt FROM t_inventory WHERE quantity <= 0").cnt

        expect:
        total == 28
        inStock == 23
        lowStock == 3
        outOfStock == 2
    }

    def "WHO: カタログseedのcreate_programはリテラルINIT_DATAである"() {
        expect:
        sql.firstRow("SELECT DISTINCT create_program FROM m_category").create_program == "INIT_DATA"
        sql.firstRow("SELECT DISTINCT create_program FROM m_product").create_program == "INIT_DATA"
        sql.firstRow("SELECT DISTINCT create_program FROM m_item").create_program == "INIT_DATA"
        sql.firstRow("SELECT DISTINCT create_program FROM t_inventory").create_program == "INIT_DATA"
    }
}
