package uc.game.statistics.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import uc.game.statistics.exception.GeneralLogicException;

public class ReportConfig {
	public static Map<String, Map<String, Report>> map = new HashMap<String, Map<String, Report>>();
	static {
		try {
			initialize();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	public static void reload()
	{
		try {
			initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	private static void initialize() throws Exception {
		SAXReader reader = new SAXReader();
		Document document = reader.read(Thread.currentThread().getContextClassLoader().getResource("reports.xml"));
		Element root = document.getRootElement();
		List<Element> applications = root.elements();
		ReportConfig config = new ReportConfig();
		for (Element application : applications) {
			Map<String, Report> reportMap = new HashMap<String, Report>();
			String applicationName = application.attributeValue("applicationName");
			List<Element> reports = application.elements();
			for (Element element : reports) {
				Report report = config.new Report();
				report.setReportName(element.attributeValue("reportName"));
				report.setRefCube(element.attributeValue("refCube"));
				report.setRefSummaryCube(element.attributeValue("refSummaryCube"));
				report.setTableName(element.attributeValue("tableName"));
				report.setCondition(element.attributeValue("condition"));
				List<Element> dimensions = element.element("dimensions").elements();
				for (Element dimension_Element : dimensions) {
					report.getDimensions().add(new ReportConfig().new Dimension(dimension_Element.attributeValue("fieldName"), dimension_Element.attributeValue("dimensionName")));
				}
				List<Element> kpis = element.element("kpis").elements();
				for (Element kpis_Element : kpis) {
					report.getKpis().add(config.new KPI(kpis_Element.attributeValue("kpiName"), kpis_Element.attributeValue("fieldName"),kpis_Element.attributeValue("newOld")));
				}
				reportMap.put(report.getReportName(), report);
			}
			map.put(applicationName, reportMap);
		}
	}

	public class Report {
		private String reportName;
		private String refCube;
		private String refSummaryCube;
		private String tableName;
		private String condition;
		private List<Dimension> dimensions = new ArrayList<Dimension>();
		private List<KPI> kpis = new ArrayList<KPI>();

		public String getReportName() {
			return reportName;
		}

		public void setReportName(String reportName) {
			this.reportName = reportName;
		}

		public String getRefCube() {
			return refCube;
		}

		public void setRefCube(String refCube) {
			this.refCube = refCube;
		}

		public String getRefSummaryCube() {
			return refSummaryCube;
		}

		public void setRefSummaryCube(String refSummaryCube) {
			this.refSummaryCube = refSummaryCube;
		}

		public List<Dimension> getDimensions() {
			return dimensions;
		}

		public void setDimensions(List<Dimension> dimensions) {
			this.dimensions = dimensions;
		}

		public List<KPI> getKpis() {
			return kpis;
		}

		public void setKpis(List<KPI> kpis) {
			this.kpis = kpis;
		}

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public String getCondition() {
			return condition;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}

		public String getFieldNameByDimensionName(String dimensionName) {
			for (Dimension dimension : this.dimensions) {
				if (dimension.getDimensionName().equals(dimensionName))
					return dimension.getFieldName();
			}
			throw new GeneralLogicException("总表维度" + dimensionName + "定义错误，必须和统计类型的维度定义一致");
		}
	}

	/**
	 * 维度
	 */
	public class Dimension {
		private String fieldName;
		private String dimensionName;
       
		public Dimension(String fieldName, String dimensionName) {
			super();
			this.fieldName = fieldName;
			this.dimensionName = dimensionName;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public String getDimensionName() {
			return dimensionName;
		}

		public void setDimensionName(String dimensionName) {
			this.dimensionName = dimensionName;
		}
	}

	/**
	 * 指标
	 */
	public class KPI {
		private String kpiName;
		private String fieldName;
		private String newOld;
		public KPI(String kpiName, String fieldName,String newOld) {
			super();
			this.kpiName = kpiName;
			this.fieldName = fieldName;
			this.newOld = newOld;
		}

		public String getKpiName() {
			return kpiName;
		}

		public void setKpiName(String kpiName) {
			this.kpiName = kpiName;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
		public String getNewOld() {
			return newOld;
		}

		public void setNewOld(String newOld) {
			this.newOld = newOld;
		}
	}

	public static Report getReport(String applicationName, String reportName) {
		return map.get(applicationName).get(reportName);
	}

	public static List<Report> getReportsByCubeName(String applicationName, String cubeName) {
		List<Report> reports = new ArrayList<Report>();
		Map<String, Report> reportMap = map.get(applicationName);
		Collection<Report> collection = reportMap.values();
		for (Report report : collection) {
           if(report.getRefCube().equals(cubeName))
           {
        	   reports.add(report);
           }
		}
		return reports;
	}
	public static List<Report> getReportsBySummaryCubeName(String applicationName, String summaryCubeName) {
		List<Report> reports = new ArrayList<Report>();
		Map<String, Report> reportMap = map.get(applicationName);
		Collection<Report> collection = reportMap.values();
		for (Report report : collection) {
           if(report.getRefSummaryCube().equals(summaryCubeName))
           {
        	   reports.add(report);
           }
		}
		return reports;
	}
}
