package net.busonline.business.data.service;


import java.util.List;
import java.util.Map;

/**
 * Created by win7 on 2017/6/29.
 */
public interface IAddataService {
    /**
     * 存放城市gps中心点
     */
    public void putCityGps();

    /**
     * 存放广告白名单
     */
    public void putWhitelist();

    /**
     * 百度给的图片且不在白名单库，入库
     */
    public void putBdWhitelist();

    /**
     * 获取策略，将策略存放进redis,百度策略
     *
     * @return
     */
    public void putStrategy();

    /**
     * 获取策略,将策略放进redis,非百度业务,bus自己的业务
     */
    public void lbs_PutStrategy();

    /**
     * 存放车辆信息
     */
    public void putBusByMac();

    /**
     * 城市分组存放车辆信息
     */
    public void putBusByCity();

    /**
     * 将未匹配到车辆的信息更新到数据库
     */
    public void updateLineBydeviceid();

    /**
     * 添加时间戳
     */
    public void addDatetime();

    /**
     * line_id获取百度信息
     */
    public void getBaiduConfig();

    /**
     * 存放广告优先级 ,给接口调取广告时候判断用
     */
    public void putAdPriority();

    /**
     * 存放位置车辆
     */
    public void positionVehicle();

    public void selectH();
    /**
     * CPT策略数据查询
     */
    public  void selectCptStrategy();
    /**
     * 查询cdn物料
     */
    public void getCDNMaterial();
    /**
     * 获取第三方回调url
     */
    public void selectCptMonitor();
}
