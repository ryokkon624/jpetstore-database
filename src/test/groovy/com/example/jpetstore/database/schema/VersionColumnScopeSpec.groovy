package com.example.jpetstore.database.schema

/**
 * AC7 (arch §4.2/§4.3) の付与範囲を、スキーマ全体を横断して検証する。
 * version 楽観ロック列は「更新が発生するエンティティ表」（m_account/m_signon/m_profile）
 * にのみ付与し、参照マスタ・純追記表・在庫表・m_code には付与しない。
 */
class VersionColumnScopeSpec extends SchemaMigrationSpecBase {

    def "version 列を持つテーブルは m_account/m_signon/m_profile の3表のみである"() {
        expect:
        tablesWithColumn("version") == ["m_account", "m_profile", "m_signon"]
    }
}
