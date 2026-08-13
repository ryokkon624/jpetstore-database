package com.example.jpetstore.database.schema

/**
 * AC-neg1 検証用の基盤。{@link SchemaMigrationSpecBase} が適用した本番相当スキーマに加えて、
 * {@code flyway/sql-test}（開発・テスト用データ）を適用する。ローカル開発の
 * `flywayClean → flywayMigrate → seedDevData` と同じ順序を Testcontainers 上で再現する。
 */
abstract class AccountFixtureSpecBase extends SchemaMigrationSpecBase {

    static {
        // flyway/sql を併せて渡す理由は SchemaMigrationSpecBase#migrate のコメントを参照。
        migrate("filesystem:flyway/sql", "filesystem:flyway/sql-test")
    }
}
