package net.busonline.business.data.quartz.impl;

import net.busonline.business.data.pojo.RedisKeyName;
import net.busonline.business.data.quartz.IAddataQuartz;
import net.busonline.business.data.service.IAddataService;
import net.busonline.business.lbsmaterial.service.ILBSMaterialService;
import net.busonline.business.syn.service.CarSynService;
import net.busonline.business.syn.service.LineStationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by win7 on 2017/7/10.
 */
@Component
public class AddataQuartz implements IAddataQuartz {
    public static Logger logger = LoggerFactory.getLogger(AddataQuartz.class);
    @Autowired
    private IAddataService addataService;
    @Autowired
    private CarSynService carSynService;
    @Autowired
    private LineStationService lineStationService;
    @Autowired
    private ShardedJedisPool shardedJedisPool;
    @Autowired
    private ILBSMaterialService lbsMaterialService;

    /**
     * 将城市中心gps存放进redis
     */
    @Scheduled(cron = "${putCityGps}")
    public void putCityGps() {
        addataService.putCityGps();
    }

    /**
     * 白名单存入redis中
     */
    @Override
    @Scheduled(cron = "${putWhitelist}")
    public void putWhitelist() {
        addataService.putWhitelist();
    }

    /**
     * 将百度返回的广告存放进数据库，状态为未通过
     */
    @Override
    @Scheduled(cron = "${putBdWhitelist}")
    public void putBdWhitelist() {
        addataService.putBdWhitelist();
    }

    /**
     * 存放请求策略
     */
    @Override
    @Scheduled(cron = "${putStrategy}")
    public void putStrategy() {
        addataService.putStrategy();
    }

    /**
     * 将未匹配到车辆匹配到车辆表中
     */
    @Override
    @Scheduled(cron = "${updateLineBydeviceid}")
    public void updateLineBydeviceid() {
        addataService.updateLineBydeviceid();
        addataService.positionVehicle();
    }

    /**
     * 存放时间戳
     */
    @Override
    @Scheduled(cron = "${addDatetime}")
    public void addDatetime() {
        addataService.addDatetime();
    }

    /**
     * line_id(主键id)获取百度信息
     */
    @Override
    @Scheduled(cron = "${getBaiduConfig}")
    public void getBaiduConfig() {
        addataService.getBaiduConfig();
    }

    /**
     * 存放广告优先级 ,给接口调取广告时候判断用
     */
    @Override
    @Scheduled(cron = "${putAdPriority}")
    public void putAdPriority() {
        addataService.putAdPriority();
    }

    @Override
    @Scheduled(cron = "${lbs_PutStrategy}")
    public void lbs_PutStrategy() {
        addataService.lbs_PutStrategy();
    }

    /**
     * 存放未知车辆
     */
    @Override
    @Scheduled(cron = "${positionVehicle}")
    public void positionVehicle() {
        addataService.positionVehicle();
    }

    @Override
    //@Scheduled(cron = "${selectH}")
    public void selectH() {
        addataService.selectH();
    }

    @Override
    @Scheduled(cron = "${selectCptStrategy}")
    public void selectCptStrategy() {
        addataService.selectCptStrategy();
    }

    @Override
      @Scheduled(cron = "${lbsMaterialBusiness}")
    public void lbsMaterialBusiness() {
        lbsMaterialService.lbsMaterialBusiness();
    }

    @Override
    @Scheduled(cron = "${getCDNMaterial}")
    public void getCDNMaterial() {
        addataService.getCDNMaterial();
    }

    @Override
    @Scheduled(cron = "${selectCptMonitor}")
    public void selectCptMonitor() {
        addataService.selectCptMonitor();
    }

    /**
     * 业务逻辑
     * 1:同步基础平台数据进ad数据库
     * 2:同步基础车辆
     * 4:同步车辆信息进redis(mac为key)
     * 5:同步虚拟车辆信息进redis(城市为key查询)
     */
    @Override
    @Scheduled(cron = "${putBusLine}")
    public void putBusLine() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
//        if (lineStationService.synLineStation()) {//同步线路信息
//            shardedJedis.hset(RedisKeyName.HEARTBEAT, "synLineStation", "同步基础平台线路数据进ad数据库" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
//            if (carSynService.synUpdateAutoCarInfo()) {//同步基础车辆
//                shardedJedis.hset(RedisKeyName.HEARTBEAT, "synUpdateAutoCarInfo", "同步基础车辆数据进ad数据库" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        try {
            addataService.putBusByMac();
            addataService.putBusByCity();
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "putBusLine== /**\n" +
                    "     * 业务逻辑\n" +
                    "     * 1:同步基础平台数据进ad数据库\n" +
                    "     * 2:同步基础车辆\n" +
                    "     * 4:同步车辆信息进redis(mac为key)\n" +
                    "     * 5:同步虚拟车辆信息进redis(城市为key查询)\n" +
                    "     */" + e);

        } finally {
            shardedJedis.close();
//                }
//            }
//
        }
    }


}
