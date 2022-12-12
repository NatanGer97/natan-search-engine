# Search engine

This project is a mini implementation of a search engine, which uses crawler to crawl over given websites
and crawls over the links found in the crawled pages.
The crawler is implemented using BFS algorithm.
For each page crawled, the crawler extracts the page content, store it (index it) on elasticsearch and
then crawl over the other links found in the page.

## Technologies and tools
* **Backend** - Java, Spring Boot
* **Database** - 
  * **Redis** - for storing the visited links and other information related to the bfs crawler (distance, etc)
  * **ElasticSearch** - for storing (**indexing**) the crawled pages content
    
* Message broker:
    * **Kafka** - for bfs queue
    * **AWS SQS** - as alternative to kafka
    
* Docker
  For running kafka and redis containers
  

