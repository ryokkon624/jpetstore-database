package com.example.jpetstore.database.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * m_code テーブルから frontend（TypeScript）の enum 定数ファイルを生成する。
 *
 * <p>出力先:
 * <ul>
 *   <li>build/generated/frontend/code.constants.ts
 * </ul>
 *
 * <p>実行: {@code ./gradlew generateEnums}
 *
 * <p>移植メモ（HwHub の MultiEnumGenerator からの差分）:
 * <ul>
 *   <li>Dart（mobile）出力（{@code generateMobile}・{@code buildDartSource} 等）は廃止。TS 専用。
 *   <li>JDBC 接続先を {@code jpetstore_db} に変更。
 *   <li>パッケージを {@code com.example.jpetstore.database.tool} に変更。
 * </ul>
 * <p>※ {@code display_name_es} は元々不参照のため、m_code の es 列廃止による影響はない。
 */
public class MultiEnumGenerator {

  private static final String JDBC_URL =
      "jdbc:mysql://localhost:3306/jpetstore_db"
          + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Tokyo";
  private static final String JDBC_USER = "jpetstore";
  private static final String JDBC_PASSWORD = "jpetstore";

  private static final Path FRONTEND_OUT =
      Path.of("build/generated/frontend/code.constants.ts");

  public static void main(String[] args) {
    try {
      new MultiEnumGenerator().generate();
      System.out.println("Enum generation finished successfully.");
    } catch (Exception e) {
      System.err.println("Enum generation failed:");
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void generate() throws Exception {
    Files.createDirectories(FRONTEND_OUT.getParent());

    try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
      List<CodeType> codeTypes = loadCodeTypes(conn);
      Map<String, List<CodeRow>> allRows = new LinkedHashMap<>();
      for (CodeType ct : codeTypes) {
        List<CodeRow> rows = loadCodesByType(conn, ct.codeType());
        if (!rows.isEmpty()) {
          allRows.put(ct.codeType(), rows);
        }
      }

      generateFrontend(codeTypes, allRows);
    }
  }

  // -------------------------------------------------------------------------
  // DB アクセス
  // -------------------------------------------------------------------------

  private List<CodeType> loadCodeTypes(Connection conn) throws SQLException {
    String sql =
        """
        SELECT DISTINCT code_type, code_type_name, code_type_name_en
        FROM m_code
        ORDER BY code_type
        """;
    List<CodeType> result = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        result.add(
            new CodeType(
                rs.getString("code_type"),
                rs.getString("code_type_name"),
                rs.getString("code_type_name_en")));
      }
    }
    return result;
  }

  private List<CodeRow> loadCodesByType(Connection conn, String codeType) throws SQLException {
    String sql =
        """
        SELECT code_value, display_name_en, display_name_ja
        FROM m_code
        WHERE code_type = ?
        ORDER BY display_order, code_value
        """;
    List<CodeRow> rows = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, codeType);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(
              new CodeRow(
                  rs.getString("code_value"),
                  rs.getString("display_name_en"),
                  rs.getString("display_name_ja")));
        }
      }
    }
    return rows;
  }

  // -------------------------------------------------------------------------
  // Frontend (TypeScript) 生成
  // -------------------------------------------------------------------------

  private void generateFrontend(List<CodeType> codeTypes, Map<String, List<CodeRow>> allRows)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("/**\n * 区分値カテゴリ（CODE_TYPE）の定義\n */\n");
    sb.append("export const CODE_TYPE = {\n");
    for (CodeType ct : codeTypes) {
      if (!allRows.containsKey(ct.codeType())) continue;
      String constName = pascalToScreamingSnake(ct.codeTypeNameEn());
      sb.append("  ")
          .append(constName)
          .append(": '")
          .append(ct.codeType())
          .append("', // ")
          .append(ct.codeTypeNameEn())
          .append(" (")
          .append(ct.codeTypeName())
          .append(")\n");
    }
    sb.append("} as const\n");

    for (CodeType ct : codeTypes) {
      List<CodeRow> rows = allRows.get(ct.codeType());
      if (rows == null) continue;

      String constName = pascalToScreamingSnake(ct.codeTypeNameEn());
      String className = ct.codeTypeNameEn();
      sb.append("\n/**\n * ")
          .append(ct.codeType())
          .append(": ")
          .append(ct.codeTypeName())
          .append(" (")
          .append(className)
          .append(")\n */\n");
      sb.append("export const ").append(constName).append(" = {\n");
      for (CodeRow row : rows) {
        String key = displayNameToScreamingSnake(row.displayNameEn());
        sb.append("  ")
            .append(key)
            .append(": '")
            .append(row.codeValue())
            .append("', // ")
            .append(row.displayNameJa())
            .append("\n");
      }
      sb.append("} as const\n");
      sb.append("export type ")
          .append(className)
          .append("Code = (typeof ")
          .append(constName)
          .append(")[keyof typeof ")
          .append(constName)
          .append("]\n");
    }

    writeFile(FRONTEND_OUT, sb.toString());
    System.out.println("Generated: " + FRONTEND_OUT);
  }

  // -------------------------------------------------------------------------
  // 名前変換ユーティリティ
  // -------------------------------------------------------------------------

  /** PascalCase → SCREAMING_SNAKE_CASE（CODE_TYPE のキー・TypeScript const 名に使用） */
  static String pascalToScreamingSnake(String pascal) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pascal.length(); i++) {
      char c = pascal.charAt(i);
      if (i > 0 && Character.isUpperCase(c)) {
        char prev = pascal.charAt(i - 1);
        if (Character.isLowerCase(prev) || Character.isDigit(prev)) {
          sb.append('_');
        }
      }
      sb.append(Character.toUpperCase(c));
    }
    return sb.toString();
  }

  /**
   * display_name_en → SCREAMING_SNAKE_CASE（TypeScript のキーに使用）
   *
   * <p>例: "Nth Weekday" → "NTH_WEEKDAY", "AI Support" → "AI_SUPPORT"
   */
  static String displayNameToScreamingSnake(String name) {
    if (name == null || name.isBlank()) return "UNKNOWN";
    String upper = name.toUpperCase();
    StringBuilder sb = new StringBuilder();
    boolean prevUnderscore = false;
    for (int i = 0; i < upper.length(); i++) {
      char c = upper.charAt(i);
      if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
        sb.append(c);
        prevUnderscore = false;
      } else {
        if (!prevUnderscore && !sb.isEmpty()) {
          sb.append('_');
          prevUnderscore = true;
        }
      }
    }
    String result = sb.toString().replaceAll("_+$", "");
    if (result.isEmpty()) result = "VALUE";
    if (result.charAt(0) >= '0' && result.charAt(0) <= '9') result = "_" + result;
    return result;
  }

  // -------------------------------------------------------------------------
  // ファイル書き込み
  // -------------------------------------------------------------------------

  private void writeFile(Path path, String content) throws IOException {
    Files.writeString(
        path,
        content,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE);
  }

  // -------------------------------------------------------------------------
  // レコード型
  // -------------------------------------------------------------------------

  private record CodeType(String codeType, String codeTypeName, String codeTypeNameEn) {}

  private record CodeRow(String codeValue, String displayNameEn, String displayNameJa) {}
}
