package uc.game.statistics.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uc.game.statistics.config.CubeConfig;
import uc.game.statistics.config.CubeConfig.Cube;
import uc.game.statistics.config.CubeConfig.KPI;
import uc.game.statistics.config.DataType;
import uc.game.statistics.config.LogConfig;
import uc.game.statistics.config.LogConfig.Log;
import uc.game.statistics.config.OperationType;
import uc.game.statistics.config.SummaryCubeConfig;
import uc.game.statistics.config.SummaryCubeConfig.CubeDimension;
import uc.game.statistics.config.SummaryCubeConfig.SummaryCube;
import uc.game.statistics.utils.Constant;
import uc.game.statistics.utils.TimeUtils;

@Service
@Transactional(readOnly = true)
public class SummaryData  extends DataBaseService{
	public static Logger logger = LoggerFactory.getLogger(SummaryData.class);
	@Resource
	private JdbcTemplate jdbcTemplate;

	@Transactional(readOnly = false)
	public void statisticsDataFromMysql(String applicationName, String summaryCubeName, String time) throws IOException, InterruptedException {
		logger.debug("生成总表{}数据", summaryCubeName);
		createSummaryDataTable(applicationName, summaryCubeName, time);
		createWholeSummaryDataTable(applicationName, summaryCubeName);
		updateSummaryData(applicationName, summaryCubeName, time);
		insertSummaryData(applicationName, summaryCubeName, time);
		insertWholeSummaryData(applicationName, summaryCubeName, time);
		logger.debug("生成总表{}数据成功", summaryCubeName);
	}

	/**
	 * 更新120天总表
	 * 
	 * @param applicationName
	 * @param logName
	 * @param time
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateSummaryData(String applicationName, String summaryCubeName, String time) {
		SummaryCube summaryCube = SummaryCubeConfig.getCube(applicationName, summaryCubeName);
		String cubeTableName = CubeConfig.getTableName(applicationName, summaryCube.getRefCube(), time);
		String summaryTableName = SummaryCubeConfig.getTableName(applicationName, summaryCubeName, time);
		String uniKey = CubeConfig.getCube(applicationName, summaryCube.getRefCube()).getUniKey(summaryCube.getUvName());
		StringBuilder selectOldDataSql = new StringBuilder("select ");
		List<CubeDimension> dimensions = summaryCube.getDimensions(); // 维度
		for (CubeDimension dimension : dimensions) {
			selectOldDataSql.append(dimension.getDimensionName() + ",");
		}
		selectOldDataSql.append(uniKey);
		selectOldDataSql.append(" from " + cubeTableName + " as A where exists(select " + uniKey + " from " + summaryTableName + " where " + uniKey + "=A." + uniKey + ")");
		List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
		if (!Constant.IS_DEV) {
			logger.debug(selectOldDataSql.toString());
			values = jdbcTemplate.queryForList(selectOldDataSql.toString());
		}
		for (Map<String, Object> map : values) {
			Object uniValue = null;
			StringBuilder updateDataSql = new StringBuilder("update " + summaryTableName + " set ");
			List<Object> objects = new ArrayList<Object>();
			Set<String> files = map.keySet();
			for (String filed : files) {
				if (filed.equals(uniKey))
					continue;
				updateDataSql.append(filed + "_new=?,");
				objects.add(map.get(filed));
			}
			uniValue = map.get(uniKey);
			updateDataSql.append("modify_time=?,");
			objects.add(time);
			if (!checkIsExists(applicationName, summaryCubeName, uniValue, uniKey, TimeUtils.getNewFormatDate(time,"yyyyMMdd", -30))) {
				updateDataSql.append("active_30=active_30+1,");
			}
			if (!checkIsExists(applicationName, summaryCubeName, uniValue, uniKey, TimeUtils.getNewFormatDate(time,"yyyyMMdd", -120))) {
				updateDataSql.append("active_set=active_set+1,");
			}
			updateDataSql.append("pt=? where " + uniKey + " =?");
			objects.add(time);
			objects.add(uniValue);
			logger.debug("更新120天总表{}的语句:{}",summaryTableName,updateDataSql.toString());
			if (!Constant.IS_DEV) {
				jdbcTemplate.update(updateDataSql.toString(), objects.toArray());
			}
			logger.debug("更新120天总表{}成功",summaryTableName);
		}
	}

	public boolean checkIsExists(String applicationName, String summaryCubeName, Object uniValue, String uinKey, String time) {
		String tableName = SummaryCubeConfig.getTableName(applicationName, summaryCubeName, time);
		int tableCount = getTableCount(tableName);
		if (tableCount <= 0) {
			return false;
		}
		String summaryTableName = SummaryCubeConfig.getTableName(applicationName, summaryCubeName, time);
		StringBuilder selectSql = new StringBuilder("select count(id) from " + summaryTableName + " ");
		selectSql.append("where ");
		selectSql.append(uinKey + "='" + uniValue);
		selectSql.append("' and modify_time='" + time + "'");
		int result = jdbcTemplate.queryForInt(selectSql.toString());
		if (result > 0) {
			return true;
		}
		return false;
	}

	/**
	 * 向120天总表中插入数据
	 * 
	 * @param applicationName
	 * @param logName
	 * @param time
	 */
	@Transactional(readOnly = false)
	public void insertSummaryData(String applicationName, String summaryCubeName, String time) {
		SummaryCube summaryCube = SummaryCubeConfig.getCube(applicationName, summaryCubeName);
		String cubeTableName = CubeConfig.getTableName(applicationName, summaryCube.getRefCube(), time);
		String summaryTableName = SummaryCubeConfig.getTableName(applicationName, summaryCubeName, time);
		StringBuilder summary = new StringBuilder("insert into " + summaryTableName + "(");
		List<CubeDimension> dimensions = summaryCube.getDimensions(); // 维度
		for (CubeDimension dimension : dimensions) {
			summary.append(dimension.getDimensionName() + "_new,");
			summary.append(dimension.getDimensionName() + "_old,");
		}
		String uinKey = CubeConfig.getCube(applicationName, summaryCube.getRefCube()).getUniKey(summaryCube.getUvName());
		if (StringUtils.isNotBlank(uinKey)) {
			summary.append(uinKey + ",");
		}
		summary.append(summaryCube.getPvName() + ",");
		summary.append("active_30,active_set,modify_time,pt) ");
		summary.append(" (select ");
		for (CubeDimension dimension : dimensions) {
			summary.append(dimension.getDimensionName() + " as " + dimension.getDimensionName() + "_new,");
			summary.append(dimension.getDimensionName() + " as " + dimension.getDimensionName() + "_old,");
		}
		for (KPI kpi : CubeConfig.getCube(applicationName, summaryCube.getRefCube()).getKpis()) {
			if (kpi.getOperationType().equals(OperationType.UNIQUE) && kpi.getKpiName().equals(summaryCube.getUvName())) {
				summary.append(kpi.getFieldName() + ",");
			}
		}
		summary.append(summaryCube.getPvName() + ",");
		summary.append("'1','1','" + time + "','" + time + "' ");
		summary.append(" from " + cubeTableName + " as A where  not exists(select " + uinKey + " from " + summaryTableName + " where " + uinKey + "=A." + uinKey + "))");
		logger.debug("向120天总表{}插入数据的语句:{}",summaryTableName,summary.toString());
		if (!Constant.IS_DEV) {
			jdbcTemplate.execute(summary.toString());
		}
		logger.debug("向120天总表{}插入数据成",summaryTableName);
	}

	/**
	 * 向累计总表中插入数据
	 * 
	 * @param applicationName
	 * @param logName
	 * @param time
	 */
	@Transactional(readOnly = false)
	public void insertWholeSummaryData(String applicationName, String summaryCubeName, String time) {
		SummaryCube summaryCube = SummaryCubeConfig.getCube(applicationName, summaryCubeName);
		String cubeTableName = CubeConfig.getTableName(applicationName, summaryCube.getRefCube(), time);
		String wholeTableName = SummaryCubeConfig.getWholeTableName(applicationName, summaryCubeName);
		StringBuilder whole = new StringBuilder("insert into " + wholeTableName + "(");
		List<CubeDimension> dimensions = summaryCube.getDimensions(); // 维度
		for (CubeDimension dimension : dimensions) {
			whole.append(dimension.getDimensionName() + "_old,");
		}
		String uinKey = CubeConfig.getCube(applicationName, summaryCube.getRefCube()).getUniKey(summaryCube.getUvName());
		if (StringUtils.isNotBlank(uinKey)) {
			whole.append(uinKey + ",");
		}
		whole.append("create_time) ");
		whole.append(" (select ");
		for (CubeDimension dimension : dimensions) {
			whole.append(dimension.getDimensionName() + ",");
		}
		for (KPI kpi : CubeConfig.getCube(applicationName, summaryCube.getRefCube()).getKpis()) {
			if (kpi.getOperationType().equals(OperationType.UNIQUE) && kpi.getKpiName().equals(summaryCube.getUvName())) {
				whole.append(kpi.getFieldName() + ",");
			}
		}
		whole.append("'" + time + "'");
		whole.append(" from " + cubeTableName + " as A where  not exists(select " + uinKey + " from " + wholeTableName + " where " + uinKey + "=A." + uinKey + "))");
		logger.debug("向累计总表{}插入数据的语句:{}",wholeTableName,whole.toString());
		if (!Constant.IS_DEV) {
			jdbcTemplate.execute(whole.toString());
		}
		logger.debug("向累计总表{}插入数据成功",wholeTableName);
	}

	/**
	 * 创建120天总表表结构
	 * 
	 * @param applicationName
	 * @param logName
	 */
	@Transactional(readOnly = false)
	private void createSummaryDataTable(String applicationName, String summaryCubeName, String time) {
		String tableName = SummaryCubeConfig.getTableName(applicationName, summaryCubeName, time);
		if (getTableCount(tableName)>0) {
			jdbcTemplate.execute("drop table " + tableName); //删除表
		}
		logger.debug("准备创建120天总表{}", tableName);
		StringBuilder builder = new StringBuilder("CREATE TABLE ");
		builder.append(tableName + "(");
		String old_tableName = SummaryCubeConfig.getTableName(applicationName, summaryCubeName, TimeUtils.getNewFormatDate(time,"yyyyMMdd", -1));
		int tableCount = getTableCount(old_tableName);
		if (tableCount > 0) {
			builder = new StringBuilder("CREATE TABLE " + tableName);
			builder.append(" like " + old_tableName + ";");
			jdbcTemplate.execute(builder.toString()); // 创建表结构以及索引
			builder = new StringBuilder("insert into " + tableName + "(");
			builder.append("select * from " + old_tableName + " where modify_time>='" + TimeUtils.getNewFormatDate(time,"yyyyMMdd", -120) + "')");
			logger.debug("创建120天总表{}，采用复制表结构方式,语句是：{}", tableName,builder.toString());
		} else {
			logger.debug("创建120天总表{}，采用新建表结构的方式", tableName);
			builder.append(" `id` bigint(20) NOT NULL AUTO_INCREMENT,");
			builder.append("pt varchar(20) NOT NULL,");
			SummaryCube summaryCube = SummaryCubeConfig.getCube(applicationName, summaryCubeName);
			Cube cube = CubeConfig.getCube(applicationName, summaryCube.getRefCube());
			Log log = LogConfig.getLog(applicationName, cube.getRefLog());
			List<CubeDimension> dimensions = summaryCube.getDimensions(); // 维度
			for (CubeDimension dimension : dimensions) {
				DataType dataType = log.getDataType(cube.getFieldNameByDimensionName(dimension.getDimensionName()));
				String sql = dataType.getCreateTableSql(dimension.getDimensionName() + "_new");
				if (sql != null) {
					builder.append(sql + ",");
				}
				sql = dataType.getCreateTableSql(dimension.getDimensionName() + "_old");
				if (sql != null) {
					builder.append(sql + ",");
				}
			}
			builder.append("modify_time varchar(20) NOT NULL,");
			builder.append("active_30 int(10) NOT NULL,");
			builder.append("active_set int(10) NOT NULL,");
			for (KPI kpi : cube.getKpis()) {
				if (kpi.getOperationType().equals(OperationType.UNIQUE) && kpi.getKpiName().equals(summaryCube.getUvName())) {
					String sql = log.getDataType(kpi.getFieldName()).getCreateTableSql(kpi.getFieldName());
					if (sql != null) {
						builder.append(sql + ",");
					}
				}
			}
			builder.append(summaryCube.getPvName() + " double NOT NULL,");
			for (CubeDimension dimension : dimensions) {
				builder.append("KEY `" + dimension.getDimensionName() + "_new` (pt,modify_time,`" + dimension.getDimensionName() + "_new`)USING HASH,");
				builder.append("KEY `" + dimension.getDimensionName() + "_old` (pt,modify_time,`" + dimension.getDimensionName() + "_old`)USING HASH,");
			}
			builder.append("KEY `pt` (pt,modify_time," + summaryCube.getPvName() + ") USING HASH,");
			builder.append("PRIMARY KEY (`id`)");
			builder.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
			logger.debug("创建120天总表{}，建表语句是：{}", tableName,builder.toString());
		}
		if (!Constant.IS_DEV) {
			jdbcTemplate.execute(builder.toString());
		}
		logger.debug("创建120天总表{}成功", tableName);
	}

	/**
	 * 创建累计总表表结构
	 * 
	 * @param applicationName
	 * @param logName
	 */
	@Transactional(readOnly = false)
	private void createWholeSummaryDataTable(String applicationName, String summaryCubeName) {
		String tableName = SummaryCubeConfig.getWholeTableName(applicationName, summaryCubeName);
		int tableCount = getTableCount(tableName);
		if (tableCount <= 0) {
			logger.debug("准备创建累计总表{}", tableName);
			StringBuilder whole = new StringBuilder("CREATE TABLE ");
			whole.append(tableName + " (");
			whole.append("`id` bigint(20) NOT NULL AUTO_INCREMENT,");
			SummaryCube summaryCube = SummaryCubeConfig.getCube(applicationName, summaryCubeName);
			Cube cube = CubeConfig.getCube(applicationName, summaryCube.getRefCube());
			Log log = LogConfig.getLog(applicationName, cube.getRefLog());
			List<CubeDimension> dimensions = summaryCube.getDimensions(); // 维度
			for (CubeDimension dimension : dimensions) {
				DataType dataType = log.getDataType(cube.getFieldNameByDimensionName(dimension.getDimensionName()));
				String sql = dataType.getCreateTableSql(dimension.getDimensionName() + "_old");
				if (sql != null) {
					whole.append(sql + ",");
				}
			}
			whole.append("create_time int(20) NOT NULL,");
			for (KPI kpi : cube.getKpis()) {
				if (kpi.getOperationType().equals(OperationType.UNIQUE) && kpi.getKpiName().equals(summaryCube.getUvName())) {
					String sql = log.getDataType(kpi.getFieldName()).getCreateTableSql(kpi.getFieldName());
					if (sql != null) {
						whole.append(sql + ",");
						whole.append("KEY `" + kpi.getFieldName() + "` (" + kpi.getFieldName() + "),");
					}
				}
			}
			for (CubeDimension dimension : dimensions) {
				whole.append("KEY `" + dimension.getDimensionName() + "_old` (create_time," + dimension.getDimensionName() + "_old)USING HASH,");
			}
			whole.append("PRIMARY KEY (`id`)");
			whole.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
			logger.debug("准备创建累计总表{},语句：{}", tableName,whole.toString());
			if (!Constant.IS_DEV) {
				jdbcTemplate.execute(whole.toString());
			}
			logger.debug("准备创建累计总表{}成功", tableName);
		}
	}

	public static void main(String[] args) {
		// new SummaryData().createSummaryDataTable("songshu",
		// "login","20140418");
		// new SummaryData().createWholeSummaryDataTable("songshu", "login");
		new SummaryData().insertSummaryData("songshu", "login", "20140418");

		// new SummaryData().insertWholeSummaryData("songshu",
		// "login","20140418");
		// new ReadData().createDataTable("songshu", "login","20140418");
		// System.out.println(new
		// ReadData().changeDataToInsertSql("time=1397712953450`ucid=982385357`count=15`platform=04`os=0`version=pp助手版本",
		// "songshu", "login", "20140418"));
		// new StatisticsData().InsertStatisticsDataToDataBase("songshu",
		// "login", "20140418");
		// new StatisticsData().createDataTable("songshu", "login","20140418");

		// new ReportData().createReportDataTable("songshu", "songshu_flux");
		// new ReportData().InsertStatisticsDataToReport("songshu",
		// "songshu_new_year", "20140418");

	}
}
