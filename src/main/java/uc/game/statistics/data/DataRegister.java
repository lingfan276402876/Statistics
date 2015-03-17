package uc.game.statistics.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Service
public class DataRegister  implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    @Autowired
	public void setApplicationContext(ApplicationContext applicationContext) {
		DataRegister.applicationContext = applicationContext;
	}
	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}
    public static DataBaseService getDataBaseService()
    {
    	return (DataBaseService) applicationContext.getBean("dataBaseService");
    }
    public static ReportData getReportData()
    {
    	return (ReportData) applicationContext.getBean("reportData");
    }
}
