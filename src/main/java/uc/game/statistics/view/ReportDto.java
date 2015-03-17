package uc.game.statistics.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import uc.game.statistics.utils.TimeUtils;

public class ReportDto {
	private String stat_tiem;
	private List<KpiView> views = new ArrayList<ReportDto.KpiView>();

	public static class KpiView {
		private String viewName;
		private String viewValue;

		public KpiView(Object viewName, Object viewValue) {
			super();
			this.viewName = String.valueOf(viewName);
			this.viewValue = String.valueOf(viewValue);
		}

		public String getViewName() {
			return viewName;
		}

		public void setViewName(String viewName) {
			this.viewName = viewName;
		}

		public String getViewValue() {
			return viewValue;
		}

		public void setViewValue(String viewValue) {
			this.viewValue = viewValue;
		}
	}

	public String getStat_tiem() {
		return stat_tiem;
	}

	public void setStat_tiem(String stat_tiem) {
		this.stat_tiem = TimeUtils.formatDate(TimeUtils.getMilliseconds(stat_tiem, "yyyyMMdd"),"dd/MM/yyyy");
	}

	public List<KpiView> getViews() {
		return views;
	}

	public void setViews(List<KpiView> views) {
		this.views = views;
	}
    
	public String getValue(String kpiName)
	{
		for(KpiView kpiView : this.views){
			if(kpiName.equals(kpiView.getViewName()))
			{
				return kpiView.getViewValue();
			}
		}
		return null;
	}
	
	public static List<ReportDto> getReportDtos(List<Map<String, Object>> list) {
		if (list == null) {
			list = new ArrayList<Map<String, Object>>();
		}
		List<ReportDto> reportDtos = new ArrayList<ReportDto>();
		String stat_time = "";
		ReportDto reportDto = new ReportDto();
		for (Map<String, Object> map : list) {
			String current_stat_time = String.valueOf(map.get("stat_time".toUpperCase()));
			if (!stat_time.equals(current_stat_time)) {
				if (StringUtils.isNotBlank(stat_time)) // 统计时间不等的情况即认定为换行
				{
					reportDtos.add(reportDto);
					reportDto = new ReportDto();
				}
				stat_time = current_stat_time;
				reportDto.setStat_tiem(stat_time);
			}
			reportDto.getViews().add(new KpiView(map.get("KPINAME"), map.get("value")));
		}
		reportDtos.add(reportDto);
		return reportDtos;
	}
}
