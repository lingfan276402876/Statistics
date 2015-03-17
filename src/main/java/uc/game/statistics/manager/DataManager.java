package uc.game.statistics.manager;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uc.game.statistics.config.CubeConfig;
import uc.game.statistics.config.CubeConfig.Cube;
import uc.game.statistics.config.KpiConfig;
import uc.game.statistics.config.LogConfig;
import uc.game.statistics.config.LogConfig.Log;
import uc.game.statistics.config.ReportConfig;
import uc.game.statistics.config.ReportConfig.Report;
import uc.game.statistics.config.ReportViewConfig;
import uc.game.statistics.config.SummaryCubeConfig;
import uc.game.statistics.config.SummaryCubeConfig.SummaryCube;
import uc.game.statistics.data.ReadData;
import uc.game.statistics.data.ReportData;
import uc.game.statistics.data.StatisticsData;
import uc.game.statistics.data.SummaryData;
import uc.game.statistics.utils.TimeUtils;

@Service
public class DataManager {
	private static final Logger logger = LoggerFactory.getLogger(StatisticsData.class);
	@Resource
	private ReadData readData;
	@Resource
	private StatisticsData statisticsData;
	@Resource
	private SummaryData summaryData;
	@Resource
	private ReportData reportData;

	private String applicationName;

	private String logName;

	public DataManager(String applicationName, String logName) {
		this.applicationName = applicationName;
		this.logName = logName;

	}

	public DataManager() {

	}

	public void readData() {
		readData(null);
	}

	public void readData(String time) {
		if (time == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DAY_OF_YEAR, -1);
			time = TimeUtils.formatDate(calendar.getTimeInMillis(),"yyyyMMdd");
		}
		try {
			logger.debug("日期为{}的{}应用日志{}分析任务开启", time, applicationName, logName);
			Log log = LogConfig.getLog(applicationName, logName);
			readData.readDataFromLog(applicationName, log.getLogName(), time);
			List<Cube> cubes = CubeConfig.getCubesByLogName(applicationName, log.getLogName());
			for (Cube cube : cubes) {
				statisticsData.statisticsDataFromMysql(applicationName, cube.getCubeName(), time);
				List<SummaryCube> summaryCubes = SummaryCubeConfig.getSummaryCubesByCubeName(applicationName, cube.getCubeName());
				for (SummaryCube summaryCube : summaryCubes) {
					summaryData.statisticsDataFromMysql(applicationName, summaryCube.getSummaryCubeName(), time);
					List<Report> reports = ReportConfig.getReportsBySummaryCubeName(applicationName, summaryCube.getSummaryCubeName());
					for (Report report : reports) {
						reportData.statisticsDataFromMysql(applicationName, report.getReportName(), time);
					}
				}
				List<Report> reports = ReportConfig.getReportsByCubeName(applicationName, cube.getCubeName());
				for (Report report : reports) {
					reportData.statisticsDataFromMysql(time, report.getReportName(), time);
				}
			}
			logger.debug("日期为{}的{}应用日志{}分析任务结束", time, applicationName, logName);
		} catch (Exception e) {
			logger.debug("日期为{}的{}应用日志{}分析任务出现异常，异常信息：{}", time, applicationName, logName, e.getMessage());
		}
	}

	public void readDataAll(String time) {
		
		if (time == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DAY_OF_YEAR, -1);
			time = TimeUtils.formatDate(calendar.getTimeInMillis(),"yyyyMMdd");
		}
		Set<String> applications = LogConfig.map.keySet();
		for (String applicationName : applications) {
			Map<String, Log> logMap = LogConfig.map.get(applicationName);
			Set<String> logs = logMap.keySet();
			for (String logName : logs) {
				logger.debug("日期为{}的{}应用日志{}分析任务开启", time, applicationName, logName);
				Log log = logMap.get(logName);
				try {
					readData.readDataFromLog(applicationName, log.getLogName(), time);
					List<Cube> cubes = CubeConfig.getCubesByLogName(applicationName, log.getLogName());
					for (Cube cube : cubes) {
						statisticsData.statisticsDataFromMysql(applicationName, cube.getCubeName(), time);
						List<SummaryCube> summaryCubes = SummaryCubeConfig.getSummaryCubesByCubeName(applicationName, cube.getCubeName());
						for (SummaryCube summaryCube : summaryCubes) {
							summaryData.statisticsDataFromMysql(applicationName, summaryCube.getSummaryCubeName(), time);
							List<Report> reports = ReportConfig.getReportsBySummaryCubeName(applicationName, summaryCube.getSummaryCubeName());
							for (Report report : reports) {
								reportData.statisticsDataFromMysql(applicationName, report.getReportName(), time);
							}
						}
						List<Report> reports = ReportConfig.getReportsByCubeName(applicationName, cube.getCubeName());
						for (Report report : reports) {
							reportData.statisticsDataFromMysql(time, report.getReportName(), time);
						}
					}
					logger.debug("日期为{}的{}应用日志{}分析任务结束", time, applicationName, logName);
				} catch (Exception e) {
					logger.debug("日期为{}的{}应用日志{}分析任务出现异常，异常信息：{}", time, applicationName, logName, e.getMessage());
				}
			}
		}
	}
	
	public void initConfig()
	{
		CubeConfig.reload();
		KpiConfig.reload();
		LogConfig.reload();
		ReportConfig.reload();
		ReportViewConfig.reload();
		SummaryCubeConfig.reload();
	}
}