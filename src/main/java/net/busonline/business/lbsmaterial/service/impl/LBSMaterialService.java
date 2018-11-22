package net.busonline.business.lbsmaterial.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.tobato.fastdfs.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import net.busonline.business.data.dao.AddataMapper;
import net.busonline.business.lbsmaterial.lbsdao.LBSMaterialMapper;
import net.busonline.business.lbsmaterial.service.ILBSMaterialService;
import net.busonline.business.sharing.utils.MD5Util;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import sun.net.www.http.HttpClient;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 4g补传业务
 *
 * @author xuanhua.hu
 * @since 3.0
 */
@Service
public class LBSMaterialService implements ILBSMaterialService {
    private Logger logger = LoggerFactory.getLogger(LBSMaterialService.class);
    //    @Autowired
//    FastFileStorageClient fastFileStorageClient;
    @Autowired
    private LBSMaterialMapper lbsMaterialMapper;
    @Autowired
    private AddataMapper addataMapper;
    @Value("${file_path}")
    String file_path;
    @Value("${cdn_url}")
    String cdn_url;
    @Value("${fastdfs_url}")
    String fastdfs_url;
    private static Map<String, String> materialMap = new HashMap<>();

    /**
     * 4g物料下载业务
     */
    @Override
    public void lbsMaterialBusiness() {
        try {
            /**
             * 下载央视物料
             */
            List<String> materiallist = lbsMaterialMapper.getMaterials();//所有已经下载的物料
            // String lbsid = lbsMaterialMapper.getVersion();//要下载的新物料
//            if (lbsid != null) {
            List<Map<String, String>> downloadmateriallist = lbsMaterialMapper.getDownloadMaterials(materiallist);//要下载的物料
            if (downloadmateriallist != null && downloadmateriallist.size() > 0) {
                int downloadmaterialcount = downloadmateriallist.size();
                DownloadByteArray callback = new DownloadByteArray();
                for (int i = 0; i < downloadmaterialcount; i++) {
//                        String group = downloadmateriallist.get(i).get("fastdfs_url").substring(0, 6);
//                        String fastdfsurl = downloadmateriallist.get(i).get("fastdfs_url").substring(7);
                    String cctv_name = downloadmateriallist.get(i).get("cctv_name");
//                        byte[] content = fastFileStorageClient.downloadFile(group, fastdfsurl, callback);
//                        getFile(content, file_path, cctv_name);
                    boolean isOk = getCCTVFile(fastdfs_url + cctv_name, file_path, cctv_name);
                    if (isOk) {
                        lbsMaterialMapper.insertMaterial(downloadmateriallist.get(i));
                    }
                }

            }


            /**
             * 将央视的物料的cdn地址绑定到ad_material物料中
             */
            List<String> baidufileList = addataMapper.getBaiduFileName();
            if (baidufileList != null && baidufileList.size() > 0) {
                List<Map<String, String>> cdnurlist = lbsMaterialMapper.getcctv_url(baidufileList);
                if (cdnurlist != null && cdnurlist.size() > 0) {
                    int cdnurcout = cdnurlist.size();
                    for (int i = 0; i < cdnurcout; i++) {
                        addataMapper.updateMaterial(cdn_url + cdnurlist.get(i).get("pi_file_name"), cdnurlist.get(i).get("baidu_file_name"));


                    }
                }
            }
            /**
             * 判断有没有漏发物料功能
             * 1:查询的全量物料
             * 2:将全量物料跟cctv库匹配出差量物料
             * 3:将差量物料发邮件给孙钰和王爽告知,以便其补发物料
             */
            List<String> fullbaidufileList = addataMapper.getfullBaiduFileName();//cctv的全量物料
            if (fullbaidufileList != null && fullbaidufileList.size() > 0) {
                List<Map<String, String>> deltacdnurlist = lbsMaterialMapper.getdeltacctv_url(fullbaidufileList);//差量物料
                if (deltacdnurlist != null && deltacdnurlist.size() > 0) {//符合条件就发送邮件
                    if (!(materialMap != null && materialMap.get(MD5Util.string2MD5(JSON.toJSONString(deltacdnurlist))) != null)) {//如果不存在就发邮件,默认只发一封
                        materialMap.put(MD5Util.string2MD5(JSON.toJSONString(deltacdnurlist)), JSON.toJSONString(deltacdnurlist));
                        sendMessage(JSON.toJSONString(deltacdnurlist));
                    }
                }

            }


            /**
             * 手动加载cdn,进行预热
             */
            List<String> listPreheatingCdn_url = addataMapper.getPreheatingCdn_url();
            if (listPreheatingCdn_url != null && listPreheatingCdn_url.size() > 0) {//预热cdn_url
                int preheatingCdn_urlCount = listPreheatingCdn_url.size();
                for (int i = 0; i < preheatingCdn_urlCount; i++) {
                    String cdn_url = listPreheatingCdn_url.get(i);
                    String status = httpGet(cdn_url);
                    logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "cdn预热的url和是否预热成功的状态值==" + cdn_url + "<>状态值status=" + status);
                    if (status != null) {
                        addataMapper.updateCdn_type(cdn_url);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(lbsMaterialBusiness)4g物料下载业务==" + e);
        }
    }

    /**
     * 根据byte数组，生成物料
     */
    public static void getFile(byte[] bfile, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {//判断文件目录是否存在
                dir.mkdirs();
            }
            file = new File(filePath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取4g补传文件
     *
     * @param url
     * @param destFileName
     * @param fileName
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public boolean getCCTVFile(String url, String destFileName, String fileName)
            throws ClientProtocolException, IOException {
        // 生成一个httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        File file = new File(destFileName);
        if (response.getStatusLine().getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
            if (!file.exists()) {
                file.mkdirs();
            }
            try {
                FileOutputStream fout = new FileOutputStream(file + File.separator + fileName);
                int l = -1;
                byte[] tmp = new byte[1024];
                while ((l = in.read(tmp)) != -1) {
                    fout.write(tmp, 0, l);
                    // 注意这里如果用OutputStream.write(buff)的话，图片会失真，大家可以试试
                }
                fout.flush();
                fout.close();

            } finally {
                // 关闭低层流。
                in.close();
            }
            httpclient.close();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 发送 get请求
     */
    public String httpGet(String url) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String status = null;
        try {
            // 创建httpget.
            HttpGet httpget = new HttpGet(url);
//            System.out.println("executing request " + httpget.getURI());
            // 执行get请求.
            CloseableHttpResponse response = httpclient.execute(httpget);

            try {
                // 获取响应实体
//                HttpEntity entity = response.getEntity();
//                System.out.println("--------------------------------------");
                // 打印响应状态
                status = response.getStatusLine().toString();
//                System.out.println(response.getStatusLine());
//                if (entity != null) {
//                    // 打印响应内容长度
//                    System.out.println("Response content length: " + entity.getContentLength());
//                    // 打印响应内容
//                    System.out.println("Response content: " + EntityUtils.toString(entity));
//                }
//                System.out.println("------------------------------------");
            } finally {
                response.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭连接,释放资源
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return status;
    }

    public static void main(String[] args) {
        String str = "group2/M00/00/2F/wKgEBloK4vSAWvK-AAJTyCMX6n0.609186";
        System.err.println(str.substring(0, 6));
        System.err.println(str.substring(7));
    }

    private void sendMessage(String material) {
        Transport transport = null;
        try {
            String myEmailAccount = "xuanhua.hu@busonline.com";
            String myEmailPassword = "hxh19880501";
            // 发件人邮箱的 SMTP 服务器地址, 必须准确, 不同邮件服务器地址不同, 一般(只是一般, 绝非绝对)格式为: smtp.xxx.com
            // 网易163邮箱的 SMTP 服务器地址为: smtp.163.com
            String myEmailSMTPHost = "smtp.busonline.com";
            // 收件人邮箱（替换为自己知道的有效邮箱）
            String receiveMailAccount = "yu.sun@busonline.com";

//            String receiveMailAccount = "xuanhua.hu@busonline.com";
//            String CCMailAccount = "xuanhua.hu@busonline.com";//抄送
            // 1. 创建参数配置, 用于连接邮件服务器的参数配置
            Properties props = new Properties();                    // 参数配置
            props.setProperty("mail.transport.protocol", "smtp");   // 使用的协议（JavaMail规范要求）
            props.setProperty("mail.smtp.host", myEmailSMTPHost);   // 发件人的邮箱的 SMTP 服务器地址
            props.setProperty("mail.smtp.auth", "true");            // 需要请求认证
            // 2. 根据配置创建会话对象, 用于和邮件服务器交互
            Session session = Session.getInstance(props);
            session.setDebug(true);                                 // 设置为debug模式, 可以查看详细的发送 log
            // 3. 创建一封邮件
            MimeMessage message = null;

            message = createMimeMessage(session, myEmailAccount, receiveMailAccount, material);

            // 4. 根据 Session 获取邮件传输对象
            transport = session.getTransport();
            transport.connect(myEmailAccount, myEmailPassword);

            // 6. 发送邮件, 发到所有的收件地址, message.getAllRecipients() 获取到的是在创建邮件对象时添加的所有收件人, 抄送人, 密送人
            transport.sendMessage(message, message.getAllRecipients());
            // 7. 关闭连接

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (transport != null)
                    transport.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param session
     * @param sendMail//发送者
     * @param receiveMail//接收着
     * @param material//漏发的物料
     * @return
     * @throws Exception
     */
    private MimeMessage createMimeMessage(Session session, String sendMail, String receiveMail, String material) throws Exception {
        String CCMailAccount1 = "shuang.wang@busonline.com";//抄送
        String CCMailAccount2 = "shuo.liu@busonline.com";//抄送
        String CCMailAccount3 = "kaige.shi@busonline.com";//抄送
        String CCMailAccount4 = "xuanhua.hu@busonline.com";//抄送
        // 1. 创建一封邮件
        MimeMessage message = new MimeMessage(session);

        // 2. From: 发件人（昵称有广告嫌疑，避免被邮件服务器误认为是滥发广告以至返回失败，请修改昵称）
        message.setFrom(new InternetAddress(sendMail, "胡宣化", "UTF-8"));

        // 3. To: 收件人（可以增加多个收件人、抄送、密送）
        message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(receiveMail, "孙钰", "UTF-8"));
        Address[] ccAdresses = new InternetAddress[4];
        ccAdresses[0] = new InternetAddress(CCMailAccount1, "王爽", "UTF-8");
        ccAdresses[1] = new InternetAddress(CCMailAccount2, "刘硕", "UTF-8");
        ccAdresses[2] = new InternetAddress(CCMailAccount3, "施凯歌", "UTF-8");
        ccAdresses[3] = new InternetAddress(CCMailAccount4, "胡宣化", "UTF-8");

        message.setRecipients(MimeMessage.RecipientType.CC, ccAdresses);
        // 4. Subject: 邮件主题（标题有广告嫌疑，避免被邮件服务器误认为是滥发广告以至返回失败，请修改标题）
        message.setSubject("广告平台物料缺失提醒", "UTF-8");

        // 5. Content: 邮件正文（可以使用html标签）（内容有广告嫌疑，避免被邮件服务器误认为是滥发广告以至返回失败，请修改发送内容）
        message.setContent(material, "text/html;charset=UTF-8");

        // 6. 设置发件时间
        message.setSentDate(new Date());

        // 7. 保存设置
        message.saveChanges();

        return message;
    }

}
