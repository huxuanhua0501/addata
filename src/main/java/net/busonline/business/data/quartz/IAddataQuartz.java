package net.busonline.business.data.quartz;


/**
 * Created by win7 on 2017/6/29.
 */

public interface IAddataQuartz {
    public void putCityGps();

    public void putWhitelist();

    public void putBdWhitelist();

    public void putStrategy();

    //  public void putBusByMac();

    //   public void putBusByCity();
    public void updateLineBydeviceid();

    public void addDatetime();

    public void putBusLine();

    public void getBaiduConfig();

    /**
     * 存放广告优先级 ,给接口调取广告时候判断用
     */
    public void putAdPriority();

    /**
     * 获取策略,将策略放进redis,非百度业务,bus自己的业务
     */
    public void lbs_PutStrategy();

    /**
     * 存放位置车辆
     */
    public void positionVehicle();

    public void selectH();

    /**
     * CPT策略
     */
    public void selectCptStrategy();

    /**
     * 4g物料下载业务
     */
    public void lbsMaterialBusiness();

    /**
     * 查询cdn物料
     */
    public void getCDNMaterial();
    /**
     * 获取第三方回调url
     */
    public  void selectCptMonitor();
}
