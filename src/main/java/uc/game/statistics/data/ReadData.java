package uc.game.statistics.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uc.game.statistics.config.CubeConfig;
import uc.game.statistics.config.CubeConfig.Dimension;
import uc.game.statistics.config.CubeConfig.KPI;
import uc.game.statistics.config.DataType;
import uc.game.statistics.config.LogConfig;
import uc.game.statistics.config.LogConfig.Field;
import uc.game.statistics.config.LogConfig.Log;
import uc.game.statistics.config.OperationType;
import uc.game.statistics.utils.Constant;

@Service
public class ReadData extends DataBaseService{
	// logger名称为类的全限定名
	private static final Logger log = LoggerFactory.getLogger(ReadData.class);
	@Resource
	private JdbcTemplate jdbcTemplate;
	@Transactional(readOnly = false)
	public void readDataFromLog(String applicationName, String logName, String time) throws Exception {
		createDataTable(applicationName, logName, time);
		String shellName = Constant.COMMAND_PREFIX + applicationName + "-" + logName;
		log.debug("开始执行日志预处理脚本{}", shellName);
		String command = "sh /home/songshu/apps/statistics/preprocess/" + shellName + " -b " + time;
		Process process = Runtime.getRuntime().exec(command);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String s = null;
		List<String> sqlList = new ArrayList<String>();
		int i = 0;
		while ((s = bufferedReader.readLine()) != null) {
			i++;
			sqlList.add(changeDataToInsertSql(s, applicationName, logName, time));
			if (sqlList.size() == Constant.BATCHSIZE) {
				insertDataToMysql(sqlList);
				sqlList.clear();
			}
		}
		if (sqlList.size() > 0) {
			insertDataToMysql(sqlList);
			sqlList.clear();
		}
		process.waitFor();
		log.debug("执行日志预处理脚本{}完毕,共处理{}条日志", shellName, i);
	}
	
	
	@Transactional(readOnly = false)
	public void insertDataToMysql(List<String> sqls) {
		jdbcTemplate.batchUpdate(sqls.toArray(new String[sqls.size()]));
	}

	public String changeDataToInsertSql(String data, String applicationName, String logName, String time) {
		String tableName = LogConfig.getTableName(applicationName, logName, time);
		String[] values = data.split(Constant.SPLIT);
		StringBuilder builder = new StringBuilder("insert into " + tableName);
		StringBuilder filed = new StringBuilder("(pt,");
		StringBuilder value = new StringBuilder("values ('" + time + "',");
		Log log = LogConfig.getLog(applicationName, logName);
		for (String string : values) {
			if (StringUtils.isBlank(string))
				break;
			String[] v = string.split("=");
			DataType dataType = log.getDataType(v[0]);
			if (dataType == null) {
				continue;
			}
			filed.append(v[0] + ",");
			value.append(dataType.changeInsertValue(v[1]) + ",");
		}
		filed = filed.replace(filed.length() - 1, filed.length(), "");
		value = value.replace(value.length() - 1, value.length(), "");
		builder.append(filed + ") ");
		builder.append(value.toString() + ") ");
		return builder.toString();
	}

	@Transactional(readOnly = false)
	public void createDataTable(String applicationName, String logName, String time) {
		String tableName = LogConfig.getTableName(applicationName, logName, time);
		if (getTableCount(tableName)>0) {
			jdbcTemplate.execute("drop table " + tableName); //删除表
		}
		log.debug("准备创建log表{}", tableName);
		StringBuilder builder = new StringBuilder("CREATE TABLE ");
		builder.append(tableName + "(");
		builder.append(" `id` bigint(20) NOT NULL AUTO_INCREMENT,pt varchar(20) NOT NULL,");
		List<Field> fields = LogConfig.getLog(applicationName, logName).getFields();
		for (Field field : fields) {
			String sql = field.getDataType().getCreateTableSql(field.getName());
			if (sql != null) {
				builder.append(sql + ",");
			}
		}
		List<Dimension> dimensions = CubeConfig.getCube(applicationName, logName).getDimensions();
		StringBuilder indexBuilder = new StringBuilder("");
		for (Dimension dimension : dimensions) {
			indexBuilder.append(dimension.getFieldName() + ",");
		}
		List<KPI> kpis = CubeConfig.getCube(applicationName, logName).getKpis();
		for (KPI kpi : kpis) {
			if (kpi.getOperationType().equals(OperationType.UNIQUE)) {
				indexBuilder.append(kpi.getFieldName() + ",");
			}
		}
		indexBuilder = indexBuilder.replace(indexBuilder.length() - 1, indexBuilder.length(), "");
		builder.append("KEY `pt` (`pt`," + indexBuilder.toString() + ")USING HASH,");
		builder.append("PRIMARY KEY (`id`)");
		builder.append(" )ENGINE=InnoDB DEFAULT CHARSET=utf8");
		log.debug("创建表{}的语句是{}", tableName, builder.toString());
		if (!Constant.IS_DEV) {
			jdbcTemplate.execute(builder.toString());
		}
		log.debug("创建表{}成功", tableName);
	}
}
