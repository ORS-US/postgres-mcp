package com.example.postgres_mcp.services;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class postgresService {

    private final JdbcTemplate jdbcTemplate;

    public postgresService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @McpTool(name = "list_tables", description = "List all tables in the public schema with their descriptions")
    public List<Map<String, Object>> listTables() {
        String sql = """
                SELECT
                    t.table_name,
                    pgd.description AS table_description
                FROM information_schema.tables t
                LEFT JOIN pg_catalog.pg_statio_all_tables st
                    ON st.schemaname = t.table_schema
                    AND st.relname = t.table_name
                LEFT JOIN pg_catalog.pg_description pgd
                    ON pgd.objoid = st.relid
                    AND pgd.objsubid = 0
                WHERE t.table_schema = 'public'
                ORDER BY t.table_name
                """;
        return jdbcTemplate.queryForList(sql);
    }

    @McpTool(name = "query", description = "Execute a read-only SQL query and return results")
    public List<Map<String, Object>> query(
            @McpToolParam(description = "The SELECT SQL query to execute") String sql) {
        if (!sql.trim().toLowerCase().startsWith("select")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        return jdbcTemplate.queryForList(sql);
    }

    @McpTool(name = "describe_table", description = "Get the columns and description of a table")
    public List<Map<String, Object>> describeTable(
            @McpToolParam(description = "The name of the table to describe") String tableName) {
        String sql = """
                SELECT
                    c.column_name,
                    c.data_type,
                    c.character_maximum_length,
                    c.is_nullable,
                    c.column_default,
                    pgd.description AS column_description
                FROM information_schema.columns c
                LEFT JOIN pg_catalog.pg_statio_all_tables st
                    ON st.schemaname = c.table_schema
                    AND st.relname = c.table_name
                LEFT JOIN pg_catalog.pg_description pgd
                    ON pgd.objoid = st.relid
                    AND pgd.objsubid = c.ordinal_position
                WHERE c.table_schema = 'public'
                  AND c.table_name = ?
                ORDER BY c.ordinal_position
                """;
        return jdbcTemplate.queryForList(sql, tableName);
    }
}