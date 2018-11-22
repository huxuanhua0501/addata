package net.busonline.business.syn.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.busonline.business.data.cctvdao.CctvMapper;
import net.busonline.business.syn.dao.LineStationMapper;
import net.busonline.business.syn.pojo.CodeEnum;
import net.busonline.business.syn.pojo.StationBean;
import net.busonline.business.syn.service.LineStationService;

/**
 * 
 *同步更新对接百度线路信息
 */
@Service
public class LineStationServiceImpl implements LineStationService{
	public static Logger logger = LoggerFactory.getLogger(LineStationServiceImpl.class);
	@Autowired
	private LineStationMapper lineStationDao;
	@Autowired
	private CctvMapper cctvMapper; 
	@Override
	public boolean synLineStation() {
	    boolean flag = true;
		try{
			logger.info("开始同步线路信息..............");
			//同步更新失效线路
			List<String> invalidLineIds = cctvMapper.getInvalidLineId();
			if(invalidLineIds.size()>0){
				int i =lineStationDao.updateInvalidStation(invalidLineIds);
				//System.out.println("============="+i);
				logger.info("同步更新失效线路.............."+i);
			}
			//同步新增线路
			List<StationBean> linestations = cctvMapper.getLineInfoById(lineStationDao.getLineIds());
			if(linestations.size()>0){
				int j = lineStationDao.insertStation(linestations);
				//System.out.println("=======同步新增"+j);
				logger.info("同步新增失效线路.............."+j);
			}
			//更改线路名称===分城市
			Map<String, Object> param = new HashMap<String, Object>();
			for(CodeEnum city : CodeEnum.values()){
	            //System.out.println( color + "  name: " + color.getName() + "  index: " + color.getIndex() );
				List<String> stationNames = lineStationDao.getlineName(city.getCode());
				param.put("list", stationNames);
				param.put("city_id", city.getCity());
				List<StationBean> stations = cctvMapper.getLineInfoByLineName(param);
				//System.out.println(stations.size());
				if(stations.size()>0){
					int  k = lineStationDao.updateStation(stations);
					logger.info("更改线路名称.............."+k);
				}
			}
			//同步更新对接百度表
		}catch(Exception e){
			e.printStackTrace();
			logger.info("同步线路信息异常======",e);
			flag = false;
		}
		return flag;
		
	}
	@Override
	public void synBaiduLineStation() {
		//删除对接百度表时
		/*int i = lineStationDao.deleteDetail();
		logger.info("删除对接百度表信息======="+i);
	    if(i>=0){
	    	int j = lineStationDao.insertDetail();
	    	logger.info("更新百度表信息======="+j);
	    }*/
		
		
		
		
		
		
	}
	public static void main(String[] args) {
		LineStationServiceImpl ll = new LineStationServiceImpl();
		ll.synLineStation();
	}
	
}
