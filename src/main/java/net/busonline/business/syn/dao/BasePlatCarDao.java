package net.busonline.business.syn.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.busonline.business.syn.pojo.CarBean;
import net.busonline.business.syn.util.JDBCUtils;

/**
 * 基础平台数据
 */
public class BasePlatCarDao extends JDBCUtils {
	
	
	//获取失效车辆
	@SuppressWarnings("static-access")
	public List<String> getInvalidCarMac(){
		List<String> cars = new ArrayList<>();
		try{
			con = this.getConnection();
			String sql = "select mac_address from terminal t where t.status != '2' ";
			pre = con.prepareStatement(sql);
			res = pre.executeQuery();
			res.beforeFirst();
			while (res.next()) {
				cars.add(res.getString("mac_address"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			try {
				JDBCUtils.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return cars;
	}
	
	//获取所有效车辆
	@SuppressWarnings("static-access")
	public List<String> getBasePlatCarMac(){
		List<String> cars = new ArrayList<>();
		try{
			con = this.getConnection();
			String sql = "select mac_address from terminal t where t.status = '2' ";
			pre = con.prepareStatement(sql);
			res = pre.executeQuery();
			res.beforeFirst();
			while (res.next()) {
				cars.add(res.getString("mac_address"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			try {
				JDBCUtils.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return cars;
		
	}
	//获取新增车辆信息
	@SuppressWarnings("static-access")
	public List<CarBean> getBasePlatCars(String str){
		List<CarBean> cars = new ArrayList<CarBean>();
		CarBean bean = null;
		try{
			con = this.getConnection();
			String sql = "SELECT "
			   +"a.NAME as name ,b.mac_address as mac_address ,b.app_key as app_key,t1.name as city,t2.name as company"
			+" FROM bus_line a"
			+" LEFT  JOIN terminal b ON a.id = b.line_id "
			+" LEFT JOIN  aread t1  on a.area_id= t1.id "
			+" LEFT JOIN bus_company t2 on a.company_id = t2.id"
			+" where a.status = '1'"
			+" and b.status='2' "
			+" and b.mac_address not in"
			+"("+str+")";
			System.out.println(sql);
			pre = con.prepareStatement(sql);
			res = pre.executeQuery();
			res.beforeFirst();
			while (res.next()) {
				bean = new CarBean();
				bean.setAppkey(res.getString("app_key"));
				bean.setName(res.getString("name"));
				bean.setCityname(res.getString("city"));
				bean.setMac_address(res.getString("mac_address"));
				cars.add(bean);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			try {
				JDBCUtils.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return cars;
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	

}
