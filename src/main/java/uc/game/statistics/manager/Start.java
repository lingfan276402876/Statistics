package uc.game.statistics.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import uc.game.statistics.utils.TimeUtils;

public class Start {
public static void main(String[] args) {
	ApplicationContext factory=new ClassPathXmlApplicationContext("classpath:applicationContext.xml"); 
	DataManager dataManager = (DataManager) factory.getBean("dataManager");
	String time = null;
	String end_time = null;
	if(args!=null)
	{
		if(args.length==1)
		{
			time = args[0];
			end_time = time;
		}else if(args.length==2)
		{
			time = args[0];
			end_time = args[1];
		}
	}
	while (true) {
		dataManager.readDataAll(time);
		if(time==null || time.equals(end_time))
		{
			break;
		}
		time = TimeUtils.getNewFormatDate(time,"yyyyMMdd", 1);
	}
	
}
}
