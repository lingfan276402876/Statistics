package uc.game.statistics.data;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uc.game.statistics.utils.Constant;

@Service
@Transactional(readOnly = true)
public class DataBaseService {
	@Resource
	private JdbcTemplate jdbcTemplate;
	private static String tableSchema = null;

	public int getTableCount(String tableName) {
		String tableSchema = getTableSchema();
		int tableCount = 0;
		if (!Constant.IS_DEV) {
			tableCount = jdbcTemplate.queryForInt("select count(TABLE_NAME) from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='" + tableSchema + "' and TABLE_NAME='" + tableName + "'");
		}
		return tableCount;
	}

	public String getTableSchema() {
		if (Constant.IS_DEV) {
			return "";
		}
		if (tableSchema != null) {
			return tableSchema;
		}
		String tableSchema = "";
		Connection connection = null;
		try {
			connection = jdbcTemplate.getDataSource().getConnection();
			tableSchema = connection.getCatalog();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		DataBaseService.tableSchema = tableSchema;
		return DataBaseService.tableSchema;
	}
}
