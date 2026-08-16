package com.example.jpetstore.database.schema

/**
 * #1 論点2/②: 在庫ステータスをm_code区分値として登録したことの表明テスト
 * （V00_000_009__insert_stock_status_code.sql）。
 *
 * code_type 0003 = StockStatus。code_value は m_code.code_value が VARCHAR(10) のため
 * OUT_OF_STOCK(12文字)ではなくOUT_STOCK(9文字)を採用する。
 */
class StockStatusCodeSpec extends SchemaMigrationSpecBase {

    def "m_code に code_type=0003(StockStatus)が3件登録されている"() {
        given:
        def rows = sql.rows("""
            SELECT code_value, code_type_name, code_type_name_en, display_name_ja, display_name_en
              FROM m_code
             WHERE code_type = '0003'
             ORDER BY display_order
        """)

        expect:
        rows.size() == 3
        rows*.code_value == ["IN_STOCK", "LOW_STOCK", "OUT_STOCK"]
        rows.every { it.code_type_name_en == "StockStatus" }
    }

    def "m_code(0003)の表示名(日英)が在庫あり/残少/在庫切れに対応している"() {
        given:
        def byValue = sql.rows("SELECT code_value, display_name_ja, display_name_en FROM m_code WHERE code_type = '0003'")
                .collectEntries { [(it.code_value): [ja: it.display_name_ja, en: it.display_name_en]] }

        expect:
        byValue["IN_STOCK"] == [ja: "在庫あり", en: "In Stock"]
        byValue["LOW_STOCK"] == [ja: "残少", en: "Low Stock"]
        byValue["OUT_STOCK"] == [ja: "在庫切れ", en: "Out of Stock"]
    }

    def "既存のcode_type(0001/0002)と衝突しない新規採番である"() {
        expect:
        sql.firstRow("SELECT COUNT(*) AS cnt FROM m_code WHERE code_type = '0003'").cnt == 3
    }
}
