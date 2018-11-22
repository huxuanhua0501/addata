package net.busonline.business.data.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.busonline.business.data.dao.AddataMapper;
import net.busonline.business.data.pojo.BaiduConfigBean;
import net.busonline.business.data.pojo.DisplayName;
import net.busonline.business.data.pojo.RedisKeyName;
import net.busonline.business.data.service.IAddataService;
import net.busonline.business.sharing.utils.PubMethod;
import org.omg.CORBA.OBJ_ADAPTER;
import org.omg.PortableInterceptor.DISCARDING;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by win7 on 2017/6/29.
 */
@Service
public class AddataService implements IAddataService {
    private Logger logger = LoggerFactory.getLogger(AddataService.class);
    @Autowired
    private ShardedJedisPool shardedJedisPool;
    @Autowired
    private AddataMapper addataMapper;
    @Autowired
    private BaiduConfigBean baiduConfigBean;
    private final String BAR = "-";

    /**
     * 存放城市gps中心点
     */
    public void putCityGps() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }

        try {
            List<Map<java.lang.String, Object>> list = addataMapper.getCityGps();
            if (list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    shardedJedis.hset(RedisKeyName.CITY_CENTER, list.get(i).remove(DisplayName.CITY).toString(), list.get(i).toString());
                }
            }
            shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.CITY_CENTER, "存放城市gps中心点" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            shardedJedis.close();
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(putCityGps)存放城市gps中心点==" + e);
        }
    }

    /**
     * 存放广告白名单
     */
    public void putWhitelist() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            List<String> list = addataMapper.getWhitelist();
            shardedJedis.del(RedisKeyName.MATERIAL);
            for (int i = 0; i < list.size(); i++) {
                shardedJedis.sadd(RedisKeyName.MATERIAL, list.get(i));//存放成set格式

            }
            shardedJedis.hset("heartbeat", RedisKeyName.MATERIAL, "存放广告白名单" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            shardedJedis.close();
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(putWhitelist)存放广告白名单==" + e);
        }

    }

    /**
     * 百度给的图片且不在白名单库，入库
     */
    @Override
    public void putBdWhitelist() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            long count = shardedJedis.scard(RedisKeyName.NEW_MATERIAL);
            if (count > 0) {
                java.lang.String str = null;
                for (int i = 0; i < count; i++) {
                    str = shardedJedis.spop(RedisKeyName.NEW_MATERIAL);
//                    System.err.println(str);
                    if (!PubMethod.isEmpty(str)) {
                        Map map = JSON.parseObject(str, Map.class);
//                        List<Map> list = JSON.parseArray(str, Map.class);
//                        for (int j = 0; j < list.size(); j++) {
                        addataMapper.insertBdWhitelist(map);
                        logger.info("入库的白名单信息=" + JSON.toJSONString(map) + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
//                        }
                    }
                }
            }
            shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.NEW_MATERIAL, "百度给的图片且不在白名单库，入库" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            shardedJedis.close();
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(putBdWhitelist)百度给的图片且不在白名单库，入库==" + e);
        }
    }

    private int getHourNum() {
        SimpleDateFormat dfs = new SimpleDateFormat("HH:mm");
        Date begin = null;
        java.util.Date end = null;
        try {
            begin = dfs.parse("08:00");
            end = dfs.parse(dfs.format(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long between = (end.getTime() - begin.getTime()) / 1000;//除以1000是为了转换成秒
        long hour1 = between % (24 * 3600) / 3600;
        long minute1 = between % 3600 / 60;
        return (int) ((hour1 * 60 + minute1) / 10);
    }

    /**
     * 获取策略,将策略放进redis,非百度业务,bus自己的业务
     * 1:拿redis
     * 2.删除原来的策略
     * 3:获取有效的广告位,如果没有直接移除策略redislist集合中某个策略,有则走下面的流程
     * 4:获取成功请求数,拼装成map,key=ad_id+strategy_id+line; value="实际数量"
     * 5:根据广告位置去拿策略
     * 6:策略里面的线路
     * 7:匹配线路的投放策略中数量,跟实际投放数量对比,如果实际投放数量大于等于策略中的数量,将其重策略的线路中移除,mergeData方法做这件事情
     * 8:如果策略中还有线路,则将剩余的线路添加到策略结合中redislist,否则就删除策略
     * 9:根据策略中的物料id,查找相关联的物料,然后存放到策略中,方法getmaterials做这件事情
     * 10:将策略集合粗放到reids中,提供给接口调用
     * redislist 有效策略集合
     * listadid   有效的广告位的集合
     * lbsDataMap 成功请求广告结合
     * linelist   线路集合
     */
    public void lbs_PutStrategy() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();//1:拿redis

        Map<String, Object> redisMap = new HashMap<>();//存储LBS计数
        Map<String, Object> redislineMap = new HashMap<>();//存储LBSlist

        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(2);
        }
        //获取redis中LBS数量

        Map<String, String> redistotal = shardedJedis.hgetAll(RedisKeyName.LBS);
        for (Map.Entry<String, String> entry : redistotal.entrySet()) {
            String[] keys = entry.getKey().split("-");
            if (keys[4].equals(datetime())) {
                redisMap.put(keys[0] + keys[1] + keys[3], entry.getValue());
                redislineMap.put(keys[0] + keys[1] + keys[3], keys[3]);

            }
        }

        collection = shardedJedis.getAllShards();
        jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }

        try {
            shardedJedis.del(RedisKeyName.LBS_STRATEGY);//删除所有策略
            List<Map<String, Object>> listadid = addataMapper.getAd();//获取有效的广告位
            List<Map<String, Object>> readLbsTimeDate = addataMapper.getLbsRealTimeData();//成功请求数量
            Map<String, Object> lbsDataMap = new HashMap<String, Object>();//实际投放广告的线路结合
            if (readLbsTimeDate != null && readLbsTimeDate.size() > 0) {//拼装实际投放后的,投放量
                for (int a = 0; a < readLbsTimeDate.size(); a++) {
                    lbsDataMap.put(readLbsTimeDate.get(a).get(DisplayName.MAPKEY).toString(), readLbsTimeDate.get(a).get(DisplayName.NUM).toString());//实时投放投放的数量,mapkey=ad_id+strategy_id+line
                }
            }
            //根据广告位置去拿策略
            if (listadid.size() > 0) {//添加策略
                SimpleDateFormat sf = new SimpleDateFormat("YYYYMMdd");
                for (int j = 0; j < listadid.size(); j++) {
                    List<Map<java.lang.String, Object>> redislist = addataMapper.getLbsStrategy(listadid.get(j).get("adslot_id").toString(), listadid.get(j).get("ad_type").toString());//得到广告位对应的策略
                    if (redislist != null && redislist.size() != 0) {
                        for (int i = 0; i < redislist.size(); i++) {
                            JSONObject obj = JSONObject.parseObject(redislist.get(i).get("throw_line_dates").toString());
                            String lines = (String) obj.get(sf.format(new Date()));//查看当天有多少线路
                            if (!PubMethod.isEmpty(lines)) {//判断当天是否有策略线路
                                Map<String, String> lbsstrategyDataMap = new HashMap<String, String>();
                                String[] alllines = lines.split(",");
                                List<String> linelist = new ArrayList<>();//线路结合
                                Collections.addAll(linelist, alllines);//拼成list集合
                                mergeData(linelist, lbsstrategyDataMap, listadid.get(j), redislist.get(i), lbsDataMap, redisMap, redislineMap);//拼装策略
                                if (linelist != null && linelist.size() > 0) {//如果还有线路没有投放完,就继续粗放策略
                                    redislist.get(i).put(DisplayName.LINE_IDS, join(linelist, ","));//存放策略
                                    getmaterials(redislist.get(i));//得到物料material_ids
                                    redislist.get(i).remove(DisplayName.MATERIAL_IDS);
                                    redislist.get(i).remove(DisplayName.THROW_LINE_DATES);
                                    redislist.get(i).remove(DisplayName.TOTAL);
                                    if ("".equals(redislist.get(i).get(DisplayName.MATERIALS))) {
                                        redislist.remove(i);
                                        i--;
                                    }
                                } else {//否则就删除该策略,因为已经投放完了
                                    redislist.remove(i);
                                    i--;
                                }
                            } else {
                                redislist.remove(i);//如果当天没有播放策略，将其移除
                                i--;
                            }
                        }
                        if (redislist.size() > 0) {//整合后，当天仍然存在策略，将其策略同步到redis中
                            shardedJedis.hset(RedisKeyName.LBS_STRATEGY, listadid.get(j).get(DisplayName.ADSLOT_ID).toString(), JSON.toJSONString(redislist));
                            shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.LBS_STRATEGY, "获取策略，lbs策略将策略存放进redis" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                        }
                    }

                }
            }
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(putBdWhitelist)获取策略，lbs策略将策略存放进redis==" + e);
        } finally {
            shardedJedis.close();
        }
    }

    /**
     * 拼装数据
     *
     * @param linelist
     * @param lbsstrategyDataMap
     * @param adidmap
     * @param redismap
     * @param lbsDataMap
     */
    private void mergeData(List<String> linelist, Map<String, String> lbsstrategyDataMap, Map<String, Object> adidmap, Map<java.lang.String, Object> redismap, Map<String, Object> lbsDataMap, Map<String, Object> redistotal, Map<String, Object> redislineMap) {
        for (String line : linelist) {//拼装,广告位置,策略跟线路,拼装成key,线路为value
            lbsstrategyDataMap.put(adidmap.get(DisplayName.ADSLOT_ID) + redismap.get(DisplayName.STRATEGY_ID).toString() + line, line);
        }
        String key = null;
        String value = null;
        for (Map.Entry<String, String> entry : lbsstrategyDataMap.entrySet()) {//匹配策略和实际投放的比较
            key = entry.getKey();//key =ad_id+strategy_id+line
            value = entry.getValue();
            int additional_copies = getHourNum();
            if (additional_copies > 72) {
                additional_copies = 72;
            }
            int totalsum = 0;
            int total = Integer.parseInt(redismap.get(DisplayName.TOTAL).toString());
            if (total % 72 != 0) {
                totalsum = (total + total % 72) /72* additional_copies;
                if (totalsum >= total) {
                    totalsum = total;
                }
            }
            if (redislineMap.containsKey(key) && Integer.parseInt(redistotal.get(key).toString()) >= totalsum) {//匹配策略跟实际播放的中广告,策略跟线路是否匹配上和数量是否请求够
                linelist.remove(value);
            }
            if (lbsDataMap.containsKey(key) && Integer.parseInt(lbsDataMap.get(key).toString()) >= totalsum) {//匹配策略跟实际播放的中广告,策略跟线路是否匹配上和数量是否请求够
                linelist.remove(value);
            }
        }
    }


    /**
     * 获取物料Lbs
     *
     * @param lbsstrategymap
     */
    private void getmaterials(Map<java.lang.String, Object> lbsstrategymap) {
        //得到物料material_ids
        String material_ids = (String) lbsstrategymap.get(DisplayName.MATERIAL_IDS);
        if (PubMethod.isEmpty(material_ids)) {
            lbsstrategymap.put(DisplayName.MATERIALS, "");

        } else {
            String[] allmaterialids = material_ids.split(",");
            List<String> allmaterialidslist = new ArrayList<>();
            Collections.addAll(allmaterialidslist, allmaterialids);//拼成list集合
            List<Map<String, Object>> materiallist = addataMapper.getMaterialBus(allmaterialidslist);
            if (materiallist != null && materiallist.size() > 0) {
                lbsstrategymap.put(DisplayName.MATERIALS, materiallist);
            } else lbsstrategymap.put(DisplayName.MATERIALS, "");
        }

    }

    /**
     * 获取策略，将策略存放进redis
     *
     * @return
     */
    @Override
    public void putStrategy() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Map<String, Object> redisMap = new HashMap<>();//存储rtb计数

        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(2);
        }
        //获取redis中rtb数量

        Map<String, String> redistotal = shardedJedis.hgetAll(RedisKeyName.RTB);
        for (Map.Entry<String, String> entry : redistotal.entrySet()) {
            String[] keys = entry.getKey().split("-");
            if (keys[3].equals(datetime()) && keys[4].equals(hour())) {
                redisMap.put(keys[0] + keys[1], entry.getValue());
            }
        }

        collection = shardedJedis.getAllShards();
        jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            SimpleDateFormat sf = new SimpleDateFormat("YYYY-MM-dd");
            List<Map<String, Object>> listadid = addataMapper.getAd();//获取有效的广告位
            List<Map<String, Object>> readTimeDate = addataMapper.getRealTimeData(new SimpleDateFormat("HH").format(new Date()));//成功返回数
            Map<String, Object> dataMap = new HashMap<String, Object>();
            if (readTimeDate != null && readTimeDate.size() > 0) {
                for (int a = 0; a < readTimeDate.size(); a++) {
                    dataMap.put(readTimeDate.get(a).get(DisplayName.MAPKEY).toString(), readTimeDate.get(a).get(DisplayName.NUM).toString());
                }
            }
            shardedJedis.del(RedisKeyName.STRATEGY);//删除所有策略
            if (listadid.size() > 0) {//添加策略
                for (int j = 0; j < listadid.size(); j++) {
                    List<Map<java.lang.String, Object>> redislist = addataMapper.getStrategy(sf.format(new Date()), listadid.get(j).get("adslot_id").toString());//得到广告位对应的策略
                    if (redislist != null && redislist.size() != 0) {
                        for (int i = 0; i < redislist.size(); i++) {
                            JSONObject obj = JSONObject.parseObject(redislist.get(i).get("times").toString());
                            String str = (String) obj.get(getEnglishWeek());//查看当天是否有播放策略
                            String st = "";
                            if (!PubMethod.isEmpty(str)) {//如果当天有播放策略
                                String key = listadid.get(j).get(DisplayName.ADSLOT_ID) + redislist.get(i).get(DisplayName.STRATEGY_ID).toString();
                                if (PubMethod.isEmpty(redisMap.get(key)) && PubMethod.isEmpty(dataMap.get(key))) {
                                    redislist.get(i).put(DisplayName.COUNT, "0");
                                    redislist.get(i).put(DisplayName.TIMES, str);//存放时间段
                                } else {
                                    int count = 0;
                                    if (!PubMethod.isEmpty(dataMap.get(key)) && !PubMethod.isEmpty(redisMap.get(key))) {
                                        if (Integer.parseInt((String) dataMap.get(key)) <= Integer.parseInt((String) redisMap.get(key))) {
                                            count = Integer.parseInt((String) redisMap.get(key));
                                        } else {
                                            count = Integer.parseInt((String) dataMap.get(key));
                                        }
                                    } else if (!PubMethod.isEmpty(redisMap.get(key))) {
                                        count = Integer.parseInt((String) redisMap.get(key));
                                    } else if (!PubMethod.isEmpty(dataMap.get(key))) {
                                        count = Integer.parseInt((String) dataMap.get(key));
                                    }
                                    if (count >= Integer.parseInt(redislist.get(i).get(DisplayName.TOTAL).toString())) {
//                                        redislist.remove(i);//判断是否播满,播满，将其移除
//                                        i--;
//                                        str = str.substring(str.indexOf(",") + 1);//如果当前播满,就删除当前小时
                                        String[] strs = str.split(",");
                                        for (int k = 0; k < strs.length; k++) {
                                            if (Integer.parseInt(hour()) != Integer.parseInt(strs[k])) {
                                                if (k != 0) {
                                                    st = st + "," + strs[k];
                                                } else {
                                                    st = strs[k];
                                                }
                                            }
                                        }
                                        redislist.get(i).put(DisplayName.TIMES, st);//存放时间段
                                        redislist.get(i).put(DisplayName.COUNT, "0");//存放时间段


                                    } else {
                                        redislist.get(i).put(DisplayName.TIMES, str);//存放时间段
                                        redislist.get(i).put(DisplayName.COUNT, count + "");
                                    }

                                }

                            } else {
                                redislist.remove(i);//如果当天没有播放策略，将其移除
                                i--;
                            }
                        }

                        if (redislist.size() > 0) {//整合后，当天仍然存在策略，将其策略同步到redis中
                            // logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(putStrategy)redis==" + JSON.toJSONString(redislist));
                            shardedJedis.hset(RedisKeyName.STRATEGY, listadid.get(j).get(DisplayName.ADSLOT_ID).toString(), JSON.toJSONString(redislist));
                            shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.STRATEGY, "获取策略，将策略存放进redis" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                        }
                    }
                }
            }
            shardedJedis.close();
        } catch (Exception e) {
            shardedJedis.close();
            e.printStackTrace();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(putStrategy)获取策略，将策略存放进redis==" + e);
        }

    }

    /**
     * 存放车辆信息
     */

    @Override
    public void putBusByMac() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            shardedJedis.del(RedisKeyName.VEHICLE);
            ShardedJedisPipeline sp = shardedJedis.pipelined();
            //  List<String> list = addataMapper.getMac();
//            if (list.size() > 0) {
//                for (int i = 0; i < list.size(); i++) {
            List<Map<String, Object>> buslist = addataMapper.getBusByMac();
            List<Map<String, Object>> listmac = new ArrayList<>();
            String macislive = null;
            for (int i = 0; i < buslist.size(); i++) {
                if (PubMethod.isEmpty(macislive)) {
                    macislive = buslist.get(i).remove(DisplayName.MAC_ADDRESS).toString();
                    listmac.add(buslist.get(i));
                } else {
                    if (macislive.equals(buslist.get(i).get(DisplayName.MAC_ADDRESS).toString())) {
                        buslist.get(i).remove(DisplayName.MAC_ADDRESS);
                        listmac.add(buslist.get(i));
                    } else {
                        sp.hset(RedisKeyName.VEHICLE, macislive, JSON.toJSONString(listmac));
                        listmac.clear();//这里做的原因是数据查询用mac做了groud by,保证了相同的mac挨着
                        macislive = buslist.get(i).remove(DisplayName.MAC_ADDRESS).toString();
                        listmac.add(buslist.get(i));
                    }
                }
            }
            sp.sync();
            shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.VEHICLE, "存放车辆信息" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            shardedJedis.disconnect();
//            }
        } catch (Exception e) {
            shardedJedis.disconnect();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(putBusByMac)存放车辆信息==" + e);
        }
    }

    /**
     * 城市分组存放车辆信息
     */
    @Override
    public void putBusByCity() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            List<String> list = addataMapper.getAllCitys();
            ShardedJedisPipeline sp = shardedJedis.pipelined();
            if (list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    List<Map<String, Object>> buslist = addataMapper.getAllLineByCity(list.get(i));
                    sp.del(list.get(i));//清空list
                    if (buslist.size() > 0) {
                        for (int j = 0; j < buslist.size(); j++) {
                            sp.lpush(list.get(i), JSON.toJSONString(buslist.get(j)));//添加list
                        }
                    }
                }
                sp.sync();
                shardedJedis.hset(RedisKeyName.HEARTBEAT, "putBusByCity", "城市分组存放车辆信息" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                shardedJedis.disconnect();
            }
        } catch (Exception e) {
            shardedJedis.disconnect();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(putBusByCity)城市分组存放车辆信息==" + e);
        }
    }

    /**
     * 将未匹配到车辆的信息更新到数据库
     */
    @Override
    public void updateLineBydeviceid() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
//        JSONObject obj1 = new JSONObject();
//        obj1.put("device_id", "23895032IIXBIWI");
//        obj1.put("mac_addr", "BC9C31F6B644111111");
//        shardedJedis.sadd("match_vehicle", obj1.toJSONString());
        try {
            Long size = shardedJedis.scard(RedisKeyName.MATCH_VEHICLE);
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    String str = shardedJedis.spop(RedisKeyName.MATCH_VEHICLE);
                    logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "将未匹配到车辆的信息更新到数据库==" + str);
                    JSONObject obj = JSONObject.parseObject(str);
                    String testLineId = addataMapper.selectTestLineId(obj.getString(DisplayName.DEVICE_ID));
                    addataMapper.updateLineBydeviceid(obj.getString(DisplayName.DEVICE_ID), obj.getString(DisplayName.MAC_ADDR).toUpperCase(), testLineId);
                    logger.info("device_id=" + obj.getString(DisplayName.DEVICE_ID) + ",mac_addr=" + obj.getString(DisplayName.MAC_ADDR).toUpperCase() + ",测试线路id=" + testLineId + new SimpleDateFormat(",更新时间=yyyy-MM-dd HH:mm:ss").format(new Date()));
                }
            }
            shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.MATCH_VEHICLE, "将未匹配到车辆的信息更新到数据库" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            shardedJedis.close();
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(updateLineBydeviceid)将未匹配到车辆的信息更新到数据库==" + e);
        }
    }

    // 将一个map对象转化为bean
    private void transMap2Bean(Map<String, Object> map, Object obj) {

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (map.containsKey(key)) {
                    Object value = map.get(key);
                    // 得到property对应的setter方法
                    Method setter = property.getWriteMethod();
                    setter.invoke(obj, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * line_id获取百度信息
     */
    @Override
    public void getBaiduConfig() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            shardedJedis.del(RedisKeyName.BAIDU_CONFIG);
            ShardedJedisPipeline pipelined = shardedJedis.pipelined();
            List<Map<String, Object>> listBaiduConfigByLineid = addataMapper.getAllBaiduConfigByline();
            Map<String, Set<BaiduConfigBean>> linemap = new HashMap<>();
            Set<BaiduConfigBean> set = null;
            BaiduConfigBean baiduConfigBean = null;
            if (listBaiduConfigByLineid.size() > 0) {//根据线路存放baiduconfig
                for (int i = 0; i < listBaiduConfigByLineid.size(); i++) {
                    String str = listBaiduConfigByLineid.get(i).remove(DisplayName.LINE_ID).toString();
                    String[] lineids = str.split(",");
                    for (String lineid : lineids) {
                        baiduConfigBean = new BaiduConfigBean();
                        transMap2Bean(listBaiduConfigByLineid.get(i), baiduConfigBean);
                        if (linemap.containsKey(lineid)) {
                            linemap.get(lineid).add(baiduConfigBean);
                        } else {
                            set = new HashSet<BaiduConfigBean>();
                            set.add(baiduConfigBean);
                            linemap.put(lineid, set);
                        }
                    }
                }
            }
            shardedJedis.close();
            List<Map<String, Object>> listBaiduConfigByCityCode = addataMapper.getAllBaiduConfigByCityCode();
            shardedJedis = shardedJedisPool.getResource();
            collection = shardedJedis.getAllShards();
            jedis = collection.iterator();
            while (jedis.hasNext()) {
                jedis.next().select(0);
            }
            pipelined = shardedJedis.pipelined();
            if (listBaiduConfigByCityCode.size() > 0) {//根据城市存放baiduconfig
                for (int j = 0; j < listBaiduConfigByCityCode.size(); j++) {
                    String lineid2 = listBaiduConfigByCityCode.get(j).remove(DisplayName.LINE_ID).toString();
                    baiduConfigBean = new BaiduConfigBean();
                    transMap2Bean(listBaiduConfigByCityCode.get(j), baiduConfigBean);
                    if (linemap.containsKey(lineid2)) {
                        linemap.get(lineid2).add(baiduConfigBean);
                    } else {
                        set = new HashSet<>();
                        set.add(baiduConfigBean);
                        linemap.put(lineid2, set);
                    }
                }
            }
            for (Map.Entry<String, Set<BaiduConfigBean>> entry : linemap.entrySet()) {
                pipelined.hset(RedisKeyName.BAIDU_CONFIG, entry.getKey(), JSON.toJSONString(entry.getValue()));
            }

            pipelined.sync();
            shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.BAIDU_CONFIG, "(getBaiduConfig)line_id获取百度信息==" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            shardedJedis.close();
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(getBaiduConfig)line_id获取百度信息==" + e);
        }
    }

    /**
     * 存放广告优先级 ,给接口调取广告时候判断用
     */
    @Override
    public void putAdPriority() {

        String adPriority = null;//获取广告优先级别
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            adPriority = addataMapper.getAdPriority();
            shardedJedis.del(RedisKeyName.PRIORITY);
            shardedJedis.set(RedisKeyName.PRIORITY, adPriority);
            shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.PRIORITY, "广告投放优先级" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(广告投放优先级" + e);
        } finally {
            shardedJedis.close();
        }

    }

    /**
     * 存放未知车辆
     */
    @Override
    public void positionVehicle() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }

        try {
            shardedJedis.del(RedisKeyName.UNKNOW_CARS);
            ShardedJedisPipeline sp = shardedJedis.pipelined();
            List<String> positionVehicleList = addataMapper.positionVehicle();
            for (String positionVehicle : positionVehicleList) {
                sp.lpush(RedisKeyName.UNKNOW_CARS, positionVehicle);
            }

            sp.hset(RedisKeyName.HEARTBEAT, RedisKeyName.UNKNOW_CARS, "存放未知车辆" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            sp.sync();
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "存放未知车辆" + e);
        } finally {
            shardedJedis.close();
        }

    }

    @Override
    public void selectH() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            shardedJedis.del(RedisKeyName.BLACKLIST);
            ShardedJedisPipeline sp = shardedJedis.pipelined();
            List<String> positionVehicleList = addataMapper.selectH();
            for (String positionVehicle : positionVehicleList) {
                sp.lpush(RedisKeyName.BLACKLIST, positionVehicle);
            }

            sp.hset(RedisKeyName.HEARTBEAT, RedisKeyName.BLACKLIST, "存放成都黑名单" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            sp.sync();
        } catch (Exception e) {
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "存放成都黑名单" + e);
        } finally {
            shardedJedis.close();
        }

    }

    /**
     * CPT策略数据查询
     * 1.查询策略的物料
     * 2.查询策略的线路
     * 3.计算redis中的计数
     * 4.计算报表中的计数
     * 5.统计先达到策略的数就停止策略相关联的线路
     * 6.没有物料删除策略
     * 7.策略是按天统计
     */
    @Override
    public void selectCptStrategy() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        /**
         *     获取redis中cpt数量
         */
        Map<String, Object> redisMap = new HashMap<>();//存储cpt计数
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(2);
        }
        Map<String, String> redistotal = shardedJedis.hgetAll(RedisKeyName.CPT);
        for (Map.Entry<String, String> entry : redistotal.entrySet()) {
            String[] keys = entry.getKey().split("-");
            if (keys[4].equals(datetime())) {
                redisMap.put(keys[0] + BAR + keys[1] + BAR + keys[2] + BAR + keys[3], entry.getValue());
            }
        }
        /**
         * 查询报表的中cpt计数
         */
        List<Map<String, String>> cptCountlist = addataMapper.cptCount();//查询cpt报表的计数
        Map<String, String> cptReportMap = new HashMap<>();
        if (cptCountlist != null && cptCountlist.size() > 0) {
            int cptCount = cptCountlist.size();
            for (int i = 0; i < cptCount; i++) {
                String key = cptCountlist.get(i).get(DisplayName.ADSLOT_ID) + BAR + cptCountlist.get(i).get(DisplayName.STRATEGY_ID) + BAR + cptCountlist.get(i).get(DisplayName.THIRD_ADSLOT_ID) + BAR + cptCountlist.get(i).get(DisplayName.DEV_ID);
                cptReportMap.put(key, cptCountlist.get(i).get(DisplayName.COUNT));
            }
        }

        collection = shardedJedis.getAllShards();
        jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        shardedJedis.del(RedisKeyName.CPT_STRATEGY);
        try {
            List<Map<String, String>> cptMateriallist = addataMapper.selectCptMaterial();//查询出所有当天第三方的所有物料
            /**
             * 物料部分
             */
            Map<String, List<Map<String, String>>> cptmaterialmap = new HashMap<>();//根据adslot_id+strategy封装物料
            if (cptMateriallist != null && cptMateriallist.size() > 0) {
                int cptMaterialcount = cptMateriallist.size();
                for (int i = 0; i < cptMaterialcount; i++) {
                    String key = cptMateriallist.get(i).get(DisplayName.ADSLOT_ID) + BAR + cptMateriallist.get(i).get(DisplayName.STRATEGY_ID) + BAR + cptMateriallist.get(i).get(DisplayName.THIRD_ADSLOT_ID);
                    String image_src = cptMateriallist.get(i).get(DisplayName.IMAGE_SRC);
                    String ad_title = cptMateriallist.get(i).get(DisplayName.AD_TITLE);
                    String description = cptMateriallist.get(i).get(DisplayName.DESCRIPTION);
                    String material_id = cptMateriallist.get(i).get(DisplayName.MATERIAL_ID);
                    Map<String, String> materialmap = new HashMap<>();
                    materialmap.put(DisplayName.IMAGE_SRC, image_src);
                    materialmap.put(DisplayName.AD_TITLE, ad_title);
                    materialmap.put(DisplayName.DESCRIPTION, description);
                    materialmap.put(DisplayName.MATERIAL_ID, material_id);
                    if (cptmaterialmap != null && cptmaterialmap.size() > 0 && cptmaterialmap.get(key) != null) {
                        cptmaterialmap.get(key).add(materialmap);
                    } else {
                        List<Map<String, String>> linelist = new ArrayList<>();
                        linelist.add(materialmap);
                        cptmaterialmap.put(key, linelist);
                    }
                }
            }

//            /**
//             * 计算每条线路上的车辆数
//             */
//            List<Map<String, String>> busTotalList = addataMapper.selectBusTotal();
//            Map<String, String> busTotalMap = new HashMap<>();
//            if (busTotalList != null && busTotalList.size() > 0) {
//                int busTotal = busTotalList.size();
//                for (int i = 0; i < busTotal; i++) {
//                    busTotalMap.put(busTotalList.get(i).get(DisplayName.DEV_ID), busTotalList.get(i).get(DisplayName.TOTAL));
//                }
//            }
            /**
             * 线路部分
             */
            Map<String, List<Map<String, String>>> cptlineIdmap = new HashMap<>();//根据adslot_id+strategy封装线路
            List<Map<String, String>> cptlineIdllist = addataMapper.selectCptlineId();//查询出所对应的线路
            if (cptlineIdllist != null && cptlineIdllist.size() > 0) {
                int cptlineIdlcount = cptlineIdllist.size();
                for (int i = 0; i < cptlineIdlcount; i++) {

                    String key = cptlineIdllist.get(i).get(DisplayName.ADSLOT_ID) + BAR + cptlineIdllist.get(i).get(DisplayName.STRATEGY_ID) + BAR + cptlineIdllist.get(i).get(DisplayName.THIRD_ADSLOT_ID);
                    String dev_id = cptlineIdllist.get(i).get(DisplayName.DEV_ID);
                    int total = Integer.parseInt(cptlineIdllist.get(i).get(DisplayName.TOTAL));//计算总次数,一辆车的总次数*总车辆数


                    /**
                     * 判断是否线路已经播满策略
                     */
                    int additional_copies = getHourNum();
                    if (additional_copies > 72) {
                        additional_copies = 72;
                    }
                    boolean isOk = true;
                    int totalsum = 0;
                    if (total % 72 != 0) {
                        totalsum = (total + total % 72)/72*additional_copies;
                        if (totalsum >= total) {
                            totalsum = total;
                        }
                    }
                    int actual = 0;
                    if (redisMap != null && redisMap.get(key + BAR + dev_id) != null && Integer.parseInt((String) redisMap.get(key + BAR + dev_id)) >= totalsum ) {
                        actual = Integer.parseInt((String) redisMap.get(key + BAR + dev_id));
                        isOk = false;
                    } else if (cptReportMap != null && cptReportMap.get(key + BAR + dev_id) != null && Integer.parseInt(cptReportMap.get(key + BAR + dev_id)) >= totalsum ) {
                        if (actual < Integer.parseInt(cptReportMap.get(key + BAR + dev_id))) {
                            actual = Integer.parseInt(cptReportMap.get(key + BAR + dev_id));
                        }
                        isOk = false;
                    }
                    Map<String, String> linemap = new HashMap<>();
                    linemap.put(DisplayName.DEV_ID, dev_id);
                    linemap.put(DisplayName.TOTAL, String.valueOf(total));
                    linemap.put(DisplayName.FORECAST, totalsum + "");
                    linemap.put(DisplayName.ACTUAL, actual + "");


                    if (cptlineIdmap != null && cptlineIdmap.size() > 0 && cptlineIdmap.get(key) != null && isOk) {
                        cptlineIdmap.get(key).add(linemap);
                    } else if (isOk) {
                        List<Map<String, String>> linelist = new ArrayList<>();
                        linelist.add(linemap);
                        cptlineIdmap.put(key, linelist);
                    }

                }
            }
            /**
             * 整合线路物料数据,生成cpt_strategy策略
             */

            Map<String, List<Object>> cptstrategyMap = new HashMap<>();//存放策略数组
            Map<String, List<Map<String, Object>>> orderMap = new HashMap<>();//存放订单关系数据
            if (cptlineIdmap != null && cptlineIdmap.size() > 0) {//如果有线路才拼装
                for (Map.Entry<String, List<Map<String, String>>> entry : cptlineIdmap.entrySet()) {
                    String key = entry.getKey();
                    List<Map<String, String>> value = entry.getValue();
                    String adslot_id = key.split("-")[0];//策略的adslot_id
                    String strategy_id = key.split("-")[1];//策略的strategy_id
                    String third_adslot_id = key.split("-")[2];//策略的third_adslot_id
                    String onestrategy = adslot_id + "-" + strategy_id;//锁定单个策略
                    Map<String, Object> third_adslot_id_map = new HashMap<>();
                    if (cptmaterialmap != null && cptmaterialmap.size() > 0 && cptmaterialmap.get(key) != null) {
                        third_adslot_id_map.put(DisplayName.THIRD_ADSLOT_ID, third_adslot_id);
                        third_adslot_id_map.put(DisplayName.MATERIALS, cptmaterialmap.get(key));
                        third_adslot_id_map.put(DisplayName.LINES, cptlineIdmap.get(key));
                        if (orderMap != null && orderMap.size() > 0 && orderMap.get(onestrategy) != null) {//组装units
                            orderMap.get(onestrategy).add(third_adslot_id_map);
                        } else {
                            List<Map<String, Object>> unitslist = new ArrayList<>();
                            unitslist.add(third_adslot_id_map);
                            orderMap.put(onestrategy, unitslist);
                        }

                    }
                }
                /**
                 *整合策略组
                 */
                if (orderMap != null && orderMap.size() > 0) {
                    for (Map.Entry<String, List<Map<String, Object>>> entry : orderMap.entrySet()) {
                        String onestrategy = entry.getKey();
                        List<Map<String, Object>> listslote = entry.getValue();
                        String adslot_id = onestrategy.split("-")[0];//策略的adslot_id
                        String strategy_id = onestrategy.split("-")[1];//策略的strategy_id
                        Map<String, Object> strategymap = new HashMap<>();
                        strategymap.put(DisplayName.STRATEGY_ID, strategy_id);
                        strategymap.put(DisplayName.UNITS, listslote);
                        strategymap.put(DisplayName.THIRD, "1");
                        if (cptstrategyMap != null && cptstrategyMap.size() > 0 && cptstrategyMap.get(adslot_id) != null) {
                            cptstrategyMap.get(adslot_id).add(strategymap);
                        } else {
                            List<Object> liststrategy = new ArrayList<>();
                            liststrategy.add(strategymap);
                            cptstrategyMap.put(adslot_id, liststrategy);
                        }

                    }
                }

                /**
                 * 扔进redis
                 */

                if (cptstrategyMap != null && cptstrategyMap.size() > 0) {
                    for (Map.Entry<String, List<Object>> entry : cptstrategyMap.entrySet()) {
                        shardedJedis.hset(RedisKeyName.CPT_STRATEGY, entry.getKey(), JSON.toJSONString(entry.getValue()));
                        shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.CPT_STRATEGY, "存放成cpt_strategy策略" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "存放cptstrategy异常" + e);
        } finally {
            shardedJedis.close();
        }
    }

    /**
     * 查询cdn物料
     */
    @Override
    public void getCDNMaterial() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        try {
            ShardedJedisPipeline sp = shardedJedis.pipelined();
            Map<String, Object> redisMap = new HashMap<>();//存储cpt计数
            Collection<Jedis> collection = shardedJedis.getAllShards();
            Iterator<Jedis> jedis = collection.iterator();
            while (jedis.hasNext()) {
                jedis.next().select(0);
            }
            shardedJedis.del(RedisKeyName.CDN_MATERIAL);
            List<Map<String, String>> listmaterial = addataMapper.getCDNMaterial();
            if (listmaterial != null && listmaterial.size() > 0) {//放进redis
                int materialcount = listmaterial.size();
                for (int i = 0; i < materialcount; i++) {
                    sp.hset(RedisKeyName.CDN_MATERIAL, listmaterial.get(i).get(DisplayName.MATERIAL_URL), listmaterial.get(i).get(DisplayName.CDN_URL));
                }
                sp.sync();
                shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.CDN_MATERIAL, "存放成CDN_MATERIAL" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            }
        } finally {
            shardedJedis.close();
        }
    }

    /**
     * 获取第三方回调url
     */
    @Override
    public void selectCptMonitor() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        try {
            ShardedJedisPipeline sp = shardedJedis.pipelined();
            Map<String, List<String>> cptMonitorMap = new HashMap<>();//存储地方回调关系
            Collection<Jedis> collection = shardedJedis.getAllShards();
            Iterator<Jedis> jedis = collection.iterator();
            while (jedis.hasNext()) {
                jedis.next().select(0);
            }
            shardedJedis.del(RedisKeyName.CPT_MONITOR);
            List<Map<String, String>> listCptMonitor = addataMapper.selectCptMonitor();
            if (listCptMonitor != null && listCptMonitor.size() > 0) {
                int cptMonitorCount = listCptMonitor.size();
                for (int i = 0; i < cptMonitorCount; i++) {
                    if (cptMonitorMap != null && cptMonitorMap.size() > 0) {
                        if (cptMonitorMap.get(listCptMonitor.get(i).get(DisplayName.MATERIAL_ID)) != null) {
                            cptMonitorMap.get(listCptMonitor.get(i).get(DisplayName.MATERIAL_ID)).add(listCptMonitor.get(i).get(DisplayName.MONITOR_URL));
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(listCptMonitor.get(i).get(DisplayName.MONITOR_URL));
                            cptMonitorMap.put(listCptMonitor.get(i).get(DisplayName.MATERIAL_ID), list);
                        }

                    } else {
                        List<String> list = new ArrayList<>();
                        list.add(listCptMonitor.get(i).get(DisplayName.MONITOR_URL));
                        cptMonitorMap.put(listCptMonitor.get(i).get(DisplayName.MATERIAL_ID), list);
                    }
                }
            }


            if (cptMonitorMap != null && cptMonitorMap.size() > 0) {
                for (Map.Entry<String, List<String>> entry : cptMonitorMap.entrySet()) {
                    String key = entry.getKey().toString();
                    List<String> listvalue = entry.getValue();
                    sp.hset(RedisKeyName.CPT_MONITOR, key, JSON.toJSONString(listvalue));
                }
                sp.sync();
                shardedJedis.hset(RedisKeyName.HEARTBEAT, RedisKeyName.CPT_MONITOR, "存放成地方回调url" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            }
        } finally {
            shardedJedis.close();
        }
    }


    /**
     * 添加时间戳
     */
    @Override
    public void addDatetime() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Collection<Jedis> collection = shardedJedis.getAllShards();
        Iterator<Jedis> jedis = collection.iterator();
        while (jedis.hasNext()) {
            jedis.next().select(0);
        }
        try {
            shardedJedis.hset(RedisKeyName.HEARTBEAT, DisplayName.ADDDATETIME, String.valueOf(System.currentTimeMillis() / 1000));
            shardedJedis.close();
        } catch (Exception e) {
            shardedJedis.close();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(addDatetime)添加时间戳==" + e);
        }
    }


    /**
     * 获取中文星期
     *
     * @return
     */

    public java.lang.String getChinaWeek() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
        java.lang.String week = sdf.format(new Date());
        return week;
    }

    /**
     * 获取数字星期
     *
     * @return
     */
    public String getEnglishWeek() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        return w + "";
    }

    public String join(Collection var0, String var1) {
        StringBuffer var2 = new StringBuffer();

        for (Iterator var3 = var0.iterator(); var3.hasNext(); var2.append((String) var3.next())) {
            if (var2.length() != 0) {
                var2.append(var1);
            }
        }

        return var2.toString();
    }

    private String hour() {
        String hour = new SimpleDateFormat("HH").format(new Date());
        return hour;
    }

    private String datetime() {
        String datetime = new SimpleDateFormat("yyyyMMdd").format(new Date());
        return datetime;
    }

    public static void main(String[] args) throws ParseException {
//     Date date = new Date(1391174450000L); // 2014-1-31 21:20:50
//        String dateStr = "2014-1-31 21:20:50 ";
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
//        try {
//            Date dateTmp = dateFormat.parse(dateStr);
//            System.out.println(dateTmp);
//
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        String dateStrTmp = dateFormat.parse(dateStr);
//        System.out.println(dateStrTmp);
//        Calendar cal = Calendar.getInstance();
//        SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss-yyyy-MM-dd");
//        cal.setTime(sf.parse("04:34:00-2017-08-15"));
//        cal.add(Calendar.HOUR, +8);
//        String yesterday = sf.format(cal.getTime());
//        System.err.println(yesterday);

//        String str = "";

// set转换为字符串

//        List<String> list = new ArrayList<>();
//        list.add("AA");
//        list.add("BB");
//        list.add("CC");
////        System.err.println(join(list, ","));
//        String ss = "AA,BB,CC";
//        String[] xx = ss.split(",");
//
//        List<String> list1 = new ArrayList<>();
//        Collections.addAll(list1, "1", "2");
////       for(int i = 0;i<list1.size();i++){
//        System.err.println(list1.size());
////       }
//        Map<String, Object> map = new HashMap<>();
//        map.put("a", "");
//        System.err.println("".equals(map.get("a")));
//        SimpleDateFormat sf = new SimpleDateFormat("HH");
//        String validationhour = sf.format(new Date());
//        String hour = sf.format(new Date());
////        String str = "1,2,9,12,13,14,15,17,21,23";
//        String str = "1,2,17";
//
//        String[] xs = str.split(",");
//        System.err.println(xs.length);
//        for (int i = xs.length - 1; i >= 0; i--) {
//            if (Integer.parseInt(validationhour) == Integer.parseInt(xs[i])) {
//                validationhour = xs[i];
//                break;
//            }
//            if (Integer.parseInt(validationhour) > Integer.parseInt(xs[i])) {
//                validationhour = xs[i + 1];
//                break;
//            }
//        }
//        System.err.println(validationhour);
//        if (Integer.parseInt(validationhour) == Integer.parseInt(hour)) {
//            validationhour = xs[0];
//        }
//        if (Integer.parseInt(validationhour) < Integer.parseInt(hour)) {
//            //移除策略
//        }
//        System.err.println(str.substring(str.indexOf(validationhour), str.length()));
//
//
//        String kk = "18";
//        System.err.println(kk.substring(kk.indexOf(",") + 1));

//        String str = "1,11";
//        String str = "17,15,21";
//        String str = "17";
////        String str = "1,11,17,15,21";
//        String st = "";
//        String[] strs = str.split(",");
//        for (int k = 0; k < strs.length; k++) {
//            if (11 != Integer.parseInt(strs[k])) {
//                if(k!=0){
//                    st = st+ ","+strs[k];
//                }else{
//                    st = strs[k];
//                }
//            }
//        }
//        System.err.println(st);
        SimpleDateFormat dfs = new SimpleDateFormat("HH:mm");
        java.util.Date begin = dfs.parse("08:00");
        java.util.Date end = dfs.parse("00:29");
        long between = (end.getTime() - begin.getTime()) / 1000;//除以1000是为了转换成秒
        long hour1 = between % (24 * 3600) / 3600;
        long minute1 = between % 3600 / 60;
        System.out.println(1000 - (hour1 * 60 + minute1) / 10);
    }
}