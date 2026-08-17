package com.example.jpetstore.database.schema

import groovy.sql.Sql
import org.flywaydb.core.Flyway
import org.testcontainers.containers.MySQLContainer
import spock.lang.Shared
import spock.lang.Specification

/**
 * information_schema 表明テストの共通基盤。
 *
 * Docker 上に MySQL 8.4 コンテナを1つだけ起動し、{@code flyway/sql}（本番相当マイグレーション）
 * を適用したうえで JVM 内の全テストクラスから共有する（Singleton container パターン）。
 * コンテナ起動・マイグレーションはこのクラスがロードされたときに一度だけ実行される。
 */
abstract class SchemaMigrationSpecBase extends Specification {

    static final String DB_NAME = "jpetstore_db"

    @Shared
    static MySQLContainer<?> mysql

    @Shared
    static Sql sql

    static {
        mysql = new MySQLContainer<>("mysql:8.4.0")
                .withDatabaseName(DB_NAME)
                .withUsername("jpetstore")
                .withPassword("jpetstore")
        mysql.start()

        migrate("filesystem:flyway/sql")

        sql = Sql.newInstance(mysql.jdbcUrl, mysql.username, mysql.password, mysql.driverClassName)
    }

    /**
     * サブクラスの static ブロックから追加のロケーション（flyway/sql-test 等）を適用するために公開する。
     *
     * ⚠ 既に適用済みのマイグレーションを含まない locations だけを渡すと、Flyway の validate が
     * "Detected applied migration not resolved locally" で失敗する（history にあるが現在の
     * locations から解決できないため）。sql-test を追加適用する際は必ず flyway/sql も併せて
     * 渡すこと（= 実運用の `flywayMigrate → seedDevData` と同じロケーション構成を再現する）。
     */
    static void migrate(String... locations) {
        Flyway.configure()
                .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
                .locations(locations)
                .load()
                .migrate()
    }

    /** information_schema.COLUMNS を1行返す。列が存在しなければ null。 */
    static Map columnOf(String tableName, String columnName) {
        sql.firstRow("""
            SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, CHARACTER_MAXIMUM_LENGTH,
                   NUMERIC_PRECISION, NUMERIC_SCALE, COLUMN_TYPE
              FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = ${tableName} AND COLUMN_NAME = ${columnName}
        """)
    }

    static List<String> columnNamesOf(String tableName) {
        sql.rows("""
            SELECT COLUMN_NAME
              FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = ${tableName}
        """).collect { it.COLUMN_NAME as String }
    }

    static boolean tableExists(String tableName) {
        sql.firstRow("""
            SELECT TABLE_NAME
              FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = ${tableName}
        """) != null
    }

    /**
     * information_schema.STATISTICS から指定索引の構成列を SEQ_IN_INDEX 順で返す（存在しなければ空リスト）。
     * 複合索引の列順序（左端prefix）を検証する際に使う。
     */
    static List<Map> indexColumnsOf(String tableName, String indexName) {
        sql.rows("""
            SELECT SEQ_IN_INDEX, COLUMN_NAME
              FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = ${tableName} AND INDEX_NAME = ${indexName}
             ORDER BY SEQ_IN_INDEX
        """)
    }

    static boolean indexExists(String tableName, String indexName) {
        sql.firstRow("""
            SELECT INDEX_NAME
              FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND TABLE_NAME = ${tableName} AND INDEX_NAME = ${indexName}
        """) != null
    }

    /**
     * 指定列を持つ業務テーブル名の一覧（{@code flyway_schema_history} は Flyway が内部管理する
     * メタデータ表で "version" 列を持つが業務テーブルではないため除外する）。
     */
    static List<String> tablesWithColumn(String columnName) {
        sql.rows("""
            SELECT TABLE_NAME
              FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = ${DB_NAME} AND COLUMN_NAME = ${columnName}
               AND TABLE_NAME != 'flyway_schema_history'
             ORDER BY TABLE_NAME
        """).collect { it.TABLE_NAME as String }
    }
}
