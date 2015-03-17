package uc.game.statistics.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uc.game.statistics.config.ReportViewConfig;
import uc.game.statistics.config.ReportViewConfig.ReportView;
import uc.game.statistics.config.ReportViewConfig.View;

public class ReportResp {
	private String reportName;
	private List<View> viewNames = new ArrayList<View>();

	private List<GeneralReport> viewValues = new ArrayList<GeneralReport>();

	public List<View> getViewNames() {
		return viewNames;
	}

	public void setViewNames(List<View> viewNames) {
		this.viewNames = viewNames;
	}

	public List<GeneralReport> getViewValues() {
		return viewValues;
	}

	public void setViewValues(List<GeneralReport> viewValues) {
		this.viewValues = viewValues;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public ReportResp(List<Map<String, Object>> list, String applicationName, String reportName, List<String> dimension_views) {
		ReportView reportView = ReportViewConfig.getReport(applicationName, reportName);
		this.reportName = reportView.getViewName();
		List<View> views = reportView.getViews();
		for (View view : views) {
			if (view.isDimension()) {
				if (dimension_views.contains(view.getFieldName())) {
					viewNames.add(view);
				}
			} else {
				viewNames.add(view);
			}

		}
		List<ReportDto> reportDtos = ReportDto.getReportDtos(list);
         for (ReportDto reportDto : reportDtos) {
    	   viewValues.add(new GeneralReport(reportDto,viewNames));
		}
	}
}
