package uc.game.statistics.data;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uc.game.statistics.config.CubeConfig;
import uc.game.statistics.config.CubeConfig.Cube;
import uc.game.statistics.config.CubeConfig.Dimension;
import uc.game.statistics.config.CubeConfig.KPI;
import uc.game.statistics.config.LogConfig;
import uc.game.statistics.config.LogConfig.Log;
import uc.game.statistics.config.OperationType;
import uc.game.statistics.utils.Constant;

@Service
public class StatisticsData extends DataBaseService{
	private static final Logger logger = LoggerFactory.getLogger(StatisticsData.class);
	@Resource
	private JdbcTemplate jdbcTemplate;
	@Transactional(readOnly = false)
	public void statisticsDataFromMysql(String applicationName, String cubeName, String time) throws IOException, InterruptedException {
		createDataTable(applicationName, cubeName, time);
		logger.debug("生成cube-{}数据",cubeName);
		InsertStatisticsDataToDataBase(applicationName, cubeName, time);
		logger.debug("生成cube-{}数据成功",cubeName);
	}

	@Transactional(readOnly = false)
	public void InsertStatisticsDataToDataBase(String applicationName, String cubeName, String time) {
		String tableName = CubeConfig.getTableName(applicationName, cubeName, time);
		StringBuilder builder = new StringBuilder("insert into " + tableName);
		StringBuilder filed = new StringBuilder("(pt,");
		StringBuilder value = new StringBuilder("select '" + time + "',");
		Cube cube = CubeConfig.getCube(applicationName, cubeName);
		List<Dimension> dimensions = cube.getDimensions(); // 维度
		List<KPI> kpis = cube.getKpis();// 指标
		StringBuilder groupByFiled = new StringBuilder("");
		for (Dimension dimension : dimensions) {
			filed.append(dimension.getDimensionName() + ",");
			value.append(dimension.getFieldName() + ",");
			groupByFiled.append(dimension.getFieldName() + ",");
		}
		for (KPI kpi : kpis) {
			if (kpi.getOperationType().equals(OperationType.UNIQUE)) {
				filed.append(kpi.getFieldName() + ",");
				value.append(kpi.getFieldName() + ",");
				groupByFiled.append(kpi.getFieldName() + ",");
			}
			filed.append(kpi.getKpiName() + ",");
			value.append(kpi.getOperationType().getStatisticsKpiSql(kpi.getFieldName()) + " as " + kpi.getKpiName() + ",");
		}
		filed = filed.replace(filed.length() - 1, filed.length(), "");
		value = value.replace(value.length() - 1, value.length(), "");
		groupByFiled = groupByFiled.replace(groupByFiled.length() - 1, groupByFiled.length(), "");
		value.append(" from " + LogConfig.getTableName(applicationName, cube.getRefLog(), time) + " where pt=" + time);
		value.append(" group by " + groupByFiled.toString());
		builder.append(filed + ") ");
		builder.append(value.toString());
		String sql = builder.toString();
		logger.debug("计算cube_{}数据的语句:{}",cubeName,sql);
		if (!Constant.IS_DEV) {
			jdbcTemplate.execute(sql);
		}
		logger.debug("计算cube_{}数据成功",cubeName);
	}

	/**
	 * 创建统计表结构
	 * 
	 * @param applicationName
	 * @param logName
	 */
	@Transactional(readOnly = false)
	public void createDataTable(String applicationName, String cubeName, String time) {
		String tableName = CubeConfig.getTableName(applicationName, cubeName, time);
		if (getTableCount(tableName)>0) {
			jdbcTemplate.execute("drop table " + tableName); //删除表
		}
		logger.debug("准备创建cube表{}", tableName);
		StringBuilder builder = new StringBuilder("CREATE TABLE ");
		builder.append(tableName + "(");
		builder.append(" `id` bigint(20) NOT NULL AUTO_INCREMENT,pt varchar(20) NOT NULL,");
		Cube cube = CubeConfig.getCube(applicationName, cubeName);
		Log log = LogConfig.getLog(applicationName, cube.getRefLog());
		List<Dimension> dimensions = cube.getDimensions(); // 维度
		List<KPI> kpis = cube.getKpis();// 指标
		for (Dimension dimension : dimensions) {
			String sql = log.getDataType(dimension.getFieldName()).getCreateTableSql(dimension.getDimensionName());
			if (sql != null) {
				builder.append(sql + ",");
			}
		}
		for (KPI kpi : kpis) {
			if (kpi.getOperationType().equals(OperationType.UNIQUE)) {
				String sql = log.getDataType(kpi.getFieldName()).getCreateTableSql(kpi.getFieldName());
				if (sql != null) {
					builder.append(sql + ",");
				}
			}
			builder.append(kpi.getKpiName() + " double not NULL,");
		}
		StringBuilder indexBuilder = new StringBuilder("");
		for (Dimension dimension : dimensions) {
			indexBuilder.append(dimension.getFieldName() + ",");
		}

		for (KPI kpi : kpis) {
			if (kpi.getOperationType().equals(OperationType.UNIQUE)) {
				indexBuilder.append(kpi.getFieldName() + ",");
			}
		}
		indexBuilder = indexBuilder.replace(indexBuilder.length() - 1, indexBuilder.length(), "");
		builder.append("KEY `pt` (`pt`," + indexBuilder.toString() + ")USING HASH,");
		builder.append("PRIMARY KEY (`id`)");
		builder.append(" )ENGINE=InnoDB DEFAULT CHARSET=utf8;");
		logger.debug("创建表{}的语句是{}", tableName, builder.toString());
		if (!Constant.IS_DEV) {
			jdbcTemplate.execute(builder.toString());
		}
		logger.debug("创建表{}成功", tableName);
	}
}
