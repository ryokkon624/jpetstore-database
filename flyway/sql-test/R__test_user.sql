-- ------------------------------------------------------------
-- R__test_user.sql
--   flyway/sql-test（開発・テスト用データ）
--
--   repeatable migration（R__）として実装する理由（Sprint Review 指摘対応）:
--   ・versioned migration（V__）はバージョン順序に組み込まれるため、このフィクスチャを
--     versioned として採番すると、将来 flyway/sql 側に version がより高いマイグレーション
--     （V00_000_007 以降）が追記された際に out-of-order となり migrate が壊れる。
--   ・repeatable migration は versioned migration がすべて適用された後に実行される仕様のため、
--     flyway/sql 側の version 採番と衝突・干渉しない。
--   ・repeatable migration は内容（checksum）が変わるたびに再適用されるため、各 INSERT を
--     「対象行が存在しない場合のみ」に限定し冪等化している（再実行してもユニーク/PK制約に
--     違反しない）。
--
--   AC-neg1（否定AC）: bannerdata が存在しなくても account+signon+profile が
--   JOIN 取得できることを確認するための最小フィクスチャ。
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
)
SELECT 'ac_neg1_user', 'ac-neg1@example.com', 'Neg1', 'Fixture', 'OK',
       '1 Test St', NULL, 'Testville', 'CA', '90000', 'USA', '555-0100',
       NULL, 'INIT_DATA', NOW(6),
       NULL, 'INIT_DATA', NOW(6)
 WHERE NOT EXISTS (SELECT 1 FROM m_account WHERE username = 'ac_neg1_user');

INSERT INTO m_signon (
    user_id, password_hash,
    create_user_id, create_program, created_at,
    update_user_id, update_program, updated_at
)
SELECT a.user_id, '$2a$10$ACNeg1FixtureOnlyDummyBcryptHashXXXXXXXXXXXXXXXXXXXXX',
       NULL, 'INIT_DATA', NOW(6),
       NULL, 'INIT_DATA', NOW(6)
  FROM m_account a
 WHERE a.username = 'ac_neg1_user'
   AND NOT EXISTS (SELECT 1 FROM m_signon s WHERE s.user_id = a.user_id);

INSERT INTO m_profile (
    user_id, language_preference, favorite_category_id,
    create_user_id, create_program, created_at,
    update_user_id, update_program, updated_at
)
SELECT a.user_id, 'english', NULL,
       NULL, 'INIT_DATA', NOW(6),
       NULL, 'INIT_DATA', NOW(6)
  FROM m_account a
 WHERE a.username = 'ac_neg1_user'
   AND NOT EXISTS (SELECT 1 FROM m_profile p WHERE p.user_id = a.user_id);
