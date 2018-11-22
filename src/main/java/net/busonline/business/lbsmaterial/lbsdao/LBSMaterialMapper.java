package net.busonline.business.lbsmaterial.lbsdao;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 4g物料补传业务
 *
 * @author xuanhua.hu
 * @since 3.0
 */
public interface LBSMaterialMapper {

    /**
     * 获取已经同步的物料
     *
     * @return
     */
    public List<String> getMaterials();

    /**
     * 获取最新未同步的版本
     *
     * @return
     */
    public String getVersion();

    /**
     * 查询要下载的物料
     */
    public List<Map<String, String>> getDownloadMaterials(@Param("list") List<String> list);

    /**
     * 将已经下载的物料入库
     */
    public void insertMaterial(@Param("map") Map<String, String> map);

    /**
     * 查询要添加cdn地址的物料
     */
    public List<Map<String,String>> getcctv_url(@Param("list") List<String> list);

    /**
     * 获取物料
     * @return
     */
    public List<Map<String,String>>  getdeltacctv_url(@Param("list") List<String> list);
}
