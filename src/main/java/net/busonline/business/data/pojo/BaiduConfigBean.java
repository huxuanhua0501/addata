package net.busonline.business.data.pojo;

import org.springframework.stereotype.Component;

@Component
public class BaiduConfigBean {
    private String adslot_id;
    private String baidu_adslot_id;
    private String baidu_app_id;
    private String adslot_type;

    public String getAdslot_id() {
        return adslot_id;
    }

    public void setAdslot_id(String adslot_id) {
        this.adslot_id = adslot_id;
    }

    public String getBaidu_adslot_id() {
        return baidu_adslot_id;
    }

    public void setBaidu_adslot_id(String baidu_adslot_id) {
        this.baidu_adslot_id = baidu_adslot_id;
    }

    public String getBaidu_app_id() {
        return baidu_app_id;
    }

    public void setBaidu_app_id(String baidu_app_id) {
        this.baidu_app_id = baidu_app_id;
    }


    public String getAdslot_type() {
        return adslot_type;
    }

    public void setAdslot_type(String adslot_type) {
        this.adslot_type = adslot_type;
    }
}
