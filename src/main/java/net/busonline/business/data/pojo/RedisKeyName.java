package net.busonline.business.data.pojo;

/**
 * 定义公共的reidskey,或者后续的一些词汇
 */
public class RedisKeyName {

    public final static String HEARTBEAT = "heartbeat";//reidis心跳key

    public final static String CITY_CENTER = "city_center";//城市中心点

    public final static String VEHICLE = "vehicle";//根据mac获取车辆信息

    public final static String MATCH_VEHICLE = "match_vehicle";//需持久化未匹配到mac车辆信息

    public final static String STRATEGY = "strategy";//获取投放策略信息

    public final static String MATERIAL = "material";//百度物料白名单（图片名称）

    public final static String NEW_MATERIAL = "new_material";//插入新百度物料白名单（图片名称）

    public final static String BAIDU_CONFIG = "baidu_config";//line_id获取百度信息

    public final static String LBS_STRATEGY = "lbs_strategy";//广告位获取lbs策略

    public final static String PRIORITY = "priority";//广告投放优先级

    public final static String UNKNOW_CARS = "unkonw_cars";//未知车辆
    public final static String CPT_STRATEGY = "cpt_strategy";//cpt策略
    public final static String LBS = "LBS";//LBS计数
    public final static String RTB = "RTB";//RTB计数
    public final static String CPT = "CPT";//CPT计数
    public final static String BLACKLIST = "blacklist";//黑名单
    public final static String CDN_MATERIAL = "cdn_material";//cdn物料
    public final static String CPT_MONITOR = "cpt_monitor";//第三方回调url

}
