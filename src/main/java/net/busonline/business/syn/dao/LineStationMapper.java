package net.busonline.business.syn.dao;

import java.util.List;

import net.busonline.business.syn.pojo.StationBean;

public interface LineStationMapper {
	public List<String> getLineIds();
	public int updateStation(List<StationBean> list);
	public int insertStation(List<StationBean> list);
	public int updateInvalidStation(List<String> list); 
	public List<String> getlineName(String cityCode);
	public int deleteDetail();
	public int insertDetail();
	public String getIdByLineName(String city,String line_name);

}
