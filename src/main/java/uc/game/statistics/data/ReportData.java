package uc.game.statistics.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uc.game.statistics.config.CubeConfig;
import uc.game.statistics.config.DataType;
import uc.game.statistics.config.KpiConfig;
import uc.game.statistics.config.KpiConfig.GeneralKpi;
import uc.game.statistics.config.ReportConfig;
import uc.game.statistics.config.ReportConfig.Dimension;
import uc.game.statistics.config.ReportConfig.KPI;
import uc.game.statistics.config.ReportConfig.Report;
import uc.game.statistics.config.SummaryCubeConfig;
import uc.game.statistics.config.SummaryCubeConfig.SummaryCube;
import uc.game.statistics.utils.Constant;
import uc.game.statistics.utils.TimeUtils;
import uc.game.statistics.view.ReportResp;

@Service
public class ReportData extends DataBaseService{
	 private static final Logger log = LoggerFactory.getLogger(ReportData.class); 
	@Resource
	private JdbcTemplate jdbcTemplate;
	

	public ReportResp findReportData(Map<String, String[]> parameters, String applicationName, String reportName, String beginTime, String endTime) {
		Report report = ReportConfig.getReport(applicationName, reportName);
		StringBuilder builder = new StringBuilder("select ");
		List<Dimension> dimensions = report.getDimensions();
		StringBuilder groupByFiled = new StringBuilder(" group by STAT_TIME,KPINAME");
		StringBuilder where = new StringBuilder(" where 1=1 ");
		List<String> dimension_views = new ArrayList<String>();
		dimension_views.add("STAT_TIME".toLowerCase());
		for (Dimension dimension : dimensions) {
		  if(parameters!=null && parameters.get(dimension.getDimensionName())!=null)
		  {
			  dimension_views.add(dimension.getFieldName());
			  builder.append(dimension.getFieldName()+",");
	          groupByFiled.append(","+dimension.getFieldName());
	          where.append("and "+dimension.getFieldName()+"='"+parameters.get(dimension.getFieldName())[0]+"'");
		  }
		}
		builder.append("STAT_TIME,KPINAME,sum(KPIVALUE) as value");
		builder.append(" from "+report.getTableName());
		builder.append(where.toString());
		builder.append(" and STAT_TIME<='"+endTime+"'");
		builder.append(" and STAT_TIME>='"+beginTime+"'");
		if(groupByFiled.length()>0){
			builder.append(groupByFiled.toString());
		}
		builder.append(" order by STAT_TIME");
		List<Map<String, Object>> map = jdbcTemplate.queryForList(builder.toString());
		ReportResp reportResp = new ReportResp(map, applicationName, reportName,dimension_views);
		return reportResp;
	}
	

	@Transactional(readOnly = false)
	public void statisticsDataFromMysql(String applicationName, String reportName, String time) throws Exception {
		createReportDataTable(applicationName, reportName);
		log.debug("计算{}的{}数据",time,reportName);
		InsertStatisticsDataToReport(applicationName, reportName, time);
		log.debug("计算{}的{}数据成功",time,reportName);
	}

	@Transactional(readOnly = false)
	public void InsertStatisticsDataToReport(String applicationName, String reportName, String time) {
		Report report = ReportConfig.getReport(applicationName, reportName);
		String tableName = report.getTableName();
		StringBuilder builder = null;
		StringBuilder filed = null;
		StringBuilder value = null;
		StringBuilder groupByFiled = null;
		List<Dimension> dimensions = report.getDimensions(); // 维度
		List<KPI> kpis = report.getKpis();// 指标
		if (StringUtils.isNotBlank(report.getRefCube())) {
			log.debug("生成cube报表{}",reportName);
			for (KPI kpi : kpis) {
				filed = new StringBuilder("(");
				value = new StringBuilder(" (select ");
				groupByFiled = new StringBuilder("");
				for (Dimension dimension : dimensions) {
					filed.append(dimension.getFieldName()+",");
					value.append(dimension.getDimensionName() +"_"+kpi.getNewOld()+",");
					groupByFiled.append(dimension.getDimensionName()+"_"+kpi.getNewOld()+",");
				}
				filed.append("STAT_TIME,KPINAME,KPIVALUE");
				groupByFiled = groupByFiled.replace(groupByFiled.length() - 1, groupByFiled.length(), "");
				deleteReportData(applicationName, reportName, time,kpi.getKpiName());
				builder = new StringBuilder("insert into " + tableName);
				builder.append(filed + ") ");
				builder.append(value.toString()+",");
				builder.append(time+",");
				builder.append("'"+kpi.getKpiName()+"',");
				uc.game.statistics.config.CubeConfig.KPI cubeKpi = CubeConfig.getCube(applicationName, report.getRefCube()).getKpiByKpiName(kpi.getKpiName());
				builder.append(cubeKpi.getOperationType().getStatisticsKpiSql(cubeKpi.getFieldName()) + " as " + kpi.getKpiName());
				builder.append(" from " + CubeConfig.getTableName(applicationName, report.getRefCube(), time) + " where 1=1 " + report.getCondition());
				if (StringUtils.isNotBlank(report.getCondition())) {
					builder.append(" and" + report.getCondition());
				}
				builder.append(" group by " + groupByFiled.toString()+")");
				log.debug("计算指标{}的sql语句:{}",kpi.getKpiName(),builder.toString());
				if (!Constant.IS_DEV) {
					jdbcTemplate.execute(builder.toString());
				}
			}
		}else
		{
			SummaryCube summaryCube = SummaryCubeConfig.getCube(applicationName, report.getRefSummaryCube());
			log.debug("生成总表报表{}",reportName);
			for (KPI kpi : kpis) {
				if(StringUtils.isBlank(kpi.getKpiName()))
				{
					continue;
				}
				filed = new StringBuilder("(");
				value = new StringBuilder(" (select ");
				groupByFiled = new StringBuilder("");
				for (Dimension dimension : dimensions) {
					filed.append(dimension.getFieldName()+",");
					value.append(dimension.getDimensionName() +"_"+kpi.getNewOld()+",");
					groupByFiled.append(dimension.getDimensionName()+"_"+kpi.getNewOld()+",");
				}
				filed.append("STAT_TIME,KPINAME,KPIVALUE");
				groupByFiled = groupByFiled.replace(groupByFiled.length() - 1, groupByFiled.length(), "");
				GeneralKpi generalKpi = KpiConfig.getGeneralKpi(kpi.getKpiName());
				deleteReportData(applicationName, reportName,TimeUtils.getNewFormatDate(time,"yyyyMMdd",generalKpi.getTimeAdd()),kpi.getKpiName());
				builder = new StringBuilder("insert into " + tableName);
				builder.append(filed + ") ");
				builder.append(value.toString());
				builder.append(TimeUtils.getNewFormatDate(time,"yyyyMMdd",generalKpi.getTimeAdd())+",");
				builder.append("'"+kpi.getKpiName()+"',");
				String expr = generalKpi.getExpr(SummaryCubeConfig.getTableName(applicationName, report.getRefSummaryCube(), time),
						SummaryCubeConfig.getWholeTableName(applicationName, report.getRefSummaryCube()),
						CubeConfig.getCube(applicationName,summaryCube.getRefCube()).getUniKey(summaryCube.getUvName()),
						summaryCube.getPvName(), time);
				if(expr == null)
				{
					log.debug("放弃计算{}的指标{}",TimeUtils.getNewFormatDate(time,"yyyyMMdd",generalKpi.getTimeAdd()),generalKpi.getKpiName());
					continue;
				}
				builder.append(expr);
				
				builder.append(" group by " + groupByFiled.toString()+");");
			    String sql = builder.toString();
			    log.debug("计算指标{}的sql语句:{}",kpi.getKpiName(),sql);
				if (!Constant.IS_DEV) {
					jdbcTemplate.execute(sql);
				}
			}
		}
	}

	@Transactional(readOnly = false)
	public void deleteReportData(String applicationName, String reportName, String time,String kpiName)
	{
		Report report = ReportConfig.getReport(applicationName, reportName);
		String tableName = report.getTableName();
		jdbcTemplate.execute("delete from "+tableName +" where STAT_TIME='"+time+"' and KPINAME='"+kpiName+"'");
	}
	
	
	/**
	 * 创建统计表结构
	 * 
	 * @param applicationName
	 * @param logName
	 */
	@Transactional(readOnly = false)
	public void createReportDataTable(String applicationName, String reportName) {
		Report report = ReportConfig.getReport(applicationName, reportName);
		String tableName = report.getTableName();
		int tableCount = getTableCount(tableName);
		if (tableCount <= 0) {
			log.debug("准备创建表{}",tableName);
			StringBuilder builder = new StringBuilder("CREATE TABLE ");
			builder.append(tableName + "(");
			builder.append(" `id` bigint(20) NOT NULL AUTO_INCREMENT,STAT_TIME varchar(20) NOT NULL,");
			builder.append("`KPINAME`  varchar(50) NOT NULL,");
			builder.append("`KPIVALUE`  DOUBLE NOT NULL,");
			List<Dimension> dimensions = report.getDimensions(); // 维度
			for (Dimension dimension : dimensions) {
				String sql = DataType.string.getCreateTableSql(dimension.getFieldName());
				if (sql != null) {
					builder.append(sql + ",");
				}
			}
			StringBuilder indexBuilder = new StringBuilder("");
			for (Dimension dimension : dimensions) {
				indexBuilder.append(dimension.getFieldName() + ",");
			}
			indexBuilder = indexBuilder.replace(indexBuilder.length() - 1, indexBuilder.length(), "");
			builder.append("KEY `pt` (`STAT_TIME`,`KPINAME`," + indexBuilder.toString() + ")USING HASH,");
			builder.append("PRIMARY KEY (`id`)");
			builder.append(" )ENGINE=InnoDB DEFAULT CHARSET=utf8;");
			log.debug("创建表{}的语句是{}",tableName,builder.toString());
			if (!Constant.IS_DEV) {
				jdbcTemplate.execute(builder.toString());
			}
			log.debug("创建表{}成功",tableName);
		}
	}
}
