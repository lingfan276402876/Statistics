package uc.game.statistics.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import uc.game.statistics.data.DataRegister;
import uc.game.statistics.utils.TimeUtils;

public class KpiConfig {
	private static Map<String, GeneralKpi> map = new HashMap<String, GeneralKpi>();
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
		Document document = reader.read(Thread.currentThread().getContextClassLoader().getResource("kpis.xml"));
		Element root = document.getRootElement();
		List<Element> kpis = root.elements();
		KpiConfig config = new KpiConfig();
		for (Element kpi : kpis) {
			GeneralKpi generalKpi = config.new GeneralKpi();
			String kpiName = kpi.attributeValue("kpiName");
			generalKpi.setKpiName(kpiName);
			generalKpi.setExpr(kpi.attributeValue("expr"));
			String timeAdd = kpi.attributeValue("timeAdd");
			if(StringUtils.isNotBlank(timeAdd))
			{
				generalKpi.setTimeAdd(Integer.parseInt(timeAdd));
			}
			map.put(kpiName, generalKpi);
		}
	}

	public class GeneralKpi {
		private String kpiName;
		private String expr;
		private int timeAdd = 0;
		public String getKpiName() {
			return kpiName;
		}

		public void setKpiName(String kpiName) {
			this.kpiName = kpiName;
		}
       
		public Integer getTimeAdd() {
			return timeAdd;
		}

		public void setTimeAdd(Integer timeAdd) {
			this.timeAdd = timeAdd;
		}

		public String getExpr(String t,String t_whole,String uniKey,String pvName,String time) {
			String temp = expr;
			temp = temp.replaceAll("\\$\\{unikey\\}",uniKey);
			temp = temp.replaceAll("\\$\\{pvName\\}",pvName);
			temp = temp.replaceAll("\\$\\{WHOLE\\}",t_whole);
			try {
				while (true) {
					int index = temp.indexOf("}", temp.indexOf("${T"));
					int days = 0;
					String replace = "";
					if(index>=0 && temp.contains("${T"))
					{
						replace = temp.substring(temp.indexOf("${T"),index+1);
						String day = replace.replaceAll("\\$\\{T","").replaceAll("\\}", "");
						if(day.length()>0)
						{
							days = Integer.parseInt(replace.replaceAll("\\$\\{T","").replaceAll("\\}", ""));
						}
						replace = replace.replace("${", "\\$\\{");
						replace = replace.replace("$}", "\\}");
						String newTableName = getNewFormatTabeName(t,days);
						if(newTableName == null)
						{
							return null;
						}
						temp = temp.replaceAll(replace,newTableName);
					}else
					{
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(temp);
			}
			try {
				while (true) {
					int index = temp.indexOf("}", temp.indexOf("${time"));
					int days = 0;
					String replace = "";
					if(index>=0 && temp.contains("${time"))
					{
						replace = temp.substring(temp.indexOf("${time"),index+1);
						String day = replace.replaceAll("\\$\\{time","").replaceAll("\\}", "");
						if(day.length()>0)
						{
							days = Integer.parseInt(replace.replaceAll("\\$\\{time","").replaceAll("\\}", ""));
						}
						replace = replace.replace("${", "\\$\\{");
						replace = replace.replace("$}", "\\}");
						temp = temp.replaceAll(replace,TimeUtils.getNewFormatDate(time,"yyyyMMdd",days));
					}else
					{
						break;
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(temp);
			}
			return temp;
		}

		public void setExpr(String expr) {
			this.expr = expr;
		}
	}
	
	public static GeneralKpi getGeneralKpi(String kipName) {
		return map.get(kipName);
	}
	
	public static String getNewFormatTabeName(String tableName,int day) {
		String date = tableName.substring(tableName.length()-8);
		tableName = tableName.substring(0,tableName.length()-8);
		tableName = tableName+TimeUtils.getNewFormatDate(date,"yyyyMMdd",day);
		if(DataRegister.getDataBaseService().getTableCount(tableName)<=0)
		{
			return null;
		}
		return tableName;
	}
	public static void main(String[] args) {
		System.out.println(map.get("v_new_keep_day1").getExpr("songshu20130406", "songshu_whole", "ucid", "ucid", "20140420"));
	}
}
