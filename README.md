# jpetstore-database

モダン版 JPetStore の **データベースリポジトリ**。
スキーマ（Flyway マイグレーション）・区分値マスタ（m_code）・区分値からの
TypeScript enum 生成（MultiEnumGenerator）を担う。

`legacy-jpetstore`（before）に対する `jpetstore-*`（after）の polyrepo 3 本
（frontend / backend / database）のうちの database repo。

---

## 役割

| 担当 | 内容 |
| --- | --- |
| スキーマ管理 | Flyway でテーブル・マスターデータをバージョン管理する |
| 区分値マスタ | `m_code`（コードマスタ）を保持する。多言語は**日英のみ** |
| enum 生成（TS） | `MultiEnumGenerator` が `m_code` → `build/generated/frontend/code.constants.ts` を生成し、`jpetstore-frontend` に取り込む |
| ローカル DB | `docker-compose.yml` でローカル MySQL を起動する（infra repo は将来） |

> **MyBatis Generator（スキーマ → entity/mapper）と Java enum 生成は backend 側**
> （`jpetstore-backend`）の責務。本 repo では扱わない。

---

## 技術スタック

| 項目 | 内容 |
| --- | --- |
| DB | MySQL 8.4（charset `utf8mb4` / collation `utf8mb4_ja_0900_as_cs`） |
| マイグレーション | Flyway 11.14.1（Gradle plugin） |
| ビルド | Gradle 8.14（wrapper 同梱）/ Java 21（Amazon Corretto） |
| enum 生成 | `MultiEnumGenerator`（Java・TS 専用出力） |

### 接続情報（ローカル）

| 項目 | 値 |
| --- | --- |
| host:port | `localhost:3306` |
| database | `jpetstore_db` |
| user / password | `jpetstore` / `jpetstore` |

---

## セットアップ・起動手順

すべて本 repo（`C:\work\java-migration\jpetstore-database`）直下で実行する。

### 1. ローカル MySQL の起動

```bash
docker compose up -d      # 起動（停止は docker compose down）
```

### 2. マイグレーション適用

ローカルでは `seedDevData`（`flyway/sql-test`）を併用するため、必ず
`flywayClean → flywayMigrate → seedDevData` の順で実行する。

```bash
./gradlew flywayClean     # ローカル環境のみ。STG/PROD では絶対に実行しない
./gradlew flywayMigrate    # flyway/sql の未適用マイグレーションを適用
./gradlew seedDevData      # flyway/sql-test の開発用データを追加適用
```

### 3. TypeScript enum 生成

`m_code` を更新した後（= `flywayMigrate` 後）に実行する。

```bash
./gradlew generateEnums    # build/generated/frontend/code.constants.ts を生成
```

生成物を `jpetstore-frontend` に取り込む。

---

## ディレクトリ構成

```
jpetstore-database/
├── build.gradle                 # Flyway + generateEnums(TS) + seedDevData
├── settings.gradle
├── docker-compose.yml           # ローカル MySQL（jpetstore_db）
├── flyway/
│   ├── sql/                     # 本番相当マイグレーション（スキーマ・マスターデータ）
│   │   ├── V00_000_001__create_tables.sql   # m_code のみ（業務テーブルは Phase 3）
│   │   └── V00_000_002__insert_m_code.sql   # 区分値サンプル（OrderStatus / CardType）
│   └── sql-test/                # 開発・テスト用データ（seedDevData で適用）
└── src/main/java/com/example/jpetstore/database/tool/
    └── MultiEnumGenerator.java  # m_code → TS enum 生成（TS 専用）
```

---

## 規約（正典）

WHO カラム・m_code・enum 生成・MyBatis Generator の詳細規約は
`migration-agent-base` の以下を正典とする。

- **横断アーキ決定**: `migration-agent-base/spec/architecture-conventions.md`
- **DB 実務手順**: `migration-agent-base/.claude/rules/database.md`

### 要点

- **WHO カラム**（全業務テーブル共通の 6 列）: `create_user_id` / `create_program` /
  `created_at` / `update_user_id` / `update_program` / `updated_at`。
  `*_program` には `ClassName#method` のテキストを格納（`ProgramType` enum・
  m_code `0012` は作らない）。値は AOP + MyBatis Interceptor が自動付与。
  Flyway の seed では `'INIT_DATA'` を明示する。
- **m_code の多言語は日英のみ**（`display_name_ja` / `display_name_en`）。`display_name_es` は持たない。
- **enum 生成は TS のみ**（`MultiEnumGenerator`）。Dart 出力は廃止。Java enum は backend 側で生成。
- 業務テーブル（account / product / order 等）は本雛形にはまだ無い。Phase 3 で
  PO の Story/仕様から起こす（WHO カラム 6 列を末尾に付与すること）。
