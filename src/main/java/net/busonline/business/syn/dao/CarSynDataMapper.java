package net.busonline.business.syn.dao;

import java.util.List;
import java.util.Map;

import net.busonline.business.syn.pojo.CarBean;

public interface CarSynDataMapper {
    
	//获取手动车辆
	public List<String> getManualCars();
	//更新失效状态
	public int updateInvalidCars(List<String> list);
	//获取失效mac
	public List<String> getInvalidCarsMac(int number);
	//获取虚拟车辆mac
	public List<String> getFicCarMac(int number);
	//更新基础数据表
	public int updateCar(List<CarBean> list);
	//删除手动车辆
	public int deleManualCar(List<String> list);
	//批量更新线路id
	public int updateCarBymac(List<CarBean> list);
	
	//==================更新新增车辆=================
	//获取根据线路获取扩容车辆ID
	public String getIdByLineNamek(CarBean car);
	//获取根据线路获取失效车辆ID
	public String getIdByLineNames(CarBean car);
	//获取城市下的扩容车辆ID
	public String getIdByCityNamek(String cityName);
	//获取城市下车辆ID 
	public String getIdByCityNames(String cityName);
	//更新车辆信息
	public int updateCarById(CarBean car);
	
	
	
	
}
