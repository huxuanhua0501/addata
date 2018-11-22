package net.busonline.business.data.cctvdao;

import java.util.List;
import java.util.Map;

import net.busonline.business.syn.pojo.CarBean;
import net.busonline.business.syn.pojo.StationBean;

/**
 * Created by win7 on 2017/7/11.
 */
public interface CctvMapper {
	//获取失效车辆
	List<String> getInvalidCarMac();
	//获取有效车辆
	List<String> getBasePlatCarMac();
	//获取基础平台新增车辆
	List<CarBean> getBasePlatCars(List<String> list);
	//获取失效ID
	List<String> getInvalidLineId();
	//根据线路ID获取新增线路
	List<StationBean> getLineInfoById(List<String> list);
	List<StationBean> getLineInfoByLineName(Map<String,Object> map);
	//获取所有车辆基本信息
	List<CarBean> getBasePlatData();
	
	

}
