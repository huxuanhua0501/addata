package net.busonline.business.data.dao;


import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Created by win7 on 2017/7/10.
 */
public interface AddataMapper {
    /**
     * 获取城市中心点
     *
     * @return
     */
    public List<Map<String, Object>> getCityGps();

    /**
     * 获取白名单
     *
     * @return
     */
    public List<String> getWhitelist();

    /**
     * 插入库里不存在的，但是百度存在的白名单，将其置于失效
     *
     * @param map
     */
    public void insertBdWhitelist(@Param("map") Map<String, Object> map);

    /**
     * 得到播放策略
     *
     * @param date
     * @return
     */
    public List<Map<String, Object>> getStrategy(@Param("date") String date, @Param("adid") String adid);

    /**
     * 得到播放策略,bus自己的广告业务的策略
     *
     * @param adid
     * @return
     */
    public List<Map<String, Object>> getLbsStrategy(@Param("adid") String adid, @Param("ad_type") String ad_type);

    /**
     * 获取失效的策略
     *
     * @return
     */
    public List<Map<String, Object>> getFailureStrategy();

    /**
     * 获取城市
     *
     * @param cityid
     * @return
     */
    public String getCitys(@Param("cityid") List<String> cityid);

    /**
     * 获取启用的广告位
     *
     * @return
     */
    public List<Map<String, Object>> getAd();

    /**
     * 获取所有mac
     *
     * @return
     */
    public List<String> getMac();

    /**
     * 获取mac所对应的车辆
     *
     * @return
     */
    public List<Map<String, Object>> getBusByMac();

    /**
     * 获取所有失效的车辆
     *
     * @return
     */
    public List<Map<String, Object>> getDeviceIdByMac();

    /**
     * 获取所有城市
     *
     * @return
     */
    public List<String> getAllCitys();

    /**
     * 获取所有的车辆，通过城市
     *
     * @param city
     * @return
     */
    public List<Map<String, Object>> getAllLineByCity(@Param("city") String city);

    /**
     * 通过device_id更新线路信息
     *
     * @param device_id
     * @param mac
     */
    public void updateLineBydeviceid(@Param("device_id") String device_id, @Param("mac") String mac, @Param("testLineId") String testLineId);

    /**
     * 查询状态ad_device_group_line '绑定类型:  1-城市; 2-线路',状态为2的线路id
     *
     * @return
     */
    public List<String> selectlineids();

    public List<Map<String, Object>> getAllBaiduConfigByline();

    public List<Map<String, Object>> getAllBaiduConfigByCityCode();

    /**
     * 实际产生的请求数量,针对于百度业务
     *
     * @return
     */
    public List<Map<String, Object>> getRealTimeData(@Param("hour") String hour);

    /**
     * 实际产生的请求数量,针对bus自己的业务
     *
     * @return
     */
    public List<Map<String, Object>> getLbsRealTimeData();

    /**
     * 得到广告优先级
     *
     * @return
     */
    public String getAdPriority();

    /**
     * 获取策略相关的物料
     *
     * @param list
     * @return
     */
    public List<Map<String, Object>> getMaterialBus(@Param("list") List<String> list);

    /**
     * 存放位置车辆
     */
    public List<String> positionVehicle();

    /**
     *
     */
    public String selectTestLineId(@Param("deviceid") String deviceid);

    /**
     * 存放成都黑名单,只存一次
     */
    public List<String> selectH();

    /**
     * CPT策略数据查询
     */
    public List<Map<String, String>> selectCptStrategy();

    /**
     * 查询cpt物料
     */
    public List<Map<String, String>> selectCptMaterial();

    /**
     * 查询cpt的线路
     *
     * @return
     */

    public List<Map<String, String>> selectCptlineId();

    /**
     * cptcount,查询报表的计数
     */
    public List<Map<String, String>> cptCount();

    /**
     * 查询所有没有cdn_url的物料名称
     */
    public List<String> getBaiduFileName();

    /**
     * 查询差量的物料
     * @return
     */
    public List<String> getfullBaiduFileName();

    /**
     * 更新物料表
     */
    public void updateMaterial(@Param("cdn_url") String cdn_url, @Param("baidu_name") String baidu_name);

    /**
     * 查询cdn物料
     */
    public List<Map<String, String>> getCDNMaterial();

    /**
     * 查询要预热的cdn_url
     */
    public List<String> getPreheatingCdn_url();

    /**
     * 更新已经预热的cdn_url
     */

    public void updateCdn_type(@Param("cdn_url") String cdn_url);

    /**
     * 获取每条线路的车辆数
     * @return
     */
    public List<Map<String, String>> selectBusTotal();

    /**
     * 获取第三方回调url
     */
    public  List<Map<String,String>> selectCptMonitor();
}
