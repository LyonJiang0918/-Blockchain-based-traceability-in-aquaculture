package com.trace.controller;

import com.trace.dao.BatchMetaRepository;
import com.zaxxer.hikari.HikariDataSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开发/排障：查看当前进程实际连接的数据库（与 Navicat 对比用）。
 */
@RestController
@RequestMapping("/system")
@Tag(name = "系统信息", description = "数据源自检（排障）")
public class SystemInfoController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private BatchMetaRepository batchMetaRepository;

    @GetMapping("/datasource")
    @Operation(summary = "当前数据源与 batch_meta 行数", description = "用于确认后端连的是否为 Navicat 里打开的同一 MySQL 库")
    public ResponseEntity<Map<String, Object>> datasource() throws SQLException {
        Map<String, Object> body = new LinkedHashMap<>();
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource h = (HikariDataSource) dataSource;
            body.put("jdbcUrl", h.getJdbcUrl());
            body.put("username", h.getUsername());
        } else {
            body.put("dataSourceType", dataSource.getClass().getName());
        }
        try (Connection c = dataSource.getConnection()) {
            body.put("catalog", c.getCatalog());
            body.put("databaseProductName", c.getMetaData().getDatabaseProductName());
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT @@hostname AS h, @@port AS p")) {
                if (rs.next()) {
                    body.put("mysqlServerHostname", rs.getString("h"));
                    body.put("mysqlServerPort", rs.getString("p"));
                }
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM batch_meta")) {
                if (rs.next()) {
                    body.put("batchMetaCountNativeSql", rs.getLong("cnt"));
                }
            }
        }
        body.put("batchMetaTableRowCount", batchMetaRepository.count());
        body.put(
            "hint",
            "若 batchMetaTableRowCount>0 而 Navicat 里仍为 0：在 Navicat「查询」里执行 SELECT @@hostname, @@port; 必须与上面 mysqlServerHostname、mysqlServerPort 一致；不一致说明连了两套 MySQL。请把 Navicat 主机改为 127.0.0.1（与 application.yml 一致）。"
        );
        body.put(
            "navicatSqlToRun",
            "SELECT @@hostname, @@port, DATABASE(); SELECT COUNT(*) FROM batch_meta;"
        );
        return ResponseEntity.ok(body);
    }
}
