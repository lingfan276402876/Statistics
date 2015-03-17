package uc.game.statistics.config;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import uc.game.statistics.view.ReportDto;

public class ReportViewConfig {
	public static Map<String, Map<String, ReportView>> map = new HashMap<String, Map<String, ReportView>>();
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
		Document document = reader.read(Thread.currentThread().getContextClassLoader().getResource("reportView.xml"));
		Element root = document.getRootElement();
		List<Element> applications = root.elements();
		ReportViewConfig config = new ReportViewConfig();
		for (Element application : applications) {
			Map<String, ReportView> reportMap = new HashMap<String, ReportView>();
			String applicationName = application.attributeValue("applicationName");
			List<Element> reports = application.elements();
			for (Element element : reports) {
				ReportView report = config.new ReportView();
				report.setReportName(element.attributeValue("reportName"));
				report.setViewName(element.attributeValue("viewName"));
				List<Element> dimensions = element.elements();
				for (Element view_Element : dimensions) {
					report.getViews().add(config.new View(view_Element.attributeValue("viewName"), view_Element.attributeValue("fieldName"),view_Element.attributeValue("aggregate"),
							view_Element.attributeValue("isDimension")));
				}
				reportMap.put(report.getReportName(), report);
			}
			map.put(applicationName, reportMap);
		}
	}

	public class ReportView {
		private String reportName;
		private String viewName;
		private List<View> views = new ArrayList<View>();

		public String getReportName() {
			return reportName;
		}

		public void setReportName(String reportName) {
			this.reportName = reportName;
		}

		public String getViewName() {
			return viewName;
		}

		public void setViewName(String viewName) {
			this.viewName = viewName;
		}

		public List<View> getViews() {
			return views;
		}

		public void setViews(List<View> views) {
			this.views = views;
		}
	}

	/**
	 * 指标展示
	 */
	public class View {
		private String viewName;
		private String fieldName;
        private String aggregate;
        private boolean isDimension=false;
		public View(String viewName, String fieldName,String aggregate,String isDimension) {
			super();
			this.viewName = viewName;
			this.fieldName = fieldName;
			this.aggregate = aggregate;
			if(StringUtils.isNotBlank(isDimension))
			{
				this.isDimension = Boolean.valueOf(isDimension);
			}
		}

		public String getViewName() {
			return viewName;
		}

		public void setViewName(String viewName) {
			this.viewName = viewName;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public String getAggregate() {
			return aggregate;
		}

		public void setAggregate(String aggregate) {
			this.aggregate = aggregate;
		}
		
		public boolean isDimension() {
			return isDimension;
		}

		public void setDimension(boolean isDimension) {
			this.isDimension = isDimension;
		}

		public String calculate(ReportDto reportDto)
		{
			double first = 0.0;
			if(reportDto.getValue(this.aggregate.split("/")[0])==null)
			{
				return "0";
			}
			first = Double.parseDouble(reportDto.getValue(this.aggregate.split("/")[0]));
			double second = 0.0;
			if(reportDto.getValue(this.aggregate.split("/")[1])==null)
			{
				return "0";
			}
			second = Double.parseDouble(reportDto.getValue(this.aggregate.split("/")[1]));
			NumberFormat numberFormat = NumberFormat.getNumberInstance();
			numberFormat.setMinimumFractionDigits(2);
			return numberFormat.format(first/second);
		}
		
	}
	
	public static ReportView getReport(String applicationName, String reportName) {
		return map.get(applicationName).get(reportName);
	}
	
	public static List<ReportView> getReportNames(String applicationName) {
		return new ArrayList<ReportViewConfig.ReportView>(map.get(applicationName).values());
	}
}
