package eu.dnetlib.iis.common.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

import java.util.Objects;
import java.util.function.Function;

/**
 * SparkSession utility methods.
 */
public class SparkSessionSupport {
    private SparkSessionSupport() {
    }

    @FunctionalInterface
    public interface Job {
        void accept(SparkSession spark) throws Exception;
    }

    /**
     * Runs a given job using SparkSession created using default builder and supplied SparkConf. Stops SparkSession
     * when SparkSession is managed. Allows to reuse SparkSession created externally.
     *
     * @param conf                  SparkConf instance
     * @param isSparkSessionManaged When true will stop SparkSession
     * @param job                   Job using constructed SparkSession
     */
    public static void runWithSparkSession(SparkConf conf,
                                           Boolean isSparkSessionManaged,
                                           Job job) {
        runWithSparkSession(
                SparkSessionFactory::withConfAndKryo,
                conf,
                isSparkSessionManaged,
                job);
    }

    /**
     * Runs a given job using SparkSession created using supplied builder and supplied SparkConf. Stops SparkSession
     * when SparkSession is managed. Allows to reuse SparkSession created externally.
     *
     * @param conf                  SparkConf instance
     * @param isSparkSessionManaged When true will stop SparkSession
     * @param job                   Job using constructed SparkSession
     */
    public static void runWithSparkSession(Function<SparkConf, SparkSession> sparkSessionBuilder,
                                           SparkConf conf,
                                           Boolean isSparkSessionManaged,
                                           Job job) {
        runWithSparkSession(
                sparkSessionBuilder.apply(conf),
                isSparkSessionManaged,
                job);
    }

    private static void runWithSparkSession(SparkSession spark,
                                            Boolean isSparkSessionManaged,
                                            Job job) {
        try {
            job.accept(spark);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (Objects.nonNull(spark) && isSparkSessionManaged) {
                spark.stop();
            }
        }
    }
}