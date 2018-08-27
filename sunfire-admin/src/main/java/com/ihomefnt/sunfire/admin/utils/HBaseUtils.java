package com.ihomefnt.sunfire.admin.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos.MutationProto.MutationType;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.util.ResourceBundles;

public class HBaseUtils {

    private static final Configuration conf;
    private static Connection con;
    private static Admin admin;
    private static ExecutorService pool;

    static {
        conf = HBaseConfiguration.create();

        //加载hbase.properties配置文件信息
        ResourceBundle rb = ResourceBundles.getBundle("hbase.properties");
        Enumeration <String> kvs = rb.getKeys();
        while (kvs.hasMoreElements()) {
            String key = kvs.nextElement();
            String value = rb.getString(key);
            conf.set(key, value);
        }
		/*pool = Executors.newCachedThreadPool();
		con = ConnectionFactory.createConnection(conf, pool);
		admin = con.getAdmin();  //管理者*/
    }

    public static Connection getConn() {
        try {
            pool = Executors.newCachedThreadPool();
            con = ConnectionFactory.createConnection(conf, pool);
            return con;
        } catch (IOException e) {
            throw new RuntimeException("数据库连接失败！", e);
        }

    }


    public static Admin getAdmin() {
        try {
            getConn();
            admin = con.getAdmin();
            return admin;//管理者
        } catch (IOException e) {
            throw new RuntimeException("取得管理者失败！", e);
        }
    }


    public static void close() {
        try {
            if (admin != null) {
                admin.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("关闭管理者失败！", e);
        }
        try {
            if (con != null && admin == null) {
                con.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("关闭连接失败！", e);
        }
        try {
            if (pool.isShutdown()) {
                pool.shutdown();
            }
        } catch (Exception e) {
            throw new RuntimeException("关闭连接池失败！", e);
        }

    }

    /**
     * 创建表
     *
     * @param tableName 表名
     * @param columnFamilies 列族
     * @return true表示创建成功反之失败
     */
    public static boolean createTable(String tableName, String... columnFamilies) {
        getAdmin();
        TableName tn = TableName.valueOf(tableName);
        try {
            if (!admin.tableExists(tn)) {
                HTableDescriptor htd = new HTableDescriptor(tn);
                for (String cf : columnFamilies) {
                    HColumnDescriptor hcd = new HColumnDescriptor(Bytes.toBytes(cf));
                    htd.addFamily(hcd);
                }
                admin.createTable(htd); //创建表
                return true;
            } else {
                throw new TableExistsException("表已经存在！！！");
            }
        } catch (IOException e) {
            throw new RuntimeException("表已经存在！");
        } finally {
            close();
        }
    }

    /**
     * 删除表操作
     *
     * @param tableName 表名
     * @return true表示删除表成功，反之失败
     */
    public static boolean deleteTable(String tableName) {
        getAdmin();
        TableName delTN = TableName.valueOf(tableName);
        try {
            if (admin.tableExists(delTN)) {
                admin.disableTable(delTN);  //使表失效
                admin.deleteTable(delTN);  //删除表
                return true;
            } else {
                throw new RuntimeException("表不存在！");
            }
        } catch (IOException e) {
            throw new RuntimeException("删除表失败！", e);
        } finally {
            close();
        }
    }


    /**
     * 修改表操作
     *
     * @param tableName 表名
     * @param mt 操作类型
     * @param rowkey 行键
     * @param params 参数（列族、单元格修饰名、单元格的值）
     */
    public static void doUpdate(String tableName, MutationType mt, String rowkey,
            String... params) {
        getAdmin();
        TableName tn = TableName.valueOf(tableName);
        try {
            if (admin.tableExists(tn)) {
                Table t = con.getTable(tn, pool);
                switch (mt) {
                    case PUT:
                        Put put = null;
                        if (params.length == 3) {
                            put = new Put(Bytes.toBytes(rowkey)).
                                    addColumn(Bytes.toBytes(params[0]), Bytes.toBytes(params[1]),
                                            Bytes.toBytes(params[2]));
                        } else {
                            throw new RuntimeException("参数必须为三个！");
                        }
                        t.put(put);
                        break;
                    case DELETE:
                        Delete del = new Delete(Bytes.toBytes(rowkey));
                        if (params != null) {
                            switch (params.length) {
                                case 1:
                                    del.addFamily(Bytes.toBytes(params[0]));
                                    break;
                                case 2:
                                    del.addColumn(Bytes.toBytes(params[0]),
                                            Bytes.toBytes(params[1]));
                                    break;
                                default:
                                    throw new RuntimeException("最多两个参数");
                            }
                        }
                        t.delete(del);
                        break;
                    default:
                        throw new RuntimeException("只能进行增删改操作！");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }


    /**
     * 多行修改
     *
     * @param params 多列族 String[]
     */
    public static void doUpdate(String tableName, MutationType mt, String rowkey,
            String[]... params) {
        getAdmin();
        TableName tn = TableName.valueOf(tableName);
        try {
            if (admin.tableExists(tn)) {
                Table t = con.getTable(tn, pool);
                switch (mt) {
                    case PUT:
                        Put put = null;
                        put = new Put(Bytes.toBytes(rowkey));
                        for (String[] ps : params) {
                            if (ps.length == 3) {
                                put.addColumn(Bytes.toBytes(ps[0]), Bytes.toBytes(ps[1]),
                                        Bytes.toBytes(ps[2]));
                            } else {
                                throw new RuntimeException("参数必须为三个！");
                            }
                        }
                        t.put(put);
                        break;
                    case DELETE:
                        Delete del = new Delete(Bytes.toBytes(rowkey));
                        for (String[] ps : params) {
                            if (ps != null) {
                                switch (ps.length) {
                                    case 1:
                                        del.addFamily(Bytes.toBytes(ps[0]));
                                        break;
                                    case 2:
                                        del.addColumn(Bytes.toBytes(ps[0]), Bytes.toBytes(ps[1]));
                                        break;
                                    default:
                                        throw new RuntimeException("最多两个参数");
                                }
                            }
                        }
                        t.delete(del);
                        break;
                    default:
                        throw new RuntimeException("只能进行增删改操作！");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }


    /**
     * 多表进行增删改操作
     *
     * @param params 多行 Map<String,List<String[]>>
     */
    public static void doUpdate(String tableName, MutationType mt,
            Map <String, List <String[]>> params) {
        getAdmin();
        TableName tn = TableName.valueOf(tableName);
        try {
            if (admin.tableExists(tn)) {
                Table t = con.getTable(tn, pool);
                switch (mt) {
                    case PUT:
                        List <Put> puts = new ArrayList <>();
                        for (Entry <String, List <String[]>> entry : params.entrySet()) {
                            Put put = new Put(Bytes.toBytes(entry.getKey()));
                            for (String[] ps : entry.getValue()) {
                                if (ps.length == 3) {
                                    put.addColumn(Bytes.toBytes(ps[0]), Bytes.toBytes(ps[1]),
                                            Bytes.toBytes(ps[2]));
                                } else {
                                    throw new RuntimeException("参数必须为三个！");
                                }
                            }
                            puts.add(put);
                        }
                        t.put(puts);
                        break;
                    case DELETE:
                        List <Delete> dels = new ArrayList <>();
                        for (Entry <String, List <String[]>> entry : params.entrySet()) {
                            Delete del = new Delete(Bytes.toBytes(entry.getKey()));
                            if (entry.getValue() != null) {
                                for (String[] ps : entry.getValue()) {
                                    if (ps != null && ps.length != 0) {
                                        switch (ps.length) {
                                            case 1:
                                                del.addFamily(Bytes.toBytes(ps[0]));
                                                break;
                                            case 2:
                                                del.addColumn(Bytes.toBytes(ps[0]),
                                                        Bytes.toBytes(ps[1]));
                                                break;
                                            default:
                                                throw new RuntimeException("最多两个参数");
                                        }
                                    }
                                }
                            }
                            dels.add(del);
                        }
                        t.delete(dels);
                        break;
                    default:
                        throw new RuntimeException("只能进行增删改操作！");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }


    /**
     * 查找一条信息
     */
    public static String get(String tableName, String rowkey, String columnFamily,
            String qualifiter) {
        getAdmin();
        try {
            Table t = con.getTable(TableName.valueOf(tableName));
            Get get = new Get(Bytes.toBytes(rowkey));
            get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifiter));
            Result result = t.get(get);
            List <Cell> cells = result.listCells();

            return Bytes.toString(CellUtil.cloneValue(cells.get(0)));

        } catch (IOException e) {
            throw new RuntimeException("获得表对象失败！！！", e);
        }
    }


    public static Map <String, String> get(String tableName, String rowkey, String columnFamily,
            String... qualifiters) {
        getAdmin();
        try {
            Table t = con.getTable(TableName.valueOf(tableName));
            Get get = new Get(Bytes.toBytes(rowkey));
            if (qualifiters != null && qualifiters.length != 0) {
                for (String qualifiter : qualifiters) {
                    get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifiter));
                }
            } else if (columnFamily != null) {
                get.addFamily(Bytes.toBytes(columnFamily));
            }

            Result result = t.get(get);
            List <Cell> cells = result.listCells();
            Map <String, String> results = null;
            if (cells != null && cells.size() != 0) {
                results = new HashMap <>();
                for (Cell cell : cells) {
                    results.put(Bytes.toString(CellUtil.cloneQualifier(cell)),
                            Bytes.toString(CellUtil.cloneValue(cell)));
                }
            }
            return results;
        } catch (IOException e) {
            throw new RuntimeException("获得表对象失败！！！", e);
        }

    }

    /**
     * 查询操作
     *
     * @param tableName 表名
     * @param rowkey 行键
     * @param columnFamily 列族名
     */
    public static <T> T get(String tableName, String rowkey, String columnFamily, Class <T> clazz) {
        getAdmin();
        try {
            Table t = con.getTable(TableName.valueOf(tableName));
            Get get = new Get(Bytes.toBytes(rowkey));
            Field[] fs = clazz.getDeclaredFields();

            for (Field f : fs) {
                get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(f.getName()));
            }
            Result result = t.get(get);
            List <Cell> cells = result.listCells();
            T tObj = clazz.newInstance();
            if (cells != null && cells.size() != 0) {
                for (Cell cell : cells) {
                    for (int i = 0; i < fs.length; i++) {
                        String valueStr = Bytes.toString(CellUtil.cloneQualifier(cell));
                        if (Bytes.toString(CellUtil.cloneQualifier(cell)).intern() == fs[i]
                                .getName().intern()) {
                            Object value = null;
                            if (fs[i].getType().getName() == "int"
                                    || fs[i].getType().getName().intern() == "java.lang.Integer") {
                                value = Integer.parseInt(valueStr);
                            } else if (fs[i].getType().getName() == "double"
                                    || fs[i].getType().getName().intern() == "java.lang.Double") {
                                value = Double.parseDouble(valueStr);
                            }
                            fs[i].setAccessible(true);
                            fs[i].set(tObj, value);
                        }
                    }
                }
            }
            return tObj;
        } catch (IOException e) {
            throw new RuntimeException("获得表对象失败！！！", e);
        } catch (Exception e) {
            throw new RuntimeException("创建对象失败！！！", e);
        }

    }

    /**
     * 扫描表查询
     *
     * @param tableName 表名
     * @param params 参数
     * @return Map<String                                                                                                                               ,
                       *       Map
                       *
                       *       <
                       *                                                                                                                                               String       ,                                                                                                                               String>>
     * String:rowkey; Map<String,String> 列族+单元格
     */
    public static Map <String, Map <String, String>> scan(String tableName, String[]... params) {
        getAdmin();
        try {
            Table t = con.getTable(TableName.valueOf(tableName));
            Scan scan = new Scan();

            if (params != null && params.length != 0) {
                for (String[] param : params) {
                    if (param != null && param.length != 0) {
                        switch (param.length) {
                            case 1:
                                scan.addFamily(Bytes.toBytes(param[0]));
                                break;
                            case 2:
                                scan.addColumn(Bytes.toBytes(param[0]), Bytes.toBytes(param[1]));
                                break;
                            default:
                                throw new RuntimeException("参数只能是1个或2个！！！");
                        }
                    }
                }
            }

            ResultScanner rs = t.getScanner(scan);  //多行记录

            Map <String, Map <String, String>> info = new HashMap <>();
            for (Result r : rs) {
                List <Cell> cells = r.listCells();
                Map <String, String> results = null;
                if (cells != null && cells.size() != 0) {
                    results = new HashMap <>();
                    for (Cell cell : cells) {
                        results.put(Bytes.toString(CellUtil.cloneFamily(cell)) + ":" + Bytes
                                        .toString(CellUtil.cloneQualifier(cell)),
                                Bytes.toString(CellUtil.cloneValue(cell)));
                    }
                }
                info.put(Bytes.toString(r.getRow()), results);
            }

            return info;
        } catch (IOException e) {
            throw new RuntimeException("获得表对象失败！！！", e);
        } catch (Exception e) {
            throw new RuntimeException("创建对象失败！！！", e);
        }

    }

}