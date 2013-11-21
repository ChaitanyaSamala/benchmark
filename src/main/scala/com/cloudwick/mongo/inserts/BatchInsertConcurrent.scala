package com.cloudwick.mongo.inserts

import com.cloudwick.mongo.OptionsConfig
import com.cloudwick.mongo.dao.LogDAO
import org.slf4j.LoggerFactory
import java.util.concurrent.{Executors, ExecutorService}
import java.util.concurrent.atomic.AtomicLong

/**
 * Initializes thread pool and hands off the insertion to pool of threads
 * @param events total number of documents to insert into mongo
 * @param config scopt options
 * @param mongo mongo log data access object
 * @author ashrith 
 */
class BatchInsertConcurrent(events: Long, config: OptionsConfig, mongo: LogDAO) extends Runnable {
  lazy val logger = LoggerFactory.getLogger(getClass)
  val threadPool: ExecutorService = Executors.newFixedThreadPool(config.threadPoolSize)
  val finalCounter:AtomicLong = new AtomicLong(0L)
  val messagesPerThread: Int = (events / config.threadsCount).toInt
  val messagesRange = Range(0, events.toInt, messagesPerThread)

  def run() = {
    try {
      (1 to config.threadsCount).foreach { threadCount =>
        logger.info("Initializing thread")
        threadPool.execute(
          new BatchInsert(
            messagesRange(threadCount - 1), // start range of thread
            messagesRange(threadCount-1) + (messagesPerThread - 1), // end range for thread
            finalCounter,
            config,
            mongo))
      }
    } finally {
      threadPool.shutdown()
    }
    while(!threadPool.isTerminated) {}
    logger.info(s"Total documents processed by ${config.threadsCount} threads: " + finalCounter)
  }
}
