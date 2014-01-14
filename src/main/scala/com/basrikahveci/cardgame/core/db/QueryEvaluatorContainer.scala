package com.basrikahveci
package cardgame.core.db

import com.twitter.querulous.query.SqlQueryFactory
import com.twitter.querulous.database.ApachePoolingDatabaseFactory
import com.twitter.util.Duration
import java.util.concurrent.TimeUnit
import com.twitter.querulous.evaluator.{QueryEvaluator, StandardQueryEvaluatorFactory}

object QueryEvaluatorContainer {

  var queryEvaluator: QueryEvaluator = null

  def init(jdbcUrl: String, dbName: String, user: String, password: String) = {
    val queryFactory = new SqlQueryFactory
    val apachePoolingDatabaseFactory = new ApachePoolingDatabaseFactory(1, 5, Duration.fromTimeUnit(1000, TimeUnit.MILLISECONDS), Duration.fromTimeUnit(1000, TimeUnit.MILLISECONDS), true, Duration.fromTimeUnit(1000, TimeUnit.MILLISECONDS))
    val queryEvaluatorFactory = new StandardQueryEvaluatorFactory(apachePoolingDatabaseFactory, queryFactory)

    queryEvaluator = queryEvaluatorFactory(jdbcUrl, dbName, user, password, Map[String, String](), "jdbc:mysql")
  }
}
