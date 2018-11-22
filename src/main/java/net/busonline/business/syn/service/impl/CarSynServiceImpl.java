package net.busonline.business.syn.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.busonline.business.data.cctvdao.CctvMapper;
import net.busonline.business.syn.dao.CarSynDataMapper;
import net.busonline.business.syn.dao.LineStationMapper;
import net.busonline.business.syn.pojo.CarBean;
import net.busonline.business.syn.pojo.CodeEnum;
import net.busonline.business.syn.pojo.StationBean;
import net.busonline.business.syn.service.CarSynService;

@Service
public class CarSynServiceImpl implements CarSynService{
	public static Logger logger = LoggerFactory.getLogger(CarSynServiceImpl.class);
	@Autowired
	private CarSynDataMapper carSynDataMapper;
	@Autowired
    private LineStationMapper lineStationDao;
	
	@Autowired
	private CctvMapper cctvMapper;
	
	public static int PORT = 0;

	@Override
	public boolean synUpdateAutoCarInfo() {
		logger.info("同步车辆表...");
    	boolean flag = true;
		try{
    		//删除已注册手动车辆和未知车辆
    		int j = carSynDataMapper.deleManualCar(cctvMapper.getBasePlatCarMac());
    		logger.info("更新手动车辆========"+j);
    		//更新失效车聊
    		int i = carSynDataMapper.updateInvalidCars(cctvMapper.getInvalidCarMac());
    		logger.info("更新失效车辆========" + i);
    		//基础平台新增车辆同步更新表
    		List<CarBean> cars = cctvMapper.getBasePlatCars(carSynDataMapper.getManualCars());
    		//新增车辆更新
    		for (int k = 0; k < cars.size(); k++) {
    			CarBean carBean = cars.get(k);
    			carBean.setId(getId(carBean));
    			//更新车辆
    			int w = carSynDataMapper.updateCarById(carBean);
    			//logger.info("开始定时同步车辆========"+carBean.getMac_address()+"==" + w);
    		}
    		logger.info("开始定时同步线路关系========");
    		//重新更新线路外键关系
    		List<CarBean> carDatas = cctvMapper.getBasePlatData();
    		for (int k = 0; k < carDatas.size(); k++) {
				carDatas.get(k).setStation_id(lineStationDao.getIdByLineName(carDatas.get(k).getCity_code(), carDatas.get(k).getName()));
    		}
    		//批量更新对应
    		int w = carSynDataMapper.updateCarBymac(carDatas);
    		logger.info("批量更新所有车辆外键关联和appkey========"+w);
    	}catch(Exception e){
    		logger.info("同步更新异常========", e);
    		flag =false;
    	}
		return flag;
	}
	
	//获取更新车辆ID
	public String getId(CarBean carBean){
		if(carSynDataMapper.getIdByLineNames(carBean) != null){
			return  carSynDataMapper.getIdByLineNames(carBean);
		}else if(carSynDataMapper.getIdByLineNamek(carBean)!= null){
			return carSynDataMapper.getIdByLineNamek(carBean);
		}else if(carSynDataMapper.getIdByCityNames(carBean.getCityname()) != null){
			return carSynDataMapper.getIdByCityNames(carBean.getCityname());
		}else if(carSynDataMapper.getIdByCityNamek(carBean.getCityname()) != null){
			return carSynDataMapper.getIdByCityNamek(carBean.getCityname());
		}
		return "";
	}
	

}
