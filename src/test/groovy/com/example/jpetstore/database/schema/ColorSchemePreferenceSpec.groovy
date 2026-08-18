package com.example.jpetstore.database.schema

/**
 * V00_000_013__add_m_profile_color_scheme_preference.sql の表明テスト。
 *
 * #36 AC8: m_profile にテーマ設定（system/light/dark）を永続化する列を追加する。
 * 参照: backlog/sprint_19/sprint_backlog.md #36 AC8。
 */
class ColorSchemePreferenceSpec extends SchemaMigrationSpecBase {

    def "AC8: m_profile.color_scheme_preference が VARCHAR(20) NOT NULL で追加されている"() {
        given:
        def col = columnOf("m_profile", "color_scheme_preference")

        expect:
        col != null
        col.DATA_TYPE == "varchar"
        col.CHARACTER_MAXIMUM_LENGTH == 20
        col.IS_NULLABLE == "NO"
    }

    def "AC8: color_scheme_preference の既定値は system である"() {
        given:
        def row = sql.firstRow("""
            SELECT COLUMN_DEFAULT
              FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = 'm_profile'
               AND COLUMN_NAME = 'color_scheme_preference'
        """)

        expect:
        row.COLUMN_DEFAULT == "system"
    }
}
