-- ------------------------------------------------------------
-- V01_000_001__insert_ac_neg1_account_fixture.sql
--   flyway/sql-test（開発・テスト用データ）
--
--   AC-neg1（否定AC）: bannerdata が存在しなくても account+signon+profile が
--   JOIN 取得できることを確認するための最小フィクスチャ。
--   本番相当マイグレーション（flyway/sql）とはバージョン番号の桁を分離し
--   （先頭セグメントを 01 に）、将来の flyway/sql への追記（V00_000_007 以降）と
--   衝突しないようにしている。
--
--   favorite_category_id はあえて NULL にし、カタログseed（他ドメインSprint）に
--   依存せずこのフィクスチャ単体で成立するようにしている。
--
--   WHO はローカルの Interceptor が効かないためリテラル 'INIT_DATA' を明示する
--   （.claude/rules/database.md）。
-- ------------------------------------------------------------

INSERT INTO m_account (
    username, email, first_name, last_name, status,
    address1, address2, city, state, postal_code, country, phone,
    create_user_id, create_program, created_at,
    update_user_id, update_program, updated_at
) VALUES (
    'ac_neg1_user', 'ac-neg1@example.com', 'Neg1', 'Fixture', 'OK',
    '1 Test St', NULL, 'Testville', 'CA', '90000', 'USA', '555-0100',
    NULL, 'INIT_DATA', NOW(6),
    NULL, 'INIT_DATA', NOW(6)
);

INSERT INTO m_signon (
    user_id, password_hash,
    create_user_id, create_program, created_at,
    update_user_id, update_program, updated_at
)
SELECT user_id, '$2a$10$ACNeg1FixtureOnlyDummyBcryptHashXXXXXXXXXXXXXXXXXXXXX',
       NULL, 'INIT_DATA', NOW(6),
       NULL, 'INIT_DATA', NOW(6)
  FROM m_account WHERE username = 'ac_neg1_user';

INSERT INTO m_profile (
    user_id, language_preference, favorite_category_id,
    create_user_id, create_program, created_at,
    update_user_id, update_program, updated_at
)
SELECT user_id, 'english', NULL,
       NULL, 'INIT_DATA', NOW(6),
       NULL, 'INIT_DATA', NOW(6)
  FROM m_account WHERE username = 'ac_neg1_user';
