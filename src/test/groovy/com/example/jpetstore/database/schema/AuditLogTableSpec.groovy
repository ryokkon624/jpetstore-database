package com.example.jpetstore.database.schema

/**
 * V00_000_006__create_audit_log.sql の表明テスト。
 * AC6（監査ログ用テーブル/基盤を用意。認可失敗・状態変更の記録先）を検証する。
 */
class AuditLogTableSpec extends SchemaMigrationSpecBase {

    def "t_audit_log が作成されている"() {
        expect:
        tableExists("t_audit_log")
    }

    def "認可失敗・状態変更を「誰が/何を/結果」で記録できる列が揃っている"() {
        given:
        def names = columnNamesOf("t_audit_log")

        expect:
        names.containsAll([
                "audit_id", "event_type",
                "actor_user_id", "actor_username",
                "action", "target_type", "target_id",
                "result", "detail", "client_ip",
        ])
    }

    def "AC7: t_audit_log にWHO6列が付与されている"() {
        given:
        def names = columnNamesOf("t_audit_log")

        expect:
        names.containsAll([
                "create_user_id", "create_program", "created_at",
                "update_user_id", "update_program", "updated_at",
        ])
    }

    def "AC7: t_audit_log に version 列は付与されていない（追記専用ログ）"() {
        expect:
        columnOf("t_audit_log", "version") == null
    }
}
