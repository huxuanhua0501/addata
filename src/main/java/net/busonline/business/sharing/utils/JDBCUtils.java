package net.busonline.business.sharing.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSourceFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@PropertySource({"classpath:/config_test.properties"})//扫描
public class JDBCUtils {
	@Autowired
	Environment env;
	  private static DataSource dataSource = null;
	   protected Connection con = null; 
		protected PreparedStatement pre = null;
		protected ResultSet res = null;
	    //声明线程共享变量
	    public static ThreadLocal<Connection> container = new ThreadLocal<Connection>();
	    //配置说明，参考官方网址
	    //http://blog.163.com/hongwei_benbear/blog/static/1183952912013518405588/
/*	    static{
	        dataSource.setUrl("jdbc:mysql://192.168.108.145:3306/cctv_dev?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true");
	        dataSource.setUsername("xuanhua.hu");//用户名
	        dataSource.setPassword("1234qwer");//密码
	        dataSource.setInitialSize(2);
	        dataSource.setMaxActive(20);
	        dataSource.setMinIdle(0);
	        dataSource.setMaxWait(60000);
	        dataSource.setValidationQuery("SELECT 1");
	        dataSource.setTestOnBorrow(false);
	        dataSource.setTestWhileIdle(true);
	        dataSource.setPoolPreparedStatements(false);
	    }*/

@PostConstruct
	public void init(){
			try{
				Properties properties = new Properties();
				properties.setProperty("url", env.getProperty("url"));
				properties.setProperty("username", env.getProperty("mysqlusername"));
				properties.setProperty("password", env.getProperty("password"));
				properties.setProperty("initialSize", env.getProperty("initialSize"));
				properties.setProperty("maxActive", env.getProperty("maxActive"));
				properties.setProperty("minIdle", env.getProperty("minIdle"));
				properties.setProperty("maxWait", env.getProperty("maxWait"));
				properties.setProperty("removeAbandoned", env.getProperty("removeAbandoned"));
				properties.setProperty("removeAbandonedTimeout", env.getProperty("removeAbandonedTimeout"));
				properties.setProperty("timeBetweenEvictionRunsMillis", env.getProperty("timeBetweenEvictionRunsMillis"));
				properties.setProperty("minEvictableIdleTimeMillis", env.getProperty("minEvictableIdleTimeMillis"));
				properties.setProperty("validationQuery", env.getProperty("validationQuery"));
				properties.setProperty("testWhileIdle", env.getProperty("testWhileIdle"));
				properties.setProperty("testOnBorrow", env.getProperty("testOnBorrow"));
				properties.setProperty("testOnReturn", env.getProperty("testOnReturn"));
				dataSource = DruidDataSourceFactory.createDataSource(properties);
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	 
	    /**
	     * 获取数据连接
	     * @return
	     */
	    public static Connection getConnection(){
	        Connection conn =null;
	        try{
	            conn = dataSource.getConnection();
	            System.out.println(Thread.currentThread().getName()+"连接已经开启......");
	            container.set(conn);
	        }catch(Exception e){
	            System.out.println("连接获取失败");
	            e.printStackTrace();
	        }
	        return conn;
	    }
	    /***获取当前线程上的连接开启事务*/
	    public static void startTransaction(){
	        Connection conn=container.get();//首先获取当前线程的连接
	        if(conn==null){//如果连接为空
	            conn=getConnection();//从连接池中获取连接
	            container.set(conn);//将此连接放在当前线程上
	            System.out.println(Thread.currentThread().getName()+"空连接从dataSource获取连接");
	        }else{
	            System.out.println(Thread.currentThread().getName()+"从缓存中获取连接");
	        }
	        try{
	            conn.setAutoCommit(false);//开启事务
	        }catch(Exception e){
	            e.printStackTrace();
	        }
	    }
	    //提交事务
	    public static void commit(){
	        try{
	            Connection conn=container.get();//从当前线程上获取连接if(conn!=null){//如果连接为空，则不做处理
	            if(null!=conn){
	                conn.commit();//提交事务
	                System.out.println(Thread.currentThread().getName()+"事务已经提交......");
	            }
	        }catch(Exception e){
	            e.printStackTrace();
	        }
	    }
	 
	 
	    /***回滚事务*/
	    public static void rollback(){
	        try{
	            Connection conn=container.get();//检查当前线程是否存在连接
	            if(conn!=null){
	                conn.rollback();//回滚事务
	                container.remove();//如果回滚了，就移除这个连接
	            }
	        }catch(Exception e){
	            e.printStackTrace();
	        }
	    }
	    /***关闭连接*/
	    public static void closeconnection(){
	        try{
	            Connection conn=container.get();
	            if(conn!=null){
	                conn.close();
	                System.out.println(Thread.currentThread().getName()+"连接关闭");
	            }
	        }catch(SQLException e){
	            throw new RuntimeException(e.getMessage(),e);
	        }finally{
	            try {
	                container.remove();//从当前线程移除连接切记
	            } catch (Exception e2) {
	                e2.printStackTrace();
	            }
	        }
	    }
	    //简单使用方式
	    public static void main(String[] args) throws SQLException {
	        //select查询
	        /*Connection conn = JDBCUtils.getConnection();
	        PreparedStatement ps = conn.prepareStatement("SELECT 1");
	        ResultSet rs = ps.executeQuery();
	        JDBCUtils.close();*/
	 
	        //update,insert,delete操作
	        Connection conn2 = JDBCUtils.getConnection();
	        //开启事务1
	        JDBCUtils.startTransaction();
	        System.out.println("执行事务操作111111111111111....");
	        JDBCUtils.commit();
	        //开启事务2
	        JDBCUtils.startTransaction();
	        System.out.println("执行事务操作222222222222....");
	        JDBCUtils.commit();
	        JDBCUtils.closeconnection();
	        for (int i = 0; i < 2; i++) {
	            new Thread(new Runnable() {
	 
	                public void run() {
	                    Connection conn2 = JDBCUtils.getConnection();
	                    for (int i = 0; i < 2; i++) {
	                        JDBCUtils.startTransaction();
	                        System.out.println(conn2);
	                        System.out.println(Thread.currentThread().getName()+"执行事务操作。。。。。。。。。。。。。");
	                        JDBCUtils.commit();
	                    }
	                    JDBCUtils.closeconnection();
	                }
	            }).start();
	        }
	         
	    }
	}