## Design Question

Design A Google Analytics like Backend System. We need to provide Google Analytic like services to our customers. Please provide a high level solution design for the backend system. Feel free to choose any open source tools as you want.

### Requirements

1. Handle large write volume: Billions of write events per day.
2. Handle large read/query volume: Millions of merchants wish to gain insight into their business. Read/Query patterns are time-series related metrics.
3. Provide metrics to customers with at most one hour delay.
4. Run with minimum downtime.
5. Have the ability to reprocess historical data in case of bugs in the processing logic.



### Background

The website owners install the Google Analytics plugins. Then the Google Analytics collects parameters like cookie, merchantId, URL, referrer per user click. The cookie is used to identify anonomous users without creating accounts. The URL field is the link that a user click. The referrer is the previous page the user visited. The collected data of each website are aggregated in batch, and are visualized on dashboards. 

## Requirement Analysis

1. Given billions of write events per day, the average traffic is 10,000 requests per second (10,000 qps) (The 99 percentile (p99) traffic should be larger, 1000,000 for example), which is far beyond the capacity of a single host. Assume that the bottle neck is disk IO, and it takes about 0.1 ms to append to disk, the write capacity is 10,000 qps. We might want to use about one hundred hosts. 
2. There are millions of merchants. What merchants care about are not the raw data, but the aggregated time-series metrics. We choose proper aggregation algorithms of good accuracy and performance. The data of different merchants should be isolated. 
3. The the metrics should be updated within one hour. This means that the raw data are not required to be proccessed in real-time. We can process the raw data in every hour. And write the aggregated data to the data warehouses. 
4. There is no downtime in my design. There is an independent thread for the data process. But I have to implement high-availability design, and to build monitoring system for the stability of the back-end service. 
5. During the data processing. Whenever there is a failure, the process should be rerun given the persisted log files. The processing failures should be monitored, and to be fixed. The raw data should be retened for a certain period of time (e.g. one month). 

### High Level Design

Given the above analysis, we design three micro-services for the back-end service. The write service, the data processing pipeline. and the read service. 

![analytics](https://user-images.githubusercontent.com/11760687/109987352-82753e00-7d41-11eb-913b-c7c2d4053afe.png)


- In the write service, for each http request, the client calls the write API, and pass over a set of parameters. It's up to our analytics requirements what arguments are of interests. In this prototype, we only collect cookie, URL and referrer. The write requests are sharded according to the URL (which distinguishes the merchants). The new data items are appended to log files sharded by merchantId and hours in the HDFS. 
- In the data processing pipeline, there is a hourly job that scans the logs in the HDFS, processes, and writes to the data warehouse. The pipeline calculates the statistical metrics, like sum, average, min and max, of the data in the last hour. The statistical metrics will be saved in the data warehouse. 
- In the read service. The dashboards on the UI displays the statistical metrics in time-series. The back-end should accept queries of different time intervals and time ranges of a dashboard. We can use ElasticSearch as the data warehouse, and use the open-source Kibana for visualization. But the discussion of Kibana is out of the scope of the back-end system design.

### Data Model Design

In the write service, the log files should be named as

```
<hostname>-<hour>.log
```

Whenever there is a request, we append the item to the log file. 

- Each item contains the merchantId, cookie, URL, referrer, timestamp etc. The timestamp is generated during writing to the log files. 

- I chose the disk logs in the design, because the append-only files has a better writing performance than database. 
- I didn't create millions of files for all merchants, but save the logs of all merchants in the range in one append-only log file. Because the it is faster in writing. Rather, random access files make the disk fraction. 
- The log file is rotated in every one hour. 
- Assume that each item is about 200 characters (400 bytes). Given one billion requests in a day, one hundred hosts in the cluster, one log files is 400 * 1,000,000,000 / 1000 / 24 = 1.7 MB in average. 
- We put a load balancer before the servers. We use the consistent hashing algorithm to evenly distribute logs of different merchants to the hosts in the cluster. In the optimal situation, the consistent hashing algorithm always distribute the requests of a merchant to the same host. This reduces the complexity at the data processing stage. 
- Local disk is preferred to shared storage like the HDFS and the AWS S3, because it is faster to write to local disk, than to transfer over the network. 
- We have to decide a proper data retention period. For example, we keep the log files for up to one week. This avoids the size of the data grows to infinite. 



In the data processing stage, we calculate the statistical metrics. For example, we can count the number of clicks on a certain URL at a minute using the following process

```sql
SELECT URL, COUNT(URL) FROM Host1-2020010100 GROUP BY merchantId WHERE timestamp > 1614769451 and timestamp < 1614769511
```

The actual implementation of the above process can be different from SQL. For example, we write Java code to process log files. 

Then we write the aggregated data to data warehouses. For example, we create the time-series tables:

```sql
CREATE TABLE CountMin (
    minute timestamp, 
    merchantId int,
    dimension varchar(255),
    count int
);
```

- There are two analysis we want to process: aggregation analysis and correlation analysis. 

- In aggregation analysis, we treat each request independently. We calculate the count, max, min, p50 etc of all users that visit a certain URL in a time window. This can be done using sampliing algorithm or the map-reduce algorithm. 

- In correlation analysis, we put multiple queries of a user together. For example, we can reconstruct a chain or a tree graph of URLs a user navigated, as to analyze user behaviours. Then we need graph algorithms for the chain/tree reconstraction in the data processing pipeline. For example, to count the number of users that visited both "urlA" and "urlB" in an hour, we use the self-join algorithm

  ```sql
  SELECT COUNT(userid) FROM Host1-2020010100 T1, Host1-2020010100 T2 WHERE T1.userid=T2.userid and T1.url='urlA' and T2.url='urlB'
  ```

  and write the process results to the data warehouse. 

- The delay is not acceptable to make the correlation analysis at the visualization step. But it's also costly to calculate all  correlation permutations. It's up to the business owners to define the correlation analysis dimensions in advance. 



Lastly the read service queries the structured tables in the data warehouse.

- For example, to display the visit to a certain URL of a certain merchant in every minute, we run

```sql
SELECT count FROM CountMin where merchantId={} and dimention={} and timestamp > 1614769451 and timestamp < 1614769511 ORDER BY minute;
```

- We should create tables of different time intervals like "CountHour", "CountDay", and of different statistical metrics like "maxMin", "maxHour" and "maxDay". In this design, we can display dashboards of different time intervals. 
- We have to decide a proper data retention period. For example, we keep the data for up to one year in the data warehouse. This avoids the size of the data grows to infinite. 



### Design Details of Special Attention

- We should encrypt the cookie in the log files, as to comply with the data privacy policies. 
- We should monitor the CPU usage, as to scale out the number of hosts automatically when the write/read requests increases. 
- Since we only write aggregated data to the data warehouse in batch asynchronously. The write pressure to the data warehouse is low. 
