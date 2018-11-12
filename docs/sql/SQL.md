# Apache Flink SQL 概览
作者: 孙金城，淘宝花名 金竹

本篇核心目标是让大家概要了解一个完整的Apache Flink SQL Job的组成部分，以及Apache Flink SQL所提供的核心算子的语义，最后会应用Tumble Window编写一个End-to-End的页面访问的统计示例。

# Apache Flink SQL Job的组成
我们做任何数据计算都离不开读取原始数据，计算逻辑和写入计算结果数据三部分，当然基于Apache Flink SQL编写的计算Job也离不开这个三部分，如下所所示：

![](../images/sqljob.png)

如上所示，一个完整的Apache Flink SQL Job 由如下三部分：
* Source Operator - Soruce operator是对外部数据源的抽象, 目前Apache Flink内置了很多常用的数据源实现，比如上图提到的Kafka。

* Query Operators - 查询算子主要完成如图的Query Logic，目前支持了Union，Join，Projection,Difference, Intersection以及window等大多数传统数据库支持的操作。

* Sink Operator - Sink operator 是对外结果表的抽象，目前Apache Flink也内置了很多常用的结果表的抽象，比如上图提到的Kafka。

# Apache Flink SQL 核心算子
SQL是Structured Quevy Language的缩写，最初是由美国计算机科学家[Donald D. Chamberlin](https://en.wikipedia.org/wiki/Donald_D._Chamberlin#/media/File:Don_Chamberlin.jpg)和Raymond F. Boyce在20世纪70年代早期从 [Early History of SQL](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=6359709) 中了解关系模型后在IBM开发的。该版本最初称为[SEQUEL: A Structured English Query Language]（结构化英语查询语言），旨在操纵和检索存储在IBM原始准关系数据库管理系统System R中的数据。SEQUEL后来改为SQL，因为“SEQUEL”是英国Hawker Siddeley飞机公司的商标。我们看一款用于特技飞行的英国皇家空军豪客Siddeley Hawk T.1A (Looks great):

![](../images/Siddeley.png)

在20世纪70年代后期，Oracle公司(当时叫 Relational Software，Inc.)开发了基于SQL的RDBMS，并希望将其出售给美国海军，Central Intelligence代理商和其他美国政府机构。 1979年6月，Oracle 公司为VAX计算机推出了第一个商业化的SQL实现，即Oracle V2。直到1986年，ANSI和ISO标准组正式采用了标准的"数据库语言SQL"语言定义。Apache Flink SQL 核心算子的语义设计也参考了[1992](http://www.contrib.andrew.cmu.edu/~shadow/sql/sql1992.txt) 、[2011](http://cs.ulb.ac.be/public/_media/teaching/infoh415/tempfeaturessql2011.pdf)等ANSI-SQL标准。接下来我们将简单为大家介绍Apache Flink SQL 每一个算子的语义。

## SELECT
SELECT 用于从数据集/流中选择数据，语法遵循ANSI-SQL标准，语义是关系代数中的投影(Projection),对关系进行垂直分割，消去某些列, 如下图所示:

![](../images/select.png)

对应的SQL语句如下：
```
SELECT ColA, ColC FROME tab ;
```
## WHERE
WHERE 用于从数据集/流中过滤数据，与SELECT一起使用，语法遵循ANSI-SQL标准，语义是关系代数的Selection，根据某些条件对关系做水平分割，即选择符合条件的记录，如下所示：

![](../images/where.png)

对应的SQL语句如下：
```
SELECT * FROM tab WHERE ColA <> 'a2' ;
```
## GROUP BY
GROUP BY 是对数据进行分组的操作，比如我需要分别计算一下一个学生表里面女生和男生的人数分别是多少，如下：

![](../images/groupby.png)

对应的SQL语句如下：
```
SELECT sex, COUNT(name) AS count FROM tab GROUP BY sex ;
```
## UNION ALL
UNION ALL 将两个表合并起来，要求两个表的字段完全一致，包括字段类型、字段顺序,语义对应关系代数的Union，只是关系代数是Set集合操作，会有去重复操作，UNION ALL 不进行去重，如下所示：

![](../images/unionall.png)

对应的SQL语句如下：
```
SELECT * FROM T1 UNION ALL SELECT * FROM T2
```

## UNION

UNION 将两个流给合并起来，要求两个流的字段完全一致，包括字段类型、字段顺序，并其UNION 不同于UNION ALL，UNION会对结果数据去重,与关系代数的Union语义一致，如下：

![](../images/union.png)

对应的SQL语句如下：
```
SELECT * FROM T1 UNION SELECT * FROM T2
```
## JOIN 
JOIN 用于把来自两个表的行联合起来形成一个宽表，Apache Flink支持的JOIN类型：
* JOIN - INNER JOIN
* LEFT JOIN - LEFT OUTER JOIN
* RIGHT JOIN - RIGHT OUTER JOIN  
* FULL JOIN - FULL OUTER JOIN

JOIN与关系代数的Join语义相同，具体如下：

![](../images/innerjoin.png)

对应的SQL语句如下(INNER JOIN)：
```
SELECT ColA, ColB, T2.ColC, ColE FROM TI JOIN T2 ON T1.ColC = T2.ColC ; 
```

`LEFT JOIN`与`INNER JOIN`的区别是当右表没有与左边相JOIN的数据时候，右边对应的字段补`NULL`输出，如下：

![](../images/leftjoin.png)

对应的SQL语句如下(LEFT JOIN)：
```
SELECT ColA, ColB, T2.ColC, ColE FROM TI LEFT JOIN T2 ON T1.ColC = T2.ColC ; 
```

**说明：** 

* 细心的读者可能发现上面T2.ColC是添加了前缀T2了，这里需要说明一下，当两张表有字段名字一样的时候，我需要指定是从那个表里面投影的。

* `RIGHT JOIN` 相当于 `LEFT JOIN` 左右两个表交互一下位置。`FULL JOIN`相当于 `RIGHT JOIN` 和 `LEFT JOIN` 之后进行`UNION ALL`操作。

## Window
在Apache Flink中有2种类型的Window，一种是OverWindow，即传统数据库的标准开窗，每一个元素都对应一个窗口。一种是GroupWindow，目前在SQL中GroupWindow都是基于时间进行窗口划分的。

### OverWindow
OVER Window 目前支持由如下三个元素组合的8中类型：
* 时间 - Processing Time 和 EventTime
* 数据集 - Bounded 和 UnBounded
* 划分方式 - ROWS 和 RANGE
我们以的Bounded ROWS 和 Bounded RANGE 两种常用类型，想大家介绍Over Window的语义

#### Bounded ROWS Over Window
Bounded ROWS OVER Window 每一行元素都视为新的计算行，即，每一行都是一个新的窗口。
##### 语法
```
SELECT 
    agg1(col1) OVER(
     [PARTITION BY (value_expression1,..., value_expressionN)] 
     ORDER BY timeCol
     ROWS 
     BETWEEN (UNBOUNDED | rowCount) PRECEDING AND CURRENT ROW) AS colName, 
... 
FROM Tab1
```
* value_expression - 进行分区的字表达式；
* timeCol - 用于元素排序的时间字段；
* rowCount - 是定义根据当前行开始向前追溯几行元素；


##### 语义
我们以3个元素(2 PRECEDING)的窗口为例，如下图:

![](../images/rowsover.png)

上图所示窗口 user 1 的 w5和w6， user 2的 窗口 w2 和 w3，虽然有元素都是同一时刻到达，但是他们仍然是在不同的窗口，这一点有别于RANGE OVER Window.

#### Bounded RANGE Over Window

Bounded RANGE OVER Window 具有相同时间值的所有元素行视为同一计算行，即，具有相同时间值的所有行都是同一个窗口；
##### 语法
Bounded RANGE OVER Window的语法如下：
```
SELECT 
    agg1(col1) OVER(
     [PARTITION BY (value_expression1,..., value_expressionN)] 
     ORDER BY timeCol
     RANGE 
     BETWEEN (UNBOUNDED | timeInterval) PRECEDING AND CURRENT ROW) AS colName, 
... 
FROM Tab1
```
* value_expression - 进行分区的字表达式；
* timeCol - 用于元素排序的时间字段；
* timeInterval - 是定义根据当前行开始向前追溯指定时间的元素行；
##### 语义
我们以3秒中数据(INTERVAL '2' SECOND)的窗口为例，如下图：

![](../images/rangeover.png)

注意: 上图所示窗口 user 1 的 w6， user 2的 窗口 w3，元素都是同一时刻到达,他们是在同一个窗口，这一点有别于ROWS OVER Window.

### GroupWindow
根据窗口数据划分的不同，目前Apache Flink有如下3种Bounded Winodw:

* Tumble - 滚动窗口，窗口数据有固定的大小，窗口数据无叠加；
* Hop - 滑动窗口，窗口数据有固定大小，并且有固定的窗口重建频率，窗口数据有叠加；
* Session - 会话窗口，窗口数据没有固定的大小，根据窗口数据活跃程度划分窗口，窗口数据无叠加；

**说明：** Aapche Flink 还支持UnBounded的 Group Window，也就是全局Window，流上所有数据都在一个窗口里面，语义非常简单，这里不做详细介绍了。

GroupWindow的语法如下：
```
SELECT 
    [gk], 
    agg1(col1),
     ... 
    aggN(colN)
FROM Tab1
GROUP BY [WINDOW(definition)], [gk]
```
* [WINDOW(definition)] - 在具体窗口语义介绍中介绍。

#### Tumble Window
Tumble 滚动窗口有固定size，窗口数据不重叠,具体语义如下：

![](../images/tumble.png)

假设我们要写一个2分钟大小的Tumble，示例SQL如下：
```
SELECT gk, COUNT(*) AS pv 
  FROM tab 
    GROUP BY TUMBLE(rowtime, INTERVAL '2' MINUTE), gk
```

#### Hop Window

Hop 滑动窗口和滚动窗口类似，窗口有固定的size，与滚动窗口不同的是滑动窗口可以通过slide参数控制滑动窗口的新建频率。因此当slide值小于窗口size的值的时候多个滑动窗口会重叠，具体语义如下：

![](../images/hop.png)

假设我们要写一个每5分钟统计近10分钟的页面访问量(PV).
```
SELECT gk, COUNT(*) AS pv 
  FROM tab 
    GROUP BY HOP(rowtime, INTERVAL '5' MINUTE, INTERVAL '10' MINUTE), gk
```

#### Session Window

Session 会话窗口 是没有固定大小的窗口，通过session的活跃度分组元素。不同于滚动窗口和滑动窗口，会话窗口不重叠,也没有固定的起止时间。一个会话窗口在一段时间内没有接收到元素时，即当出现非活跃间隙时关闭。一个会话窗口 分配器通过配置session gap来指定非活跃周期的时长,具体语义如下：

![](../images/session.png)

假设我们要写一个统计连续的两个访问用户之间的访问时间间隔不超过3分钟的的页面访问量(PV).
```
SELECT gk, COUNT(*) AS pv  
  FROM pageAccessSession_tab
    GROUP BY SESSION(rowtime, INTERVAL '3' MINUTE), gk
```
**说明:** 很多场景用户需要获得Window的开始和结束时间，上面的GroupWindow的SQL示例中没有体现，那么窗口的开始和结束时间应该怎样获取呢? Apache Flink 我们提供了如下辅助函数：
* TUMBLE_START/TUMBLE_END
* HOP_START/HOP_END
* SESSION_START/SESSION_END

这些辅助函数如何使用，请参考如下完整示例的使用方式。

# 完整的 SQL Job 案例
上面我们介绍了Apache Flink SQL核心算子的语法及语义，这部分将选取Bounded EventTime Tumble Window为例为大家编写一个完整的包括Source和Sink定义的Apache Flink SQL Job。假设有一张淘宝页面访问表(PageAccess_tab)，有地域，用户ID和访问时间。我们需要按不同地域统计每2分钟的淘宝首页的访问量(PV). 具体数据如下：

| region | userId | accessTime |
| --- | --- | --- |
| ShangHai | U0010 | 2017-11-11 10:01:00 |
| BeiJing |  U1001| 2017-11-11 10:01:00 |
| BeiJing | U2032 | 2017-11-11 10:10:00 |
| BeiJing  | U1100 | 2017-11-11 10:11:00 |
| ShangHai | U0011 | 2017-11-11 12:10:00 |

## Source 定义
自定义Apache Flink Stream Source需要实现`StreamTableSource`, `StreamTableSource`中通过`StreamExecutionEnvironment` 的`addSource`方法获取`DataStream`, 
所以我们需要自定义一个 `SourceFunction`, 并且要支持产生WaterMark，也就是要实现`DefinedRowtimeAttributes`接口。出于代码篇幅问题，我们如下只介绍核心部分，完整代码
请查看: [EventTimeTumbleWindowDemo.scala](https://github.com/sunjincheng121/flink-study/blob/test/src/main/scala/org/apache/flink/study/sql/EventTimeTumbleWindowDemo.scala)

### Source Function定义
支持接收携带EventTime的数据集合，Either的数据结构Right是WaterMark，Left是元数据:
```
class MySourceFunction[T](dataList: Seq[Either[(Long, T), Long]]) extends SourceFunction[T] {
  override def run(ctx: SourceContext[T]): Unit = {
    dataList.foreach {
      case Left(t) => ctx.collectWithTimestamp(t._2, t._1) 
      case Right(w) => ctx.emitWatermark(new Watermark(w)) // emit watermark
    }
  }
}
```

### 定义 StreamTableSource

我们自定义的Source要携带我们测试的数据，以及对应的WaterMark数据，具体如下:
```
class MyTableSource extends StreamTableSource[Row] with DefinedRowtimeAttributes {

  // 页面访问表数据 rows with timestamps and watermarks
  val data = Seq(
     // Data
     Left(1510365660000L, Row.of(new JLong(1510365660000L), "ShangHai", "U0010")),
     // Watermark
     Right(1510365660000L),
    ..
  )
  
  val fieldNames = Array("accessTime", "region", "userId")
  val schema = new TableSchema(fieldNames, Array(Types.SQL_TIMESTAMP, Types.STRING, Types.STRING))
  val rowType = new RowTypeInfo(
    Array(Types.LONG, Types.STRING, Types.STRING).asInstanceOf[Array[TypeInformation[_]]],
    fieldNames)
  
  override def getDataStream(execEnv: StreamExecutionEnvironment): DataStream[Row] = {
    // 添加数据源实现
    execEnv.addSource(new MySourceFunction[Row](data)).setParallelism(1).returns(rowType)
  }
  ...
}
  ```
  
## Sink 定义
我们简单的将计算结果写入到Apache Flink内置支持的CSVSink中，定义Sink如下：
```
def getCsvTableSink: TableSink[Row] = {
    val tempFile = ...
    new CsvTableSink(tempFile.getAbsolutePath).configure(
      Array[String]("region", "winStart", "winEnd", "pv"),
      Array[TypeInformation[_]](Types.STRING, Types.SQL_TIMESTAMP, Types.SQL_TIMESTAMP, Types.LONG))
  }
```

## 构建主程序
主程序包括执行环境的定义，Source/Sink的注册以及统计查SQL的执行，具体如下：
```
def main(args: Array[String]): Unit = {
    // Streaming 环境
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env)

    // 设置EventTime
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    //方便我们查出输出数据
    env.setParallelism(1)

    val sourceTableName = "mySource"
    // 创建自定义source数据结构
    val tableSource = new MyTableSource

    val sinkTableName = "csvSink"
    // 创建CSV sink 数据结构
    val tableSink = getCsvTableSink

    // 注册source
    tEnv.registerTableSource(sourceTableName, tableSource)
    // 注册sink
    tEnv.registerTableSink(sinkTableName, tableSink)

    val sql =
      "SELECT  " +
      "  region, " +
      "  TUMBLE_START(accessTime, INTERVAL '2' MINUTE) AS winStart," +
      "  TUMBLE_END(accessTime, INTERVAL '2' MINUTE) AS winEnd, COUNT(region) AS pv " +
      " FROM mySource " +
      " GROUP BY TUMBLE(accessTime, INTERVAL '2' MINUTE), region"

    tEnv.sqlQuery(sql).insertInto(sinkTableName);
    env.execute()
  }
```

## 执行并查看运行结果
执行主程序后我们会在控制台得到Sink的文件路径，如下：
```
Sink path : /var/folders/88/8n406qmx2z73qvrzc_rbtv_r0000gn/T/csv_sink_8025014910735142911tem
```
Cat 方式查看计算结果，如下：
```
jinchengsunjcdeMacBook-Pro:FlinkTableApiDemo jincheng.sunjc$ cat /var/folders/88/8n406qmx2z73qvrzc_rbtv_r0000gn/T/csv_sink_8025014910735142911tem
ShangHai,2017-11-11 02:00:00.0,2017-11-11 02:02:00.0,1
BeiJing,2017-11-11 02:00:00.0,2017-11-11 02:02:00.0,1
BeiJing,2017-11-11 02:10:00.0,2017-11-11 02:12:00.0,2
ShangHai,2017-11-11 04:10:00.0,2017-11-11 04:12:00.0,1
```

# 小结
本篇概要的介绍了Apache Flink SQL 的所有核心算子，并以一个End-to-End的示例展示了如何编写Apache Flink SQL的Job. 希望对大家有所帮助，更多Apache Flink SQL的介绍欢迎大家访问作者的如下专栏或专辑：
* 51CTO 金竹 专栏 - http://zhuanlan.51cto.com/columnlist/jinzhu
* 云栖社区 金竹 专辑 - https://yq.aliyun.com/album/206 