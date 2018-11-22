package net.busonline.business.syn.quartz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.busonline.business.syn.service.CarSynService;
import net.busonline.business.syn.service.LineStationService;

@Component
public class SynCarData {
	public static Logger logger = LoggerFactory.getLogger(SynCarData.class);
	@Autowired
	private CarSynService carSynService;
	@Autowired
	private LineStationService lineStationService; 
	
	//@Scheduled(cron = "0 08 19 * * ?")
	public void sysCar(){
		logger.info("定时任务开始........");
		//carSynService.synUpdateAutoCarInfo();
		lineStationService.synLineStation();
		//lineStationService.synBaiduLineStation();
	}
	
	

}
