package net.busonline.business.lbsmaterial.service;

public interface ILBSMaterialService {
    /**
     * 查询数据日志数据
     * 检索lbs数据(不包含日志数据)
     * 根据检索出的数据去fastdns中下载物料
     * 将物料上传到cdn服务器
     * 更新广告平台ad库中物料,将cdn的物料地址更新到物料表中
     *
     */
    public void lbsMaterialBusiness();
}
